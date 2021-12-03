package dk.kb.storage.facade;

import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.kb.storage.config.ServiceConfig;
import dk.kb.storage.model.v1.DsRecordDto;
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


	public static void validateBase(String base) throws Exception{

		if (ServiceConfig.getAllowedBases().get(base) == null) {    		
			throw new InvalidArgumentServiceException("Unknown record base:"+base);
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


	public static void markRecordForDelete(String recordId) throws Exception {
		//TODO touch children etc.
		try (DsStorage storage = new DsStorage();) {

			try {             
				storage.markRecordForDelete(recordId);            		            	            	
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
