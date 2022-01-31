package dk.kb.storage.storage;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.kb.storage.config.ServiceConfig;
import dk.kb.util.Resolver;


/*
 * Setup for the environment for unittest the same way as done in the InitialContext loader in the web container.
 * 
 * 1) Create a h2 database for unittests with schema defined
 * 2) Load the Yaml property files.
 * 
 */
public abstract class DsStorageUnitTestUtil {

    protected static final String CREATE_TABLES_DDL_FILE = "ddl/create_ds_storage_h2_unittest.ddl";

    protected static final String DRIVER = "org.h2.Driver";

    //We need the relative location. This works both in IDE's and Maven.
    protected static final String TEST_CLASSES_PATH = new File(Thread.currentThread().getContextClassLoader().getResource("logback-test.xml").getPath()).getParentFile().getAbsolutePath();
    protected static final String URL = "jdbc:h2:"+TEST_CLASSES_PATH+"/h2/ds_storage;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE";
    protected static final String USERNAME = "";
    protected static final String PASSWORD = "";

    protected static DsStorage storage = null;

    private static final Logger log = LoggerFactory.getLogger(DsStorageUnitTestUtil.class);


    @BeforeAll
    public static void beforeClass() throws Exception {

        ServiceConfig.initialize("conf/ds-storage*.yaml"); 	    
        createEmptyDBFromDDL();
        DsStorage.initialize(DRIVER, URL, USERNAME, PASSWORD);
        storage = new DsStorage();


    }

    /*
     * Delete all records between each unittest. The clearTableRecords is only called from here. 
     * The facade class is reponsible for committing transactions. So clean up between unittests.
     */
    @BeforeEach
    public void beforeEach() throws Exception {                     
        storage.clearTableRecords();
        storage.commit();
    }


    @AfterAll
    public static void afterClass() {
        // No reason to delete DB data file after test, since we clear table it before each test.
        // This way you can open the DB in a DB-browser after a unittest and see the result.
        // Just run that single test and look in the DB
        DsStorage.shutdown();


    }



    protected static void createEmptyDBFromDDL() throws Exception {
        //  Instead of deleting h2 database completely, we reuse the table between unittests instead.
        //	doDelete(new File(TEST_CLASSES_PATH +"/h2"));
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

    
    //Use KB-util to resolve file. 
    protected static File getFile(String resource) throws IOException {
       return Resolver.getPathFromClasspath(resource).toFile(); 
    }

/*
    // file.delete does not work for a directory unless it is empty. hence this method
    protected static void doDelete(File file) {
        try {
            FileUtils.deleteDirectory(file); //Will delete recursive
        }
        catch(Exception e) {
            log.error("failed to delete h2-folder from unittest.",e.getMessage());

        }

    }
*/



}
