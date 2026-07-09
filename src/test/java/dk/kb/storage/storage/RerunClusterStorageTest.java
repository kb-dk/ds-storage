package dk.kb.storage.storage;

import dk.kb.storage.model.v1.RecordsCountDto;
import dk.kb.storage.model.v1.RerunClusterDto;
import org.apache.commons.dbcp2.BasicDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class RerunClusterStorageTest {

    private BasicDataSource mockedDataSource;
    private Connection mockedConnection;
    private PreparedStatement mockedStatement;
    private RerunClusterStorage rerunClusterStorage;

    @BeforeEach
    public void setUp() throws Exception {
        // Create all mocks
        mockedDataSource = Mockito.mock(BasicDataSource.class);
        mockedConnection = Mockito.mock(Connection.class);
        mockedStatement = Mockito.mock(PreparedStatement.class);

        // Configure mocks
        Mockito.when(mockedDataSource.getConnection()).thenReturn(mockedConnection);
        Mockito.when(mockedConnection.prepareStatement(Mockito.anyString()))
                .thenReturn(mockedStatement);

        BaseModuleStorage.dataSource = mockedDataSource;

        // Now create the instance (constructor will use mocked dataSource)
        rerunClusterStorage = new RerunClusterStorage();
    }

    @AfterEach
    public void tearDown() {
        // Close resources if needed
        if (rerunClusterStorage != null) {
            rerunClusterStorage.close();
        }
    }

    @Test
    public void updateRerunClustersTable_whenNewRowsIsPresent_thenReturnHowManyRowsWasInsertedOrUpdated()
            throws Exception {
        // Arrange
        ResultSet resultSet = Mockito.mock(ResultSet.class);

        // Mock the column getters by name
        Mockito.when(resultSet.getInt("rerun_clusters_count")).thenReturn(1);
        Mockito.when(resultSet.getInt("ds_records_count")).thenReturn(1);
        Mockito.when(mockedStatement.executeQuery()).thenReturn(resultSet);

        // Act
        RecordsCountDto result = rerunClusterStorage.updateRerunClustersTable();

        // Assert
        Mockito.verify(mockedStatement, Mockito.times(1)).executeQuery();
        assertNotNull(result);
        assertEquals(1, result.getCount());
    }

    @Test
    public void getRerunClusterByFileId_whenFileIdExists_thenReturnRerunClusterDto() throws Exception {
        // Arrange
        UUID id = UUID.fromString("0011e17f-2fa0-454f-98d2-f1c690de2df1");
        UUID fileId = UUID.fromString("0022e17f-2fa0-454f-98d2-f1c690de2df1");
        UUID rerunClusterId = UUID.fromString("9c79bde1-9030-47a8-bb5f-3abaf2bb4ecf");
        Integer rerunClusterIdCount = 2;
        OffsetDateTime created = OffsetDateTime.parse("2026-04-30T12:26:57.570Z");
        UUID jobId = UUID.fromString("0033e17f-2fa0-454f-98d2-f1c690de2df1");
        OffsetDateTime inserted = OffsetDateTime.parse("2026-06-01T12:26:57.570Z");
        OffsetDateTime updated = OffsetDateTime.parse("2026-06-04T12:26:57.570Z");

        ResultSet resultSet = Mockito.mock(ResultSet.class);

        // Mock next() to return true once, then false (simulates one row)
        Mockito.when(resultSet.next()).thenReturn(true).thenReturn(false);

        // Mock the column getters by name
        Mockito.when(resultSet.getObject("id", UUID.class)).thenReturn(id);
        Mockito.when(resultSet.getObject("file_id", UUID.class)).thenReturn(fileId);
        Mockito.when(resultSet.getObject("rerun_cluster_id", UUID.class)).thenReturn(rerunClusterId);
        Mockito.when(resultSet.getInt("rerun_cluster_id_count")).thenReturn(rerunClusterIdCount);
        Mockito.when(resultSet.getObject("created", OffsetDateTime.class)).thenReturn(created);
        Mockito.when(resultSet.getObject("job_id", UUID.class)).thenReturn(jobId);
        Mockito.when(resultSet.getObject("inserted", OffsetDateTime.class)).thenReturn(inserted);
        Mockito.when(resultSet.getObject("updated", OffsetDateTime.class)).thenReturn(updated);

        Mockito.when(mockedStatement.executeQuery()).thenReturn(resultSet);

        // Act
        RerunClusterDto returnedRerunClusterDto = rerunClusterStorage.getRerunClusterByFileId(fileId);

        // Assert
        Mockito.verify(mockedStatement, Mockito.times(1)).executeQuery();
        assertNotNull(returnedRerunClusterDto);
        assertEquals(id, returnedRerunClusterDto.getId());
        assertEquals(fileId, returnedRerunClusterDto.getFileId());
        assertEquals(rerunClusterId, returnedRerunClusterDto.getRerunClusterId());
        assertEquals(rerunClusterIdCount, returnedRerunClusterDto.getRerunClusterIdCount());
        assertEquals(created, returnedRerunClusterDto.getCreated());
        assertEquals(jobId, returnedRerunClusterDto.getJobId());
        assertEquals(inserted, returnedRerunClusterDto.getInserted());
        assertEquals(updated, returnedRerunClusterDto.getUpdated());
    }

    @Test
    public void getRerunClusterByFileId_whenFileIdDoNotExists_thenReturnNull() throws Exception {
        // Arrange
        UUID fileId = UUID.fromString("0022e17f-2fa0-454f-98d2-f1c690de2df1");
        ResultSet resultSet = Mockito.mock(ResultSet.class);

        // Mock next() to return false once simulate zero rows
        Mockito.when(resultSet.next()).thenReturn(false);
        Mockito.when(mockedStatement.executeQuery()).thenReturn(resultSet);

        // Act
        RerunClusterDto rerunClusterDto = rerunClusterStorage.getRerunClusterByFileId(fileId);

        // Assert
        Mockito.verify(mockedStatement, Mockito.times(1)).executeQuery();
        assertNull(rerunClusterDto);
    }
}
