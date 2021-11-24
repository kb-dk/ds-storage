package dk.kb.storage.facade;

import java.sql.SQLException;

import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.kb.storage.model.v1.DsRecordDto;
import dk.kb.storage.storage.DsStorage;
import dk.kb.storage.webservice.exception.InternalServiceException;



public class DsStorageFacade {

    private static final Logger log = LoggerFactory.getLogger(DsStorageFacade.class);
	

    
    public static void createOrUpdateRecord(DsRecordDto record) throws Exception {
    	
    	//TODO validate base!    	
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
	
    
    /*
     * Return null if record does not exist
     * 
     */
 public static DsRecordDto getRecord(String recordId) throws Exception {
    	
    	//TODO validate base!    	
    	  try (DsStorage storage = new DsStorage();) {
              
              try {             
            		DsRecordDto record = storage.loadRecord(recordId);            		
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
    
    
}
