package dk.kb.storage.mapper;

import dk.kb.storage.model.v1.RerunClusterResponseDto;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class RerunClusterResponseDtoMapper {

    /**
     * Create a {@link RerunClusterResponseDtoMapper} from a ResultSet
     *
     * @param resultSet containing values from rerun_cluster table
     * @return RerunClusterResponseDtoMapper populated with data
     * @throws SQLException
     */
    public RerunClusterResponseDto map(ResultSet resultSet) throws SQLException {
        RerunClusterResponseDto output = new RerunClusterResponseDto();

        output.setId(resultSet.getObject("id", Integer.class));
        output.setFileId(resultSet.getObject("file_id", UUID.class));
        output.setRerunClusterId(resultSet.getObject("rerun_cluster_id", UUID.class));
        output.setClusterIdCreationDate(resultSet.getObject("cluster_id_creation_date", OffsetDateTime.class));
        output.setCreatedTime(OffsetDateTime.parse(resultSet.getObject("created_time", OffsetDateTime.class).format(DateTimeFormatter.ISO_INSTANT)));
        output.setModifiedTime(OffsetDateTime.parse(resultSet.getObject("modified_time", OffsetDateTime.class).format(DateTimeFormatter.ISO_INSTANT)));

        return output;
    }
}
