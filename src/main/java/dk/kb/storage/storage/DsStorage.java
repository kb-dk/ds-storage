package dk.kb.storage.storage;



import org.apache.commons.dbcp2.BasicDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class DsStorage implements AutoCloseable {

	private static final Logger log = LoggerFactory.getLogger(DsStorage.class);



	private static final String RECORDS_TABLE = "ds_records";
	private static final String ID_COLUMN = "id";
	private static final String BASE_COLUMN = "base";
	private static final String DELETED_COLUMN = "deleted";
	private static final String INDEXABLE_COLUMN = "indexable";
	private static final String DATA_COLUMN = "data";
	private static final String CTIME_COLUMN = "ctime";
	private static final String MTIME_COLUMN = "mtime";
	private static final String PARENT_ID_COLUMN = "parentId";


	private static String createRecordStatement="INSERT INTO " + RECORDS_TABLE + " ("
			+ ID_COLUMN + ", "
			+ BASE_COLUMN + ", "
			+ DELETED_COLUMN + ", "
			+ INDEXABLE_COLUMN + ", "
			+ CTIME_COLUMN + ", "
			+ MTIME_COLUMN + ", "
			+ DATA_COLUMN +  " "
			+ ") VALUES (?,?,?,?,?,?,?)";
	
	private static String getRecordByIdStatement="SELECT * FROM " + RECORDS_TABLE + " WHERE ID= ?";
	
	private static BasicDataSource dataSource;


	// statistics shown on monitor.jsp page
	public static Date INITDATE = null;
	private static UniqueTimestampGenerator timestampGenerator = new UniqueTimestampGenerator();

	private Connection connection;


	public static void initialize(String driverName, String driverUrl, String userName, String password) {
		dataSource = new BasicDataSource();
		dataSource.setDriverClassName(driverName);
		dataSource.setUsername(userName);
		dataSource.setPassword(password);
		dataSource.setUrl(driverUrl);

		dataSource.setDefaultReadOnly(false);
		dataSource.setDefaultAutoCommit(false);

		//enable detection and logging of connection leaks
		/*
        dataSource.setRemoveAbandonedOnBorrow(
                AlmaPickupNumbersPropertiesHolder.PICKUPNUMBERS_DATABASE_TIME_BEFORE_RECLAIM > 0);
        dataSource.setRemoveAbandonedOnMaintenance(
                AlmaPickupNumbersPropertiesHolder.PICKUPNUMBERS_DATABASE_TIME_BEFORE_RECLAIM > 0);
        dataSource.setRemoveAbandonedTimeout(AlmaPickupNumbersPropertiesHolder.PICKUPNUMBERS_DATABASE_TIME_BEFORE_RECLAIM); //1 hour
        dataSource.setLogAbandoned(AlmaPickupNumbersPropertiesHolder.PICKUPNUMBERS_DATABASE_TIME_BEFORE_RECLAIM > 0);
        dataSource.setMaxWaitMillis(AlmaPickupNumbersPropertiesHolder.PICKUPNUMBERS_DATABASE_POOL_CONNECT_TIMEOUT);
		 */
		dataSource.setMaxTotal(10); //

		INITDATE = new Date();

		log.info("DsStorage initialized");
	}

	public DsStorage() throws SQLException {
		connection = dataSource.getConnection();

	}


	public DsRecord loadRecord(String id) throws SQLException {
		 try (PreparedStatement stmt = connection.prepareStatement(getRecordByIdStatement);) {
			 stmt.setString(1, id);

			 try (ResultSet rs = stmt.executeQuery();) {
	                if(!rs.next()) {	                	                	
                      return null;// Or throw exception?	
	                }				 
	                DsRecord record = createRecordFromRS(rs);
		            return record;
	            }			 
		 }			
	}
	

	public void createNewRecord(DsRecord record) throws Exception {

		//Sanity check
		if (record.getId() == null) {
			throw new Exception("Id must not be null");		//TODO exception enum types, messages?	
		}
		if (record.getId().equals(record.getParentId())) {
			throw new Exception("Record with id has itself as parent:"+record.getId());			
		}
				
		 long nowStamp = timestampGenerator.next();
  		 log.debug("Creating new record: " + record.getId());
		
  		 try (PreparedStatement stmt = connection.prepareStatement(createRecordStatement);) {
			stmt.setString(1,record.getId());
			stmt.setString(2, record.getBase());
			stmt.setInt(3,0);
			stmt.setInt(4, boolToInt(record.isIndexable()));
			stmt.setLong(5, nowStamp);
			stmt.setLong(6, nowStamp);
			stmt.setString(7,record.getData());
			stmt.executeUpdate();			
			
		} catch (SQLException e) {
			String message = "SQL Exception in createNewRecord with id:"+record.getId() +" error:"+e.getMessage();
			log.error(message);
			throw new SQLException(message, e);
		}

	}


	private static DsRecord createRecordFromRS(ResultSet rs) throws SQLException {
        
        String id = rs.getString(ID_COLUMN);
        String base = rs.getString(BASE_COLUMN);
        boolean deleted = rs.getInt(DELETED_COLUMN)==1;
        boolean indexable = rs.getInt(INDEXABLE_COLUMN)==1;
        String data = rs.getString(DATA_COLUMN);
        long cTime = rs.getLong(CTIME_COLUMN);
        long mTime = rs.getLong(MTIME_COLUMN);	
        String parentId = rs.getString(PARENT_ID_COLUMN);
               
        DsRecord record = new DsRecord(id, base, indexable, data, parentId);
        record.setcTime(cTime);
        record.setmTime(mTime);
        record.setDeleted(deleted);
        return record;
    }
    
	
	
	/*
	 * Called by by big-sister.
	 */

	public int getCountToday() throws SQLException {
		String todayDate = "TEST";    
		try (PreparedStatement stmt = connection.prepareStatement(
				"SELECT count(*) from pickupnumber where scandate = ? ");) {
			stmt.setString(1, todayDate);
			try (ResultSet rs = stmt.executeQuery();) {
				rs.next(); //Always has 1 row
				int count = rs.getInt(1);
				return count;
			}

		} catch (SQLException e) {
			String message = "SQL Exception in totalCountToday:";
			log.error(message, e);
			throw new SQLException();
		}
	}

	private int boolToInt(boolean isTrue) {
        return isTrue ? 1 : 0;
    }


	public void commit() throws SQLException {
		connection.commit();
	}


	public void rollback() {
		try {
			connection.rollback();
		} catch (Exception e) {
			//nothing to do here
		}
	}

	@Override
	public void close() {
		try {
			connection.close();
		} catch (Exception e) {
			//nothing to do here
		}
	}

	// This is called by from InialialziationContextListener by the Web-container when server is shutdown,
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