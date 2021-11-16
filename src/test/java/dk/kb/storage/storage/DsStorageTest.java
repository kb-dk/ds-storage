package dk.kb.storage.storage;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public class DsStorageTest {

	 private static final Logger log = LoggerFactory.getLogger(DsStorageTest.class);
	    private static final String CREATE_TABLES_DDL_FILE = "ddl/create_ds_storage.ddl";

	    private static final String DRIVER = "org.h2.Driver";
	    
	    //We need the relative location. This works both in IDE's and Maven.
	    private static final String TEST_CLASSES_PATH = new File(Thread.currentThread().getContextClassLoader().getResource("logback-test.xml").getPath()).getParentFile().getAbsolutePath();
	    private static final String URL = "jdbc:h2:"+TEST_CLASSES_PATH+"/h2/ds_storage;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE";
	    private static final String USERNAME = "";
	    private static final String PASSWORD = "";
	
	    private DsStorage storage = null;

	    
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
	        	    	
	    	createEmptyDBFromDDL();
	        DsStorage.initialize(DRIVER, URL, USERNAME, PASSWORD);
	        
	        
	        
	    }
	    @BeforeEach
	    public void beforeEach() throws Exception {	        	    	
	    	storage = new  DsStorage();	        	        	        
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
	    public void testCreateAndLoadRecord() throws Exception {
	    	String id ="id1";
	    	String base="base_test";
	    	boolean indexable = true;
	    	String data = "Hello";
	    	String parentId="id_1_parent";
	  	    	
	    	DsRecord record = new DsRecord(id, base,indexable,data, parentId);
            storage.createNewRecord(record );
              
            //Load and check values are correct
            DsRecord recordLoaded = storage.loadRecord(id);
            Assertions.assertEquals(id,recordLoaded.getId());
            Assertions.assertEquals(base,recordLoaded.getBase());
            Assertions.assertEquals(indexable,recordLoaded.isIndexable());
            Assertions.assertFalse(recordLoaded.isDeleted());
            Assertions.assertEquals(parentId,record.getParentId());        
            Assertions.assertTrue(recordLoaded.getcTime() > 0);
            Assertions.assertEquals(recordLoaded.getcTime(), recordLoaded.getmTime());                  
	    
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
