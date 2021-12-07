package dk.kb.storage.facade;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.kb.storage.config.ServiceConfig;
import dk.kb.storage.model.v1.DsRecordDto;
import dk.kb.storage.model.v1.RecordBaseCountDto;
import dk.kb.storage.model.v1.RecordBaseDto;
import dk.kb.storage.model.v1.UpdateStrategyDto;
import dk.kb.storage.storage.DsStorage;
import dk.kb.storage.webservice.exception.InternalServiceException;
import dk.kb.storage.webservice.exception.InvalidArgumentServiceException;



public class DsStorageFacade {

    private static final Logger log = LoggerFactory.getLogger(DsStorageFacade.class);



    public static void createOrUpdateRecord(DsRecordDto record) throws Exception {

        validateBase(record.getBase());

        try (DsStorage storage = new DsStorage();) {

            try {             
                boolean recordExists = storage.recordExists(record.getId());

                if (!recordExists) { //Create new record
                    log.info("Creating new record with id:"+record.getId());
                    storage.createNewRecord(record);            		                                                 
                }
                else {
                    log.info("Updating record with id:"+record.getId());
                    storage.updateRecord(record);            		
                }
                updateMTimeForParentChild(storage,record.getId()); 

                storage.commit(); 

            } catch (SQLException e) {
                log.error("Error create or update for record:"+record.getId() +" :"+e.getMessage());
                storage.rollback();
                throw new InternalServiceException(e);
            }
        } catch (SQLException e) { //Connecting to storage failed
            throw new InternalServiceException(e);
        }

    }


    public static ArrayList<RecordBaseCountDto> getRecordBaseStatistics() throws Exception {
        try (DsStorage storage = new DsStorage();) {
            try {             
                ArrayList<RecordBaseCountDto> baseStatictics = storage.getBaseStatictics();              
                return baseStatictics;                          
            } catch (SQLException e) {
                log.error("Error in getRecordBaseStatistics :"+e.getMessage());
                storage.rollback();
                throw new InternalServiceException(e);
            }
        } catch (SQLException e) { //Connecting to storage failed
            throw new InternalServiceException(e);
        }

    }

    public static ArrayList<DsRecordDto> getRecordsModifiedAfter(String recordBase, long mTime, int batchSize) throws Exception {
        try (DsStorage storage = new DsStorage();) {
            try {             
                ArrayList<DsRecordDto> records= storage.getRecordsModifiedAfter(recordBase, mTime, batchSize);
                return records;                          
            } catch (SQLException e) {
                log.error("Error in getRecordBaseStatistics :"+e.getMessage());
                storage.rollback();
                throw new InternalServiceException(e);
            }
        } catch (SQLException e) { //Connecting to storage failed
            throw new InternalServiceException(e);
        }

    }


    /*
     * Return null if record does not exist
     * 
     */
    public static DsRecordDto getRecord(String recordId) throws Exception {

        try (DsStorage storage = new DsStorage();) {
            try {             
                DsRecordDto record = storage.loadRecord(recordId);            		
                if (record== null) {
                    return null;                    
                }

                if (record.getParentId() == null) { //can not have children if also has parent (1 level only hieracy)                    
                    ArrayList<String> childrenIds = storage.getChildrenIds(record.getId());
                    record.setChildren(childrenIds);                    
                }                
                return record;            	            	
            } catch (SQLException e) {
                log.error("Error getRecord for :"+recordId +" :"+e.getMessage());
                storage.rollback();
                throw new InternalServiceException(e);
            }
        } catch (SQLException e) { //Connecting to storage failed
            throw new InternalServiceException(e);
        }

    }


    public static Integer markRecordForDelete(String recordId) throws Exception {
        //TODO touch children etc.
        try (DsStorage storage = new DsStorage();) {

            try {             
                Integer updated = storage.markRecordForDelete(recordId);   
                updateMTimeForParentChild(storage,recordId); 
                storage.commit();
                return updated;                                
            } catch (SQLException e) {
                log.error("Error markRecordForDelete for :"+recordId +" :"+e.getMessage());
                storage.rollback();
                throw new InternalServiceException(e);
            }
        } catch (SQLException e) { //Connecting to storage failed
            throw new InternalServiceException(e);
        }
    }   


    public static int deleteMarkedForDelete(String recordBase) throws Exception {

        validateBase(recordBase);

        try (DsStorage storage = new DsStorage();) {

            try {             
                int numberDeleted =  storage.deleteMarkedForDelete(recordBase);                                
                storage.commit();
                log.info("Delete marked for delete records for recordbase:"+recordBase +" number deleted:"+numberDeleted);
                //We are not touching parent/children relation when deleting for real.
                return numberDeleted;

            } catch (SQLException e) {
                log.error("Error deleteMarkedForDelete for :"+recordBase +" :"+e.getMessage());
                storage.rollback();
                throw new InternalServiceException(e);
            }
        } catch (SQLException e) { //Connecting to storage failed
            throw new InternalServiceException(e);
        }

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
        RecordBaseDto recordBase = ServiceConfig.getAllowedBases().get(record.getBase());       
        UpdateStrategyDto updateStrategy = recordBase.getUpdateStrategy();

        boolean hasParent = (record.getParentId() != null);
        log.info("Updating parent/child relation for recordId:"+recordId +" with updateStrategy:"+updateStrategy);
        
        if (UpdateStrategyDto.NONE == updateStrategy){
        //Do nothing
        }
        else if (UpdateStrategyDto.CHILD == updateStrategy){            
            //update all children one at a time
            ArrayList<String> childrenIds = storage.getChildrenIds(recordId);            
            for (String childId : childrenIds) {
                log.debug("Updating mTime for children:"+childId+" for parent:"+record.getParentId());
                storage.updateMTimeForRecord(childId);                                 
            }

        }
        else if (UpdateStrategyDto.PARENT == updateStrategy){
            if (!hasParent) {
              return;  
            }            
            log.info("Parentid:"+record.getParentId());
             storage.updateMTimeForRecord(record.getParentId());                     
                      

        }
        else if (UpdateStrategyDto.ALL == updateStrategy){
            //Find parent and then update mTime, and update all children. 
            //If parent or one of the children matches the record, it has already been updated with a new mTime,  so skip it.                        
            DsRecordDto topParent = null;
            if (!hasParent) {
                topParent=record;
            }
            else {
                topParent=storage.loadRecord(record.getParentId());   //can be null                                                     
                System.out.println("parent loaded: "+topParent.getId());
            }            

            //TopParent can be null if record does not exist
            if (topParent == null) {                   
                return;
            }
           
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
        else { //Sanity check
            throw new InvalidArgumentServiceException("Update strategy not implemented:"+updateStrategy);

        }                
    }

    private static void validateBase(String base) throws Exception{

        if (ServiceConfig.getAllowedBases().get(base) == null) {      
            throw new InvalidArgumentServiceException("Unknown record base:"+base);
        }

    }



}
