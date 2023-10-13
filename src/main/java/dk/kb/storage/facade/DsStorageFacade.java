package dk.kb.storage.facade;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
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
import dk.kb.util.webservice.exception.NotFoundServiceException;



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
               throw new NotFoundServiceException("No record with id:"+recordId);
            }
            

           ArrayList<String> childrenIds = storage.getChildrenIds(record.getId());
           record.setChildrenIds(childrenIds);

            return record;
        });
    }

    /**
     *   Will load full object tree. The DsDecordDto return will a pointer the record with the recordId in the tree
     *       
     *  Logic: Find top parent recursive and load children.
     * 
     *  @param recordId The full object tree will be returned with a pointer to this record   
     * 
     *  Return null if record does not exist.
     */
    public static DsRecordDto getRecordTree(String recordId) {
      
        
        return performStorageAction("getRecord(" + recordId + ")", storage -> {
        String idNorm = IdNormaliser.normaliseId(recordId);          
        DsRecordDto record = getRecord(idNorm); //Load from facade as this will set children
        
        if (record== null) {
            throw new NotFoundServiceException("No record with id:"+recordId);             
        }
        
         DsRecordDto topParent = getTopParent(record); //this will also detect a cycle.              
         
         
         loadAndSetChildRelations(topParent,new HashSet<>(), record); //Resursive method     
                   
         
          
         return record;
         
        });
    }
    
    /**
     * Will recursive go up in the tree to find the top parent.
     * Throws an exception if a cycle is detected.
     *  
     * @param record
     * @throws InternalServiceException If a cycle is detected.
     * @return
     */
    private static DsRecordDto getTopParent(DsRecordDto record) throws InternalServiceException{
    
      HashSet<String> ids = new HashSet<String>();   
      DsRecordDto topParent = record;
      while (topParent.getParentId() != null) {          
      
          if (ids.contains(topParent.getId())) {
              log.error("Cycle detected for recordId:"+topParent.getId());
              throw new InternalServiceException("Cycle detected for recordId:"+topParent.getId());
              
          }          
          ids.add(topParent.getId());
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
     * This method will call itself recursively
     * The callstack length will only be equal to depth of tree, so not an issue.
     * Call this method with top-parent of the record tree to get the full tree.
     * 
     * @param parentRecord Top record in the object tree. The tree will only be loaded from this node and down.
     * @param previousIdsForCycleDetection Set to keep track of cycles. When calling this method supply it with a new empty HashSet
     * @param recordId This is the recordId that will be put into the returnObject   
     * @param returnObject This List will always has 1 element matching the recordId  
     */
    
    private static void loadAndSetChildRelations(DsRecordDto parentRecord, HashSet<String> previousIdsForCycleDetection, DsRecordDto origo) throws SQLException {
       
                
        List<String> childrenIds = parentRecord.getChildrenIds();                
        List<DsRecordDto> childrenRecords = new ArrayList<DsRecordDto>(); 
        for (String childId: childrenIds) {
                        
            //DsRecordDto child = getRecord(childId);          
            DsRecordDto child = childId.equals(origo.getId()) ? origo: getRecord(childId);
            child.setParent(parentRecord);
            childrenRecords.add(child);

            
            
            if(previousIdsForCycleDetection.contains(child.getId())){
                log.error("Parent-child cycle detected for id (stopped loading rest of hierarchy tree): {} ", child.getId());
                throw new InternalServiceException("Parent-child cycle detected for id:"+child.getId());
            }
            previousIdsForCycleDetection.add(child.getId());
            loadAndSetChildRelations(child, previousIdsForCycleDetection,origo); //This is the recursive call
        }             
       
        parentRecord.setChildren(childrenRecords);
        
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
