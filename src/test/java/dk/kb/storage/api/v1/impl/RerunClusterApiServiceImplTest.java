package dk.kb.storage.api.v1.impl;

import dk.kb.storage.model.v1.RecordsCountDto;
import dk.kb.storage.model.v1.RerunClusterDto;
import dk.kb.storage.storage.BaseModuleStorage;
import dk.kb.util.webservice.exception.InternalServiceException;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

public class RerunClusterApiServiceImplTest {

    @Test
    public void updateRerunClusterTable_whenNewRowsIsPresent_thenReturnHowManyRowsWasInsertedOrUpdated() {
        // Arrange
        RecordsCountDto recordsCountDto = new RecordsCountDto();
        recordsCountDto.setCount(1);

        try (MockedStatic<BaseModuleStorage> mockedStatic = Mockito.mockStatic(BaseModuleStorage.class)) {
            // Mock the static method to return your test data directly
            mockedStatic.when(() -> BaseModuleStorage.performStorageAction(anyString(), any(), any())).thenReturn(recordsCountDto);

            RerunClusterApiServiceImpl rerunClusterApiServiceImpl = new RerunClusterApiServiceImpl();

            // Act
            RecordsCountDto returnedRecordsCountDto = rerunClusterApiServiceImpl.updateRerunClusterTable();

            // Assert
            assertNotNull(returnedRecordsCountDto);
            assertEquals(1, returnedRecordsCountDto.getCount());
        }
    }

    @Test
    public void getRerunClusterByFileId_whenFileIdExists_thenReturnRerunClusterDto() {
        // Arrange
        UUID id = UUID.fromString("0011e17f-2fa0-454f-98d2-f1c690de2df1");
        UUID fileId = UUID.fromString("0022e17f-2fa0-454f-98d2-f1c690de2df1");
        UUID rerunClusterId = UUID.fromString("9c79bde1-9030-47a8-bb5f-3abaf2bb4ecf");
        Integer rerunClusterIdCount = 2;
        OffsetDateTime created = OffsetDateTime.parse("2026-04-30T12:26:57.570Z");
        UUID jobId = UUID.fromString("0033e17f-2fa0-454f-98d2-f1c690de2df1");
        OffsetDateTime inserted = OffsetDateTime.parse("2026-06-01T12:26:57.570Z");
        OffsetDateTime updated = OffsetDateTime.parse("2026-06-04T12:26:57.570Z");

        RerunClusterDto rerunClusterDto = new RerunClusterDto();
        rerunClusterDto.setId(id);
        rerunClusterDto.setFileId(fileId);
        rerunClusterDto.setRerunClusterId(rerunClusterId);
        rerunClusterDto.setRerunClusterIdCount(rerunClusterIdCount);
        rerunClusterDto.setCreated(created);
        rerunClusterDto.setJobId(jobId);
        rerunClusterDto.setInserted(inserted);
        rerunClusterDto.setUpdated(updated);

        try (MockedStatic<BaseModuleStorage> mockedStatic = Mockito.mockStatic(BaseModuleStorage.class)) {
            // Mock the static method to return your test data directly
            mockedStatic.when(() -> BaseModuleStorage.performStorageAction(anyString(), any(), any())).thenReturn(rerunClusterDto);

            RerunClusterApiServiceImpl rerunClusterApiServiceImpl = new RerunClusterApiServiceImpl();

            // Act
            RerunClusterDto returnedRerunClusterDto = rerunClusterApiServiceImpl.getRerunClusterByFileId(fileId);

            // Assert
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
    }

    @Test
    public void getRerunClusterByFileId_whenFileIdDoNotExists_thenThrowNotFoundException() {
        // Arrange
        UUID fileId = UUID.fromString("0022e17f-2fa0-454f-98d2-f1c690de2df1");

        String expectedMessage = "dk.kb.util.webservice.exception.InternalServiceException: javax.ws.rs.NotFoundException: rerunCluster fileId='" + fileId + "' not found";

        try (MockedStatic<BaseModuleStorage> mockedStatic = Mockito.mockStatic(BaseModuleStorage.class)) {
            // Mock the static method to return your test data directly
            mockedStatic.when(() -> BaseModuleStorage.performStorageAction(anyString(), any(), any())).thenThrow(new InternalServiceException("dk.kb.util.webservice.exception.InternalServiceException: javax.ws.rs.NotFoundException: rerunCluster fileId='" + fileId + "' not found"));

            RerunClusterApiServiceImpl rerunClusterApiServiceImpl = new RerunClusterApiServiceImpl();

            // Act
            Exception exception = assertThrows(InternalServiceException.class, () -> rerunClusterApiServiceImpl.getRerunClusterByFileId(fileId));

            // Assert
            assertEquals(expectedMessage, exception.getMessage());
        }
    }
}
