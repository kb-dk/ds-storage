package dk.kb.storage.storage;

import java.io.File;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.kb.storage.config.ServiceConfig;
import dk.kb.storage.util.H2DbUtil;



/*
 * Setup for the environment for unittest the same way as done in the InitialContext loader in the web container.
 * 
 * 1) Create a h2 database for unittests with schema defined
 * 2) Load the Yaml property files.
 * 
 */
public abstract class DsStorageUnitTestUtil {

    

    protected static final String DRIVER = "org.h2.Driver";

    //We need the relative location. This works both in IDE's and Maven.
    protected static final String TEST_CLASSES_PATH = new File(Thread.currentThread().getContextClassLoader().getResource("logback-test.xml").getPath()).getParentFile().getAbsolutePath();
    protected static final String URL = "jdbc:h2:"+TEST_CLASSES_PATH+"/h2/ds_storage;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE";
    protected static final String USERNAME = "";
    protected static final String PASSWORD = "";

    protected static DsStorageForUnitTest storage = null;

    private static final Logger log = LoggerFactory.getLogger(DsStorageUnitTestUtil.class);


    @BeforeAll
    public static void beforeClass() throws Exception {

        ServiceConfig.initialize("conf/ds-storage*.yaml"); 	    
        H2DbUtil.createEmptyH2DBFromDDL(URL,DRIVER,USERNAME,PASSWORD);
        DsStorage.initialize(DRIVER, URL, USERNAME, PASSWORD);
        storage = new DsStorageForUnitTest();


    }

    /*
     * Delete all records between each unittest. The clearTableRecords is only called from here. 
     * The facade class is responsible for committing transactions. So clean up between unittests.
     */
    @BeforeEach
    public void beforeEach() throws Exception {                     
        storage.clearMappingAndRecordTable();
        storage.commit();
    }


    @AfterAll
    public static void afterClass() {
        // No reason to delete DB data file after test, since we clear table it before each test.
        // This way you can open the DB in a DB-browser after the unittest and see the result.
        // Just run that single test and look in the DB
        DsStorage.shutdown();


    }




}
