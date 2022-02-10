package dk.kb.storage.facade;



import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import dk.kb.storage.facade.DsStorageFacade;
import dk.kb.storage.model.v1.DsRecordDto;
import dk.kb.storage.storage.DsStorageUnitTestUtil;

public class DsStorageFacadeTest extends DsStorageUnitTestUtil{

    
    //THIS UNTTEST MUST BE UPDATED WHEN VALIDATION RULES ARE MORE CLEAR!
    @Test
    public void testInvalidId() throws Exception {
        //TODO rescribe flow below

        
        String id ="doms.radio:idÆæ123"; // Æ and æ invalid and replaced b y AE and ae
        String base="doms.radio"; //Must be defined in yaml properties as allowed base
        String data = "Hello";
        
        DsRecordDto record = new DsRecordDto();
        record.setId(id);
        record.setBase(base);
        record.setData(data);
        DsStorageFacade.createOrUpdateRecord(record );

        //Load and see it is marked invalid
        
        DsRecordDto recordInvalid = DsStorageFacade.getRecord(id);
        
        
        
        
    }
    
    
    
    @Test
    public void testCreateAndUpdate() throws Exception {
        //TODO rescribe flow below

        DsRecordDto r1 = DsStorageFacade.getRecord("test.base:does_not_exist");
        Assertions.assertTrue(r1 == null);

        String id ="doms.radio:id1";
        String base="doms.radio"; //Must be defined in yaml properties as allowed base
        String data = "Hello";
        String parentId="doms.radio:id_1_parent";

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
        String parentIdUpdated="doms.radio:id_2_parent";
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
        String id ="unkown.base:id1";
        String base="unkown.base";	    	
        String data = "Hello";

        DsRecordDto record = new DsRecordDto();
        record.setId(id);
        record.setBase(base);
        record.setData(data);
        
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
    - name: recordbase.strategy.none   
      update_strategy: NONE

    - name: recordbase.strategy.all    
      update_strategy: ALL

    - name: recordBase_strategy_child    
      update_strategy: CHILD

    - name: recordbase.strategy.parent
      update_strategy: PARENT
     *  
     * The 4 unittest has minor differences in assertions about that is updated
     *   
     */

    @Test
    public void testUpdateStrategy_NONE() throws Exception {
        createTestHierachyParentAndTwoChildren("recordbase.strategy.none");
        DsRecordDto parentBefore = DsStorageFacade.getRecord("recordbase.strategy.none:parent");
        DsRecordDto child1Before = DsStorageFacade.getRecord("recordbase.strategy.none:child1");
        DsRecordDto child2Before = DsStorageFacade.getRecord("recordbase.strategy.none:child2");

        //Update parent
        parentBefore.setData("parent updated");     
        DsStorageFacade.createOrUpdateRecord(parentBefore);

        DsRecordDto child1After = DsStorageFacade.getRecord("recordbase.strategy.none:child1");

        //test that child1 does not have changed mTime
        Assertions.assertEquals(child1Before.getmTime(), child1After.getmTime());

        //update child2 and test parent is not touched.
        child2Before.setData("child2 updated");
        parentBefore = DsStorageFacade.getRecord("recordbase.strategy.none:parent");
        DsStorageFacade.createOrUpdateRecord(child2Before);     
        DsRecordDto parentAfter = DsStorageFacade.getRecord("recordbase.strategy.none:parent");
        Assertions.assertEquals(parentBefore.getmTime(), parentAfter.getmTime());     
    }


    @Test
    public void testUpdateStrategy_PARENT() throws Exception {
        createTestHierachyParentAndTwoChildren("recordbase.strategy.parent");
        DsRecordDto parentBefore = DsStorageFacade.getRecord("recordbase.strategy.parent:parent");
        DsRecordDto child1Before = DsStorageFacade.getRecord("recordbase.strategy.parent:child1");
        DsRecordDto child2Before = DsStorageFacade.getRecord("recordbase.strategy.parent:child2");

        //Update parent
        parentBefore.setData("parent updated");     
        DsStorageFacade.createOrUpdateRecord(parentBefore);

        DsRecordDto child1After = DsStorageFacade.getRecord("recordbase.strategy.parent:child1");

        //test that child1 does not have changed mTime
        Assertions.assertEquals(child1Before.getmTime(), child1After.getmTime());

        //update child2 and test parent is touched
        child2Before.setData("child2 updated");
        parentBefore = DsStorageFacade.getRecord("recordbase.strategy.parent:parent");
        DsStorageFacade.createOrUpdateRecord(child2Before);     
        DsRecordDto parentAfter = DsStorageFacade.getRecord("recordbase.strategy.parent:parent");
        Assertions.assertTrue(parentBefore.getmTime() < parentAfter.getmTime()); //Parent has been touched as expected!
    }

    @Test
    public void testUpdateStrategy_CHILD() throws Exception {
        createTestHierachyParentAndTwoChildren("recordbase.strategy.child");
        DsRecordDto parentBefore = DsStorageFacade.getRecord("recordbase.strategy.child:parent");
        DsRecordDto child1Before = DsStorageFacade.getRecord("recordbase.strategy.child:child1");
        DsRecordDto child2Before = DsStorageFacade.getRecord("recordbase.strategy.child:child2");

        //Update parent
        parentBefore.setData("parent updated");     
        DsStorageFacade.createOrUpdateRecord(parentBefore);

        DsRecordDto child1After = DsStorageFacade.getRecord("recordbase.strategy.child:child1");

        //test that child11 has new mTime
        Assertions.assertTrue(child1Before.getmTime() < child1After.getmTime());

        //update child2 and test parent is not touched.
        child2Before.setData("child2 updated");
        parentBefore = DsStorageFacade.getRecord("recordbase.strategy.child:parent");
        DsStorageFacade.createOrUpdateRecord(child2Before);     
        DsRecordDto parentAfter = DsStorageFacade.getRecord("recordbase.strategy.child:parent");
        Assertions.assertEquals(parentBefore.getmTime(), parentAfter.getmTime()); //Not changed
    }


    @Test
    public void testUpdateStrategy_ALL() throws Exception {
        createTestHierachyParentAndTwoChildren("recordbase.strategy.all");
        DsRecordDto parentBefore = DsStorageFacade.getRecord("recordbase.strategy.all:parent");
        DsRecordDto child1Before = DsStorageFacade.getRecord("recordbase.strategy.all:child1");
        DsRecordDto child2Before = DsStorageFacade.getRecord("recordbase.strategy.all:child2");

        //Update parent
        parentBefore.setData("parent updated");     
        DsStorageFacade.createOrUpdateRecord(parentBefore);

        DsRecordDto child1After = DsStorageFacade.getRecord("recordbase.strategy.all:child1");

        //test that child11 has new mTime
        Assertions.assertTrue(child1Before.getmTime() < child1After.getmTime());

        //update child2 and test parent is not touched.
        child2Before.setData("child2 updated");
        parentBefore = DsStorageFacade.getRecord("recordbase.strategy.all:parent");
        DsStorageFacade.createOrUpdateRecord(child2Before);     
        DsRecordDto parentAfter = DsStorageFacade.getRecord("recordbase.strategy.all:parent");
        Assertions.assertTrue(parentBefore.getmTime() < parentAfter.getmTime()); //Parent touched
    }
    

    @Test
    public void testIdStartsWithRecordBase() throws Exception {
        String id ="recordbase.unknown:id1";
        String base="recordbase.strategy.none";      
        
        String data = "Hello";
        
        DsRecordDto record = new DsRecordDto();
        record.setId(id);
        record.setBase(base);
        record.setData(data);        
        try {
            DsStorageFacade.createOrUpdateRecord(record );
            Assertions.fail("Should fail with recordId does not have recordBase as prefix");    
        }
        catch(Exception e) { //ignore   
        }
         
        id ="recordbase.strategy.none:id1";
         record.setId(id);
         try {
            DsStorageFacade.createOrUpdateRecord(record );
          //ignore
        }
        catch(Exception e) {
            e.printStackTrace();
            Assertions.fail("Should not fail since recordId now has recordBase as prefix",e);
        }
              
    }
    






    private void createTestHierachyParentAndTwoChildren(String recordBase) throws Exception {
        String parentId="parent";

        DsRecordDto parentRecord = new DsRecordDto();
        parentRecord.setId(recordBase+":"+parentId);
        parentRecord.setBase(recordBase);
        parentRecord.setData("parent data");

        DsRecordDto child1 = new DsRecordDto();
        child1.setId(recordBase+":"+"child1");
        child1.setBase(recordBase);
        child1.setData("child1 data");
        child1.setParentId(recordBase+":"+parentId);

        DsRecordDto child2 = new DsRecordDto();
        child2.setId(recordBase+":"+"child2");
        child2.setBase(recordBase);
        child2.setData("child2 data");
        child2.setParentId(recordBase+":"+parentId);

        DsStorageFacade.createOrUpdateRecord(parentRecord);
        DsStorageFacade.createOrUpdateRecord(child1);
        DsStorageFacade.createOrUpdateRecord(child2);

    }



}
