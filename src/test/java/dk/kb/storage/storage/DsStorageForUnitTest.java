package dk.kb.storage.storage;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * This class is a small extension of the DsStorage with a few methods used for unittest 
 * that we do not want in the production code.
 *
 * <p>
 * Between each unittest the all tables are cleared for data and the method is only defined in this subclass  
 */
public class DsStorageForUnitTest extends DsStorage  {

    private static final Logger log = LoggerFactory.getLogger(DsStorageForUnitTest.class);

    private static String clearTableRecordsStatement = "DELETE FROM DS_RECORDS";
    private static String clearTableTranscriptionsStatement = "DELETE FROM TRANSCRIPTIONS";

    
    public  DsStorageForUnitTest() throws SQLException {
        super();
    }
   

    /** 
     * Will clear data in ds_records and ds_mapping tables. Unit test functionality only. 
     * 
     */
    public void clearMappingAndRecordTable() throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(clearTableRecordsStatement)) {
            stmt.execute(); //No result set to close
        }        
        
        try (PreparedStatement stmt = connection.prepareStatement(clearTableTranscriptionsStatement)) {
            stmt.execute(); //No result set to close
        }
        
        connection.commit();
        log.info("Tables cleared for unittest");
    }

}