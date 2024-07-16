package dk.kb.storage.storage;

import dk.kb.storage.model.v1.DsRecordDto;
import dk.kb.storage.model.v1.DsRecordMinimalDto;
import dk.kb.storage.model.v1.MappingDto;
import dk.kb.storage.model.v1.OriginCountDto;
import dk.kb.storage.model.v1.RecordTypeDto;
import dk.kb.storage.util.UniqueTimestampGenerator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


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
        String refenceId="kalturaRefenceId_123";
        String refenceIdUpdated="kalturaRefenceId_123_updated";
        RecordTypeDto recordType=RecordTypeDto.MANIFESTATION;
        
        DsRecordDto record = new DsRecordDto();
        record.setId(id);
        record.setOrigin(origin);
        record.setData(data);
        record.setParentId(parentId);
        record.setRecordType(RecordTypeDto.MANIFESTATION);
        record.setReferenceId(refenceId);
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
        Assertions.assertEquals(refenceId, recordLoaded.getReferenceId());
        
        //Now update

        String dataUpdate = "Hello updated";
        String parentIdUpdated="origin.test:id_2_parent";
        long cTimeBefore = recordLoaded.getcTime(); //Must be the same

        record.setData(dataUpdate);
        record.setParentId(parentIdUpdated);            
        record.setReferenceId(refenceIdUpdated);
        storage.updateRecord(record);

        //Check new updated record is correct.
        DsRecordDto recordUpdated = storage.loadRecord(id);

        Assertions.assertEquals(id,recordUpdated .getId());
        Assertions.assertEquals(origin,recordUpdated .getOrigin());
        Assertions.assertEquals(parentIdUpdated,record.getParentId());        
        Assertions.assertTrue(recordUpdated.getmTime() >recordUpdated.getcTime() ); //Modified is now newer
        Assertions.assertEquals(cTimeBefore, recordUpdated.getcTime());  //Created time is not changed on updae                                            
        Assertions.assertEquals(refenceIdUpdated, recordUpdated.getReferenceId());
        
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
    public void testMappingCRUD() throws Exception {
        
        //Create with both ids
        String id1="test_id_1";
        String id2="test_id_2";
        String kalturaId1="test_kalturaid_1";
        String kalturaId1Updated="test_kalturaid_1_updated";
        
        MappingDto mappingDto1 = new MappingDto();
        mappingDto1.setReferenceId(id1);
        mappingDto1.setKalturaId(kalturaId1);    
        storage.createNewMapping(mappingDto1);
        
        //create with only id
        MappingDto mappingDto2 = new MappingDto();
        mappingDto2.setReferenceId(id2);
        storage.createNewMapping(mappingDto2);
        
        //Load records
        MappingDto map1 = storage.getMappingByReferenceId(id1);
        assertEquals(id1,map1.getReferenceId());
        assertEquals(kalturaId1,map1.getKalturaId());
                
        MappingDto map2 = storage.getMappingByReferenceId(id2);
        assertEquals(id2,map2.getReferenceId());
        assertNull(map2.getKalturaId());        
       
        //Test id not exists
        MappingDto notExist = storage.getMappingByReferenceId("does_not_exist");
        assertNull(notExist);    
        
        //update
        mappingDto1.setKalturaId(kalturaId1Updated);
        storage.updateMapping(mappingDto1);
        MappingDto map1Updated = storage.getMappingByReferenceId(id1); //load again
        assertEquals(kalturaId1Updated,map1Updated.getKalturaId());                        
        
    }
        
    @Test
    public void testUpdateKalturaId() throws Exception {
        String recordId="test_123";
        String kalturaReferenceId="kaltura_ref_123";
        String kalturaId="kaltura_id_123";
        DsRecordDto record = new DsRecordDto();
        record.setId(recordId);
        record.setOrigin("origin_123");
        record.setData("");        
        record.setRecordType(RecordTypeDto.MANIFESTATION);
        record.setReferenceId(kalturaReferenceId);
        storage.createNewRecord(record );
        
        //Update kaltura Id.
        storage.updateKalturaIdForRecord(kalturaReferenceId, kalturaId);
     
        //Load and test kalturaId correct
        DsRecordDto recordUpdated = storage.loadRecord(recordId);
        assertEquals(kalturaReferenceId,recordUpdated.getReferenceId());
        assertEquals(kalturaId,recordUpdated.getKalturaId());
    }


    @Test
    public void testReferenceId() throws Exception {
        String data="some data";
        String recordId="id_123";
        String referenceId="reference_123";        
        DsRecordDto record = new DsRecordDto();
        record.setId(recordId);
        record.setOrigin("origin_123");
        record.setData(data);        
        record.setRecordType(RecordTypeDto.MANIFESTATION);        
        storage.createNewRecord(record );
        
        //Update refereneId Id.
        storage.updateReferenceIdForRecord(recordId,referenceId);
     
        //Load and test referenceId is correct
        DsRecordDto recordUpdated = storage.loadRecord(recordId);
        assertEquals(referenceId,recordUpdated.getReferenceId());       
        assertEquals(data,recordUpdated.getData());//Data not modified
    }

    
    @Test
    public void testGetMtimeAfterWithLimit() throws Exception {
        String parentId="test.origin:mega_parent_id";
        long beforeTime = UniqueTimestampGenerator.next();
        createMegaParent(parentId,"test.origin");
        long afterTime = UniqueTimestampGenerator.next();

        long maxTime = storage.getMaxMtime("test.origin");
        assertTrue(maxTime > beforeTime, "Max time should be higher than before time");
        assertTrue(maxTime < afterTime, "Max time should be lower than after time");
    }

    @Test
    public void testGetMtimeAfterWithLimitCollection() throws Exception {
        String parentId="test.origin:mega_parent_id";
        long beforeTime = UniqueTimestampGenerator.next();
        createMegaParent(parentId,"test.origin");
        long afterTime = UniqueTimestampGenerator.next();

        long maxTime = storage.getMaxMtime("test.origin", RecordTypeDto.COLLECTION);
        assertTrue(maxTime > beforeTime, "Max time should be higher than before time");
        assertTrue(maxTime < afterTime, "Max time should be lower than after time");
    }
    
    
    
    /**
    * Data structure
    * 
    * RECORD table:
    * |ID |REFERENCEID |KALTURAID
    * -------------------------------
    * |id1|referenceid1|     (null) |        (This can be enriched with kalturaid)
    * |id2|referenceid2|     (null) |        (this record needs to be enriched, but referenceid2 is not in the mapping table) 
    * |id3|null        |     (null) |        (this record can not be enriched with kalturaid, no referenceId)
    * |id4|referenceid4| kalturaid4 |        (this  already has kalturaid)
    * 
    * MAPPING table:
    * |REFERENCEID | KALTURAID|
    * ------------------------- 
    * |referenceid1|kalturaid1|              (this mappen can be used to enrich record with id1)
    * |referenceid4|kalturaid4|              (no record that has referenceid4, so not used)
    *   
    * This is the result of the SQL inner join, only one row is generated from data structure
    * 
    * JOIN-RESULT-SET
    * |ID| REFERENCEID | KALTURAID|
    * -----------------------------
    * |id1|referenceid1|kalturaid1|   
    * 
    * After updating the kalturaid the record with id1 will now have kalturaid
    * |id1|referenceid1|kalturaid1|          (this is the change in record data by calling the updatekalturaId method)
    */    
    @Test
    public void testUpdateKalturaIdForRecords() throws Exception {
        
        //Create test data
        createTestMappingData();
       
        String recordId="id1";
        //load record1 and see kalturaid is not set
        DsRecordDto recordBefore = storage.loadRecord(recordId);
        assertNull(recordBefore.getKalturaId());
        
        
        int updated = storage.updateKalturaIdForRecords();        
        assertEquals(1,updated);
        
        //load record1 and see kalturaid is now set
        DsRecordDto recordAfter = storage.loadRecord(recordId);
        assertEquals("kalturaid1",recordAfter.getKalturaId()); 
        
        
        //batch over records and records are extract with correct values              
        ArrayList<DsRecordMinimalDto> records = storage.getReferenceIds("test_origin1", 0, 10);
        
        //records order are id2,id3,id4,id1  (since id1 was modified it is last)
        DsRecordMinimalDto id2 = records.get(0);        
        assertEquals("id2",id2.getId());
        assertEquals("referenceid2",id2.getReferenceId());
        assertNull(id2.getKalturaId());
        
        DsRecordMinimalDto id3 = records.get(1);        
        assertEquals("id3",id3.getId());
        assertNull(id3.getReferenceId());
        assertNull(id3.getKalturaId());
        
        DsRecordMinimalDto id4 = records.get(2);        
        assertEquals("id4",id4.getId());
        assertEquals("referenceid4",id4.getReferenceId());
        assertEquals("kalturaid4",id4.getKalturaId());
                
        DsRecordMinimalDto id1 = records.get(3);        
        assertEquals("id1",id1.getId());        
        assertEquals("referenceid1",id1.getReferenceId());
        assertEquals("kalturaid1",id1.getKalturaId());   
        assertTrue(id1.getmTime()>0L);
    }

    @Test
    public void testGetMtimeAfterWithLimitManifestation() throws Exception {
        String parentId="test.origin:mega_parent_id";
        long beforeTime = UniqueTimestampGenerator.next();
        createMegaParent(parentId,"test.origin");
        long afterTime = UniqueTimestampGenerator.next();

        long maxTime = storage.getMaxMtime("test.origin", RecordTypeDto.MANIFESTATION);
        assertTrue(maxTime > beforeTime, "Max time should be higher than before time");
        assertTrue(maxTime < afterTime, "Max time should be lower than after time");
    }

    @Test
    public void testHighestModified() throws Exception {
        String parentId="test.origin:mega_parent_id";
        long beforeTime = UniqueTimestampGenerator.next();
        createMegaParent(parentId,"test.origin");
        long afterTime = UniqueTimestampGenerator.next();

        long maxBefore = storage.getMaxMtimeAfter("test.origin", beforeTime, 100).getLeft();
        long maxMiddle = storage.getMaxMtimeAfter("test.origin", (beforeTime+afterTime)/2, 100).getLeft();
        Long maxAfter = storage.getMaxMtimeAfter("test.origin", afterTime, 100).getLeft();
        
        assertTrue(beforeTime < maxBefore, "Max mTime with start before should be after beforeTime");
        assertTrue(maxBefore < afterTime, "Max mTime with start before should be before afterTime");
        assertTrue(maxBefore < maxMiddle, "Max mTime with start beforeTime should be before max mTime with start in the middle");
        assertTrue(maxMiddle < afterTime, "Max mTime with start in the middle should be before afterTime");
        assertEquals(0, maxAfter, "Max mTime with start afterTime should be 0");
        List<DsRecordDto> bRecords = storage.getRecordsModifiedAfter("test.origin", beforeTime, 100);

//        System.out.println("First record: " + bRecords.get(0).getmTime());
//        System.out.println("Stated mTime: " + maxBefore);
//        System.out.println("Last  record: " + bRecords.get(bRecords.size()-1).getmTime());

        assertEquals(maxBefore, bRecords.get(bRecords.size()-1).getmTime(),
                     "The mTime for the last bRecord should match maxBefore");
    }

    @Test
    public void testHighestModifiedManifestation() throws Exception {
        String parentId="test.origin:mega_parent_id";
        long beforeTime = UniqueTimestampGenerator.next();
        createMegaParent(parentId,"test.origin");
        long afterTime = UniqueTimestampGenerator.next();

        long maxBefore = storage.getMaxMtimeAfter("test.origin", RecordTypeDto.MANIFESTATION, beforeTime, 100).getLeft();
        long maxMiddle = storage.getMaxMtimeAfter("test.origin", RecordTypeDto.MANIFESTATION, (beforeTime+afterTime)/2, 100).getLeft();
        Long maxAfter = storage.getMaxMtimeAfter("test.origin", RecordTypeDto.MANIFESTATION, afterTime, 100).getLeft();

        assertTrue(beforeTime < maxBefore, "Max mTime with start before should be after beforeTime");
        assertTrue(maxBefore < afterTime, "Max mTime with start before should be before afterTime");
        assertTrue(maxBefore < maxMiddle, "Max mTime with start beforeTime should be before max mTime with start in the middle");
        assertTrue(maxMiddle < afterTime, "Max mTime with start in the middle should be before afterTime");
        assertEquals(0, maxAfter, "Max mTime with start afterTime should be 0");
        List<String> bRecords = storage.getRecordsIdsByRecordTypeModifiedAfter("test.origin", RecordTypeDto.MANIFESTATION, beforeTime, 100);

//        System.out.println("First record: " + bRecords.get(0).getmTime());
//        System.out.println("Stated mTime: " + maxBefore);
//        System.out.println("Last  record: " + bRecords.get(bRecords.size()-1).getmTime());

        assertEquals(maxBefore, storage.loadRecord(bRecords.get(bRecords.size()-1)).getmTime(),
                     "The mTime for the last bRecord should match maxBefore");
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
    public void testDeleteRecordsForOrigin() throws Exception {
        String parentId="test.origin:mega_parent_id";
        long before = UniqueTimestampGenerator.next();
        createMegaParent(parentId,"test.origin");

        //Test they are created
        ArrayList<DsRecordDto> list1 = storage.getRecordsModifiedAfter("test.origin", before, 10000);
        assertEquals(1001, list1.size()); //1000 children +1 parent
        
         //Delete those before (empty set). 
        int deleted= storage.deleteRecordsForOrigin("test.origin", 0L, before);
        assertEquals(0,deleted);

        //still 1001
        ArrayList<DsRecordDto> list2 = storage.getRecordsModifiedAfter("test.origin", before, 10000);
        assertEquals(1001, list2.size()); //1000 children +1 parent
        
        //Now delete all
        long after = UniqueTimestampGenerator.next();
        deleted= storage.deleteRecordsForOrigin("test.origin", 0L, after);
        assertEquals(1001,deleted);
        
        //None left
        ArrayList<DsRecordDto> list3 = storage.getRecordsModifiedAfter("test.origin", before, 10000);
        assertEquals(0, list3.size());


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

        //Load with children and record at once.
        DsRecordDto recordsWithChildren = storage.loadRecordWithChildIds(parentId);                         
        Assertions.assertEquals(1000, recordsWithChildren.getChildrenIds().size());                
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
            child.setRecordType(RecordTypeDto.MANIFESTATION);

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
        r1.setRecordType(RecordTypeDto.MANIFESTATION);
        storage.createNewRecord(r1);

        DsRecordDto r2 = new DsRecordDto();
        r2.setId("Id2");
        r2.setOrigin("test_origin1");
        r2.setData("id2 text");
        r2.setRecordType(RecordTypeDto.MANIFESTATION);
        storage.createNewRecord(r2);

        DsRecordDto r3 = new DsRecordDto();
        r3.setId("Id3");
        r3.setOrigin("test_origin2");
        r3.setData("id3 text");
        r3.setRecordType(RecordTypeDto.MANIFESTATION);        
        storage.createNewRecord(r3);

        DsRecordDto r4 = new DsRecordDto();
        r4.setId("Id4");
        r4.setOrigin("test_origin3");
        r4.setData("id4 text");
        r4.setRecordType(RecordTypeDto.MANIFESTATION);
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
    
   /*
    * See {@link #testUpdateKalturaIdForRecords() testUpdateKalturaIdForRecords} method for data visualization
    *       
    */   
    private void createTestMappingData()  throws Exception{
                
        DsRecordDto r1 = new DsRecordDto();
        r1.setId("id1"); 
        r1.setOrigin("test_origin1");
        r1.setData("id1");
        r1.setRecordType(RecordTypeDto.MANIFESTATION);
        r1.setReferenceId("referenceid1");
        storage.createNewRecord(r1);
        
        DsRecordDto r2 = new DsRecordDto();
        r2.setId("id2");  
        r2.setOrigin("test_origin1");
        r2.setData("id2");
        r2.setRecordType(RecordTypeDto.MANIFESTATION);
        r2.setReferenceId("referenceid2");
        storage.createNewRecord(r2);
        
        DsRecordDto r3 = new DsRecordDto();
        r3.setId("id3");  
        r3.setOrigin("test_origin1");
        r3.setData("id3");
        r3.setRecordType(RecordTypeDto.MANIFESTATION);
        //No referenceid
        storage.createNewRecord(r3);
        
        DsRecordDto r4 = new DsRecordDto();
        r4.setId("id4");  
        r4.setOrigin("test_origin1");
        r4.setData("id4");
        r4.setRecordType(RecordTypeDto.MANIFESTATION);
        r4.setReferenceId("referenceid4");
        r4.setKalturaId("kalturaid4");
        storage.createNewRecord(r4);
        
        MappingDto map = new MappingDto();
        map.setReferenceId("referenceid1");
        map.setKalturaId("kalturaid1");            
        storage.createNewMapping(map);       
    }
           
    
}
