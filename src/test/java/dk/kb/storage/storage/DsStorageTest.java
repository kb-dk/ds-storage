package dk.kb.storage.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.kb.storage.config.ServiceConfig;
import dk.kb.storage.model.v1.DsRecordDto;
import dk.kb.storage.model.v1.RecordBaseCountDto;
import dk.kb.storage.util.UniqueTimestampGenerator;



public class DsStorageTest extends DsStorageUnitTestUtil{

	private static final Logger log = LoggerFactory.getLogger(DsStorageTest.class);

    //Storage does not commit automatic, so just rollback to get empty table for next test
	@BeforeEach
	public void beforeEach() throws Exception {	        	    		    	
		storage.rollback(); //Important so each unittest has clean table
	}


	@Test
	public void testBasicCRUD() throws Exception {
     //TODO rescribe flow below
		
        //Test record not exist
		assertFalse(storage.recordExists("unknown_id"));
	
		String id ="id1";
		String base="base_test";	    	
		String data = "Hello";
		String parentId="id_1_parent";

		DsRecordDto record = new DsRecordDto();
		record.setId(id);
		record.setBase(base);
		record.setData(data);
		record.setParentId(parentId);
		storage.createNewRecord(record );
		
	    //Test record not exist
 	    assertTrue(storage.recordExists(id));
		

		//Load and check values are correct
		DsRecordDto recordLoaded = storage.loadRecord(id);
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

		record.setData(dataUpdate);
		record.setParentId(parentIdUpdated);            

		storage.updateRecord(record);

		//Check new updated record is correct.
		DsRecordDto recordUpdated = storage.loadRecord(id);

		Assertions.assertEquals(id,recordUpdated .getId());
		Assertions.assertEquals(base,recordUpdated .getBase());
		Assertions.assertEquals(parentIdUpdated,record.getParentId());        
		Assertions.assertTrue(recordUpdated.getmTime() >recordUpdated.getcTime() ); //Modified is now newer
		Assertions.assertEquals(cTimeBefore, recordUpdated.getcTime());  //Created time is not changed on updae                	                           
	   
		//Mark record for delete				
		storage.markRecordForDelete(id);
		
		DsRecordDto record_deleted = storage.loadRecord(id);
		Assertions.assertTrue(record_deleted.getDeleted());
	
		//MTime must also be updated when mark for delete  
		Assertions.assertTrue(recordUpdated.getmTime() < record_deleted.getmTime());		
		
		//Update it and deleted flag should be removed
  record.setData("bla bla bla2");
		storage.updateRecord(record);		
		DsRecordDto record_updated_after_delete = storage.loadRecord(id);
		Assertions.assertFalse(record_updated_after_delete.getDeleted());				
	
		//delete if marked for delete.
		int deleted = storage.deleteMarkedForDelete("base_test");
	 Assertions.assertEquals(0,deleted); //Was not marked for deletes
		
	 //Mark record for delete again
  storage.markRecordForDelete(id);
  deleted = storage.deleteMarkedForDelete("base_test");
  Assertions.assertEquals(1,deleted); //Now it is deleted
		
  DsRecordDto deletedReally = storage.loadRecord(id);
  Assertions.assertNull(deletedReally);
    
	}
	
	@Test
	public void testGetModifiedAfterParentsOnly() throws Exception {	    
		String parentId="mega_parent_id";	          	        
		long before = UniqueTimestampGenerator.next();


		createMegaParent(parentId);	

		ArrayList<DsRecordDto> list1 = storage.getModifiedAfterParentsOnly("does_not_exist", before, 100);
		assertEquals(0, list1.size());


		ArrayList<DsRecordDto> list2 = storage.getModifiedAfterParentsOnly("test_base", before, 100);
		assertEquals(1, list2.size());

		//Noone after last
		long lastModified = list2.get(0).getmTime();

		ArrayList<DsRecordDto> list3 = storage.getModifiedAfterParentsOnly("test_base", lastModified, 100);
		assertEquals(0, list3.size());	    	 	    	 	    	 
	}


