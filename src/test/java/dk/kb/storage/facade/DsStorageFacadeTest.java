package dk.kb.storage.facade;



import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dk.kb.storage.model.v1.DsRecordDto;
import dk.kb.storage.storage.DsStorageUnitTestUtil;


public class DsStorageFacadeTest extends DsStorageUnitTestUtil{

    
    //THIS UNTTEST MUST BE UPDATED WHEN VALIDATION RULES ARE MORE CLEAR!
    @Test
    public void testInvalidId() throws Exception {
        //TODO describe flow below

        
        String id ="doms.radio:idÆæ123"; // Æ and æ invalid and replaced b y AE and ae
        String origin="doms.radio"; //Must be defined in yaml properties as allowed origin
        String data = "Hello";
        
        DsRecordDto record = new DsRecordDto();
        record.setId(id);
        record.setOrigin(origin);
        record.setData(data);
        DsStorageFacade.createOrUpdateRecord(record );

        //Load and see it is marked invalid
        
        DsRecordDto recordInvalid = DsStorageFacade.getRecord(id);                
        
    }
    
    
    
    @Test
    public void testCreateAndUpdate() throws Exception {
        //TODO describe flow below

        DsRecordDto r1 = DsStorageFacade.getRecord("test.origin:does_not_exist");
        Assertions.assertTrue(r1 == null);

        String id ="doms.radio:id1";
        String origin="doms.radio"; //Must be defined in yaml properties as allowed origin
        String data = "Hello";
        String parentId="doms.radio:id_1_parent";

        DsRecordDto record = new DsRecordDto();
        record.setId(id);
        record.setOrigin(origin);
        record.setData(data);
        record.setParentId(parentId);
        DsStorageFacade.createOrUpdateRecord(record );

        DsRecordDto recordLoaded = DsStorageFacade.getRecord(id);
        Assertions.assertTrue(recordLoaded != null);

        //Load and check values are correct
        Assertions.assertEquals(id,recordLoaded.getId());
        Assertions.assertEquals(origin,recordLoaded.getOrigin());
        Assertions.assertFalse(recordLoaded.getDeleted());
        Assertions.assertEquals(parentId,record.getParentId());        
        Assertions.assertTrue(recordLoaded.getmTime() > 0);
        Assertions.assertEquals(recordLoaded.getcTime(), recordLoaded.getmTime());                  


        //Now update
        String dataUpdate = "Hello updated";
        String parentIdUpdated="doms.radio:id_2_parent";
        long cTimeBefore = recordLoaded.getcTime(); //Must be the same

        record.setData(dataUpdate);
        record.setParentId(parentIdUpdated);            

        DsStorageFacade.createOrUpdateRecord(record);

        //Check new updated record is correct.
        DsRecordDto recordUpdated = DsStorageFacade.getRecord(id);
        Assertions.assertEquals(id,recordUpdated .getId());
        Assertions.assertEquals(origin,recordUpdated .getOrigin());
        Assertions.assertEquals(parentIdUpdated,record.getParentId());        
        Assertions.assertTrue(recordUpdated.getmTime() >recordUpdated.getcTime() ); //Modified is now newer
        Assertions.assertEquals(cTimeBefore, recordUpdated.getcTime());  //Created time is not changed on updae                	                           


    }

