package dk.kb.storage.storage;

import org.apache.commons.dbcp2.BasicDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.kb.storage.model.v1.DsRecordDto;
import dk.kb.storage.model.v1.RecordBaseCountDto;
import dk.kb.storage.util.UniqueTimestampGenerator;


import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;


/*
 * This class will be called by the facade class. The facade class is also responsible for commit or rollback
 * 
 */

public class DsStorage implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(DsStorage.class);

    private static final String RECORDS_TABLE = "ds_records";
    private static final String ID_COLUMN = "id";
    private static final String BASE_COLUMN = "base";
    private static final String DELETED_COLUMN = "deleted";
    private static final String DATA_COLUMN = "data";
    private static final String CTIME_COLUMN = "ctime";
    private static final String MTIME_COLUMN = "mtime";
    private static final String PARENT_ID_COLUMN = "parentId";

    private static String clearTableRecordsStatement = "DELETE FROM " + RECORDS_TABLE;


    private static String createRecordStatement = "INSERT INTO " + RECORDS_TABLE +
            " (" + ID_COLUMN + ", " + BASE_COLUMN + ", " + DELETED_COLUMN + ", " + CTIME_COLUMN + ", " + MTIME_COLUMN + ", " + DATA_COLUMN + ", " + PARENT_ID_COLUMN +  ")"+
            " VALUES (?,?,?,?,?,?,?)";

    private static String updateRecordStatement = "UPDATE " + RECORDS_TABLE + " SET  "+			 
            DATA_COLUMN + " = ? , "+ 						 
            MTIME_COLUMN + " = ? , "+
            DELETED_COLUMN + " = 0 , "+
            PARENT_ID_COLUMN + " = ?  "+
            "WHERE "+
            ID_COLUMN + "= ?";

    private static String markRecordForDelete = "UPDATE " + RECORDS_TABLE + " SET  "+			 
            DELETED_COLUMN + " = 1,  "+
            MTIME_COLUMN + " = ? "+
            "WHERE "+
            ID_COLUMN + "= ?";


    private static String childrenIdsStatement = "SELECT " + ID_COLUMN +" FROM " + RECORDS_TABLE +
            " WHERE "
            + PARENT_ID_COLUMN + "= ?";

    private static String recordByIdStatement = "SELECT * FROM " + RECORDS_TABLE + " WHERE ID= ?";



    //SELECT * FROM  ds_records  WHERE base= 'test_base' AND mtime  > 1637237120476001 ORDER BY mtime ASC LIMIT 100
    private static String recordsModifiedAfterStatement =
            "SELECT * FROM " + RECORDS_TABLE +
            " WHERE " +BASE_COLUMN +"= ?" +
            " AND "+MTIME_COLUMN+" > ?" +
            " ORDER BY "+MTIME_COLUMN+ " ASC LIMIT ?";

    //SELECT * FROM  ds_records  WHERE base= 'test_base' AND mtime  > 1637237120476001 AND PARENTID IS NOT NULL ORDER BY mtime ASC LIMIT 100
    private static String recordsModifiedAfterChildrenOnlyStatement =
            "SELECT * FROM " + RECORDS_TABLE +
            " WHERE +"+BASE_COLUMN +"= ?" +
            " AND "+MTIME_COLUMN+" > ?" +
            " AND "+PARENT_ID_COLUMN+" IS NOT NULL"+
            " ORDER BY "+MTIME_COLUMN+ " ASC LIMIT ?";

    //SELECT * FROM  ds_records  WHERE base= 'test_base' AND mtime  > 1637237120476001 AND parentId IS NULL ORDER BY mtime ASC LIMIT 100	
    private static String recordsModifiedAfterParentsOnlyStatement =
            "SELECT * FROM " + RECORDS_TABLE +
            " WHERE +"+BASE_COLUMN +"= ?" +
            " AND "+MTIME_COLUMN+" > ?" +
            " AND "+PARENT_ID_COLUMN+" IS NULL"+
            " ORDER BY "+MTIME_COLUMN+ " ASC LIMIT ?";


    private static String baseStatisticsStatement = "SELECT " + BASE_COLUMN + " ,COUNT(*) AS COUNT FROM "
            + RECORDS_TABLE + " group by " + BASE_COLUMN;


    private static String recordIdExistsStatement = "SELECT COUNT(*) AS COUNT FROM " + RECORDS_TABLE+ " WHERE "+ID_COLUMN +" = ?";			


    private static BasicDataSource dataSource;

    // statistics shown on monitor.jsp page
    public static Date INITDATE = null;

    private Connection connection;

    public static void initialize(String driverName, String driverUrl, String userName, String password) {
        dataSource = new BasicDataSource();
        dataSource.setDriverClassName(driverName);
        dataSource.setUsername(userName);
        dataSource.setPassword(password);
        dataSource.setUrl(driverUrl);

        dataSource.setDefaultReadOnly(false);
        dataSource.setDefaultAutoCommit(false);



        //TODO maybe set some datasource options.
        // enable detection and logging of connection leaks
        /*
         * dataSource.setRemoveAbandonedOnBorrow(
         * AlmaPickupNumbersPropertiesHolder.PICKUPNUMBERS_DATABASE_TIME_BEFORE_RECLAIM
         * > 0); dataSource.setRemoveAbandonedOnMaintenance(
         * AlmaPickupNumbersPropertiesHolder.PICKUPNUMBERS_DATABASE_TIME_BEFORE_RECLAIM
         * > 0); dataSource.setRemoveAbandonedTimeout(AlmaPickupNumbersPropertiesHolder.
         * PICKUPNUMBERS_DATABASE_TIME_BEFORE_RECLAIM); //1 hour
         * dataSource.setLogAbandoned(AlmaPickupNumbersPropertiesHolder.
         * PICKUPNUMBERS_DATABASE_TIME_BEFORE_RECLAIM > 0);
         * dataSource.setMaxWaitMillis(AlmaPickupNumbersPropertiesHolder.
         * PICKUPNUMBERS_DATABASE_POOL_CONNECT_TIMEOUT);
         */
        dataSource.setMaxTotal(10); //

        INITDATE = new Date();

        log.info("DsStorage initialized");
    }

    public DsStorage() throws SQLException {
        connection = dataSource.getConnection();

    }

    public DsRecordDto  loadRecord(String id) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(recordByIdStatement);) {
            stmt.setString(1, id);

            try (ResultSet rs = stmt.executeQuery();) {
                if (!rs.next()) {
                    return null;// Or throw exception?
                }
                DsRecordDto  record = createRecordFromRS(rs);
                return record;
            }
        }
    }

    public boolean recordExists(String id) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(recordIdExistsStatement);) {
            stmt.setString(1, id);

            try (ResultSet rs = stmt.executeQuery();) {
                rs.next(); //Count has always next
                int count = rs.getInt("COUNT");
                return  count == 1;				
            }
        }
    }


    public ArrayList<String> getChildrenIds(String parentId) throws SQLException {

        ArrayList<String> childIds = new ArrayList<String>();
        try (PreparedStatement stmt = connection.prepareStatement(childrenIdsStatement);) {
            stmt.setString(1, parentId);
            try (ResultSet rs = stmt.executeQuery();) {
                while (rs.next()) {
                    String id = rs.getString(ID_COLUMN);
                    childIds.add(id);
                }
            }
        }
        return childIds;
    }


    /*
     * Only called from unittests, not exposed on facade class
     * 
     */
    public void clearTableRecords() throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(clearTableRecordsStatement);) {
            stmt.execute(); //No resultset to close
        }		 
    }

    /*
     * Will only extract with records strightly  larger than mTime!
     * Will be sorted by mTime. Latest is last
     * 
     * Only parents posts (those that have children) will be load or only children (those that have parent)
     * 
     */
    public ArrayList<DsRecordDto > getModifiedAfterParentsOnly(String base, long mTime, int batchSize) throws Exception {

        if (batchSize <1 || batchSize > 100000) { //No doom switch
            throw new Exception("Batchsize must be in range 1 to 100000");			
        }
        ArrayList<DsRecordDto > records = new ArrayList<DsRecordDto >();
        try (PreparedStatement stmt = connection.prepareStatement(recordsModifiedAfterParentsOnlyStatement);) {

            stmt.setString(1, base);
            stmt.setLong(2, mTime);
            stmt.setLong(3, batchSize);
            try (ResultSet rs = stmt.executeQuery();) {
                while (rs.next()) {
                    DsRecordDto  record = createRecordFromRS(rs);
                    records.add(record);

                }
            }
        }
        catch(Exception e) {
            e.printStackTrace();

        }

        return records;	
    }

    /*
     * Will only extract with records strightly  larger than mTime!
     * Will be sorted by mTime. Latest is last
     * 
     * Will extract all no matter of parent or child ids
     * 
     */
    public ArrayList<DsRecordDto > getRecordsModifiedAfter(String base, long mTime, int batchSize) throws Exception {

        if (batchSize <1 || batchSize > 10000) { //No doom switch
            throw new Exception("Batchsize must be in range 1 to 10000");			
        }
        ArrayList<DsRecordDto> records = new ArrayList<DsRecordDto>();
        try (PreparedStatement stmt = connection.prepareStatement(recordsModifiedAfterStatement);) {
            
            
            log.info("AFTER SQL:"+recordsModifiedAfterStatement);
            stmt.setString(1, base);
            stmt.setLong(2, mTime);
            stmt.setLong(3, batchSize);
            try (ResultSet rs = stmt.executeQuery();) {
                while (rs.next()) {                    
                    log.info("RS FOUND");
                    DsRecordDto record = createRecordFromRS(rs);
                    records.add(record);
                }
            }
        }
        catch(Exception e) {
            String message = "SQL Exception in getRecordsModifiedAfter";
            e.printStackTrace();
            log.error(message);
            throw new SQLException(message, e);
        }

        return records;	
    }



    /*
     * Will only extract with records strightly larger than mTime!
     * Will be sorted by mTime. Latest is last
     * 
     * Will only fetch children records. That is those that has a parent.
     * 
     */
    public ArrayList<DsRecordDto>  getModifiedAfterChildrenOnly(String base, long mTime, int batchSize) throws Exception {

        if (batchSize <1 || batchSize > 100000) { //No doom switch
            throw new Exception("Batchsize must be in range 1 to 100000");			
        }
        ArrayList<DsRecordDto> records = new ArrayList<DsRecordDto>();
        try (PreparedStatement stmt = connection.prepareStatement(recordsModifiedAfterChildrenOnlyStatement);) {

            stmt.setString(1, base);
            stmt.setLong(2, mTime);
            stmt.setLong(3, batchSize);
            try (ResultSet rs = stmt.executeQuery();) {
                while (rs.next()) {
                    DsRecordDto record = createRecordFromRS(rs);
                    records.add(record);

                }
            }
        }
        catch(Exception e) {
            String message = "SQL Exception in getModifiedAfterChildrenOnly";
            e.printStackTrace();
            log.error(message);
            throw new SQLException(message, e);
        }

        return records;	
    }

    public ArrayList<RecordBaseCountDto> getBaseStatictics() throws SQLException {

        ArrayList<RecordBaseCountDto> baseCountList = new ArrayList<RecordBaseCountDto>();
        try (PreparedStatement stmt = connection.prepareStatement(baseStatisticsStatement);) {

            try (ResultSet rs = stmt.executeQuery();) {
                while (rs.next()) {
                    RecordBaseCountDto baseStats = new RecordBaseCountDto();                    
                    String base = rs.getString(BASE_COLUMN);
                    long count = rs.getLong("COUNT");
                    baseStats.setRecordBase(base);
                    baseStats.setCount(count);
                    baseCountList.add(baseStats);
                }
            }
        }
        return baseCountList;
    }

    public void createNewRecord(DsRecordDto record) throws Exception {

        // Sanity check
        if (record.getId() == null) {
            throw new Exception("Id must not be null"); // TODO exception enum types, messages?
        }
        if (record.getId().equals(record.getParentId())) {
            throw new Exception("Record with id has itself as parent:" + record.getId());
        }

        long nowStamp = UniqueTimestampGenerator.next();
        //log.debug("Creating new record: " + record.getId());

        try (PreparedStatement stmt = connection.prepareStatement(createRecordStatement);) {
            stmt.setString(1, record.getId());
            stmt.setString(2, record.getBase());
            stmt.setInt(3, 0);
            stmt.setLong(4, nowStamp);
            stmt.setLong(5, nowStamp);
            stmt.setString(6, record.getData());
            stmt.setString(7, record.getParentId());
            stmt.executeUpdate();

        } catch (SQLException e) {
            String message = "SQL Exception in createNewRecord with id:" + record.getId() + " error:" + e.getMessage();
            e.printStackTrace();
            log.error(message);
            throw new SQLException(message, e);
        }

    }

    public void markRecordForDelete(String recordId) throws Exception {

        // Sanity check
        if (recordId == null) {
            throw new Exception("Id must not be null"); // TODO exception enum types, messages?
        }

        long nowStamp = UniqueTimestampGenerator.next();
        //log.debug("Creating new record: " + record.getId());

        try (PreparedStatement stmt = connection.prepareStatement(markRecordForDelete);) {		
            stmt.setLong(1, nowStamp);						
            stmt.setString(2, recordId);

            stmt.executeUpdate();
        } catch (SQLException e) {
            String message = "SQL Exception in markRecordForDelete  with id:" + recordId + " error:" + e.getMessage();
            e.printStackTrace();
            log.error(message);
            throw new SQLException(message, e);
        }

    }

    public void updateRecord(DsRecordDto record) throws Exception {

        // Sanity check
        if (record.getId() == null) {
            throw new Exception("Id must not be null"); // TODO exception enum types, messages?
        }
        if (record.getId().equals(record.getParentId())) {
            throw new Exception("Record with id has itself as parent:" + record.getId());
        }

        long nowStamp = UniqueTimestampGenerator.next();
        //log.debug("Creating new record: " + record.getId());

        try (PreparedStatement stmt = connection.prepareStatement(updateRecordStatement);) {

            stmt.setString(1, record.getData());
            stmt.setLong(2, nowStamp);						
            stmt.setString(3, record.getParentId());
            stmt.setString(4, record.getId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            String message = "SQL Exception in updateRecord with id:" + record.getId() + " error:" + e.getMessage();
            e.printStackTrace();
            log.error(message);
            throw new SQLException(message, e);
        }

    }

    private static DsRecordDto createRecordFromRS(ResultSet rs) throws SQLException {

        String id = rs.getString(ID_COLUMN);
        String base = rs.getString(BASE_COLUMN);
        boolean deleted = rs.getInt(DELETED_COLUMN) == 1;		
        String data = rs.getString(DATA_COLUMN);
        long cTime = rs.getLong(CTIME_COLUMN);
        long mTime = rs.getLong(MTIME_COLUMN);
        String parentId = rs.getString(PARENT_ID_COLUMN);

        DsRecordDto record = new DsRecordDto();
        record.setId(id);
        record.setBase(base);
        record.setData(data);
        record.setParentId(parentId);
        record.setcTime(cTime);
        record.setmTime(mTime);
        record.setDeleted(deleted);
        return record;
    }


    /*
     * FOR TEST JETTY RUN ONLY!
     * 
     */
    public void createNewDatabase(String ddlFile) throws SQLException {	
        connection.createStatement().execute("RUNSCRIPT FROM '" + ddlFile+ "'");		
    }


    public void commit() throws SQLException {
        connection.commit();
    }

    public void rollback() {
        try {
            connection.rollback();
        } catch (Exception e) {
            // nothing to do here
        }
    }

    @Override
    public void close() {
        try {
            connection.close();
        } catch (Exception e) {
            // nothing to do here
        }
    }

    // This is called by from InialialziationContextListener by the Web-container
    // when server is shutdown,
    // Just to be sure the DB lock file is free.
    public static void shutdown() {
        log.info("Shutdown ds-storage");
        try {
            if (dataSource != null) {
                dataSource.close();
            }
        } catch (Exception e) {
            // ignore errors during shutdown, we cant do anything about it anyway
            log.error("shutdown failed", e);
        }
    }


    private int boolToInt(Boolean isTrue) {
        if (isTrue == null) {
            return 0;
        }

        return isTrue ? 1 : 0;
    }
}