	@Test
	public void testGetModifiedAfterChildrenOnly() throws Exception {	    
		String parentId="mega_parent_id";	          	        
		long before = UniqueTimestampGenerator.next();

		createMegaParent(parentId);	

		ArrayList<DsRecordDto> list1 = storage.getModifiedAfterChildrenOnly("does_not_exist", before, 1000);
		assertEquals(0, list1.size());	    	 

		ArrayList<DsRecordDto> list2 = storage.getModifiedAfterChildrenOnly("test_base", before, 1000);
		assertEquals(1000, list2.size());

		//Noone after last
		long lastModified = list2.get(999).getmTime();

		ArrayList<DsRecordDto> list3 = storage.getModifiedAfterChildrenOnly("test_base", lastModified, 1000);
		assertEquals(0, list3.size());

		//Test Pagination (cursor)
		//only get 500
		ArrayList<DsRecordDto> list4 = storage.getModifiedAfterChildrenOnly("test_base", before, 500);
		assertEquals(500, list4.size());

		//get next 500
		long nextTime = list4.get(499).getmTime();	    
		ArrayList<DsRecordDto> list5 = storage.getModifiedAfterChildrenOnly("test_base", nextTime, 500);
		assertEquals(500, list5.size());

		//And no more
		nextTime = list5.get(499).getmTime();	    	 
		ArrayList<DsRecordDto> list6 = storage.getModifiedAfterChildrenOnly("test_base", nextTime, 500);
		assertEquals(0, list6.size());	    	 
	}


	@Test
	public void testGetModifiedAfter() throws Exception {	    
		String parentId="mega_parent_id";	          	        
		long before = UniqueTimestampGenerator.next();

		createMegaParent(parentId);	

		ArrayList<DsRecordDto> list1 = storage.getRecordsModifiedAfter("test_base", before, 10000);
		assertEquals(1001, list1.size()); //100 children +1 parent	    	 	    		    
	}	    

	/*
	 * Example of parent with 1K children
	 */
	@Test
	public void testManyChildren1K() throws Exception{	    		    		   
		String parentId="mega_parent_id";	  
		createMegaParent(parentId);

		ArrayList<String> childIds = storage.getChildrenIds(parentId);
		assertEquals(1000, childIds.size());	        	      
	}

	/*
	 * Created a record with 1000 children
	 */
	private void createMegaParent(String id)  throws Exception{


		DsRecordDto megaParent = new DsRecordDto();
		megaParent.setId(id);
		megaParent.setBase("test_base");
		megaParent.setData("mega parent data");
		megaParent.setParentId(null);

		storage.createNewRecord(megaParent);

		for (int i=1;i<=1000;i++){	        
			DsRecordDto child = new DsRecordDto();
			child.setId("child"+i);
			child.setBase("test_base");
			child.setData("child data "+i);
			child.setParentId(id);


			storage.createNewRecord(child);	            	        
		}

	}

	@Test
	public void testBaseStatistics() throws Exception{

		//3 different bases. 2 records in of them 
		DsRecordDto r1 = new DsRecordDto();
				r1.setId("Id1");
		r1.setBase("test_base1");
		r1.setData("id1 text");   				    	
		storage.createNewRecord(r1);

		DsRecordDto r2 = new DsRecordDto();
		r2.setId("Id2");
		r2.setBase("test_base1");
		r2.setData("id2 text");   				    	
		storage.createNewRecord(r2);

		DsRecordDto r3 = new DsRecordDto();
		r3.setId("Id3");
		r3.setBase("test_base2");
		r3.setData("id3 text");   				    	
		storage.createNewRecord(r3);

		DsRecordDto r4 = new DsRecordDto();
		r4.setId("Id4");
		r4.setBase("test_base3");
		r4.setData("id4 text");   				    	
		storage.createNewRecord(r4);

		 ArrayList<RecordBaseCountDto> baseStatisticsList = storage.getBaseStatictics();
		 Comparator<RecordBaseCountDto> compareByRecordbase = Comparator.comparing( RecordBaseCountDto :: getRecordBase);
		 Collections.sort(baseStatisticsList, compareByRecordbase);
		 assertEquals(3,baseStatisticsList.size());
  //Sort so we know order
		
		 RecordBaseCountDto item0 = baseStatisticsList.get(0);
	  assertEquals(2,item0.getCount());
   assertEquals("test_base1",item0.getRecordBase());
		 
   RecordBaseCountDto item1 = baseStatisticsList.get(1);
   assertEquals(1,item1.getCount());
   assertEquals("test_base2",item1.getRecordBase());
   
   RecordBaseCountDto item2 = baseStatisticsList.get(2);
   assertEquals(1,item2.getCount());
   assertEquals("test_base3",item2.getRecordBase());
	        	     
	}


}
