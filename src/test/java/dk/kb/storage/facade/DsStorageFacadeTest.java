package dk.kb.storage.facade;



import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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

        record.setData(dataUpdate);
        record.setParentId(parentIdUpdated);            

        DsStorageFacade.createOrUpdateRecord(record);

        //Check new updated record is correct.
        DsRecordDto recordUpdated = DsStorageFacade.getRecord(id);
        Assertions.assertEquals(id,recordUpdated .getId());
        Assertions.assertEquals(base,recordUpdated .getBase());
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

    /*
     *   For all update strategy tests there will be created a parent (p) with two children (c1 and c2).
     *   parentId: parent
     *   child1id: child1
     *   child2id: child2
     *   
     *   Each test will use a recordbase that has defined that update strategy:
     * 
     *   The 4 record bases defined in yaml used the config
     *
    - name: recordBase_stategy_none   
      update_strategy: NONE

    - name: recordBase_strategy_all    
      update_strategy: ALL

    - name: recordBase_strategy_child    
      update_strategy: CHILD

    - name: recordBase_strategy_parent
      update_strategy: PARENT
     *  
     * The 4 unittest has minor differences in assertions about that is updated
     *   
     */

    @Test
    public void testUpdateStrategy_NONE() throws Exception {
        createTestHierachyParentAndTwoChildren("recordBase_stategy_none");
        DsRecordDto parentBefore = DsStorageFacade.getRecord("parent");
        DsRecordDto child1Before = DsStorageFacade.getRecord("child1");
        DsRecordDto child2Before = DsStorageFacade.getRecord("child2");

        //Update parent
        parentBefore.setData("parent updated");     
        DsStorageFacade.createOrUpdateRecord(parentBefore);

        DsRecordDto child1After = DsStorageFacade.getRecord("child1");

        //test that child1 does not have changed mTime
        Assertions.assertEquals(child1Before.getmTime(), child1After.getmTime());

        //update child2 and test parent is not touched.
        child2Before.setData("child2 updated");
        parentBefore = DsStorageFacade.getRecord("parent");
        DsStorageFacade.createOrUpdateRecord(child2Before);     
        DsRecordDto parentAfter = DsStorageFacade.getRecord("parent");
        Assertions.assertEquals(parentBefore.getmTime(), parentAfter.getmTime());     
    }


    @Test
    public void testUpdateStrategy_PARENT() throws Exception {
        createTestHierachyParentAndTwoChildren("recordBase_strategy_parent");
        DsRecordDto parentBefore = DsStorageFacade.getRecord("parent");
        DsRecordDto child1Before = DsStorageFacade.getRecord("child1");
        DsRecordDto child2Before = DsStorageFacade.getRecord("child2");

        //Update parent
        parentBefore.setData("parent updated");     
        DsStorageFacade.createOrUpdateRecord(parentBefore);

        DsRecordDto child1After = DsStorageFacade.getRecord("child1");

        //test that child1 does not have changed mTime
        Assertions.assertEquals(child1Before.getmTime(), child1After.getmTime());

        //update child2 and test parent is touched
        child2Before.setData("child2 updated");
        parentBefore = DsStorageFacade.getRecord("parent");
        DsStorageFacade.createOrUpdateRecord(child2Before);     
        DsRecordDto parentAfter = DsStorageFacade.getRecord("parent");
        Assertions.assertTrue(parentBefore.getmTime() < parentAfter.getmTime()); //Parent has been touched as expected!
    }

    @Test
    public void testUpdateStrategy_CHILD() throws Exception {
        createTestHierachyParentAndTwoChildren("recordBase_strategy_child");
        DsRecordDto parentBefore = DsStorageFacade.getRecord("parent");
        DsRecordDto child1Before = DsStorageFacade.getRecord("child1");
        DsRecordDto child2Before = DsStorageFacade.getRecord("child2");

        //Update parent
        parentBefore.setData("parent updated");     
        DsStorageFacade.createOrUpdateRecord(parentBefore);

        DsRecordDto child1After = DsStorageFacade.getRecord("child1");

        //test that child11 has new mTime
        Assertions.assertTrue(child1Before.getmTime() < child1After.getmTime());

        //update child2 and test parent is not touched.
        child2Before.setData("child2 updated");
        parentBefore = DsStorageFacade.getRecord("parent");
        DsStorageFacade.createOrUpdateRecord(child2Before);     
        DsRecordDto parentAfter = DsStorageFacade.getRecord("parent");
        Assertions.assertEquals(parentBefore.getmTime(), parentAfter.getmTime()); //Not changed
    }


    @Test
    public void testUpdateStrategy_ALL() throws Exception {
        createTestHierachyParentAndTwoChildren("recordBase_strategy_all");
        DsRecordDto parentBefore = DsStorageFacade.getRecord("parent");
        DsRecordDto child1Before = DsStorageFacade.getRecord("child1");
        DsRecordDto child2Before = DsStorageFacade.getRecord("child2");

        //Update parent
        parentBefore.setData("parent updated");     
        DsStorageFacade.createOrUpdateRecord(parentBefore);

        DsRecordDto child1After = DsStorageFacade.getRecord("child1");

        //test that child11 has new mTime
        Assertions.assertTrue(child1Before.getmTime() < child1After.getmTime());

        //update child2 and test parent is not touched.
        child2Before.setData("child2 updated");
        parentBefore = DsStorageFacade.getRecord("parent");
        System.out.println("about to update child2");
        DsStorageFacade.createOrUpdateRecord(child2Before);     
        System.out.println("about to update child2");
        DsRecordDto parentAfter = DsStorageFacade.getRecord("parent");
        Assertions.assertTrue(parentBefore.getmTime() < parentAfter.getmTime()); //Parent touched
    }






    private void createTestHierachyParentAndTwoChildren(String recordBase) throws Exception {
        String parentId="parent";

        DsRecordDto parentRecord = new DsRecordDto();
        parentRecord.setId(parentId);
        parentRecord.setBase(recordBase);
        parentRecord.setData("parent data");

        DsRecordDto child1 = new DsRecordDto();
        child1.setId("child1");
        child1.setBase(recordBase);
        child1.setData("child1 data");
        child1.setParentId(parentId);

        DsRecordDto child2 = new DsRecordDto();
        child2.setId("child2");
        child2.setBase(recordBase);
        child2.setData("child2 data");
        child2.setParentId(parentId);

        DsStorageFacade.createOrUpdateRecord(parentRecord);
        DsStorageFacade.createOrUpdateRecord(child1);
        DsStorageFacade.createOrUpdateRecord(child2);

    }



}
