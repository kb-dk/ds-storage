package dk.kb.storage.facade;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;

import dk.kb.util.webservice.stream.ExportWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.kb.storage.config.ServiceConfig;
import dk.kb.storage.model.v1.DsRecordDto;
import dk.kb.storage.model.v1.OriginCountDto;
import dk.kb.storage.model.v1.OriginDto;
import dk.kb.storage.model.v1.UpdateStrategyDto;
import dk.kb.storage.storage.DsStorage;
import dk.kb.storage.util.IdNormaliser;
import dk.kb.util.webservice.exception.InternalServiceException;
import dk.kb.util.webservice.exception.InvalidArgumentServiceException;



public class DsStorageFacade {

    private static final Logger log = LoggerFactory.getLogger(DsStorageFacade.class);

    public static void createOrUpdateRecord(DsRecordDto record)  {
        performStorageAction("createOrUpdateRecord(" + record.getId() + ")", storage -> {
            validateOriginExists(record.getOrigin());
            validateIdHasOriginPrefix(record.getOrigin(), record.getId());
            
            String orgId = record.getId();
            if (record.getParentId() != null) { //Parent ID must belong to same collection and also validate
              validateIdHasOriginPrefix(record.getOrigin(), record.getParentId());
            }
            
            String idNorm = IdNormaliser.normaliseId(record.getId());
            if (!orgId.equals(idNorm)) {
                record.setOrgid(orgId); //set this before changing value below
                record.setId(idNorm);                
                record.setIdError(true);
                log.warn("ID was normalized from:"+orgId + " to "+ idNorm);
            }
            
            if (record.getParentId() != null) { //Also normalize parentID
                record.setParentId(IdNormaliser.normaliseId(record.getParentId()));                
            }            
            
            boolean recordExists = storage.recordExists(record.getId());
            if (recordExists) {
                log.info("Updating record with id:"+record.getId());
                storage.updateRecord(record);
            } else {               
                log.info("Creating new record with id:"+record.getId());
                storage.createNewRecord(record);
            }
            updateMTimeForParentChild(storage,record.getId());
            return null; // Something must be returned
        });
    }


    public static ArrayList<OriginCountDto> getOriginStatistics() {
        return performStorageAction("getOriginStatistics", DsStorage::getOriginStatictics);
    }


    public static void getRecordsModifiedAfter(
            ExportWriter writer, String origin, long mTime, long maxRecords, int batchSize) {
        String id = String.format(Locale.ROOT, "writeRecordsModifiedAfter(origin='%s', mTime=%d, maxRecords=%d, batchSize=%d)",
                                  origin, mTime, maxRecords, batchSize);
        long pending = maxRecords == -1 ? Long.MAX_VALUE : maxRecords; // -1 = all records
        final AtomicLong lastMTime = new AtomicLong(mTime);
        while (pending > 0) {
            int request = pending < batchSize ? (int) pending : batchSize;
            long delivered = performStorageAction(id, storage -> {
                ArrayList<DsRecordDto> records = storage.getRecordsModifiedAfter(origin, lastMTime.get(), request);
                writer.writeAll(records);
                if (!records.isEmpty()) {
                    lastMTime.set(records.get(records.size()-1).getmTime());
                }
                return (long)records.size();
            });
            if (delivered == 0) {
                break;
            }
            pending -= delivered;
        }
    }

    /*
     * Return null if record does not exist
     * 
     */
    public static DsRecordDto getRecord(String recordId) {
        return performStorageAction("getRecord(" + recordId + ")", storage -> {
        String idNorm = IdNormaliser.normaliseId(recordId);
           DsRecordDto record = storage.loadRecord(idNorm);
                      
           if (record== null) {
                return null;
            }
            
            if (record.getParentId() == null) { //can not have children if also has parent (1 level only hieracy)
                ArrayList<String> childrenIds = storage.getChildrenIds(record.getId());
                record.setChildrenIds(childrenIds);
            }
            return record;
        });
    }

