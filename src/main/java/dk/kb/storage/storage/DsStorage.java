package dk.kb.storage.storage;

import dk.kb.util.Pair;
import dk.kb.util.webservice.exception.InvalidArgumentServiceException;

import org.apache.commons.dbcp2.BasicDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.kb.storage.model.v1.DsRecordDto;
import dk.kb.storage.model.v1.MappingDto;
import dk.kb.storage.model.v1.OriginCountDto;
import dk.kb.storage.model.v1.RecordTypeDto;
import dk.kb.storage.util.UniqueTimestampGenerator;


import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;


/*
 * This class will be called by the facade class. The facade class is also responsible for commit or rollback
*/

public class DsStorage implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(DsStorage.class);

    private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ssZ",Locale.getDefault());
    
    
    private static final String RECORDS_TABLE = "ds_records";
    private static final String MAPPING_TABLE = "ds_mapping";
    private static final String ID_COLUMN = "id";
    private static final String ORGID_COLUMN = "orgid";
    private static final String IDERROR_COLUMN = "id_error";
    private static final String ORIGIN_COLUMN = "origin";
    private static final String RECORDTYPE_COLUMN = "recordtype";
    private static final String DELETED_COLUMN = "deleted";
    private static final String DATA_COLUMN = "data";
    private static final String CTIME_COLUMN = "ctime";
    private static final String MTIME_COLUMN = "mtime";
    private static final String PARENT_ID_COLUMN = "parentid";
    private static final String KALTURA_REFERENCE_ID_COLUMN = "kalturareferenceid";
    private static final String KALTURA_INTERNAL_ID_COLUMN = "kalturainternalid";
    private static final String MAPPING_ID_COLUMN = "id";
    private static final String MAPPING_KALTURA_INTERNAL_ID_COLUMN = "kalturainternalid";

    private static String clearTableRecordsStatement = "DELETE FROM " + RECORDS_TABLE;


    private static String createRecordStatement = "INSERT INTO " + RECORDS_TABLE +
            " (" + ID_COLUMN + ", " + ORIGIN_COLUMN + ", " +ORGID_COLUMN + ","+ RECORDTYPE_COLUMN +"," + IDERROR_COLUMN +","+ DELETED_COLUMN + ", " + CTIME_COLUMN + ", " + MTIME_COLUMN + ", " + DATA_COLUMN + ", " + PARENT_ID_COLUMN +  " , " + KALTURA_REFERENCE_ID_COLUMN +" , "+KALTURA_INTERNAL_ID_COLUMN+")"+
            " VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";


    private static String createMapping = "INSERT INTO " + MAPPING_TABLE +
            " (" + MAPPING_ID_COLUMN + ", " + MAPPING_KALTURA_INTERNAL_ID_COLUMN +")"+
            " VALUES (?,?)";

    private static String mappingByIdStatement = "SELECT * FROM " + MAPPING_TABLE + " WHERE ID= ?";
    
    private static String updateMappingStatement = "UPDATE " + MAPPING_TABLE + " SET  "+                         
            KALTURA_INTERNAL_ID_COLUMN + " = ?  "+            
            "WHERE "+
            MAPPING_ID_COLUMN + "= ?";
    
    
    private static String updateRecordStatement = "UPDATE " + RECORDS_TABLE + " SET  "+			 
            RECORDTYPE_COLUMN + " = ?  ,"+
            DATA_COLUMN + " = ? , "+ 						 
            MTIME_COLUMN + " = ? , "+
            DELETED_COLUMN + " = 0 , "+
            KALTURA_REFERENCE_ID_COLUMN + " = ? , "+             
            PARENT_ID_COLUMN + " = ?  "+            
            "WHERE "+
            ID_COLUMN + "= ?";
    

    private static String updateKalturaReferenceIdStatement = "UPDATE " + RECORDS_TABLE + " SET  "+ 
            KALTURA_INTERNAL_ID_COLUMN + " = ? ,"+
            MTIME_COLUMN + " = ?  "+
            "WHERE "+
            KALTURA_REFERENCE_ID_COLUMN + "= ?";
    

    private static String markRecordForDeleteStatement = "UPDATE " + RECORDS_TABLE + " SET  "+			 
            DELETED_COLUMN + " = 1,  "+
            MTIME_COLUMN + " = ? "+
            "WHERE "+
            ID_COLUMN + "= ?";

    private static String deleteRecordsForOriginStateMent= "DELETE FROM " + RECORDS_TABLE + " WHERE  "+			 
            ORIGIN_COLUMN + " = ? AND "+
            MTIME_COLUMN +" >=  ? AND "+            
            MTIME_COLUMN +" <=  ?";
    
    private static String updateMTimeForRecordStatement = "UPDATE " + RECORDS_TABLE + " SET  "+    
            MTIME_COLUMN + " = ? "+
            "WHERE "+
            ID_COLUMN + "= ?";
    
    
    private static String childrenIdsStatement = "SELECT " + ID_COLUMN +" FROM " + RECORDS_TABLE +
            " WHERE "
            + PARENT_ID_COLUMN + "= ?";

    private static String recordByIdStatement = "SELECT * FROM " + RECORDS_TABLE + " WHERE ID= ?";

    // SELECT mtime FROM ds_records WHERE origin= 'test_base' ORDER BY mtime DESC
    private static final String maxMtimeStatement =
            "SELECT " + MTIME_COLUMN + " FROM " + RECORDS_TABLE +
            " WHERE " + ORIGIN_COLUMN + "= ?" +
            " ORDER BY " + MTIME_COLUMN + " DESC";

    // SELECT mtime FROM ds_records WHERE origin= 'test_base' AND recordtype='record type' ORDER BY mtime DESC
    private static final String maxMtimeTypeStatement =
            "SELECT " + MTIME_COLUMN + " FROM " + RECORDS_TABLE +
            " WHERE " + ORIGIN_COLUMN + "= ?" +
            " AND " + RECORDTYPE_COLUMN + "= ?" +
            " ORDER BY " + MTIME_COLUMN + " DESC";

    // TODO: Optimise this
    // The current implementation creates a temporary table
    // Alternative 1: Make a plain select and step through to the end
    // Alternative 2: First count the number of "hits", then use that as OFFSET
    private static final String maxMtimeAfterWithLimitStatement =
            "SELECT MAX (" + MTIME_COLUMN + ") AS max_mtime, " +
            "       COUNT (*) AS limit_count " +
            "FROM " +
            "( SELECT " + MTIME_COLUMN +
            "  FROM " + RECORDS_TABLE +
            "  WHERE " + ORIGIN_COLUMN + "= ?" +
            "  AND " + MTIME_COLUMN + " > ?" +
            "  ORDER BY " + MTIME_COLUMN + " ASC" +
            "  LIMIT ?) AS max_mtime_sub";

    // TODO: Optimise this after maxMtimeAfterWithLimitStatement has been optimised
    private static final String maxMtimeAfterWithLimitTypeStatement =
            "SELECT MAX (" + MTIME_COLUMN + ") AS max_mtime, " +
            "       COUNT (*) AS limit_count " +
            "FROM " +
            "( SELECT " + MTIME_COLUMN +
            "  FROM " + RECORDS_TABLE +
            "  WHERE " + ORIGIN_COLUMN + "= ?" +
            "  AND " + RECORDTYPE_COLUMN + "= ?" +
            "  AND " + MTIME_COLUMN + " > ?" +
            "  ORDER BY " + MTIME_COLUMN + " ASC" +
            "  LIMIT ?) AS max_mtime_sub";

    //SELECT * FROM  ds_records  WHERE origin= 'test_base' AND mtime  > 1637237120476001 ORDER BY mtime ASC LIMIT 100
    private static final String recordsModifiedAfterStatement =
            "SELECT * FROM " + RECORDS_TABLE +
            " WHERE " +ORIGIN_COLUMN +"= ?" +
            " AND "+MTIME_COLUMN+" > ?" +
            " ORDER BY "+MTIME_COLUMN+ " ASC LIMIT ?";

    //SELECT ID FROM  ds_records  WHERE origin= 'test_base' AND recordtype = 'MANIFESTATION' AND mtime  > 1637237120476001 ORDER BY mtime ASC LIMIT 100
     private static String recordsIDByRecordTypeModifiedAfterStatement =
             "SELECT "+ ID_COLUMN+ " FROM " + RECORDS_TABLE +
             " WHERE " +ORIGIN_COLUMN +"= ?" +
             " AND "+RECORDTYPE_COLUMN+" = ?" +
             " AND "+MTIME_COLUMN+" > ?" +
             " ORDER BY "+MTIME_COLUMN+ " ASC LIMIT ?";
    
    
    //SELECT * FROM  ds_records  WHERE origin= 'test_origin' AND mtime  > 1637237120476001 AND PARENTID IS NOT NULL ORDER BY mtime ASC LIMIT 100
    private static String recordsModifiedAfterChildrenOnlyStatement =
            "SELECT * FROM " + RECORDS_TABLE +
            " WHERE +"+ORIGIN_COLUMN +"= ?" +
            " AND "+MTIME_COLUMN+" > ?" +
            " AND "+PARENT_ID_COLUMN+" IS NOT NULL"+
            " ORDER BY "+MTIME_COLUMN+ " ASC LIMIT ?";

    //SELECT * FROM  ds_records  WHERE origin= 'test_origin' AND mtime  > 1637237120476001 AND parentId IS NULL ORDER BY mtime ASC LIMIT 100	
    private static String recordsModifiedAfterParentsOnlyStatement =
            "SELECT * FROM " + RECORDS_TABLE +
            " WHERE +"+ORIGIN_COLUMN +"= ?" +
            " AND "+MTIME_COLUMN+" > ?" +
            " AND "+PARENT_ID_COLUMN+" IS NULL"+
            " ORDER BY "+MTIME_COLUMN+ " ASC LIMIT ?";


    private static String originsStatisticsStatement = "SELECT " + ORIGIN_COLUMN + " ,COUNT(*) AS COUNT , SUM("+DELETED_COLUMN+") AS deleted,  max("+MTIME_COLUMN + ") AS MAX FROM " + RECORDS_TABLE + " group by " + ORIGIN_COLUMN;
    private static String deleteMarkedForDeleteStatement = "DELETE FROM " + RECORDS_TABLE + " WHERE "+ORIGIN_COLUMN +" = ? AND "+DELETED_COLUMN +" = 1" ;   
    private static String recordIdExistsStatement = "SELECT COUNT(*) AS COUNT FROM " + RECORDS_TABLE+ " WHERE "+ID_COLUMN +" = ?";
    private static String countRecordsInOriginStatement = "SELECT COUNT(*) FROM " + RECORDS_TABLE +  " WHERE " + ORIGIN_COLUMN + " = ? AND " + MTIME_COLUMN + " > ?";


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

        log.info("DsStorage initialized with driverName='{}', driverURL='{}'", driverName, driverUrl);
    }

    public DsStorage() throws SQLException {
        connection = dataSource.getConnection();
    }
    
    /*
     * Load a record. Will not load childrenIds
     */
    public DsRecordDto loadRecord(String id) throws SQLException {
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
    
    /**
     * Load a record and also load children ids
     *  Return null if record does not exist
     */
    public DsRecordDto loadRecordWithChildIds(String id) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(recordByIdStatement);) {
            stmt.setString(1, id);

            try (ResultSet rs = stmt.executeQuery();) {
                if (!rs.next()) {
                    return null;
                }
                DsRecordDto  record = createRecordFromRS(rs);

                //load children                
                record.setChildrenIds(getChildrenIds(id));                
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

    /**
     * Will only extract with records strightly  larger than mTime!
     * Will be sorted by mTime. Latest is last
     * 
     * Only parents posts (those that have children) will be load or only children (those that have parent)
     * 
     */
    public ArrayList<DsRecordDto > getModifiedAfterParentsOnly(String origin, long mTime, int batchSize) throws Exception {

        if (batchSize <1 || batchSize > 100000) { //No doom switch
            throw new Exception("Batchsize must be in range 1 to 100000");			
        }
        ArrayList<DsRecordDto > records = new ArrayList<DsRecordDto >();
        try (PreparedStatement stmt = connection.prepareStatement(recordsModifiedAfterParentsOnlyStatement);) {

            stmt.setString(1, origin);
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
            throw new Exception("SQL error getModifiedAfterParentsOn",e);

        }

        return records;	
    }

    /**
     * Extract max {@code record.mTime} in {@code origin}.
     * @param origin only records from the {@code origin} will be inspected.
     * @param recordType only records with the given type will be inspected.
     * @return max {@code record.mTime} within the given {@code origin} and woth the given {@code recordType} or 0
     *         if there were no records.
     */
    public long getMaxMtime(String origin, RecordTypeDto recordType) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(maxMtimeTypeStatement)) {
            stmt.setString(1, origin);
            stmt.setString(2, recordType.getValue());
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getLong(MTIME_COLUMN) : 0;
            }
        } catch(Exception e) {
            String message = "SQL Exception in getMaxMtime with recordType";
            log.error(message);
            throw new SQLException(message, e);
        }
    }

    /**
     * Extract max {@code record.mTime} in {@code origin}.
     * @param origin only records from the {@code origin} will be inspected.
     * @return max {@code record.mTime} within the given {@code origin} or 0 if there were no records.
     */
    public long getMaxMtime(String origin) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(maxMtimeStatement)) {
            stmt.setString(1, origin);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getLong(MTIME_COLUMN) : 0;
            }
        } catch(Exception e) {
            String message = "SQL Exception in getMaxMtime";
            log.error(message);
            throw new SQLException(message, e);
        }
    }

    /**
     * Extract max {@code record.mTime}, where {@code record.mTime > mTime} in {@code origin},
     * ordered by {@code record.mTime} and limited to {@code maxRecords}.
     * Secondarily, check whether there are any records with record.mTime higher than the returned
     * maximum mTime.
     * @param origin only records from the {@code origin} will be inspected.
     * @param mTime only records with modification time larger than {@code mTime} will be inspected.
     * @param maxRecords only this number of records will be inspected. {@code -1} means no limit.
     * @return pair of (maximum {@code record.mTime} or null if no match, true if there exists at
     *         least 1 record with {@code record.mTime} higher than the maximum within the constraints).
     */
    public Pair<Long, Boolean> getMaxMtimeAfter(String origin, long mTime, long maxRecords) throws SQLException {
        // No maxRecords is simple: Just check the last record.mTime > mTime
        if (maxRecords == -1) {
            long maxMtime = getMaxMtime(origin);
            return new Pair<>(maxMtime == 0L || maxMtime <= mTime ? null : maxMtime,
                              false);
        }

        // Determine max record.mTime and count the number of records within the limits
        Long maxMTime = null;
        Long totalCount = null;
        try (PreparedStatement stmt = connection.prepareStatement(maxMtimeAfterWithLimitStatement)) {
            stmt.setString(1, origin);
            stmt.setLong(2, mTime);
            stmt.setLong(3, maxRecords);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    maxMTime = rs.getLong("max_mtime");
                    totalCount = rs.getLong("limit_count");
                }
            }
        } catch(Exception e) {
            String message = "SQL Exception in getMaxMtimeAfter(origin='" + origin + "', mTime=" + mTime +
                    ", maxRecords=" + maxRecords + ")";
            log.error(message);
            throw new SQLException(message, e);
        }

        if (maxMTime == null) { // No match (and no subsequent records)
            return new Pair<>(null, false);
        }

        if (totalCount <maxRecords) { // Exhaustive match (no subsequent records)
            return new Pair<>(maxMTime, false);
        }
        
        // Check whether there are extra records available (extra call, but a light one)
        long absoluteMaxMtime = getMaxMtime(origin);
        return maxMTime < absoluteMaxMtime ?
                new Pair<>(maxMTime, true) : // Subsequent records available
                new Pair<>(maxMTime, false); // No subsequent records
    }

    /**
     * Extract max {@code record.mTime}, where {@code record.mTime > mTime} in {@code origin},
     * ordered by {@code record.mTime} and limited to {@code maxRecords}.
     * Secondarily, check whether there are any records with record.mTime higher than the returned
     * maximum mTime.
     * @param origin only records from the {@code origin} will be inspected.
     * @param recordType only records with the given type will be inspected.
     * @param mTime only records with modification time larger than {@code mTime} will be inspected.
     * @param maxRecords only this number of records will be inspected. {@code -1} means no limit.
     * @return pair of (maximum {@code record.mTime} or null if no match, true if there exists at
     *         least 1 record with {@code record.mTime} higher than the maximum within the constraints).
     */
    public Pair<Long, Boolean> getMaxMtimeAfter(String origin, RecordTypeDto recordType, long mTime, long maxRecords)
            throws SQLException {
        // No maxRecords is simple: Just check the last record.mTime > mTime
        if (maxRecords == -1) {
            long maxMtime = getMaxMtime(origin, recordType);
            return new Pair<>(maxMtime == 0L || maxMtime <= mTime ? null : maxMtime,
                              false);
        }

        // Determine max record.mTime and count the number of records within the limits
        Long maxMTime = null;
        Long totalCount = null;
        try (PreparedStatement stmt = connection.prepareStatement(maxMtimeAfterWithLimitTypeStatement)) {
            stmt.setString(1, origin);
            stmt.setString(2, recordType.getValue());
            stmt.setLong(3, mTime);
            stmt.setLong(4, maxRecords);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    maxMTime = rs.getLong("max_mtime");
                    totalCount = rs.getLong("limit_count");
                }
            }
        } catch(Exception e) {
            String message = "SQL Exception in getMaxMtimeAfter(origin='" + origin + "', recordType='" + recordType +
                             "', mTime=" + mTime + ", maxRecords=" + maxRecords + ")";
            log.error(message);
            throw new SQLException(message, e);
        }

        if (maxMTime == null) { // No match (and no subsequent records)
            return new Pair<>(null, false);
        }

        if (totalCount <maxRecords) { // Exhaustive match (no subsequent records)
            return new Pair<>(maxMTime, false);
        }

        // Check whether there are extra records available (extra call, but a light one)
        long absoluteMaxMtime = getMaxMtime(origin, recordType);
        return maxMTime < absoluteMaxMtime ?
                new Pair<>(maxMTime, true) : // Subsequent records available
                new Pair<>(maxMTime, false); // No subsequent records
    }

    /**
     * Will only extract with records strightly  larger than mTime!
     * Will be sorted by mTime. Latest is last
     *
     * Will extract all no matter of parent or child ids
     *
     */
    public ArrayList<DsRecordDto > getRecordsModifiedAfter(String origin, long mTime, int batchSize) throws Exception {

        if (batchSize <1 || batchSize > 10000) { //No doom switch
            throw new Exception("Batchsize must be in range 1 to 10000");
        }
        ArrayList<DsRecordDto> records = new ArrayList<DsRecordDto>();
        try (PreparedStatement stmt = connection.prepareStatement(recordsModifiedAfterStatement);) {

            stmt.setString(1, origin);
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
            String message = "SQL Exception in getRecordsModifiedAfter";
            log.error(message);
            throw new SQLException(message, e);
        }

        return records;
    }

    

    /**
     * Will only extract ID. 
     * Will be sorted by mTime. Latest is last     * 
     * Will extract all no matter of parent or child ids
     * 
     */
    public ArrayList<String> getRecordsIdsByRecordTypeModifiedAfter(String origin, RecordTypeDto recordType, long mTime, int batchSize) throws Exception {

        if (batchSize <1 || batchSize > 10000) { //No doom switch
            throw new Exception("Batchsize must be in range 1 to 10000");   
        }
        ArrayList<String> recordsIds = new ArrayList<String>();
        try (PreparedStatement stmt = connection.prepareStatement(recordsIDByRecordTypeModifiedAfterStatement);) {
                       
            stmt.setString(1, origin);
            stmt.setString(2, recordType.getValue());
            stmt.setLong(3, mTime);
            stmt.setLong(4, batchSize);
            try (ResultSet rs = stmt.executeQuery();) {
                while (rs.next()) {                    
                    recordsIds.add(rs.getString(ID_COLUMN));
                }
            }
        }
        catch(Exception e) {
            String message = "SQL Exception in getRecordsIdsByRecordTypeModifiedAfter";
            log.error(message);
            throw new SQLException(message, e);
        }

        return recordsIds; 
    }
    


    /**
     * Will only extract with records strightly larger than mTime!
     * Will be sorted by mTime. Latest is last
     * 
     * Will only fetch children records. That is those that has a parent.
     * 
     */
    public ArrayList<DsRecordDto>  getModifiedAfterChildrenOnly(String origin, long mTime, int batchSize) throws Exception {

        if (batchSize <1 || batchSize > 100000) { //No doom switch
            throw new Exception("Batchsize must be in range 1 to 100000");			
        }
        ArrayList<DsRecordDto> records = new ArrayList<DsRecordDto>();
        try (PreparedStatement stmt = connection.prepareStatement(recordsModifiedAfterChildrenOnlyStatement);) {

            stmt.setString(1, origin);
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
            log.error(message);
            throw new SQLException(message, e);
        }

        return records;	
    }

    public ArrayList<OriginCountDto> getOriginStatictics() throws SQLException {

        ArrayList<OriginCountDto> originCountList = new ArrayList<OriginCountDto>();
        try (PreparedStatement stmt = connection.prepareStatement(originsStatisticsStatement);) {
            
            try (ResultSet rs = stmt.executeQuery();) {
                while (rs.next()) {
                    OriginCountDto originStats = new OriginCountDto();                    
                    String origin = rs.getString(ORIGIN_COLUMN);
                    long count = rs.getLong("COUNT");
                    long deleted = rs.getLong("DELETED");
                    long lastMTime = rs.getLong("MAX");
                    originStats.setOrigin(origin);                    
                    originStats.setCount(count);
                    originStats.setDeleted(deleted);
                    originCountList.add(originStats);
                    originStats.setLatestMTime(lastMTime);
                    originStats.setLastMTimeHuman(convertToHumanDate(lastMTime));                    
                }
            }
        }
        return originCountList;
    }

    /**
     * Get total amount of records for a specific {@link #ORIGIN_COLUMN}.
     * @param origin the origin to query for in the database.
     * @param mTime  is needed to only deliver the values that are actually extracted.
     * @return the amount of records for the specified origin.
     */
    public Long getAmountOfRecordsForOrigin(String origin, Long mTime) throws SQLException {
        long recordsInOrigin = 0L;
        try (PreparedStatement statement = connection.prepareStatement(countRecordsInOriginStatement)){
            statement.setString(1, origin);
            statement.setLong(2, Objects.requireNonNullElse(mTime, 0L));

            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()){
                    recordsInOrigin = rs.getLong(1);
                }
            }
        }

        return recordsInOrigin;
    }

    public void createNewRecord(DsRecordDto record) throws Exception {

        // Sanity check
        if (record.getId() == null) {
            throw new Exception("Id must not be null"); // TODO exception enum types, messages?
        }
        if (record.getId().equals(record.getParentId())) {
            throw new Exception("Record with id has itself as parent:" + record.getId());
        }

        if (record.getIdError() == null) {
            record.setIdError(false); // can not make default to work in open API.            
        }
        long nowStamp = UniqueTimestampGenerator.next();
        //log.debug("Creating new record: " + record.getId());

        try (PreparedStatement stmt = connection.prepareStatement(createRecordStatement);) {
            stmt.setString(1, record.getId());
            stmt.setString(2, record.getOrigin());
            stmt.setString(3, record.getOrgid());                        
            stmt.setString(4, record.getRecordType().getValue());
            stmt.setInt(5, boolToInt(record.getIdError()));                
            stmt.setInt(6, 0);
            stmt.setLong(7, nowStamp);
            stmt.setLong(8, nowStamp);
            stmt.setString(9, record.getData());
            stmt.setString(10, record.getParentId());
            stmt.setString(11, record.getKalturaReferenceId());
            stmt.setString(12, record.getKalturaInternalId()); //This value is probably null. It will be updated by a batch job later. 
            stmt.executeUpdate();

        } catch (SQLException e) {
            String message = "SQL Exception in createNewRecord with id:" + record.getId() + " error:" + e.getMessage();
            log.error(message);
            throw new SQLException(message, e);
        }

    }

    
    public int updateMTimeForRecord(String recordId) throws Exception {
        // Sanity check
        if (recordId == null) {
            throw new Exception("Id must not be null"); // TODO exception enum types, messages?
        }

        long nowStamp = UniqueTimestampGenerator.next();
        //log.debug("Creating new record: " + record.getId());

        try (PreparedStatement stmt = connection.prepareStatement(updateMTimeForRecordStatement);) {  
            stmt.setLong(1, nowStamp);      
            stmt.setString(2, recordId);
           int numberUpdated =  stmt.executeUpdate();           
           return numberUpdated;
        } catch (SQLException e) {
            String message = "SQL Exception in updateMTimeForRecord with id:" + recordId + " error:" + e.getMessage();
            log.error(message);
            throw new SQLException(message, e);
        }
    }
    
    
    
      /**
       * Create a new entry in the mapping table. The kalturainternal id can be null and will be updated by a job later.
       * 
       * @param mapping The mapping.id must not be null. The kalturainternalid can be null-
       * @throws Exception If mapping.id already exists.
       */
     public void createNewMapping(MappingDto mappingDto) throws Exception {

        if (mappingDto.getId() == null) {
            throw new InvalidArgumentServiceException("Id must not be null"); // TODO exception enum types, messages?
        }

        try (PreparedStatement stmt = connection.prepareStatement(createMapping);) {
            stmt.setString(1, mappingDto.getId());
            stmt.setString(2, mappingDto.getKalturainternalid()); 
            stmt.executeUpdate();

        } catch (SQLException e) {
            String message = "SQL Exception in createNewMappingwith id:" + mappingDto.getId() + " error:" + e.getMessage();
            log.error(message);
            throw new SQLException(message, e);
        }
    }
    
     
     /**
      * Create a new entry in the mapping table. The kalturainternal id can be null and will be updated by a job later.
      * 
      * @param mappingId The id key for the mapping
      * @return The mappingDto or null if the id is mappingId is not found
      * 
      * @throws Exception
      */
    public MappingDto getMappingById(String mappingId) throws Exception {

       if (mappingId == null) {
           throw new InvalidArgumentServiceException("Id must not be null"); // TODO exception enum types, messages?
       }

       try (PreparedStatement stmt = connection.prepareStatement(mappingByIdStatement);) {
           stmt.setString(1, mappingId);
 
           try (ResultSet rs = stmt.executeQuery();) {
               if (!rs.next()) {
                 return null; //ID not found
               }
             return createMappingFromRS(rs);               
           }           

       } catch (SQLException e) {
           String message = "SQL Exception in getMappingById( id:" + mappingId + " error:" + e.getMessage();
           log.error(message);
           throw new SQLException(message, e);
       }
   }
   
     
     
    public int markRecordForDelete(String recordId) throws Exception {

        // Sanity check
        if (recordId == null) {
            throw new Exception("Id must not be null"); // TODO exception enum types, messages?
        }

        long nowStamp = UniqueTimestampGenerator.next();
        //log.debug("Creating new record: " + record.getId());

        try (PreparedStatement stmt = connection.prepareStatement(markRecordForDeleteStatement);) {		
            stmt.setLong(1, nowStamp);						
            stmt.setString(2, recordId);
           int numberUpdated =  stmt.executeUpdate();           
           return numberUpdated;
        } catch (SQLException e) {
            String message = "SQL Exception in markRecordForDelete  with id:" + recordId + " error:" + e.getMessage();
            log.error(message);
            throw new SQLException(message, e);
        }

    }
    
    /**
     * Delete all records for an origin that has been modified time interval. The records will be deleted and not just marked for deletion
     * 
     * @param origin The origin for the collection. Value must be define in the configuration
     * @param mTimeFrom modified time from. Format is millis +3 digits
     * @param mTimeTo modified time to. Format is millis +3 digits
     */    
    public int deleteRecordsForOrigin(String origin, long mTimeFrom,long mTimeTo) throws Exception {    	
        try (PreparedStatement stmt = connection.prepareStatement(deleteRecordsForOriginStateMent);) {		
            stmt.setString(1, origin);						
            stmt.setLong(2, mTimeFrom);
            stmt.setLong(3, mTimeTo);            
            return stmt.executeUpdate();                       
        } catch (SQLException e) {
            String message = "SQL Exception in deleteRecordsForOrigin for origin:" + origin + " error:" + e.getMessage();
            log.error(message,e);
            throw new SQLException(message, e);
        }
    }
    
    
    public int deleteMarkedForDelete(String origin) throws Exception {

        // Sanity check
        if (origin == null) {
            throw new Exception("Origin must not be null"); // TODO exception enum types, messages?
        }
    
        try (PreparedStatement stmt = connection.prepareStatement(deleteMarkedForDeleteStatement);) {        
            stmt.setString(1, origin);
            int numberDeleted = stmt.executeUpdate();
            return numberDeleted;                    
            
        } catch (SQLException e) {
            String message = "SQL Exception in deleteMarkedForDelete for origin:" + origin + " error:" + e.getMessage();
            log.error(message);
            throw new SQLException(message, e);
        }

    }

    
    /**
     * 
     * Update a mapping with a new internalKalturaId. The internalKalturaId can be null and remove and existing value
     * 
     * @param mappingDto
     * @throws Exception Will throw exception if id is not found
     */     
    public void updateMapping(MappingDto mappingDto) throws Exception {
       // Sanity check
        if (mappingDto.getId() == null) {
            throw new InvalidArgumentServiceException("Id must not be null"); 
        }
                                              
        try (PreparedStatement stmt = connection.prepareStatement(updateMappingStatement);) {
            stmt.setString(1, mappingDto.getKalturainternalid());
            stmt.setString(2, mappingDto.getId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            String message = "SQL Exception in  updateMapping with id:" + mappingDto.getId() + " error:" + e.getMessage();
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
            stmt.setString(1, record.getRecordType().getValue());
            stmt.setString(2, record.getData());
            stmt.setLong(3, nowStamp);			
            stmt.setString(4, record.getKalturaReferenceId());
            stmt.setString(5, record.getParentId());
            stmt.setString(6, record.getId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            String message = "SQL Exception in updateRecord with id:" + record.getId() + " error:" + e.getMessage();
            log.error(message);
            throw new SQLException(message, e);
        }
    }


    public void updateKalturaInternalForRecord(String kalturaReferenceId, String kalturaId) throws Exception {
      
        long nowStamp = UniqueTimestampGenerator.next();      
        try (PreparedStatement stmt = connection.prepareStatement(updateKalturaReferenceIdStatement);) {
            stmt.setString(1, kalturaId);
            stmt.setLong(2, nowStamp);
            stmt.setString(3, kalturaReferenceId);  
            stmt.executeUpdate();
        } catch (SQLException e) {
            String message = "SQL Exception in updateKalturaInternal for kalturaReferenceId:" + kalturaReferenceId + " error:" + e.getMessage();
            log.error(message);
            throw new SQLException(message, e);
        }

    }
    
    
    private static MappingDto createMappingFromRS(ResultSet rs) throws SQLException {

        String id = rs.getString(MAPPING_ID_COLUMN);
        String kalturaId = rs.getString(KALTURA_INTERNAL_ID_COLUMN);

        MappingDto mapping = new MappingDto();
        mapping.setId(id);
        mapping.setKalturainternalid(kalturaId);        
        return mapping;
    }
    
    
    private static DsRecordDto createRecordFromRS(ResultSet rs) throws SQLException {

        String id = rs.getString(ID_COLUMN);
        String origin = rs.getString(ORIGIN_COLUMN);
        boolean idError = rs.getInt(IDERROR_COLUMN) == 1;
        String orgid = rs.getString(ORGID_COLUMN);
        String recordType = rs.getString(RECORDTYPE_COLUMN);
        boolean deleted = rs.getInt(DELETED_COLUMN) == 1;		                
        String data = rs.getString(DATA_COLUMN);
        long cTime = rs.getLong(CTIME_COLUMN);
        long mTime = rs.getLong(MTIME_COLUMN);
        String parentId = rs.getString(PARENT_ID_COLUMN);
        String kalturaReferenceId = rs.getString(KALTURA_REFERENCE_ID_COLUMN);
        String kalturaInternalId = rs.getString(KALTURA_INTERNAL_ID_COLUMN);
        
        DsRecordDto record = new DsRecordDto();
        record.setId(id);
        record.setOrigin(origin);
        record.setOrgid(orgid);
        record.setRecordType(RecordTypeDto.valueOf(recordType));
        record.setIdError(idError);
        record.setData(data);
        record.setParentId(parentId);
        record.setcTime(cTime);
        record.setmTime(mTime);
        record.setDeleted(deleted);
        record.setKalturaReferenceId(kalturaReferenceId);
        record.setKalturaInternalId(kalturaInternalId);

        //Set the two dates as human readable
        record.setcTimeHuman(convertToHumanDate(cTime));
        record.setmTimeHuman(convertToHumanDate(mTime));
        
        return record;
    }

    private static int boolToInt(Boolean isTrue) {
        if (isTrue == null) {
            return 0;
        }

        return isTrue ? 1 : 0;
    }
    
   /*
   * Method is syncronized because simpledateformat is not thread safe. Faster to reuse syncronized than to construct new every time.
   */
    private static synchronized String convertToHumanDate(long millis_time_1000) {
     return dateFormat.format(new Date(millis_time_1000/1000));
        
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


}
