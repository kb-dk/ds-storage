package dk.kb.storage.facade;

import dk.kb.storage.model.v1.TranscriptionDto;
import dk.kb.storage.storage.BaseModuleStorage;
import dk.kb.storage.storage.TranscriptionStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TranscriptionFacade {
    private static final Logger log = LoggerFactory.getLogger(TranscriptionFacade.class);

    /**
     * Load full transcription for a stream
     *
     * @param fileId FileId for the stream, this is the stream filename.
     * @return TranscriptionDto Return empty transcriptionDto if none is found
     */
    public static TranscriptionDto getTranscription(String fileId) {
        return BaseModuleStorage.performStorageAction("getTranscription(fileId='" + fileId + ")",
                TranscriptionStorage.class, storage ->
                        ((TranscriptionStorage) storage).getTranscriptionByFileId(fileId));
    }

    /**
     * Create or update a new transcriptionDto. The primary key is fileId that comes from the external system.
     * The transcriptionDto text is the full text and transcription_lines are lines with start-end followed by the
     * sentence and with a new line in the end.
     *
     * @param transcriptionDto The entry to be created or updated
     */
    public static void createOrUpdateTranscription(TranscriptionDto transcriptionDto) {
        BaseModuleStorage.performStorageAction("createOrUpdateTranscription(" + transcriptionDto.getFileId() +
                ")", TranscriptionStorage.class, storage -> {
            String fileId = transcriptionDto.getFileId();

            // Sanity check
            if (fileId == null) {
                throw new Exception("'fileId' must not be null");
            }

            int count = ((TranscriptionStorage) storage).countTranscriptionByFileId(fileId);

            if (count > 0) {
                ((TranscriptionStorage) storage).deleteTranscriptionByFileId(fileId);
            }

            ((TranscriptionStorage) storage).createTranscription(transcriptionDto);
            // Touch the record in the ds_records table so will be selected in next indexing job and transcriptions will be indexed as well.
            int touched = ((TranscriptionStorage) storage).updateMTimeForRecordByFileId(fileId);
            log.info("Create/Updated transcriptionDto with fileId='{}' number of records touched='{}'", fileId, touched);

            return null; // Something must be returned
        });
    }
}