    /**
     *   Will load full object tree. 
     *  It is assumed we only have 2 levels in the object tree
     * 
     *  Logic: Find top parent recursive and load children.'
     *  If the record asked for is one of the children, it will be added to the list so it is the same object as returned.
     * 
     *  @param recordId The full object tree will be return. Point will be to this recordId which can be parent or one of the children.  
     * 
     *  Return null if record does not exist.
     */
    public static DsRecordDto getRecordTree(String recordId) {
      
        
        return performStorageAction("getRecord(" + recordId + ")", storage -> {
        String idNorm = IdNormaliser.normaliseId(recordId);          
        DsRecordDto record = getRecord(idNorm); //Load from facade as this will set children
        
        if (record== null) {
            return record;
        }
        DsRecordDto topParent = getTopParent(record);
                          
        // now we just load all children.
        //Notice this code has to have another level of recurssion if we need to support depth =>2 trees.                
        List<String> childrenIds = topParent.getChildrenIds();   
        List<DsRecordDto> childrenRecords = new ArrayList<DsRecordDto>();
        topParent.setChildren(childrenRecords);
                
        for (String childId : childrenIds) {
            if (childId.equals(record.getId())) { //This is the child we already have loaded. Use it instead of loading a new object. Also point to the object is kept.
                childrenRecords.add(record);
                record.setParent(topParent);
            }
            else {
                childrenRecords.add(getRecord(childId));                
            }                        
        }
        
            return record;
        });
    }
    
    
    //will follow parent recursive until there is no parent. (root)
    //So far we only have 1 level of children, but this method is prepared if that changes.
    private static DsRecordDto getTopParent(DsRecordDto record) {
      DsRecordDto topParent = record;
      while (topParent.getParentId() != null) {          
          if (topParent.getParentId().equals(topParent.getId())) {
              log.error("Invalid record, has itself as parent, id:"+topParent.getId());
              return topParent; //No need to throw exception.
          }          
          topParent = getRecord(topParent.getParentId());                                 
          
      }
      return topParent;        
    }
    
    

    public static Integer markRecordForDelete(String recordId) {
        //TODO touch children etc.
        return performStorageAction("markRecordForDelete(" + recordId + ")", storage -> {
            String idNorm = IdNormaliser.normaliseId(recordId);            
            int updated = storage.markRecordForDelete(idNorm);
            updateMTimeForParentChild(storage,recordId);
            log.info("Record marked for delete:"+recordId);
            return updated;
        });
    }


    public static int deleteMarkedForDelete(String origin) {
        return performStorageAction("deleteMarkedForDelete(" + origin + ")", storage -> {
            validateOriginExists(origin);

            int numberDeleted =  storage.deleteMarkedForDelete(origin);
            log.info("Delete marked for delete records for origin:"+origin +" number deleted:"+numberDeleted);
            //We are not touching parent/children relation when deleting for real.
            return numberDeleted;
        });
    }

    /*
     * This is called when ever a record is modified (create/update/markfordelete). The recordId here
     * has already been assigned a new mTime. Update mTime for parent and/or children according to  update strategy for that origin.
     * 
     * This method will not commit/rollback as this is handled by the calling metod. 
     * 
     * See UpdateStrategyDto
     */
    private static void updateMTimeForParentChild(DsStorage storage,String recordId) throws Exception{
        DsRecordDto record=  storage.loadRecord(recordId); //Notice for performancing tuning, recordDto this can sometimes be given to the method. No premature optimization...
        if (record==null) { //Can happen when marking records for delete and record is not in storage.            
            return;            
        }
        OriginDto origin = ServiceConfig.getAllowedOrigins().get(record.getOrigin());       
        UpdateStrategyDto updateStrategy = origin.getUpdateStrategy();

        log.info("Updating parent/child relation for recordId:"+recordId +" with updateStrategy:"+updateStrategy);

        switch (updateStrategy) {
            case NONE:
                // Do nothing
                break;
            case CHILD:
                updateMTimeForChildren(storage, recordId);
                break;
            case PARENT:
                updateMTimeForParent(storage, record);
                break;
            case ALL:
                updateMTimeForAll(storage, record);
                break;
            default:
                throw new InvalidArgumentServiceException("Update strategy not implemented:"+updateStrategy);
        }
    }

    /**
     * Update mTime for all children of the Record with the given parentId.
     * @param storage ready for updates.
     * @param parentId the ID of the parent record.
     * @throws Exception if updating failed.
     */
    private static void updateMTimeForChildren(DsStorage storage, String parentId) throws Exception {
        //update all children one at a time
        ArrayList<String> childrenIds = storage.getChildrenIds(parentId);
        for (String childId : childrenIds) {
            storage.updateMTimeForRecord(childId);
        }
    }

