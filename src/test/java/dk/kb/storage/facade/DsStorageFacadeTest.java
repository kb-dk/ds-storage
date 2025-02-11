package dk.kb.storage.facade;


import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;


import dk.kb.storage.model.v1.DsRecordDto;
import dk.kb.storage.model.v1.MappingDto;
import dk.kb.storage.model.v1.RecordTypeDto;
import dk.kb.storage.storage.DsStorageUnitTestUtil;
import dk.kb.util.webservice.exception.InternalServiceException;

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
        record.setRecordType(RecordTypeDto.MANIFESTATION);
        DsStorageFacade.createOrUpdateRecord(record );

        //Load and see it is marked invalid
        
        DsRecordDto recordInvalid = DsStorageFacade.getRecord(id,false);                       
    }
    
    
    
    @Test
    public void testCreateAndUpdate() throws Exception {
        //TODO describe flow below

        try {
          DsRecordDto r1 = DsStorageFacade.getRecord("test.origin:does_not_exist",false);
          assertNull(r1); 
        }
        catch (Exception e) {
         //ignore    
        }
        

        String id ="doms.radio:id1";
        String origin="doms.radio"; //Must be defined in yaml properties as allowed origin
        String data = "Hello";
        String parentId="doms.radio:id_1_parent";

        DsRecordDto record = new DsRecordDto();
        record.setId(id);
        record.setOrigin(origin);
        record.setData(data);
        record.setParentId(parentId);
        record.setRecordType(RecordTypeDto.MANIFESTATION);
        DsStorageFacade.createOrUpdateRecord(record );

        DsRecordDto recordLoaded = DsStorageFacade.getRecord(id,false);
        assertTrue(recordLoaded != null);

        //Load and check values are correct
        assertEquals(id,recordLoaded.getId());
        assertEquals(origin,recordLoaded.getOrigin());
        assertFalse(recordLoaded.getDeleted());
        assertEquals(parentId,record.getParentId());        
        assertTrue(recordLoaded.getmTime() > 0);
        assertEquals(recordLoaded.getcTime(), recordLoaded.getmTime());                  


        //Now update
        String dataUpdate = "Hello updated";
        String parentIdUpdated="doms.radio:id_2_parent";
        long cTimeBefore = recordLoaded.getcTime(); //Must be the same

        record.setData(dataUpdate);
        record.setParentId(parentIdUpdated);            

        DsStorageFacade.createOrUpdateRecord(record);

        //Check new updated record is correct.
        DsRecordDto recordUpdated = DsStorageFacade.getRecord(id,false);
        assertEquals(id,recordUpdated.getId());
        assertEquals(origin,recordUpdated .getOrigin());
        assertEquals(parentIdUpdated,record.getParentId());        
        assertTrue(recordUpdated.getmTime() >recordUpdated.getcTime() ); //Modified is now newer
        assertEquals(cTimeBefore, recordUpdated.getcTime());  //Created time is not changed on updae                	                           


    }

    
    @Test
    public void testKeepKalturaId() throws Exception {
        String id ="doms.radio:id1";
        String origin="doms.radio";
        String data = "Hello";       
        String referenceId="referenceId_123";
        String referenceIdUpdated="referenceId_123_updated";
        String kalturaId="kalturaId";        
        
        DsRecordDto record = new DsRecordDto();
        record.setId(id);
        record.setOrigin(origin);
        record.setData(data);

        record.setRecordType(RecordTypeDto.MANIFESTATION);
        record.setReferenceId(referenceId);
        record.setKalturaId(kalturaId);
        DsStorageFacade.createOrUpdateRecord(record);
        
        //See referenceId and kalturaId is set correct
        DsRecordDto newCreatedRecord=DsStorageFacade.getRecordTree(id);
        assertEquals(referenceId,newCreatedRecord.getReferenceId());
        assertEquals(kalturaId,newCreatedRecord.getKalturaId());

        //New update record, but the new record does not have kalturaId, but has a difference referenceId
        record.setReferenceId(referenceIdUpdated);        
        record.setKalturaId(null); //blank, but updated post will still have old value
        DsStorageFacade.createOrUpdateRecord(record);        
        DsRecordDto updatedRecord=DsStorageFacade.getRecordTree(id);
        assertEquals(kalturaId,updatedRecord.getKalturaId());            
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
        record.setRecordType(RecordTypeDto.MANIFESTATION);
        
        try {
            DsStorageFacade.createOrUpdateRecord(record );
            fail("Should fail with unknown origin");				
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
      updateStrategy: NONE

    - name: origin.strategy.all    
      updateStrategy: ALL

    - name: origin_strategy_child    
      updateStrategy: CHILD

    - name: oritin.strategy.parent
      updateStrategy: PARENT
     *  
     * The 4 unittest has minor differences in assertions about that is updated
     *   
     */

    @Test
    public void testUpdateStrategy_NONE() throws Exception {
        createTestHierachyParentAndTwoChildren("origin.strategy.none");
        DsRecordDto parentBefore = DsStorageFacade.getRecord("origin.strategy.none:parent",false);
        DsRecordDto child1Before = DsStorageFacade.getRecord("origin.strategy.none:child1",false);
        DsRecordDto child2Before = DsStorageFacade.getRecord("origin.strategy.none:child2",false);

        //Update parent
        parentBefore.setData("parent updated");     
        DsStorageFacade.createOrUpdateRecord(parentBefore);

        DsRecordDto child1After = DsStorageFacade.getRecord("origin.strategy.none:child1",false);

        //test that child1 does not have changed mTime
        assertEquals(child1Before.getmTime(), child1After.getmTime());

        //update child2 and test parent is not touched.
        child2Before.setData("child2 updated");
        parentBefore = DsStorageFacade.getRecord("origin.strategy.none:parent",false);
        DsStorageFacade.createOrUpdateRecord(child2Before);     
        DsRecordDto parentAfter = DsStorageFacade.getRecord("origin.strategy.none:parent",false);
        assertEquals(parentBefore.getmTime(), parentAfter.getmTime());     
    }


    @Test
    public void testUpdateStrategy_PARENT() throws Exception {
        createTestHierachyParentAndTwoChildren("origin.strategy.parent");
        DsRecordDto parentBefore = DsStorageFacade.getRecord("origin.strategy.parent:parent",false);
        DsRecordDto child1Before = DsStorageFacade.getRecord("origin.strategy.parent:child1",false);
        DsRecordDto child2Before = DsStorageFacade.getRecord("origin.strategy.parent:child2",false);

        //Update parent
        parentBefore.setData("parent updated");     
        DsStorageFacade.createOrUpdateRecord(parentBefore);

        DsRecordDto child1After = DsStorageFacade.getRecord("origin.strategy.parent:child1",false);

        //test that child1 does not have changed mTime
        assertEquals(child1Before.getmTime(), child1After.getmTime());

        //update child2 and test parent is touched
        child2Before.setData("child2 updated");
        parentBefore = DsStorageFacade.getRecord("origin.strategy.parent:parent",false);
        DsStorageFacade.createOrUpdateRecord(child2Before);     
        DsRecordDto parentAfter = DsStorageFacade.getRecord("origin.strategy.parent:parent",false);
        assertTrue(parentBefore.getmTime() < parentAfter.getmTime()); //Parent has been touched as expected!
    }

    @Test
    public void testUpdateStrategy_CHILD() throws Exception {
        createTestHierachyParentAndTwoChildren("origin.strategy.child");
        DsRecordDto parentBefore = DsStorageFacade.getRecord("origin.strategy.child:parent",false);
        DsRecordDto child1Before = DsStorageFacade.getRecord("origin.strategy.child:child1",false);
        DsRecordDto child2Before = DsStorageFacade.getRecord("origin.strategy.child:child2",false);

        //Update parent
        parentBefore.setData("parent updated");     
        DsStorageFacade.createOrUpdateRecord(parentBefore);

        DsRecordDto child1After = DsStorageFacade.getRecord("origin.strategy.child:child1",false);
        
        //test that child11 has new mTime
        assertTrue(child1Before.getmTime() < child1After.getmTime());

        //update child2 and test parent is not touched.
        child2Before.setData("child2 updated");
        parentBefore = DsStorageFacade.getRecord("origin.strategy.child:parent",false);
        DsStorageFacade.createOrUpdateRecord(child2Before);     
        DsRecordDto parentAfter = DsStorageFacade.getRecord("origin.strategy.child:parent",false);
        assertEquals(parentBefore.getmTime(), parentAfter.getmTime()); //Not changed
    }


    @Test
    public void testUpdateStrategy_ALL() throws Exception {
        createTestHierachyParentAndTwoChildren("origin.strategy.all");
        DsRecordDto parentBefore = DsStorageFacade.getRecord("origin.strategy.all:parent",false);
        DsRecordDto child1Before = DsStorageFacade.getRecord("origin.strategy.all:child1",false);
        DsRecordDto child2Before = DsStorageFacade.getRecord("origin.strategy.all:child2",false);

        //Update parent
        parentBefore.setData("parent updated");     
        DsStorageFacade.createOrUpdateRecord(parentBefore);

        DsRecordDto child1After = DsStorageFacade.getRecord("origin.strategy.all:child1",false);

        //test that child11 has new mTime
        assertTrue(child1Before.getmTime() < child1After.getmTime());

        //update child2 and test parent is not touched.
        child2Before.setData("child2 updated");
        parentBefore = DsStorageFacade.getRecord("origin.strategy.all:parent",false);
        DsStorageFacade.createOrUpdateRecord(child2Before);     
        DsRecordDto parentAfter = DsStorageFacade.getRecord("origin.strategy.all:parent",false);
        assertTrue(parentBefore.getmTime() < parentAfter.getmTime()); //Parent touched
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
        record.setRecordType(RecordTypeDto.MANIFESTATION);
        try {
            DsStorageFacade.createOrUpdateRecord(record );
            fail("Should fail with recordId does not have origin as prefix");    
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
            fail("Should not fail since recordId now has origin as prefix",e);
        }
      
    }
    
    @Test
    public void testParentCycle() throws Exception {
        //Datastructure is a tree with 3 nodes and depth=2. Node at deepth2  points back to top node.        
        
        createParentCycle("doms.aviser");
        try {
        DsRecordDto record = DsStorageFacade.getRecordTree("doms.aviser:p3");       
        fail("Cycle should have been detected");
        }
        catch (InternalServiceException e) {
            //Expected
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
        assertEquals(parentId,record.getId());        
        assertTrue(record.getParent() == null);        
      
        //Check children loaded as records
        assertEquals(record.getChildren().size(), 2);               
        assertEquals(record.getChildren().get(0).getId(), child1Id);
        assertEquals(record.getChildren().get(1).getId(), child2Id);
       
        //Check child has parent loaded, both in ID list and as object
        assertEquals(record.getChildren().get(0).getParentId(), record.getId());
        assertEquals(record.getChildren().get(0).getParent().getId(), record.getId());
        
       // Check depth=2
        assertEquals(record.getChildren().get(0).getChildren().get(0).getId(), child1_1Id);
        assertEquals(record.getChildren().get(0).getChildren().get(0).getParent().getId(), child1Id);
        assertEquals(record.getChildren().get(0).getChildren().get(1).getId(), child1_2Id);
        
        //Test2, load depth=1 record      
        DsRecordDto child1 = DsStorageFacade.getRecordTree(child1Id);        
 
        //Test the parent is now set
        assertEquals(child1.getParentId(), parentId);        
        DsRecordDto parent = child1.getParent();      
        assertEquals(parentId,parent.getId());
        assertEquals(2,parent.getChildrenIds().size()); //ID list         
        assertEquals(2,parent.getChildren().size()); //Record objects               

        //Go from parent down to other child
        DsRecordDto child2 =parent.getChildren().get(1);
        assertEquals(child2Id,child2.getId());
        DsRecordDto parentFromChild2 = child2.getParent();
        assertEquals(parentId,parentFromChild2.getId());
        
                
        // Test3, load depth=3 record 
        DsRecordDto child1_1 = DsStorageFacade.getRecordTree(child1_1Id);
        DsRecordDto parent1_1 = child1_1.getParent(); 
        assertEquals(child1Id,parent1_1.getId());

       
        assertEquals(2,parent1_1.getChildrenIds().size());   
        assertEquals(2,parent1_1.getChildren().size());
        
    
        DsRecordDto topParent = parent1_1.getParent();
        assertEquals(parentId,topParent.getId());
        assertEquals(2,topParent.getChildren().size());
    
    }
    
    @Test
    public void testLocalRecordTree() throws Exception {        
        //Datastructure is a tree with 5 nodes and depth=2. (See method for visualization)       
        //Test 1: Load c1 
        //Test 2: Load p
        //Test 3: Load c1_1
        
        //Setup
        String parentId="doms.aviser:p";
        String child1Id="doms.aviser:c1";
        String child2Id="doms.aviser:c2";
        String child1_1Id="doms.aviser:c1_1";
        String child1_2Id="doms.aviser:c1_2";
        
        createTestDepth2Tree("doms.aviser"); // See this method for visualization of the tree.
        
        //Test 1
        DsRecordDto child1 = DsStorageFacade.getRecord(child1Id,true);       
        assertEquals(child1Id,child1.getId()); //Test record itself
        assertEquals(parentId,child1.getParentId());    //Test Parent id
        assertEquals(parentId,child1.getParent().getId());    //Test Parent object        
        assertNull(child1.getParent().getChildren());    //Important, do not load children from parent        
        assertEquals(2,child1.getChildren().size()); //Test both children are loaded
        assertNull(child1.getChildren().get(0).getParent());    //Important, do not load parent from child
                
        //Test 2
        DsRecordDto parent = DsStorageFacade.getRecord(parentId,true);
        assertNull(parent.getParent());//No parent
        assertNull(parent.getParentId()); //no parent
        assertEquals(2,child1.getChildren().size()); //Test both children are loaded
        assertNull(child1.getChildren().get(0).getParent());    //Important, do not load parent from child
        
        //Test 3
        DsRecordDto child1_1= DsStorageFacade.getRecord(child1_1Id,true);
        assertNull(child1_1.getChildren());  //no further children
        assertEquals(child1Id, child1_1.getParentId());
        assertEquals(child1Id, child1_1.getParent().getId());
        assertNull(child1_1.getParent().getChildren()); //Parent do not point back down.
        assertEquals(2, child1_1.getParent().getChildrenIds().size()); //But id's to children must be there
    }
   
    private void createTestHierachyParentAndTwoChildren(String origin) throws Exception {
        String parentId="parent";

        DsRecordDto parentRecord = new DsRecordDto();
        parentRecord.setId(origin+":"+parentId);
        parentRecord.setOrigin(origin);
        parentRecord.setData("parent data");
        parentRecord.setRecordType(RecordTypeDto.COLLECTION);
        
        DsRecordDto child1 = new DsRecordDto();
        child1.setId(origin+":"+"child1");
        child1.setOrigin(origin);
        child1.setData("child1 data");
        child1.setParentId(origin+":"+parentId);
        child1.setRecordType(RecordTypeDto.MANIFESTATION);
        
        DsRecordDto child2 = new DsRecordDto();
        child2.setId(origin+":"+"child2");
        child2.setOrigin(origin);
        child2.setData("child2 data");
        child2.setParentId(origin+":"+parentId);
        child2.setRecordType(RecordTypeDto.MANIFESTATION);
        
        DsStorageFacade.createOrUpdateRecord(parentRecord);
        DsStorageFacade.createOrUpdateRecord(child1);
        DsStorageFacade.createOrUpdateRecord(child2);
    }
    
    /*
     *   Data structure:
     *   
     *                     p1----->
     *                      |     |
     *                     p2     |
     *                      |     |
     *                     p3-----<
     * 
     * 
     */
    private void createParentCycle(String origin) throws Exception {
        DsRecordDto parentRecord = new DsRecordDto();
        parentRecord.setId(origin+":p1");
        parentRecord.setOrigin(origin);
        parentRecord.setData("p1 data");
        parentRecord.setParentId(origin+":p3");
        parentRecord.setRecordType(RecordTypeDto.COLLECTION);
        
        
        DsRecordDto child1 = new DsRecordDto();
        child1.setId(origin+":p2");
        child1.setOrigin(origin);
        child1.setData("p2 data");
        child1.setParentId(origin+":p1");
        child1.setRecordType(RecordTypeDto.MANIFESTATION);
        
        DsRecordDto child2 = new DsRecordDto();
        child2.setId(origin+":p3");
        child2.setOrigin(origin);
        child2.setData("p3 data");
        child2.setParentId(origin+":p1"); // This is the loop
        child2.setRecordType(RecordTypeDto.MANIFESTATION);  
        
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
        p.setRecordType(RecordTypeDto.COLLECTION);
        
        DsRecordDto c1 = new DsRecordDto();
        c1.setId(origin+":"+"c1");
        c1.setOrigin(origin);
        c1.setData("c1 data");
        c1.setParentId(origin+":"+parentId);
        c1.setRecordType(RecordTypeDto.MANIFESTATION);
        
        DsRecordDto c2 = new DsRecordDto();
        c2.setId(origin+":"+"c2");
        c2.setOrigin(origin);
        c2.setData("child2 data");
        c2.setParentId(origin+":"+parentId);
        c2.setRecordType(RecordTypeDto.MANIFESTATION);
        
        DsRecordDto c1_1 = new DsRecordDto();
        c1_1.setId(origin+":"+"c1_1");
        c1_1.setOrigin(origin);
        c1_1.setData("c1_1 data");
        c1_1.setParentId(c1.getId()); //c1 as parent
        c1_1.setRecordType(RecordTypeDto.MANIFESTATION);
        
        DsRecordDto c1_2 = new DsRecordDto();
        c1_2.setId(origin+":"+"c1_2");
        c1_2.setOrigin(origin);
        c1_2.setData("c1_2 data");
        c1_2.setParentId(c1.getId()); // c1 as parent
        c1_2.setRecordType(RecordTypeDto.MANIFESTATION);
        
        DsStorageFacade.createOrUpdateRecord(p);
        DsStorageFacade.createOrUpdateRecord(c1);
        DsStorageFacade.createOrUpdateRecord(c2);
        DsStorageFacade.createOrUpdateRecord(c1_1);
        DsStorageFacade.createOrUpdateRecord(c1_2);

    }
    @Test    
    public void testCreateAndUpdateMapping() throws Exception {
    
        String refId="referenceid_unittest_id123";
        try {
           MappingDto mapping = DsStorageFacade.getMapping("does_not_exist");
           fail();          
        }
        catch (Exception e) {
            //expected    
        }
     
        //Create
        try {            
            MappingDto mapping = new MappingDto();
            mapping.setReferenceId(refId);
            mapping.setKalturaId("kaltura_unittest_id123");
            DsStorageFacade.createOrUpdateMapping(mapping);                      
         }
         catch (Exception e) {
             e.printStackTrace();
           fail();    
         }
        
        //update
        try {
            MappingDto mapping = new MappingDto();
            mapping.setReferenceId(refId);
            mapping.setKalturaId("kaltura_unittest_id1234"); //4 added
            DsStorageFacade.createOrUpdateMapping(mapping);                      
         }
         catch (Exception e) {
           fail();    
         }
        
        //read updated value
        
        try {        
            MappingDto mapping = DsStorageFacade.getMapping(refId);
            assertEquals("kaltura_unittest_id1234",mapping.getKalturaId());
         }
         catch (Exception e) {
           fail();    
         }        
    }

}
