package dk.kb.storage.storage;

import dk.kb.storage.mapper.TranscriptionDtoMapper;
import dk.kb.storage.model.v1.TranscriptionDto;
import dk.kb.storage.util.UniqueTimestampGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class TranscriptionStorage extends BaseModuleStorage {
    private static final Logger log = LoggerFactory.getLogger(TranscriptionStorage.class);

    private final static TranscriptionDtoMapper transcriptionDtoMapper = new TranscriptionDtoMapper();

    private static String transcriptionByFileIdStatement = """
            SELECT
                fileid,
                filename,
                mtime,
                transcription,
                transcription_lines
            FROM
                transcriptions
            WHERE
                fileid = ?
            """;

    private static String transcriptionByFileIdCountStatement = """
            SELECT
                count(*) as count
            FROM
                transcriptions
            WHERE
                fileid = ?
            """;

    private static String createTranscriptionStatement = """
            INSERT INTO transcriptions (
                fileid,
                filename,
                mtime,
                transcription,
                transcription_lines
            )
            VALUES (
                ?,
                ?,
                ?,
                ?,
                ?
            )
            """;

    private static String deleteTranscriptionByFileIdStatement = """
            DELETE FROM
                transcriptions
            WHERE
                fileid = ?
            """;

    public TranscriptionStorage() throws SQLException {
        super();
    }

    /**
     * Load a transcription by fileId.
     *
     * @param fileId the fileId to load
     * @return TranscriptionDto. If fileId is not found will return null
     */
    public TranscriptionDto getTranscriptionByFileId(String fileId) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(transcriptionByFileIdStatement)) {
            stmt.setString(1, fileId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    TranscriptionDto empty = new TranscriptionDto(); //DsStorageClient can not handle null values when serializing.
                    empty.setFileId(fileId);
                    return empty;
                }

                TranscriptionDto trans = transcriptionDtoMapper.map(rs);
                return trans;
            }
        }
    }

    /**
     * Count number of transcriptions by fileId. This is a fast method so see if a transcriptions exists instead of loading all text.
     *
     * @param fileId the fileId count
     * @return 0 or 1. FileId is unique
     */
    public int countTranscriptionByFileId(String fileId) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(transcriptionByFileIdCountStatement)) {
            stmt.setString(1, fileId);

            try (ResultSet rs = stmt.executeQuery()) {
                rs.next();//always value
                return rs.getInt("count");
            }
        }
    }

    /**
     * Delete a transcription by fileId.
     *
     * @param fileId the fileId. If fileId is not found, nothing will be deleted, but it will be logged.
     * @return Number of deleted records. Value 1 should be expected but can be higher if several records by mistake have same stream
     * @throws Exception Only if unexpected SQL exception happens.
     */
    public int deleteTranscriptionByFileId(String fileId) throws Exception {
        try (PreparedStatement stmt = connection.prepareStatement(deleteTranscriptionByFileIdStatement)) {
            stmt.setString(1, fileId);
            int numberDeleted = stmt.executeUpdate();

            if (numberDeleted != 1) {
                log.warn("Delete transcription by fileId did not delete 1 as expected. Deleted='{}', FileId='{}'", numberDeleted, fileId);
            }

            return numberDeleted;

        } catch (SQLException e) {
            String message = "SQL Exception in deleteTranscriptionByFileId for fileId:" + fileId + " error:" + e.getMessage();
            log.error(message);
            throw new SQLException(message, e);
        }
    }

    /**
     * @param transcription fileId must not be full
     */
    public void createTranscription(TranscriptionDto transcription) throws Exception {
        long nowStamp = UniqueTimestampGenerator.next();

        try (PreparedStatement stmt = connection.prepareStatement(createTranscriptionStatement)) {
            stmt.setString(1, transcription.getFileId());
            stmt.setString(2, transcription.getFileName());
            stmt.setLong(3, nowStamp);
            stmt.setString(4, transcription.getTranscription());
            stmt.setString(5, transcription.getTranscriptionLines());
            stmt.executeUpdate();

        } catch (SQLException e) {
            String message = "SQL Exception in createTranscription with fileid:" + transcription.getFileId() + " error:" + e.getMessage();
            log.error(message);
            throw new SQLException(message, e);
        }
    }
}
