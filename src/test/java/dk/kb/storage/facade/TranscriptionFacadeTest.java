package dk.kb.storage.facade;

import dk.kb.storage.config.ServiceConfig;
import dk.kb.storage.storage.BaseModuleStorage;
import dk.kb.storage.storage.DsStorage;
import dk.kb.storage.storage.TranscriptionStorageForUnitTest;
import dk.kb.storage.storage.UnitTestUtil;
import dk.kb.storage.util.H2DbUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import java.util.List;

public class TranscriptionFacadeTest extends UnitTestUtil {
    protected static TranscriptionStorageForUnitTest storage = null;

    @BeforeAll
    public static void beforeClass() throws Exception {
        ServiceConfig.initialize("conf/ds-storage*.yaml");
        H2DbUtil.createEmptyH2DBFromDDL(URL, DRIVER, USERNAME, PASSWORD, List.of("ddl/create_ds_storage_h2_unittest.ddl"));
        BaseModuleStorage.initialize(DRIVER, URL, USERNAME, PASSWORD);
        storage = new TranscriptionStorageForUnitTest();
    }

    /**
     * Delete all records between each unittest. The clearTableRecords is only called from here.
     * The facade class is responsible for committing transactions. So clean up between unittests.
     */
    @BeforeEach
    public void beforeEach() throws Exception {
        storage.clearTableRecords();
        storage.commit();
    }

    /**
     * No reason to delete DB data file after test, since we clear table it before each test.
     * This way you can open the DB in a DB-browser after the unittest and see the result.
     * Just run that single test and look in the DB
     */
    @AfterAll
    public static void afterClass() {
        DsStorage.shutdown();
    }
}
