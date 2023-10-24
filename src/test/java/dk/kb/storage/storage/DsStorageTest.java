package dk.kb.storage.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;


import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.kb.storage.model.v1.DsRecordDto;
import dk.kb.storage.model.v1.OriginCountDto;
import dk.kb.storage.model.v1.RecordTypeDto;
import dk.kb.storage.util.UniqueTimestampGenerator;



public class DsStorageTest extends DsStorageUnitTestUtil{

    private static final Logger log = LoggerFactory.getLogger(DsStorageTest.class);
  

    @Test
    public void testBasicCRUD() throws Exception {
        //TODO rescribe flow below

        //Test record not exist
        assertFalse(storage.recordExists("origin:unknown"));

        String id ="origin.test:id1";
        String origin="origin.test";	    	
        String data = "Hello";
        String parentId="origin.test:id_1_parent";
        RecordTypeDto recordType=RecordTypeDto.METADATA;
        
        DsRecordDto record = new DsRecordDto();
        record.setId(id);
        record.setOrigin(origin);
        record.setData(data);
        record.setParentId(parentId);
        record.setRecordType(RecordTypeDto.METADATA);
        storage.createNewRecord(record );

        //Test record not exist
        assertTrue(storage.recordExists(id));


        //Load and check values are correct
        DsRecordDto recordLoaded = storage.loadRecord(id);
        Assertions.assertEquals(id,recordLoaded.getId());
        Assertions.assertEquals(origin,recordLoaded.getOrigin());
        Assertions.assertFalse(recordLoaded.getDeleted());
        Assertions.assertEquals(parentId,record.getParentId());        
        Assertions.assertTrue(recordLoaded.getmTime() > 0);
        Assertions.assertEquals(recordLoaded.getcTime(), recordLoaded.getmTime());                  
        Assertions.assertEquals(recordType, recordLoaded.getRecordType());

        //Now update

        String dataUpdate = "Hello updated";
        String parentIdUpdated="origin.test:id_2_parent";
        long cTimeBefore = recordLoaded.getcTime(); //Must be the same

        record.setData(dataUpdate);
        record.setParentId(parentIdUpdated);            

        storage.updateRecord(record);

        //Check new updated record is correct.
        DsRecordDto recordUpdated = storage.loadRecord(id);

        Assertions.assertEquals(id,recordUpdated .getId());
        Assertions.assertEquals(origin,recordUpdated .getOrigin());
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

        //test updateMtime
        int updated = storage.updateMTimeForRecord(id);
        Assertions.assertEquals(1,updated);
        DsRecordDto record_after_mtime_touch = storage.loadRecord(id);
        Assertions.assertTrue(record_after_mtime_touch.getmTime() > record_updated_after_delete.getmTime());


        //delete if marked for delete.
        int deleted = storage.deleteMarkedForDelete("origin.test");
        Assertions.assertEquals(0,deleted); //Was not marked for deletes

        //Mark record for delete again
        storage.markRecordForDelete(id);
        deleted = storage.deleteMarkedForDelete("origin.test");
        Assertions.assertEquals(1,deleted); //Now it is deleted

        DsRecordDto deletedReally = storage.loadRecord(id);
        Assertions.assertNull(deletedReally);

    }

    @Test
    public void testGetModifiedAfterParentsOnly() throws Exception {	    
        String parentId="test.origin:mega_parent_id";	          	        
        long before = UniqueTimestampGenerator.next();


        createMegaParent(parentId,"test.origin");	

        ArrayList<DsRecordDto> list1 = storage.getModifiedAfterParentsOnly("test.origin:does_not_exist", before, 100);
        assertEquals(0, list1.size());


        ArrayList<DsRecordDto> list2 = storage.getModifiedAfterParentsOnly("test.origin", before, 100);
        assertEquals(1, list2.size());

        //Noone after last
        long lastModified = list2.get(0).getmTime();

        ArrayList<DsRecordDto> list3 = storage.getModifiedAfterParentsOnly("test.origin", lastModified, 100);
        assertEquals(0, list3.size());	    	 	    	 	    	 
    }


