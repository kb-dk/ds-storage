package dk.kb.storage.config.facade;


import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


import dk.kb.storage.config.ServiceConfig;
import dk.kb.storage.facade.DsStorageFacade;
import dk.kb.storage.model.v1.DsRecordDto;
import dk.kb.storage.storage.DsStorageUnitTestUtil;

public class DsStorageFacadeTest extends DsStorageUnitTestUtil{
	
	 
	
	/*
	 * Delete all records between each unittest. The clearTableRecords is only called from here. 
	 * The facade class is reponsible for committing transactions. So clean up between unittests.
	 */
	@BeforeEach
	public void beforeEach() throws Exception {	        	    		    	
		storage.clearTableRecords();
        storage.commit();
	}
		
	
	@Test
	public void testCreateAndUpdate() throws Exception {
       //TODO rescribe flow below
		System.out.println(ServiceConfig.getAllowedBases());
	
		DsRecordDto r1 = DsStorageFacade.getRecord("does_not_exist");
		Assertions.assertTrue(r1 == null);
		
		String id ="id1";
		String base="doms_radio"; //Must be defined in yaml properties as allowed base
		String data = "Hello";
		String parentId="id_1_parent";

		DsRecordDto record = new DsRecordDto();
		record.setId(id);
		record.setBase(base);
		record.setData(data);
		record.setParentId(parentId);
		DsStorageFacade.createOrUpdateRecord(record );
		
		DsRecordDto recordLoaded = DsStorageFacade.getRecord(id);
		Assertions.assertTrue(recordLoaded != null);

		//Load and check values are correct
		Assertions.assertEquals(id,recordLoaded.getId());
		Assertions.assertEquals(base,recordLoaded.getBase());
		Assertions.assertFalse(recordLoaded.getDeleted());
		Assertions.assertEquals(parentId,record.getParentId());        
		Assertions.assertTrue(recordLoaded.getmTime() > 0);
		Assertions.assertEquals(recordLoaded.getcTime(), recordLoaded.getmTime());                  


		//Now update
		String dataUpdate = "Hello updated";
		String parentIdUpdated="id_2_parent";
		long cTimeBefore = recordLoaded.getcTime(); //Must be the same

		record.setDeleted(true);
		record.setData(dataUpdate);
		record.setParentId(parentIdUpdated);            

		 DsStorageFacade.createOrUpdateRecord(record);

		//Check new updated record is correct.
		DsRecordDto recordUpdated = DsStorageFacade.getRecord(id);
		Assertions.assertEquals(id,recordUpdated .getId());
		Assertions.assertEquals(base,recordUpdated .getBase());
		Assertions.assertTrue(recordUpdated.getDeleted()); //It is now deleted
		Assertions.assertEquals(parentIdUpdated,record.getParentId());        
		Assertions.assertTrue(recordUpdated.getmTime() >recordUpdated.getcTime() ); //Modified is now newer
		Assertions.assertEquals(cTimeBefore, recordUpdated.getcTime());  //Created time is not changed on updae                	                           

	
	}

	@Test
	public void testUnknownBase() throws Exception {
		String id ="id1";
		String base="unkown_base";	    	
		String data = "Hello";
		String parentId="id_1_parent";

		DsRecordDto record = new DsRecordDto();
		record.setId(id);
		record.setBase(base);
		record.setData(data);
		record.setParentId(parentId);
		try {
		DsStorageFacade.createOrUpdateRecord(record );
    		Assertions.fail("Should fail with unknown base");				
		}
		catch(Exception e) { //ignore			
			
		}
		
	}

	
	
	
}
