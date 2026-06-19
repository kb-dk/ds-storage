package dk.kb.storage.api.v1.impl;

import dk.kb.storage.config.ServiceConfig;
import dk.kb.storage.model.v1.RerunClusterRequestDto;
import dk.kb.storage.model.v1.RerunClusterResponseDto;
import dk.kb.storage.storage.*;
import dk.kb.storage.util.H2DbUtil;
import dk.kb.util.webservice.exception.InternalServiceException;
import dk.kb.util.webservice.exception.InvalidArgumentServiceException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

import static java.time.ZoneOffset.UTC;
import static org.junit.jupiter.api.Assertions.*;

public class RerunClusterApiServiceImplTest extends UnitTestUtil {
    protected static RerunClusterStorageForUnitTest storage = null;

    @BeforeAll
    public static void beforeClass() throws Exception {
        ServiceConfig.initialize("conf/ds-storage*.yaml");
        H2DbUtil.createEmptyH2DBFromDDL(URL, DRIVER, USERNAME, PASSWORD);
        BaseModuleStorage.initialize(DRIVER, URL, USERNAME, PASSWORD);
        storage = new RerunClusterStorageForUnitTest();
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
        RerunClusterStorage.shutdown();
    }

    @Test
    public void createOrUpdateRerunCluster_whenCreatingRerunCluster_thenReturnCreatedRerunCluster() {
        // Arrange
        UUID fileId = UUID.fromString("0022e17f-2fa0-454f-98d2-f1c690de2df1");
        UUID rerunClusterId = UUID.fromString("9c79bde1-9030-47a8-bb5f-3abaf2bb4ecf");
        OffsetDateTime clusterIdCreationDate = OffsetDateTime.parse("2026-04-30T12:26:57.570+02:00");

        RerunClusterRequestDto rerunClusterRequestDto = new RerunClusterRequestDto();
        rerunClusterRequestDto.setFileId(fileId);
        rerunClusterRequestDto.setRerunClusterId(rerunClusterId);
        rerunClusterRequestDto.setClusterIdCreationDate(clusterIdCreationDate);

        RerunClusterApiServiceImpl rerunClusterApiServiceImpl = new RerunClusterApiServiceImpl();

        // Act
        RerunClusterResponseDto rerunClusterResponseDto = rerunClusterApiServiceImpl.createOrUpdateRerunCluster(rerunClusterRequestDto);

        // Assert
        assertNotNull(rerunClusterResponseDto);
        assertNotNull(rerunClusterResponseDto.getId());
        assertEquals(fileId, rerunClusterResponseDto.getFileId());
        assertEquals(rerunClusterId, rerunClusterResponseDto.getRerunClusterId());
        assertEquals(clusterIdCreationDate, rerunClusterResponseDto.getClusterIdCreationDate());
        assertTrue(OffsetDateTime.now(UTC).isAfter(rerunClusterResponseDto.getCreatedTime()));
        assertTrue(OffsetDateTime.now(UTC).isAfter(rerunClusterResponseDto.getModifiedTime()));
    }

    @Test
    public void createOrUpdateRerunCluster_whenUpdatingRerunCluster_thenReturnUpdatedRerunCluster() {
        // Arrange
        UUID fileId = UUID.fromString("0022e17f-2fa0-454f-98d2-f1c690de2df1");
        UUID rerunClusterId = UUID.fromString("9c79bde1-9030-47a8-bb5f-3abaf2bb4ecf");
        OffsetDateTime clusterIdCreationDate = OffsetDateTime.parse("2026-04-30T12:26:57.570+02:00");

        UUID updateRerunClusterId = UUID.fromString("1a79bde1-9030-47a8-bb5f-3abaf2bb4ecf");
        OffsetDateTime updateClusterIdCreationDate = OffsetDateTime.parse("2026-05-30T00:00:00.001+02:00");

        RerunClusterRequestDto rerunClusterRequestDto = new RerunClusterRequestDto();
        rerunClusterRequestDto.setFileId(fileId);
        rerunClusterRequestDto.setRerunClusterId(rerunClusterId);
        rerunClusterRequestDto.setClusterIdCreationDate(clusterIdCreationDate);

        RerunClusterRequestDto updateRerunClusterRequestDto = new RerunClusterRequestDto();
        updateRerunClusterRequestDto.setFileId(fileId);
        updateRerunClusterRequestDto.setRerunClusterId(updateRerunClusterId);
        updateRerunClusterRequestDto.setClusterIdCreationDate(updateClusterIdCreationDate);

        RerunClusterApiServiceImpl rerunClusterApiServiceImpl = new RerunClusterApiServiceImpl();

        RerunClusterResponseDto createdRerunClusterResponseDto = rerunClusterApiServiceImpl.createOrUpdateRerunCluster(rerunClusterRequestDto);

        // Act
        RerunClusterResponseDto updatedRerunClusterResponseDto = rerunClusterApiServiceImpl.createOrUpdateRerunCluster(updateRerunClusterRequestDto);

        // Assert
        assertNotNull(updatedRerunClusterResponseDto);
        assertEquals(createdRerunClusterResponseDto.getId(), updatedRerunClusterResponseDto.getId());

        assertEquals(createdRerunClusterResponseDto.getFileId(), updatedRerunClusterResponseDto.getFileId());

        assertNotEquals(createdRerunClusterResponseDto.getRerunClusterId(), updatedRerunClusterResponseDto.getRerunClusterId());
        assertEquals(updateRerunClusterId, updatedRerunClusterResponseDto.getRerunClusterId());

        assertNotEquals(createdRerunClusterResponseDto.getClusterIdCreationDate(), updatedRerunClusterResponseDto.getClusterIdCreationDate());
        assertEquals(updateClusterIdCreationDate, updatedRerunClusterResponseDto.getClusterIdCreationDate());

        assertEquals(createdRerunClusterResponseDto.getCreatedTime(), updatedRerunClusterResponseDto.getCreatedTime());
        assertTrue(createdRerunClusterResponseDto.getModifiedTime().isBefore(updatedRerunClusterResponseDto.getModifiedTime()));
    }