    @Test
    public void testGetModifiedAfterChildrenOnly() throws Exception {	    
        String parentId="test.origin:mega_parent_id";	          	        
        long before = UniqueTimestampGenerator.next();

        createMegaParent(parentId,"test.origin");	

        ArrayList<DsRecordDto> list1 = storage.getModifiedAfterChildrenOnly("test.origin.unknown", before, 1000);
        assertEquals(0, list1.size());	    	 

        ArrayList<DsRecordDto> list2 = storage.getModifiedAfterChildrenOnly("test.origin", before, 1000);
        assertEquals(1000, list2.size());

        //Noone after last
        long lastModified = list2.get(999).getmTime();

        ArrayList<DsRecordDto> list3 = storage.getModifiedAfterChildrenOnly("test.origin", lastModified, 1000);
        assertEquals(0, list3.size());

        //Test Pagination (cursor)
        //only get 500
        ArrayList<DsRecordDto> list4 = storage.getModifiedAfterChildrenOnly("test.origin", before, 500);
        assertEquals(500, list4.size());

        //get next 500
        long nextTime = list4.get(499).getmTime();	    
        ArrayList<DsRecordDto> list5 = storage.getModifiedAfterChildrenOnly("test.origin", nextTime, 500);
        assertEquals(500, list5.size());

        //And no more
        nextTime = list5.get(499).getmTime();	    	 
        ArrayList<DsRecordDto> list6 = storage.getModifiedAfterChildrenOnly("test.origin", nextTime, 500);
        assertEquals(0, list6.size());	    	 
    }


    @Test
    public void testGetModifiedAfter() throws Exception {	    
        String parentId="test.origin:mega_parent_id";	          	        
        long before = UniqueTimestampGenerator.next();

        createMegaParent(parentId,"test.origin");	

        ArrayList<DsRecordDto> list1 = storage.getRecordsModifiedAfter("test.origin", before, 10000);
        assertEquals(1001, list1.size()); //100 children +1 parent	    	 	    		    
    }	    

    /*
     * Example of parent with 1K children
     */
    @Test
    public void testManyChildren1K() throws Exception{	    		    		   
        String parentId="mega_parent_id";	  
        createMegaParent(parentId,"test.origin");

        ArrayList<String> childIds = storage.getChildrenIds(parentId);
        assertEquals(1000, childIds.size());	        	      
    }

    /*
     * Created a record with 1000 children
     */
    private void createMegaParent(String id,String origin)  throws Exception{


        DsRecordDto megaParent = new DsRecordDto();
        megaParent.setId(id);
        megaParent.setOrigin(origin);
        megaParent.setData("mega_parent_data");
        megaParent.setParentId(null);
        megaParent.setRecordType(RecordTypeDto.COLLECTION);
        
        storage.createNewRecord(megaParent);

        for (int i=1;i<=1000;i++){	        
            DsRecordDto child = new DsRecordDto();
            child.setId(origin+":child"+i);
            child.setOrigin(origin);
            child.setData("child data "+i);
            child.setParentId(id);
            child.setRecordType(RecordTypeDto.METADATA);

            storage.createNewRecord(child);	            	        
        }

    }

    @Test
    public void testOriginStatistics() throws Exception{

        //3 different origins. 2 records in of them 
        DsRecordDto r1 = new DsRecordDto();
        r1.setId("Id1"); //TODO 
        r1.setOrigin("test_origin1");
        r1.setData("id1 text");   			
        r1.setRecordType(RecordTypeDto.METADATA);
        storage.createNewRecord(r1);

        DsRecordDto r2 = new DsRecordDto();
        r2.setId("Id2");
        r2.setOrigin("test_origin1");
        r2.setData("id2 text");   				    	
        r2.setRecordType(RecordTypeDto.METADATA);
        storage.createNewRecord(r2);

        DsRecordDto r3 = new DsRecordDto();
        r3.setId("Id3");
        r3.setOrigin("test_origin2");
        r3.setData("id3 text");   				    	
        r3.setRecordType(RecordTypeDto.METADATA);        
        storage.createNewRecord(r3);

        DsRecordDto r4 = new DsRecordDto();
        r4.setId("Id4");
        r4.setOrigin("test_origin3");
        r4.setData("id4 text");
        r4.setRecordType(RecordTypeDto.METADATA);
        storage.createNewRecord(r4);

        ArrayList<OriginCountDto> originStatisticsList = storage.getOriginStatictics();
        Comparator<OriginCountDto> compareByOrigin = Comparator.comparing( OriginCountDto :: getOrigin);
        Collections.sort(originStatisticsList, compareByOrigin);
        assertEquals(3,originStatisticsList.size());
        //Sort so we know order

        OriginCountDto item0 = originStatisticsList.get(0);
        assertEquals(2,item0.getCount());
        assertEquals("test_origin1",item0.getOrigin());

        OriginCountDto item1 = originStatisticsList.get(1);
        assertEquals(1,item1.getCount());
        assertEquals("test_origin2",item1.getOrigin());

        OriginCountDto item2 = originStatisticsList.get(2);
        assertEquals(1,item2.getCount());
        assertEquals("test_origin3",item2.getOrigin());

    }


}
