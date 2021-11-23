package dk.kb.storage.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
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
import dk.kb.storage.util.UniqueTimestampGenerator;



public class DsStorageTest {

	private static final Logger log = LoggerFactory.getLogger(DsStorageTest.class);
	private static final String CREATE_TABLES_DDL_FILE = "ddl/create_ds_storage.ddl";

	private static final String DRIVER = "org.h2.Driver";

	//We need the relative location. This works both in IDE's and Maven.
	private static final String TEST_CLASSES_PATH = new File(Thread.currentThread().getContextClassLoader().getResource("logback-test.xml").getPath()).getParentFile().getAbsolutePath();
	private static final String URL = "jdbc:h2:"+TEST_CLASSES_PATH+"/h2/ds_storage;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE";
	private static final String USERNAME = "";
	private static final String PASSWORD = "";

	private static DsStorage storage = null;


	private static void createEmptyDBFromDDL() throws Exception {
		// Delete if exists
		doDelete(new File(TEST_CLASSES_PATH +"/h2"));
		try {
			Class.forName(DRIVER); // load the driver
		} catch (ClassNotFoundException e) {

			throw new SQLException(e);
		}


		try (Connection connection = DriverManager.getConnection(URL, USERNAME, PASSWORD)){
			File file = getFile(CREATE_TABLES_DDL_FILE);
			log.info("Running DDL script:" + file.getAbsolutePath());

			if (!file.exists()) {
				log.error("DDL script not found:" + file.getAbsolutePath());
				throw new RuntimeException("DDL Script file not found:" + file.getAbsolutePath());
			}

			connection.createStatement().execute("RUNSCRIPT FROM '" + file.getAbsolutePath() + "'");

			connection.createStatement().execute("SHUTDOWN");
		}
		catch(Exception e) {
			e.printStackTrace();
		}

	}


	@BeforeAll
	public static void beforeClass() throws Exception {
		ServiceConfig.initialize("conf/ds-storage*.yaml"); 	    
		createEmptyDBFromDDL();
		DsStorage.initialize(DRIVER, URL, USERNAME, PASSWORD);
		storage = new  DsStorage();


	}
	@BeforeEach
	public void beforeEach() throws Exception {	        	    		    	
		storage.rollback(); //Important so each unittest has clean table
	}



	@AfterAll
	public static void afterClass() {
		// No reason to delete DB data after test, since we delete it before each test.
		// This way you can open the DB in a DB-browser after a unittest and see the result.
		DsStorage.shutdown();
	}


	// file.delete does not work for a directory unless it is empty. hence this method
	protected static void doDelete(File path) {
		if (path.isDirectory()) {
			for (File child : path.listFiles()) {
				doDelete(child);
			}
		}
		if (!path.delete()) {
			log.info("Could not delete " + path);
		}
	}


	@Test
	public void testBAsicCRUD() throws Exception {
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
		long mTimeBefore = recordLoaded.getmTime(); // Must be newer after update
		long cTimeBefore = recordLoaded.getcTime(); //Must be the same

		record.setDeleted(true);
		record.setData(dataUpdate);
		record.setParentId(parentIdUpdated);            

		storage.updateRecord(record);

		//Check new updated record is correct.
		DsRecordDto recordUpdated = storage.loadRecord(id);

		Assertions.assertEquals(id,recordUpdated .getId());
		Assertions.assertEquals(base,recordUpdated .getBase());
		Assertions.assertTrue(recordUpdated.getDeleted()); //It is now deleted
		Assertions.assertEquals(parentIdUpdated,record.getParentId());        
		Assertions.assertTrue(recordUpdated.getmTime() >recordUpdated.getcTime() ); //Modified is now newer
		Assertions.assertEquals(cTimeBefore, recordUpdated.getcTime());  //Created time is not changed on updae                	                           
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

		ArrayList<DsRecordDto> list1 = storage.getModifiedAfter("test_base", before, 10000);
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




		HashMap<String, Long> baseStatictics = storage.getBaseStatictics();

		assertEquals(3,baseStatictics.size());
		assertEquals(2,baseStatictics.get("test_base1"));
		assertEquals(1,baseStatictics.get("test_base2"));
		assertEquals(1,baseStatictics.get("test_base3"));	        	     
	}

	//TODO TOES? Is this somewhere in kb-util ?
	/**
	 * Multi protocol resource loader. Primary attempt is direct file, secondary is classpath resolved to File.
	 *
	 * @param resource a generic resource.
	 * @return a File pointing to the resource.
	 */
	protected static File getFile(String resource) throws IOException {
		File directFile = new File(resource);
		if (directFile.exists()) {
			return directFile;
		}
		URL classLoader = Thread.currentThread().getContextClassLoader().getResource(resource);
		if (classLoader == null) {
			throw new FileNotFoundException("Unable to locate '" + resource + "' as direct File or on classpath");
		}
		String fromURL = classLoader.getFile();
		if (fromURL == null || fromURL.isEmpty()) {
			throw new FileNotFoundException("Unable to convert URL '" + fromURL + "' to File");
		}
		return new File(fromURL);
	}

}
