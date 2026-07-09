package dk.kb.storage.storage;

import dk.kb.storage.config.ServiceConfig;
import dk.kb.storage.model.v1.TranscriptionDto;
import dk.kb.storage.util.H2DbUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TranscriptionStorageTest extends UnitTestUtil {

    private static TranscriptionStorageForUnitTest storage = null;

    private final String fileId = "a3332323-3323233-333333";
    private final String fileName = "a3332323-3323233-333333.mp3";
    private final String transcription = "This is linie1. This is linie2";
    private final String transcriptionLines = "00:00 - 10:00This is linie1.\n10:00 - 20:00  This is linie2";

    @BeforeAll
    public static void beforeClass() throws Exception {
        ServiceConfig.initialize("conf/ds-storage*.yaml");
        H2DbUtil.createEmptyH2DBFromDDL(URL, DRIVER, USERNAME, PASSWORD, List.of("ddl/create_ds_storage_h2_unittest.ddl"));
        BaseModuleStorage.initialize(DRIVER, URL, USERNAME, PASSWORD);
        storage = new TranscriptionStorageForUnitTest();
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
        TranscriptionStorage.shutdown();
    }

    @Test
    public void createTranscription_whenNewTranscription_thenCountIsOne() throws Exception {
        // Arrange
        TranscriptionDto transcriptionDto = new TranscriptionDto();
        transcriptionDto.setFileId(fileId);
        transcriptionDto.setFileName(fileName);
        transcriptionDto.setTranscription(transcription);
        transcriptionDto.setTranscriptionLines(transcriptionLines);

        // Act
        storage.createTranscription(transcriptionDto);

        int count = storage.countTranscriptionByFileId(fileId);

        // Assert
        assertEquals(1, count);
    }

    @Test
    public void getTranscriptionByFileId_whenFileIdExists_thenReturnTranscription() throws Exception {
        // Arrange
        TranscriptionDto transcriptionDto = new TranscriptionDto();
        transcriptionDto.setFileId(fileId);
        transcriptionDto.setFileName(fileName);
        transcriptionDto.setTranscription(transcription);
        transcriptionDto.setTranscriptionLines(transcriptionLines);

        storage.createTranscription(transcriptionDto);

        // Act
        TranscriptionDto returnedTranscriptionDto = storage.getTranscriptionByFileId(fileId);

        // Assert
        assertNotNull(returnedTranscriptionDto);
        assertEquals(fileId, returnedTranscriptionDto.getFileId());
        assertEquals(fileName, returnedTranscriptionDto.getFileName());
        assertTrue(returnedTranscriptionDto.getmTime() > 0);
        assertEquals(transcription, returnedTranscriptionDto.getTranscription());
        assertEquals(transcriptionLines, returnedTranscriptionDto.getTranscriptionLines());
    }

    @Test
    public void getTranscriptionByFileId_whenFileIdDoNotExists_thenReturnTranscription() throws SQLException {
        // Act
        TranscriptionDto returnedTranscriptionDto = storage.getTranscriptionByFileId(fileId);

        // Assert
        assertNotNull(returnedTranscriptionDto);
        assertEquals(fileId, returnedTranscriptionDto.getFileId());
        assertNull(returnedTranscriptionDto.getFileName());
        assertNull(returnedTranscriptionDto.getmTime());
        assertNull(returnedTranscriptionDto.getTranscription());
        assertNull(returnedTranscriptionDto.getTranscriptionLines());
    }

    @Test
    public void deleteTranscriptionByFileId_whenFileIdExists_thenNoTranscriptionIsReturned() throws Exception {
        // Arrange
        TranscriptionDto transcriptionDto = new TranscriptionDto();
        transcriptionDto.setFileId(fileId);
        transcriptionDto.setFileName(fileName);
        transcriptionDto.setTranscription(transcription);
        transcriptionDto.setTranscriptionLines(transcriptionLines);

        storage.createTranscription(transcriptionDto);

        // Act
        storage.deleteTranscriptionByFileId(fileId);

        int count = storage.countTranscriptionByFileId(fileId);

        TranscriptionDto deletedTranscriptionDto = storage.getTranscriptionByFileId(fileId);

        // Assert
        assertEquals(0, count);

        assertNull(deletedTranscriptionDto.getTranscription());
    }

    @Test
    public void deleteTranscriptionByFileId_whenFileIdDoNotExists_thenNoTranscriptionIsReturned() throws Exception {
        // Act
        storage.deleteTranscriptionByFileId(fileId);

        int count = storage.countTranscriptionByFileId(fileId);

        TranscriptionDto deletedTranscriptionDto = storage.getTranscriptionByFileId(fileId);

        // Assert
        assertEquals(0, count);

        assertNull(deletedTranscriptionDto.getTranscription());
    }
}
