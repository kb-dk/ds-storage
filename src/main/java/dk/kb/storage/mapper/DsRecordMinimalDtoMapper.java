package dk.kb.storage.mapper;

import dk.kb.storage.model.v1.DsRecordMinimalDto;

import java.sql.ResultSet;
import java.sql.SQLException;

public class DsRecordMinimalDtoMapper {

    /**
     * Create a {@link DsRecordMinimalDto} from a ResultSet
     *
     * @param resultSet containing values from rerun_clusteresultSet table
     * @return DsRecordMinimalDto populated with data
     * @throws SQLException
     */
    public DsRecordMinimalDto map(ResultSet resultSet) throws SQLException {
        DsRecordMinimalDto dsRecordMinimalDto = new DsRecordMinimalDto();

        dsRecordMinimalDto.setId(resultSet.getString("id"));
        dsRecordMinimalDto.setmTime(resultSet.getLong("mtime"));
        dsRecordMinimalDto.setReferenceId(resultSet.getString("referenceid"));
        dsRecordMinimalDto.setKalturaId(resultSet.getString("kalturaid"));

        return dsRecordMinimalDto;
    }
}