    @Test
    public void testUnknownOrigin() throws Exception {
        String id ="unkown.origin:id1";
        String origin="unkown.origin";	    	
        String data = "Hello";

        DsRecordDto record = new DsRecordDto();
        record.setId(id);
        record.setOrigin(origin);
        record.setData(data);
        
        try {
            DsStorageFacade.createOrUpdateRecord(record );
            Assertions.fail("Should fail with unknown origin");				
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
     *   Each test will use an origin that has defined that update strategy:
     * 
     *   The 4 record origin defined in yaml used the config
     *
    - name: origin.strategy.none   
      update_strategy: NONE

    - name: origin.strategy.all    
      update_strategy: ALL

    - name: origin_strategy_child    
      update_strategy: CHILD

    - name: oritin.strategy.parent
      update_strategy: PARENT
     *  
     * The 4 unittest has minor differences in assertions about that is updated
     *   
     */

    @Test
    public void testUpdateStrategy_NONE() throws Exception {
        createTestHierachyParentAndTwoChildren("origin.strategy.none");
        DsRecordDto parentBefore = DsStorageFacade.getRecord("origin.strategy.none:parent");
        DsRecordDto child1Before = DsStorageFacade.getRecord("origin.strategy.none:child1");
        DsRecordDto child2Before = DsStorageFacade.getRecord("origin.strategy.none:child2");

        //Update parent
        parentBefore.setData("parent updated");     
        DsStorageFacade.createOrUpdateRecord(parentBefore);

        DsRecordDto child1After = DsStorageFacade.getRecord("origin.strategy.none:child1");

        //test that child1 does not have changed mTime
        Assertions.assertEquals(child1Before.getmTime(), child1After.getmTime());

        //update child2 and test parent is not touched.
        child2Before.setData("child2 updated");
        parentBefore = DsStorageFacade.getRecord("origin.strategy.none:parent");
        DsStorageFacade.createOrUpdateRecord(child2Before);     
        DsRecordDto parentAfter = DsStorageFacade.getRecord("origin.strategy.none:parent");
        Assertions.assertEquals(parentBefore.getmTime(), parentAfter.getmTime());     
    }


    @Test
    public void testUpdateStrategy_PARENT() throws Exception {
        createTestHierachyParentAndTwoChildren("origin.strategy.parent");
        DsRecordDto parentBefore = DsStorageFacade.getRecord("origin.strategy.parent:parent");
        DsRecordDto child1Before = DsStorageFacade.getRecord("origin.strategy.parent:child1");
        DsRecordDto child2Before = DsStorageFacade.getRecord("origin.strategy.parent:child2");

        //Update parent
        parentBefore.setData("parent updated");     
        DsStorageFacade.createOrUpdateRecord(parentBefore);

        DsRecordDto child1After = DsStorageFacade.getRecord("origin.strategy.parent:child1");

        //test that child1 does not have changed mTime
        Assertions.assertEquals(child1Before.getmTime(), child1After.getmTime());

        //update child2 and test parent is touched
        child2Before.setData("child2 updated");
        parentBefore = DsStorageFacade.getRecord("origin.strategy.parent:parent");
        DsStorageFacade.createOrUpdateRecord(child2Before);     
        DsRecordDto parentAfter = DsStorageFacade.getRecord("origin.strategy.parent:parent");
        Assertions.assertTrue(parentBefore.getmTime() < parentAfter.getmTime()); //Parent has been touched as expected!
    }

    @Test
    public void testUpdateStrategy_CHILD() throws Exception {
        createTestHierachyParentAndTwoChildren("origin.strategy.child");
        DsRecordDto parentBefore = DsStorageFacade.getRecord("origin.strategy.child:parent");
        DsRecordDto child1Before = DsStorageFacade.getRecord("origin.strategy.child:child1");
        DsRecordDto child2Before = DsStorageFacade.getRecord("origin.strategy.child:child2");

        //Update parent
        parentBefore.setData("parent updated");     
        DsStorageFacade.createOrUpdateRecord(parentBefore);

        DsRecordDto child1After = DsStorageFacade.getRecord("origin.strategy.child:child1");

        //test that child11 has new mTime
        Assertions.assertTrue(child1Before.getmTime() < child1After.getmTime());

        //update child2 and test parent is not touched.
        child2Before.setData("child2 updated");
        parentBefore = DsStorageFacade.getRecord("origin.strategy.child:parent");
        DsStorageFacade.createOrUpdateRecord(child2Before);     
        DsRecordDto parentAfter = DsStorageFacade.getRecord("origin.strategy.child:parent");
        Assertions.assertEquals(parentBefore.getmTime(), parentAfter.getmTime()); //Not changed
    }


    @Test
    public void testUpdateStrategy_ALL() throws Exception {
        createTestHierachyParentAndTwoChildren("origin.strategy.all");
        DsRecordDto parentBefore = DsStorageFacade.getRecord("origin.strategy.all:parent");
        DsRecordDto child1Before = DsStorageFacade.getRecord("origin.strategy.all:child1");
        DsRecordDto child2Before = DsStorageFacade.getRecord("origin.strategy.all:child2");

        //Update parent
        parentBefore.setData("parent updated");     
        DsStorageFacade.createOrUpdateRecord(parentBefore);

        DsRecordDto child1After = DsStorageFacade.getRecord("origin.strategy.all:child1");

        //test that child11 has new mTime
        Assertions.assertTrue(child1Before.getmTime() < child1After.getmTime());

        //update child2 and test parent is not touched.
        child2Before.setData("child2 updated");
        parentBefore = DsStorageFacade.getRecord("origin.strategy.all:parent");
        DsStorageFacade.createOrUpdateRecord(child2Before);     
        DsRecordDto parentAfter = DsStorageFacade.getRecord("origin.strategy.all:parent");
        Assertions.assertTrue(parentBefore.getmTime() < parentAfter.getmTime()); //Parent touched
    }
    

    @Test
    public void testIdStartsWithOrigin() throws Exception {
        String id ="origin.unknown:id1";
        String origin="origin.strategy.none";      
        
        String data = "Hello";
        
        DsRecordDto record = new DsRecordDto();
        record.setId(id);
        record.setOrigin(origin);
        record.setData(data);        
        try {
            DsStorageFacade.createOrUpdateRecord(record );
            Assertions.fail("Should fail with recordId does not have origin as prefix");    
        }
        catch(Exception e) { //ignore   
        }
         
        id ="origin.strategy.none:id1";
         record.setId(id);
         try {
            DsStorageFacade.createOrUpdateRecord(record );
          //ignore
        }
        catch(Exception e) {
            e.printStackTrace();
            Assertions.fail("Should not fail since recordId now has origin as prefix",e);
        }
      
    }
    
    
   
    
    @Test
    public void testRecordTree() throws Exception {        
        //Datastructure is a tree with 5 nodes and depth=2. (See method for visualization)       
        //Test 1: Load parent and test tree is correct
        //Test 2: Load of the children at depth 1 and test tree is correct
        //Test 3: Load of the children at depth 2 and test tree is correct
        
        //Setup
        String parentId="doms.aviser:p";
        String child1Id="doms.aviser:c1";
        String child2Id="doms.aviser:c2";
        String child1_1Id="doms.aviser:c1_1";
        String child1_2Id="doms.aviser:c1_2";
        
        createTestDepth2Tree("doms.aviser"); // See this method for visualization of the tree.
        
      
        //Test1, top load parent
     
        DsRecordDto record = DsStorageFacade.getRecordTree(parentId);       
        
        //Check it is parent we have
        Assertions.assertEquals(parentId,record.getId());        
        Assertions.assertTrue(record.getParent() == null);        
      
        //Check children loaded as records
        Assertions.assertEquals(record.getChildren().size(), 2);               
        Assertions.assertEquals(record.getChildren().get(0).getId(), child1Id);
        Assertions.assertEquals(record.getChildren().get(1).getId(), child2Id);
       
        //Check child has parent loaded, both in ID list and as object
        Assertions.assertEquals(record.getChildren().get(0).getParentId(), record.getId());
        Assertions.assertEquals(record.getChildren().get(0).getParent().getId(), record.getId());
        
       // Check depth=2
        Assertions.assertEquals(record.getChildren().get(0).getChildren().get(0).getId(), child1_1Id);
        Assertions.assertEquals(record.getChildren().get(0).getChildren().get(0).getParent().getId(), child1Id);
        
        //Test2, load depth=1 record      
        DsRecordDto child1 = DsStorageFacade.getRecordTree(child1Id);        
 
        //Test the parent is now set
        Assertions.assertEquals(child1.getParentId(), parentId);        
        DsRecordDto parent = child1.getParent();      
        Assertions.assertEquals(parentId,parent.getId());
        Assertions.assertEquals(2,parent.getChildrenIds().size()); //ID list         
        Assertions.assertEquals(2,parent.getChildren().size()); //Record objects               

        //Go from parent down to other child
        DsRecordDto child2 =parent.getChildren().get(1);
        Assertions.assertEquals(child2Id,child2.getId());
        DsRecordDto parentFromChild2 = child2.getParent();
        Assertions.assertEquals(parentId,parentFromChild2.getId());
        
                
        // Test3, load depth=3 record 
        DsRecordDto child1_1 = DsStorageFacade.getRecordTree(child1_1Id);
        DsRecordDto parent1_1 = child1_1.getParent(); 
        Assertions.assertEquals(child1Id,parent1_1.getId());

       
        Assertions.assertEquals(2,parent1_1.getChildrenIds().size());   
        Assertions.assertEquals(2,parent1_1.getChildren().size());
        
    
        DsRecordDto topParent = parent1_1.getParent();
        Assertions.assertEquals(parentId,topParent.getId());
        Assertions.assertEquals(2,topParent.getChildren().size());
    
    }
    

    private void createTestHierachyParentAndTwoChildren(String origin) throws Exception {
        String parentId="parent";

        DsRecordDto parentRecord = new DsRecordDto();
        parentRecord.setId(origin+":"+parentId);
        parentRecord.setOrigin(origin);
        parentRecord.setData("parent data");

        DsRecordDto child1 = new DsRecordDto();
        child1.setId(origin+":"+"child1");
        child1.setOrigin(origin);
        child1.setData("child1 data");
        child1.setParentId(origin+":"+parentId);

        DsRecordDto child2 = new DsRecordDto();
        child2.setId(origin+":"+"child2");
        child2.setOrigin(origin);
        child2.setData("child2 data");
        child2.setParentId(origin+":"+parentId);

        DsStorageFacade.createOrUpdateRecord(parentRecord);
        DsStorageFacade.createOrUpdateRecord(child1);
        DsStorageFacade.createOrUpdateRecord(child2);

    }

    
    /* Create a depth=2 tree
     *  
     * Data structure:
     * The node names  are the Ids in the test. 
     * 
     *           
     *                     p
     *                 /       \
     *               c1        c2
     *               /  \
     *           c1_2     c1_1   
     * 
     */
    private void createTestDepth2Tree(String origin) throws Exception {
        String parentId="p";

        DsRecordDto p = new DsRecordDto();
        
        p.setId(origin+":"+parentId);
        p.setOrigin(origin);
        p.setData("parent data");

        DsRecordDto c1 = new DsRecordDto();
        c1.setId(origin+":"+"c1");
        c1.setOrigin(origin);
        c1.setData("c1 data");
        c1.setParentId(origin+":"+parentId);

        DsRecordDto c2 = new DsRecordDto();
        c2.setId(origin+":"+"c2");
        c2.setOrigin(origin);
        c2.setData("child2 data");
        c2.setParentId(origin+":"+parentId);

        
        DsRecordDto c1_1 = new DsRecordDto();
        c1_1.setId(origin+":"+"c1_1");
        c1_1.setOrigin(origin);
        c1_1.setData("c1_1 data");
        c1_1.setParentId(c1.getId()); //c1 as parent
        
        DsRecordDto c1_2 = new DsRecordDto();
        c1_2.setId(origin+":"+"c1_2");
        c1_2.setOrigin(origin);
        c1_2.setData("c1_2 data");
        c1_2.setParentId(c1.getId()); // c1 as parent
        
        
        DsStorageFacade.createOrUpdateRecord(p);
        DsStorageFacade.createOrUpdateRecord(c1);
        DsStorageFacade.createOrUpdateRecord(c2);
        DsStorageFacade.createOrUpdateRecord(c1_1);
        DsStorageFacade.createOrUpdateRecord(c1_2);

    }

    


}
