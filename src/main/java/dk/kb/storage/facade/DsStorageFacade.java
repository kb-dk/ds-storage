package dk.kb.storage.facade;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;

import dk.kb.util.Pair;
import dk.kb.util.webservice.stream.ExportWriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.kb.storage.config.ServiceConfig;
import dk.kb.storage.model.v1.DsRecordDto;
import dk.kb.storage.model.v1.DsRecordMinimalDto;
import dk.kb.storage.model.v1.MappingDto;
import dk.kb.storage.model.v1.OriginCountDto;
import dk.kb.storage.model.v1.OriginDto;
import dk.kb.storage.model.v1.RecordTypeDto;
import dk.kb.storage.model.v1.RecordsCountDto;
import dk.kb.storage.model.v1.TranscriptionDto;
import dk.kb.storage.model.v1.UpdateStrategyDto;
import dk.kb.storage.storage.DsStorage;
import dk.kb.storage.util.IdNormaliser;
import dk.kb.util.webservice.exception.InternalServiceException;
import dk.kb.util.webservice.exception.InvalidArgumentServiceException;
import dk.kb.util.webservice.exception.NotFoundServiceException;



public class DsStorageFacade {

    private static final Logger log = LoggerFactory.getLogger(DsStorageFacade.class);


    /**
     * <p>
     * Get a list of records after a given mTime. The records will only have fields
     * id, mTime, referenceid and kalturaid defined
     * </p>
     *
     *@param origin The origin to fetch records from
     *@param mTime only fetch records with mTime larger that this
     *@param batchSize Number of maximum records to return
     *
     * @return List of records only have fields id, mTime, referenceid and kalturaid
     */
    public static  ArrayList<DsRecordMinimalDto>  getReferenceIds(String origin, long mTime, int batchSize)  {                       
        String id = String.format(Locale.ROOT, "getReferenceIds(origin='%s', mTime=%d, batchSize=%d)", origin, mTime, batchSize);
        return performStorageAction(id, storage -> storage.getReferenceIds(origin, mTime, batchSize));
    }