    @Test
    public void createOrUpdateRerunCluster_whenFileIdIsNull_thenThrowInvalidArgumentServiceException() {
        // Arrange
        UUID rerunClusterId = UUID.fromString("9c79bde1-9030-47a8-bb5f-3abaf2bb4ecf");
        OffsetDateTime clusterIdCreationDate = OffsetDateTime.parse("2026-04-30T12:26:57.570+02:00");

        RerunClusterRequestDto rerunClusterRequestDto = new RerunClusterRequestDto();
        rerunClusterRequestDto.setFileId(null);
        rerunClusterRequestDto.setRerunClusterId(rerunClusterId);
        rerunClusterRequestDto.setClusterIdCreationDate(clusterIdCreationDate);

        String expectedMessage = "'fileId' can not be null";

        RerunClusterApiServiceImpl rerunClusterApiServiceImpl = new RerunClusterApiServiceImpl();

        // Act
        Exception exception = assertThrows(InvalidArgumentServiceException.class, () -> rerunClusterApiServiceImpl.createOrUpdateRerunCluster(rerunClusterRequestDto));

        // Assert
        assertEquals(expectedMessage, exception.getMessage());
    }

    @Test
    public void createOrUpdateRerunCluster_whenRerunClusterIdIsNull_thenThrowInvalidArgumentServiceException() {
        // Arrange
        UUID fileId = UUID.fromString("0022e17f-2fa0-454f-98d2-f1c690de2df1");
        OffsetDateTime clusterIdCreationDate = OffsetDateTime.parse("2026-04-30T12:26:57.570+02:00");

        RerunClusterRequestDto rerunClusterRequestDto = new RerunClusterRequestDto();
        rerunClusterRequestDto.setFileId(fileId);
        rerunClusterRequestDto.setRerunClusterId(null);
        rerunClusterRequestDto.setClusterIdCreationDate(clusterIdCreationDate);

        String expectedMessage = "'rerunClusterId' can not be null";

        RerunClusterApiServiceImpl rerunClusterApiServiceImpl = new RerunClusterApiServiceImpl();

        // Act
        Exception exception = assertThrows(InvalidArgumentServiceException.class, () -> rerunClusterApiServiceImpl.createOrUpdateRerunCluster(rerunClusterRequestDto));

        // Assert
        assertEquals(expectedMessage, exception.getMessage());
    }

    @Test
    public void createOrUpdateRerunCluster_whenClusterIdCreationDateIdIsNull_thenThrowInvalidArgumentServiceException() {
        // Arrange
        UUID fileId = UUID.fromString("0022e17f-2fa0-454f-98d2-f1c690de2df1");
        UUID rerunClusterId = UUID.fromString("9c79bde1-9030-47a8-bb5f-3abaf2bb4ecf");

        RerunClusterRequestDto rerunClusterRequestDto = new RerunClusterRequestDto();
        rerunClusterRequestDto.setFileId(fileId);
        rerunClusterRequestDto.setRerunClusterId(rerunClusterId);
        rerunClusterRequestDto.setClusterIdCreationDate(null);

        String expectedMessage = "'clusterIdCreationDate' can not be null";

        RerunClusterApiServiceImpl rerunClusterApiServiceImpl = new RerunClusterApiServiceImpl();

        // Act
        Exception exception = assertThrows(InvalidArgumentServiceException.class, () -> rerunClusterApiServiceImpl.createOrUpdateRerunCluster(rerunClusterRequestDto));

        // Assert
        assertEquals(expectedMessage, exception.getMessage());
    }

    @Test
    public void getRerunClusterByFileId_whenFileIdDoNotExists_thenThrowNotFoundException() {
        // Arrange
        UUID fileId = UUID.fromString("0022e17f-2fa0-454f-98d2-f1c690de2df1");

        String expectedMessage = "dk.kb.util.webservice.exception.InternalServiceException: javax.ws.rs.NotFoundException: rerunCluster fileId='" + fileId + "' not found";

        RerunClusterApiServiceImpl rerunClusterApiServiceImpl = new RerunClusterApiServiceImpl();

        // Act
        Exception exception = assertThrows(InternalServiceException.class, () -> rerunClusterApiServiceImpl.getRerunClusterByFileId(fileId));

        // Assert
        assertEquals(expectedMessage, exception.getMessage());
    }
}
