package dk.kb.storage.mapper;

import dk.kb.storage.model.v1.DsRecordDto;
import dk.kb.storage.model.v1.RecordTypeDto;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class DsRecordDtoMapper {

    /**
     * Create a {@link DsRecordDto} from a ResultSet
     *
     * @param resultSet containing values from ds_records table
     * @return DsRecordDto populated with data
     * @throws SQLException
     */
    public DsRecordDto map(ResultSet resultSet) throws SQLException {
        DsRecordDto dsRecordDto = new DsRecordDto();

        dsRecordDto.setId(resultSet.getString("id"));
        dsRecordDto.setOrigin(resultSet.getString("origin"));
        dsRecordDto.setOrgid(resultSet.getString("orgid"));
        dsRecordDto.setIdError(resultSet.getInt("id_error") == 1);
        dsRecordDto.setDeleted(resultSet.getInt("deleted") == 1);
        dsRecordDto.setData(resultSet.getString("data"));
        dsRecordDto.setcTime(resultSet.getLong("ctime"));
        dsRecordDto.setmTime(resultSet.getLong("mtime"));
        dsRecordDto.setParentId(resultSet.getString("parentid"));
        dsRecordDto.setRecordType(RecordTypeDto.valueOf(resultSet.getString("recordtype")));
        dsRecordDto.setReferenceId(resultSet.getString("referenceid"));
        dsRecordDto.setKalturaId(resultSet.getString("kalturaid"));

        //Set the two dates as human-readable.
        dsRecordDto.setcTimeHuman(convertToHumanDate(dsRecordDto.getcTime()));
        dsRecordDto.setmTimeHuman(convertToHumanDate(dsRecordDto.getmTime()));

        return dsRecordDto;
    }

    /**
     * Method is synchronized because simple dateformat is not thread safe. Faster to reuse synchronized than to construct new every time.
     */
    private static synchronized String convertToHumanDate(long millis_time_1000) {
        return OffsetDateTime.ofInstant(Instant.ofEpochMilli(millis_time_1000 / 1000), ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ssZ", Locale.ROOT));
    }
}
