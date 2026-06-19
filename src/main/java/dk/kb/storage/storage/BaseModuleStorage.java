package dk.kb.storage.storage;

import dk.kb.storage.config.ServiceConfig;
import dk.kb.util.webservice.exception.InternalServiceException;
import dk.kb.util.webservice.exception.InvalidArgumentServiceException;
import org.apache.commons.dbcp2.BasicDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Date;

/**
 * The BaseModuleStorage, which sets up the connection to the database which is then used by {@link DsStorage}, {@link RerunClusterStorage}.
 * This class only sets up the connection, while the other three are responsible for implementing the interactions with the database.
 */
public abstract class BaseModuleStorage implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(BaseModuleStorage.class);

    // statistics shown on monitor.jsp page
    public static Date INITDATE = null;

    protected Connection connection = null; // private
    protected static BasicDataSource dataSource = null; // shared

    public BaseModuleStorage() throws SQLException {
        connection = dataSource.getConnection();
    }

    /**
     * Initialize the connection to a database
     *
     * @param driverName name of the database driver
     * @param driverUrl  url of the database
     * @param userName   database username
     * @param password   password for the user
     */
    public static void initialize(String driverName, String driverUrl, String userName, String password) {
        int connectionPoolSize = ServiceConfig.getConnectionPoolSize();

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
        //Idle settings defaults (min/max) has good values.
        dataSource.setMaxOpenPreparedStatements(connectionPoolSize);
        INITDATE = new Date();

        log.info("DsStorage BaseModuleStorage initialized with driverName='{}', driverURL='{}', connectionPoolSize='{}' ", driverName, driverUrl, connectionPoolSize);
    }

    /**
     * Close the connection to the database. You should probably perform a commit or rollback before closing the connection.
     */
    @Override
    public void close() {
        try {
            connection.close();
        } catch (Exception e) {
            // nothing to do here
        }
    }

    /**
     * Commit all changes made since the last rollback or commit
     */
    public void commit() throws SQLException {
        connection.commit();
    }

    /**
     * Rollback all changes in the current transaction.
     */
    public void rollback() {
        try {
            connection.rollback();
        } catch (Exception e) {
            // nothing to do here
        }
    }

    /**
     * This is called by from InialialziationContextListener by the Web-container when server is shutdown,
     * just to be sure the DB lock file is free.
     */
    public static void shutdown() {
        log.info("Shutdown ds-storage");
        try {
            if (dataSource != null) {
                dataSource.close();
            }
        } catch (SQLException e) {
            // ignore errors during shutdown, we cant do anything about it anyway
            log.error("shutdown failed", e);
        }
    }

    /**
     * Start a storage transaction and performs the given action on it, returning the result from the action.
     * <p>
     * If the action throws an exception, a {@link BaseModuleStorage#rollback()} is performed.
     * If the action passes without exceptions, a {@link BaseModuleStorage#commit()} is performed.
     *
     * @param actionID     a debug-oriented ID for the action, typically the name of the calling method.
     * @param storageClass what Storage class triggered the method
     * @param action       the action to perform on the storage.
     * @return return value from the action.
     * @throws InternalServiceException if anything goes wrong.
     */
    public static <T> T performStorageAction(String actionID,
                                             Class<? extends BaseModuleStorage> storageClass,
                                             BaseModuleStorage.StorageAction<T> action) {
        long start = System.currentTimeMillis();
        try (BaseModuleStorage storage = storageClass.getDeclaredConstructor().newInstance()) {
            T result;
            try {
                result = action.process(storage);
            } catch (InvalidArgumentServiceException e) {
                log.warn("Exception performing action '{}'. Initiating rollback", actionID, e.getMessage());
                storage.rollback();
                throw new InvalidArgumentServiceException(e);
            } catch (Exception e) {
                log.warn("Exception performing action '{}'. Initiating rollback", actionID, e);
                storage.rollback();
                throw new InternalServiceException(e);
            }

            try {
                storage.commit();
            } catch (SQLException e) {
                log.error("Exception committing after action '{}'", actionID, e);
                throw new InternalServiceException(e);
            }

            log.debug("ds-storage method '{}' SQL time in millis: {} ", actionID, (System.currentTimeMillis() - start));
            return result;
        } catch (Exception e) {
            log.error("Exception performing action '{}'", actionID, e);
            throw new InternalServiceException(e);
        }
    }

    /**
     * Callback used with {@link #performStorageAction(String, Class, StorageAction)}.
     *
     * @param <T> the object returned from the {@link BaseModuleStorage.StorageAction#process(BaseModuleStorage)} method.
     */
    @FunctionalInterface
    public interface StorageAction<T> {
        /**
         * Access or modify the given storage inside a transaction.
         * If the method throws an exception, it will be logged, a {@link BaseModuleStorage#rollback()} will be performed and
         * a wrapping {@link dk.kb.util.webservice.exception.ServiceException} will be thrown.
         *
         * @param storage a storage ready for requests and updates.
         * @return custom return value.
         * @throws Exception if something went wrong.
         */
        T process(BaseModuleStorage storage) throws Exception;
    }
}