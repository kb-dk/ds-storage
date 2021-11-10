package dk.kb.storage.storage;



import org.apache.commons.dbcp2.BasicDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class DsStorage implements AutoCloseable {
    
    private static final Logger log = LoggerFactory.getLogger(DsStorage.class);
    
    
    private static BasicDataSource dataSource;
    //private static SecureRandom random = new SecureRandom();
    
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
