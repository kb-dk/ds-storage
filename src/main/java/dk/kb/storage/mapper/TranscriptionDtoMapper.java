package dk.kb.storage.mapper;

import dk.kb.storage.model.v1.TranscriptionDto;

import java.sql.ResultSet;
import java.sql.SQLException;

public class TranscriptionDtoMapper {

    /**
     * Create a {@link TranscriptionDto} from a ResultSet
     *
     * @param resultSet containing values from transcription table
     * @return TranscriptionDto populated with data
     * @throws SQLException
     */
    public TranscriptionDto map(ResultSet resultSet) throws SQLException {
        TranscriptionDto transcription = new TranscriptionDto();

        transcription.setFileId(resultSet.getString("fileid"));
        transcription.setFileName(resultSet.getString("filename"));
        transcription.setmTime(resultSet.getLong("mtime"));
        transcription.setTranscription(resultSet.getString("transcription"));
        transcription.setTranscriptionLines(resultSet.getString("transcription_lines"));

        return transcription;
    }
}