    /**
     * Update mTime for the parent of the Record, if it has any.
     * @param storage ready for updates.
     * @param record the Record to update parent mTime for.
     * @throws Exception if updating failed.
     */
     private static void updateMTimeForParent(DsStorage storage, DsRecordDto record) throws Exception {
        //Notice for performancing tuning, recordDto this can sometimes be given to the method. No premature optimization...
        boolean hasParent = (record.getParentId() != null);
        if (!hasParent) {
            return;
        }
        storage.updateMTimeForRecord(record.getParentId());
    }

    /**
     * Update mTime for all children and the parent of the Record, if it has any.
     * @param storage ready for updates.
     * @param record the Record to update children and parent mTime for.
     * @throws Exception if updating failed.
     */
    private static void updateMTimeForAll(DsStorage storage, DsRecordDto record) throws Exception {
        boolean hasParent = (record.getParentId() != null);
        //Find parent and then update mTime, and update all children.
        //If parent or one of the children matches the record, it has already been updated with a new mTime,  so skip it.
        DsRecordDto topParent = null;
        if (!hasParent) {
            topParent= record;
        }
        else {
            topParent= storage.loadRecord(record.getParentId());   //can be null
        }

        //TopParent can be null if record does not exist
        if (topParent == null) {
            return;
        }

        String recordId = record.getId();
        if (!recordId.equals(topParent.getId())) {
            storage.updateMTimeForRecord(topParent.getId());
        }
        //And all children
        ArrayList<String> childrenIds = storage.getChildrenIds(recordId);
        for (String childId: childrenIds) {
            if (!recordId.equals(childId)) {
                storage.updateMTimeForRecord(childId);
            }
        }
    }

    /**
     * Check that the given origin is defined in the setup.
     * @param origin name.
     */
    private static void validateOriginExists(String origin) {
        if (ServiceConfig.getAllowedOrigins().get(origin) == null) {
            System.out.println("origins:"+ServiceConfig.getAllowedOrigins());
            
            throw new InvalidArgumentServiceException("Unknown record origin: "+origin);
        }
    }
   
    /**
     * Check that the recordId starts with the origin as prefix
     * @param origin name.
     */
    private static void validateIdHasOriginPrefix(String origin, String recordId) {
        if (!recordId.startsWith(origin)) {
            throw new InvalidArgumentServiceException("Id must have origin as prefix. Id:"+recordId);
        }
    }
    

    /**
     * Starts a storage transaction and performs the given action on it, returning the result from the action.
     *
     * If the action throws an exception, a {@link DsStorage#rollback()} is performed.
     * If the action passes without exceptions, a {@link DsStorage#commit()} is performed.
     * @param actionID a debug-oriented ID for the action, typically the name of the calling method.
     * @param action the action to perform on the storage.
     * @return return value from the action.
     * @throws InternalServiceException if anything goes wrong.
     */
    private static <T> T performStorageAction(String actionID, StorageAction<T> action) {
        try (DsStorage storage = new DsStorage()) {
            T result;
            try {
                result = action.process(storage);
            }
            catch(InvalidArgumentServiceException e) {
                log.warn("Exception performing action '{}'. Initiating rollback", actionID, e.getMessage());
                storage.rollback();
                throw new InvalidArgumentServiceException(e);                
            }            
            catch (Exception e) {
                log.warn("Exception performing action '{}'. Initiating rollback", actionID, e);
                storage.rollback();
                throw new InternalServiceException(e);
            }

            try {
                storage.commit();
            } catch (SQLException e) {
                log.error("Exception committing after action '{}'", actionID, e);
                throw new InternalServiceException(e);
            }

            return result;
        } catch (SQLException e) { //Connecting to storage failed
            log.error("SQLException performing action '{}'", actionID, e);
            throw new InternalServiceException(e);
        }
    }

    /**
     * Callback used with {@link #performStorageAction(String, StorageAction)}.
     * @param <T> the object returned from the {@link StorageAction#process(DsStorage)} method.
     */
    @FunctionalInterface
    private interface StorageAction<T> {
        /**
         * Access or modify the given storage inside of a transaction.
         * If the method throws an exception, it will be logged, a {@link DsStorage#rollback()} will be performed and
         * a wrapping {@link dk.kb.util.webservice.exception.ServiceException} will be thrown.
         * @param storage a storage ready for requests and updates.
         * @return custom return value.
         * @throws Exception if something went wrong.
         */
        T process(DsStorage storage) throws Exception;
    }

}