    public static Long getMinimalRecordsModifiedAfter(
            ExportWriter writer, String origin, long mTime, long maxRecords, int batchSize) {
        String id = String.format(Locale.ROOT, "getMinimalRecordsModifiedAfter(origin='%s', mTime=%d, maxRecords=%d, batchSize=%d)",
                origin, mTime, maxRecords, batchSize);
        long pending = maxRecords == -1 ? Long.MAX_VALUE : maxRecords; // -1 = all records
        final AtomicLong lastMTime = new AtomicLong(mTime);
        long totalDelivered = 0L;
        while (pending > 0) {
            int request = pending < batchSize ? (int) pending : batchSize;
            long delivered = performStorageAction(id, storage -> {
                ArrayList<DsRecordMinimalDto> records = storage.getReferenceIds(origin, lastMTime.get(), request);
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
            totalDelivered += delivered;
        }
        log.info("Delivered '{}' records", totalDelivered);
        return totalDelivered;
    }
    

    
    /**
     * <p>
     * Get a mapping having (referenceid,kalturaid). If there is no entry for the referenceid in the mapping table null will be returned.
     * The referenceid can exist but kalturaid can be null even if the entry is uploaded to kaltura, but the mapping table has not been updated yet.
     * </p>
     * 
     * @param referenceId The referenceId for the record. 
     * 
     */
    public static MappingDto getMapping(String referenceId)  {                       
        return performStorageAction("getMapping(" + referenceId + ")", storage -> storage.getMappingByReferenceId(referenceId));
    }    
    

    /**
     * <p>
     * If the mapping does not exist a new entry will be created in the mapping table.<br>
     * The referenceid can not be null, but kalturaId can be null.<br>
     * If the mapping already exist for the referenceid, the kalturaId value will be updated
     * </p>
     * 
     * @param mappingDto The mapping entry to be created or updated
     * 
     */
    public static void createOrUpdateMapping(MappingDto mappingDto)  {
        performStorageAction("createOrUpdateMapping(" + mappingDto.getReferenceId() + ")", storage -> {                      
            try {
                storage.getMappingByReferenceId(mappingDto.getReferenceId()); //throws exception if not found
                storage.updateMapping(mappingDto);
                log.info("Updated mapping referenceId={}, kalturaId={}",mappingDto.getReferenceId(),mappingDto.getKalturaId());
            }
            catch(Exception e){
                storage.createNewMapping(mappingDto);
                log.info("Created new mapping referenceId={}, kalturaId={}",mappingDto.getReferenceId(),mappingDto.getKalturaId());
           }                        
            return null; // Something must be returned
        });
    }


    /**
     * <p>
     * Create or update a new transcription. The primary key is fileId that comes from
     * the external system. The transcription text is the full text and transcription_lines
     * are lines with start-end followed by the sentence and with a new line in the end.     
     *  
     * @param TranscriptionDto The entry to be created or updated
     * 
     */
    public static void createOrUpdateTranscription(TranscriptionDto transcription)   {
        performStorageAction("createOrUpdatTranscription(" + transcription.getFileId() + ")", storage -> {                      
           String fileId=transcription.getFileId();     
           int count = storage.countTranscriptionByFileId(fileId);
           if (count>0) {
             storage.deleteTranscriptionByFileId(fileId);
            }              
            storage.createNewTranscription(transcription);      
            int touched=storage.updateMTimeForRecordByFileId(fileId);
            log.info("Create/Updated transcription with fileId='{}' number of records touched='{}'",fileId,touched);                                         
            return null; // Something must be returned
        });
    }

    
    
    public static void createOrUpdateRecord(DsRecordDto record)  {
        performStorageAction("createOrUpdateRecord(" + record.getId() + ")", storage -> {
            validateOriginExists(record.getOrigin());        
            validateIdHasOriginPrefix(record.getOrigin(), record.getId());
            validateRecordType(record.getRecordType());
            String orgId = record.getId();
            if (record.getParentId() != null) { //Parent ID must belong to same collection and also validate
              validateIdHasOriginPrefix(record.getOrigin(), record.getParentId());
            }
            
            String idNorm = IdNormaliser.normaliseId(record.getId());
            if (!orgId.equals(idNorm)) {
                record.setOrgid(orgId); //set this before changing value below
                record.setId(idNorm);                
                record.setIdError(true);
                log.warn("ID was normalized from: '{}' to '{}'", orgId, idNorm);
            }
            
            if (record.getParentId() != null) { //Also normalize parentID
                record.setParentId(IdNormaliser.normaliseId(record.getParentId()));                
            }            
            
            boolean recordExists = storage.recordExists(record.getId());
            if (recordExists) {
                //Have to load record to see if referenceId has changed. Then we need to clear kalturaId
                DsRecordDto oldRecord = storage.loadRecord(record.getId());
              
                //Keep old kalturaId if referenceid is the same.
                 if (record.getKalturaId() == null && record.getReferenceId() != null && !record.getReferenceId().equals(oldRecord.getReferenceId())) {                   
                    record.setKalturaId(oldRecord.getKalturaId());                      
                 }                                
                log.info("Updating record with id: '{}'", record.getId());
                storage.updateRecord(record);
            } else {               
                log.info("Creating new record with id: '{}'", record.getId());
                storage.createNewRecord(record);
            }
            updateMTimeForParentChild(storage,record.getId());
            return null; // Something must be returned
        });
    }

    
    /**
     * Update kaltura id for a record. The kaltura id is given to the record when uploaded to Kaltura. The Kaltura id must then later be updated with this method.
     * 
     * @param referenceId The referenceId given to the record when uploaded to Kaltura
     * @param kalturaId The Kaltura id in the kaltura system. The id is given to a record after upload.
     */
    public static void updateKalturaIdForRecord(String referenceId, String kalturaId){
         performStorageAction("updateKalturaIdForRecord(" + referenceId + ")", storage -> {
         storage.updateKalturaIdForRecord(referenceId, kalturaId);         
        return null;    // Something must be returned
        });
    }
    
    /**
     * Update reference id for a record. The referenceId is the id value for the record in the external system. For preservica referenceId is the name of the stream file.
     * 
     * @param recordId of the record to update
     * @param referenceId The referenceId to set for the record
     */
    public static void updateReferenceIdForRecord(String recordId, String referenceId){
         performStorageAction("updateKalturaIdForRecord(" + referenceId + ")", storage -> {
         storage.updateReferenceIdForRecord(recordId,referenceId);         
        return null;    // Something must be returned
        });
    }
    
    
    
    
    /**
     * <p>
     * Update all records that have referenceId but missing kalturaId.<br>
     * If the mapping exist in the mapping table referenceId <-> kalturaId, then the record will be updated with the kaltura.<br>
     * If the mapping does not exist (yet), the record will not be updated with kaltura id.<br>
     * <br>
     * If many records needs to be updated this can take some time. 1M records is estimated to take 15 minutes. 
     * </p>
     *    
     * @return Number of records that was enriched with kalturaId 
     */
    public static RecordsCountDto updateKalturaIdForRecords() {
       return performStorageAction("updateKalturaIdForRecords", DsStorage::updateKalturaIdForRecords);
    }  
    
    public static ArrayList<OriginCountDto> getOriginStatistics() {
        return performStorageAction("getOriginStatistics", DsStorage::getOriginStatictics);
    }

    /**
     * Get the count of records from a specific origin
     * @param origin to count amount of records from.
     * @param mTime  is needed to deliver a number that is equal to the extracted values.
     * @return the number of records sent through the stream.
     */
    public static long countRecordsInOrigin(String origin, long mTime){
        return performStorageAction("getAmountOfRecordsForOrigin(origin: " + origin +")", storage -> {
            validateOriginExists(origin);
            return storage.getAmountOfRecordsForOrigin(origin, mTime);
        } );
    }

    /**
    *   Retrieve records (DsRecordDs) as a list. The local record tree will not be loaded as objects
    *
    *   @param origin origin for the record. Origins are defined in the yaml file
    *   @param mTime Retrieve records starting from this time
    *   @param maxRecords Number of maximum records to extract total
    *   @param batchSize Number of records batch. No reason to change the default 1000.
    *   @return a long representing the total amount of records that have been written from storage.
    */
    public static Long getRecordsModifiedAfter(
            ExportWriter writer, String origin, long mTime, long maxRecords, int batchSize) {
        String id = String.format(Locale.ROOT, "writeRecordsModifiedAfter(origin='%s', mTime=%d, maxRecords=%d, batchSize=%d)",
                                  origin, mTime, maxRecords, batchSize);
        long pending = maxRecords == -1 ? Long.MAX_VALUE : maxRecords; // -1 = all records
        final AtomicLong lastMTime = new AtomicLong(mTime);
        long totalDelivered = 0L;
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
            totalDelivered += delivered;
        }
        log.info("Delivered '{}' records", totalDelivered);
        return totalDelivered;
    }

    /**
     *   Retrieve records (DsRecordDs) as a list with the local tree loaded as object.
     *
     *   @param origin origin for the record. Origins are defined in the yaml file
     *   @param recordType Only retrieve records with this recordType
     *   @param mTime Retrieve records starting from this time
     *   @param maxRecords Number of maximum records to extract total
     *   @param batchSize Number of records batch. No reason to change the default 1000. 
     */
    public static Long getRecordsByRecordTypeModifiedAfterWithLocalTree(
            ExportWriter writer, String origin, RecordTypeDto recordType, long mTime, long maxRecords, int batchSize) {
        String id = String.format(Locale.ROOT, "getRecordsByRecordTypeModifiedAfterWithLocalTree(origin='%s', recordType='%s' mTime=%d, maxRecords=%d, batchSize=%d)",
                                  origin, recordType, mTime, maxRecords, batchSize);
        long pending = maxRecords == -1 ? Long.MAX_VALUE : maxRecords; // -1 = all records
        final AtomicLong lastMTime = new AtomicLong(mTime);
        long totalDelivered = 0L;
        while (pending > 0) {
            int request = pending < batchSize ? (int) pending : batchSize;
            long delivered = performStorageAction(id, storage -> {

                //important. Only load id's for performance. Then load the recordTree
                ArrayList<String> ids = storage.getRecordsIdsByRecordTypeModifiedAfter(origin, recordType,lastMTime.get(), request);

                ArrayList<DsRecordDto> records = new ArrayList<>();
                for (String singleId : ids) {
                    records.add(getRecord(singleId));
                }
                                
                // We have to load the localTree for the records                
                records.forEach(DsStorageFacade::setLocalTreeForRecord);
                
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
            totalDelivered += delivered;
        }
        return totalDelivered;
    }
    
    
    
    
    /**
     *  Load a record with childrenIds and parentId if they exist 
     *  If the value includeLocalTree is true also load the local tree for the given record. Parent will be loaded and all children. Siblings will not be loaded.
     *  <ol>
     *    <li>If there is a parent record, the given record will point to it, but the parent will not point back to this child</li>
     *    <li>If there is a parent record, the given record will point to it, but the parent will not point back to this child</li>
     *  </ol>
     *   
     *  @param recordId The record id . If includeLocalTree is set the object tree will be returned with a pointer to this record
     *  @param  includeLocalTree Load the parent and children as object and not just IDs.
     *
     */
    public static DsRecordDto getRecord(String recordId, Boolean includeLocalTree) {
      if (!includeLocalTree) {
           DsRecordDto record = getRecord(recordId);
           if (record==null) {
               throw new NotFoundServiceException("No recordId found for:"+recordId);
           }
           return record;
      }
      else {
          return getRecordTreeLocal(recordId);
      }      
    }

    
    /**
     * Load a record with childrenIds
     * <p>
     * Return null if record does not exist
     * 
     */
    private static DsRecordDto getRecord(String recordId) {
        return performStorageAction(" getRecord(" + recordId + ")", storage -> {
        String idNorm = IdNormaliser.normaliseId(recordId);
           DsRecordDto record = storage.loadRecordWithChildIds(idNorm);
           return record;
        });
    }

    /**
     *   Will load full object tree. The DsRecordDto return will a pointer the record with the recordId in the tree
     * <p>
     *  Logic: Find top parent recursive and load children.
     * 
     *  @param recordId The full object tree will be returned with a pointer to this record   
     * 
     */
    public static DsRecordDto getRecordTree(String recordId) {
             
        return performStorageAction("getRecord(" + recordId + ")", storage -> {
        String idNorm = IdNormaliser.normaliseId(recordId);          
        DsRecordDto record = getRecord(idNorm); //Load from facade as this will set children. Will return null if record not found
                
         DsRecordDto topParent = getTopParent(record); //this will also detect a cycle.              
                  
         loadAndSetChildRelations(topParent,new HashSet<>(), record); //Recursive method
                    
         return record;
         
        });
    }
  

    /**
     *  Will load the local tree for the given record. Parent will be loaded and all children. Siblings will not be loaded. The tree will only point one way
     *  from the record.
     *  1) If there is a parent record, the given record will point to it, but the parent will not point back to this child
     *  2) Children will be loaded, but the children will not point back to this parent record.      
     * 
     *  @param recordId The local object tree will be returned with a pointer to this record   
     * 
     */
    private static DsRecordDto getRecordTreeLocal(String recordId) {
           
        return performStorageAction("getRecordTreeLocal(" + recordId + ")", storage -> {
        String idNorm = IdNormaliser.normaliseId(recordId);          
        DsRecordDto record = getRecord(idNorm); //Load from facade as this will set children as id's. 
        setLocalTreeForRecord(record);                                     
        return record;
         
        });
    }

    /**
     * Touch a record and update its mTime
     * @param recordId of record to touch.
     * @throws NotFoundServiceException when a record cannot be found in storage.
     */
    public static RecordsCountDto touchRecord(String recordId) {
        RecordsCountDto countDto = performStorageAction("updateMTimeForRecord(" + recordId +")", storage -> {
            String idNorm = IdNormaliser.normaliseId(recordId);
            return storage.updateMTimeForRecord(idNorm);
        });

        if (countDto.getCount() == null | countDto.getCount() < 1){
            log.error("The record with id: '{}' was not touched as it doesn't exist in DS-storage", recordId);
            throw new NotFoundServiceException("The record with id: '{}' doesn't exist in DS-storage");
        }

        return countDto;
    }
  
    /**
     * Will recursive go up in the tree to find the top parent.
     * Throws an exception if a cycle is detected.
     * If a parent does not exist it will return last valid record instead. This is due to inconsistent data.
     *  
     * @param record to retrieve parent for.
     * @throws InternalServiceException If a cycle is detected.
     * @return parent record
     */
    private static DsRecordDto getTopParent(DsRecordDto record) throws InternalServiceException{
    
      HashSet<String> ids = new HashSet<>();
      DsRecordDto topParent = record;
      while (topParent.getParentId() != null) {          
      
          if (ids.contains(topParent.getId())) {
              log.error("Cycle detected for recordId: '{}'", topParent.getId());
              throw new InternalServiceException("Cycle detected for recordId:"+topParent.getId());              
          }          
          ids.add(topParent.getId());
          DsRecordDto nextParent = getRecord(topParent.getParentId());                                           
          if (nextParent==null) { //inconsistent data.
              log.warn("Inconsistent data. Parent with ID does not exist: '{}' and is set for record: '{}'", topParent.getParentId(), topParent.getId());
              return topParent; 
          }
          topParent=nextParent;
      }
      return topParent;        
    }
    
    
    /**
     * Delete all records for an origin that has been modified time interval. The records will be deleted and not just marked for deletion
     * 
     * @param origin The origin for the collection. Value must be defined in the configuration
     * @param mTimeFrom modified time from. Format is millis +3 digits
     * @param mTimeTo modified time to. Format is millis +3 digits
     */
    public static RecordsCountDto deleteRecordsForOrigin(String origin, long mTimeFrom, long mTimeTo) {
        return performStorageAction("deleteRecordsForOrigin(" + origin + ")", storage -> {
            validateOriginExists(origin);
            RecordsCountDto count = storage.deleteRecordsForOrigin(origin,mTimeFrom,mTimeTo);                       
            log.info("Deleted {} records from origin={}",count.getCount(),origin);                                            
            return count;
        });
    }
    

    public static RecordsCountDto markRecordForDelete(String recordId) {
        //TODO touch children etc.
        return performStorageAction("markRecordForDelete(" + recordId + ")", storage -> {
            String idNorm = IdNormaliser.normaliseId(recordId);            
            RecordsCountDto countDto = storage.markRecordForDelete(idNorm);
            updateMTimeForParentChild(storage,recordId);
            log.info("Record marked for delete: '{}'", recordId);                       
            return countDto;
        });
    }


    public static RecordsCountDto deleteMarkedForDelete(String origin) {
        return performStorageAction("deleteMarkedForDelete(" + origin + ")", storage -> {
            validateOriginExists(origin);

            RecordsCountDto count =  storage.deleteMarkedForDelete(origin);
            log.info("Deleted all marked for delete records for origin: '{}'. Number deleted: '{}'", origin, count.getCount());

            //We are not touching parent/children relation when deleting for real.
            return count;
        });
    }

    /**
     * Extract max {@code record.mTime}, where {@code record.mTime > mTime} in {@code origin},
     * ordered by {@code record.mTime} and limited to {@code maxRecords}.
     * Secondarily, check whether there are any records with record.mTime higher than the returned
     * maximum mTime.
     * @param origin only records from the {@code origin} will be inspected.
     * @param mTime only records with modification time larger than {@code mTime} will be inspected.
     * @param maxRecords only this number of records will be inspected. {@code -1} means no limit.
     * @return pair of (maximum {@code record.mTime} or null if no match, true if there exists at
     *         least 1 record with {@code record.mTime} higher than the maximum within the constraints).
     */
    public static Pair<Long, Boolean> getMaxMtimeAfter(String origin, long mTime, long maxRecords) {
        return performStorageAction(
                "getMaxMtimeAfter(origin='" + origin + "', mTime=" + mTime + ", maxRecords=" + maxRecords + ")",
                storage -> storage.getMaxMtimeAfter(origin, mTime, maxRecords));
    }

    /**
     * Extract max {@code record.mTime}, where {@code record.mTime > mTime} in {@code origin},
     * ordered by {@code record.mTime} and limited to {@code maxRecords}.
     * Secondarily, check whether there are any records with record.mTime higher than the returned
     * maximum mTime.
     * @param origin only records from the {@code origin} will be inspected.
     * @param recordType only records with the given type will be inspected.
     * @param mTime only records with modification time larger than {@code mTime} will be inspected.
     * @param maxRecords only this number of records will be inspected. {@code -1} means no limit.
     * @return pair of (maximum {@code record.mTime} or null if no match, true if there exists at
     *         least 1 record with {@code record.mTime} higher than the maximum within the constraints).
     */
    public static Pair<Long, Boolean> getMaxMtimeAfter(
            String origin, RecordTypeDto recordType, long mTime, long maxRecords) {
        return performStorageAction(
                "getMaxMtimeAfter(origin='" + origin + "', type='" + recordType + "', mTime=" + mTime +
                ", maxRecords=" + maxRecords + ")",
                storage -> storage.getMaxMtimeAfter(origin, recordType, mTime, maxRecords));
    }


    /*
     * This is called whenever a record is modified (create/update/markfordelete). The recordId here
     * has already been assigned a new mTime. Update mTime for parent and/or children according to  update strategy for that origin.
     * 
     * This method will not commit/rollback as this is handled by the calling method.
     * 
     * See UpdateStrategyDto
     */
    private static void updateMTimeForParentChild(DsStorage storage, String recordId) throws Exception{
        DsRecordDto record=  storage.loadRecord(recordId); //Notice for performance tuning, recordDto can sometimes be given to the method. No premature optimization...
        if (record==null) { //Can happen when marking records for delete and record is not in storage.            
            return;            
        }
        OriginDto origin = ServiceConfig.getAllowedOrigins().get(record.getOrigin());       
        UpdateStrategyDto updateStrategy = origin.getUpdateStrategy();

        log.info("Updating parent/child relation for recordId: '{}' with updateStrategy: '{}'", recordId, updateStrategy);

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

            RecordsCountDto count = storage.updateMTimeForRecord(childId);
           if (count.getCount() == 0) {
               log.warn("Children with id does not exist:"+childId);           
           }
        }
    }

    /**
     * Update mTime for the parent of the Record, if it has any.
     * @param storage ready for updates.
     * @param record the Record to update parent mTime for.
     * @throws Exception if updating failed.
     */
     private static void updateMTimeForParent(DsStorage storage, DsRecordDto record) throws Exception {
        //Notice for performance tuning, recordDto can sometimes be given to the method. No premature optimization...
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
        DsRecordDto topParent = getTopParent(record);
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
     * Validate recordType is not null
     * 
     * @param type Record type to validate
     */
    private static void validateRecordType(RecordTypeDto type) {
        if (type==null) {
            throw new InvalidArgumentServiceException("RecordType must not be null");
        }
    }
    
    
    /**
     * Starts a storage transaction and performs the given action on it, returning the result from the action.
     * <p>
     * If the action throws an exception, a {@link DsStorage#rollback()} is performed.
     * If the action passes without exceptions, a {@link DsStorage#commit()} is performed.
     * @param actionID a debug-oriented ID for the action, typically the name of the calling method.
     * @param action the action to perform on the storage.
     * @return return value from the action.
     * @throws InternalServiceException if anything goes wrong.
     */
    private static <T> T performStorageAction(String actionID, StorageAction<T> action) {
         long start=System.currentTimeMillis();
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

            log.debug("Storage method '{}' SQL time in millis: {} ", actionID, (System.currentTimeMillis()-start));
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
     * @param currentRecord Top record in the object tree. The tree will only be loaded from this node and down.
     * @param previousIdsForCycleDetection Set to keep track of cycles. When calling this method supply it with a new empty HashSet
     * @param origo     record used as recursive parameter
     */
    
    private static void loadAndSetChildRelations(DsRecordDto currentRecord, HashSet<String> previousIdsForCycleDetection, DsRecordDto origo)  {
       
                
        List<String> childrenIds = currentRecord.getChildrenIds();                
        List<DsRecordDto> childrenRecords = new ArrayList<>();
        for (String childId: childrenIds) {
                        
            //DsRecordDto child = getRecord(childId);          
            DsRecordDto child = childId.equals(origo.getId()) ? origo: getRecord(childId);
            child.setParent(currentRecord);
            childrenRecords.add(child);
            
            if(previousIdsForCycleDetection.contains(child.getId())){
                log.error("Parent-child cycle detected for id (stopped loading rest of hierarchy tree): {} ", child.getId());
                throw new InternalServiceException("Parent-child cycle detected for id:"+child.getId());
            }
            previousIdsForCycleDetection.add(child.getId());
            loadAndSetChildRelations(child, previousIdsForCycleDetection,origo); //This is the recursive call
        }             
       
        currentRecord.setChildren(childrenRecords);
        
    }

    
    /**
     * This method will load the local tree around the given record. It will
     * 1) Load the parent if it exists, and this will be set as parent. Parent will not point down to this child
     * 2) Load all children and set them as children. The children will not point back to this parent.   
     * 
     * @param record The input record with the local tree set
     * @exception InvalidArgumentServiceException is thrown if a record has over 1000 children. It is not expected any caller would want this, but is instead seen as mistake.
     */
    
    private static void setLocalTreeForRecord(DsRecordDto record)  {

        //Doom switch prevention.
        if (record.getChildrenIds() != null && record.getChildrenIds().size() > 1000) { // It seems our collections will have a very few or millions. 
            throw new InvalidArgumentServiceException("Record has too many children, id:"+record.getId());           
        }
        
        //Set parent
        String parentId=record.getParentId();
        if (parentId != null) {
            DsRecordDto parent = getRecord(parentId);
            record.setParent(parent);
        }
        
        record.getChildrenIds().stream()
        .map(DsStorageFacade::getRecord)
        .forEach(record::addChildrenItem);
      
         
        //just alternative method                  
        //childrenIds.forEach( c -> record.getChildren().add(getRecord(c))); // Just to make Toke happy, but only as a comment instead of the for-loop        
    }


    /**
     * Callback used with {@link #performStorageAction(String, StorageAction)}.
     * @param <T> the object returned from the {@link StorageAction#process(DsStorage)} method.
     */
    @FunctionalInterface
    private interface StorageAction<T> {
        /**
         * Access or modify the given storage inside a transaction.
         * If the method throws an exception, it will be logged, a {@link DsStorage#rollback()} will be performed and
         * a wrapping {@link dk.kb.util.webservice.exception.ServiceException} will be thrown.
         * @param storage a storage ready for requests and updates.
         * @return custom return value.
         * @throws Exception if something went wrong.
         */
        T process(DsStorage storage) throws Exception;
    }

}
