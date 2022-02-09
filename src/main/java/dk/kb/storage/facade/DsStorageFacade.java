package dk.kb.storage.facade;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.kb.storage.config.ServiceConfig;
import dk.kb.storage.model.v1.DsRecordDto;
import dk.kb.storage.model.v1.RecordBaseCountDto;
import dk.kb.storage.model.v1.RecordBaseDto;
import dk.kb.storage.model.v1.UpdateStrategyDto;
import dk.kb.storage.storage.DsStorage;
import dk.kb.storage.util.IdNormaliser;
import dk.kb.storage.webservice.exception.InternalServiceException;
import dk.kb.storage.webservice.exception.InvalidArgumentServiceException;



public class DsStorageFacade {

    private static final Logger log = LoggerFactory.getLogger(DsStorageFacade.class);

    public static void createOrUpdateRecord(DsRecordDto record)  {
        performStorageAction("createOrUpdateRecord(" + record.getId() + ")", storage -> {
            validateBaseExists(record.getBase());
            validateIdHasRecordBasePrefix(record.getBase(), record.getId());
            
            if (record.getParentId() != null) { //Parent ID must belong to same collection and also validate
              validateIdHasRecordBasePrefix(record.getBase(), record.getParentId());
            }
            
            String idNorm = IdNormaliser.normaliseId(record.getId());
            if (!record.getId().equals(idNorm)) {
                record.setOrgid(record.getId()); //set this before changing value below
                record.setId(idNorm);                
                record.setIdError(true);
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


    public static ArrayList<RecordBaseCountDto> getRecordBaseStatistics() {
        return performStorageAction("getRecordBaseStatistics", DsStorage::getBaseStatictics);
    }


    public static ArrayList<DsRecordDto> getRecordsModifiedAfter(String recordBase, long mTime, int batchSize) {
        String id = String.format(Locale.ROOT, "getRecordsModifiedAfter(recordBase='%s', mTime=%d, batchSize=%d)",
                                  recordBase, mTime, batchSize);
        return performStorageAction(id, storage -> {
            ArrayList<DsRecordDto> records = storage.getRecordsModifiedAfter(recordBase, mTime, batchSize);
            //Load children. This can be optimized  in SQL, but this is much simpler.
            // Are children even needed here? Will improve performance a lot.
            for (DsRecordDto record : records) {
                record.setChildren(storage.getChildrenIds(record.getId()));
            }
            return records;
        });
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
                record.setChildren(childrenIds);
            }
            return record;
        });
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


    public static int deleteMarkedForDelete(String recordBase) {
        return performStorageAction("deleteMarkedForDelete(" + recordBase + ")", storage -> {
            validateBaseExists(recordBase);

            int numberDeleted =  storage.deleteMarkedForDelete(recordBase);
            log.info("Delete marked for delete records for recordbase:"+recordBase +" number deleted:"+numberDeleted);
            //We are not touching parent/children relation when deleting for real.
            return numberDeleted;
        });
    }

    /*
     * This is called when ever a record is modified (create/update/markfordelete). The recordId here
     * has already been assigned a new mTime. Update mTime for parent and/or children according to  update strategy for that base.
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
        RecordBaseDto recordBase = ServiceConfig.getAllowedBases().get(record.getBase());       
        UpdateStrategyDto updateStrategy = recordBase.getUpdateStrategy();

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
     * Check that the given base is defined in the setup.
     * @param base base name.
     */
    private static void validateBaseExists(String base) {
        if (ServiceConfig.getAllowedBases().get(base) == null) {
            throw new InvalidArgumentServiceException("Unknown record base:"+base);
        }
    }
   
    /**
     * Check that the recordId starts with the recordbase as prefix
     * @param base base name.
     */
    private static void validateIdHasRecordBasePrefix(String base, String recordId) {
        if (!recordId.startsWith(base)) {
            throw new InvalidArgumentServiceException("Id must have recordbase as prefix. Id:"+recordId);
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
            } catch (Exception e) {
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
         * a wrapping {@link dk.kb.storage.webservice.exception.ServiceException} will be thrown.
         * @param storage a storage ready for requests and updates.
         * @return custom return value.
         * @throws Exception if something went wrong.
         */
        T process(DsStorage storage) throws Exception;
    }

}
