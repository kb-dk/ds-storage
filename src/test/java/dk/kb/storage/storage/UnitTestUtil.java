package dk.kb.storage.storage;

import java.io.File;

/**
 * Setup for the environment for unittest the same way as done in the InitialContext loader in the web container.
 * 1) Create a h2 database for unittests with schema defined
 * 2) Load the Yaml property files.
 */
public abstract class UnitTestUtil {
    protected static final String DRIVER = "org.h2.Driver";

    //We need the relative location. This works both in IDE's and Maven.
    protected static final String TEST_CLASSES_PATH = new File(Thread.currentThread().getContextClassLoader().getResource("logback-test.xml").getPath()).getParentFile().getAbsolutePath();
    protected static final String URL = "jdbc:h2:" + TEST_CLASSES_PATH + "/h2/ds_storage;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE";
    protected static final String USERNAME = "";
    protected static final String PASSWORD = "";

    protected static DsStorageForUnitTest storage = null;
}
