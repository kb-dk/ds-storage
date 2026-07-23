package dk.kb.storage.mapper;

import dk.kb.storage.model.v1.OriginCountDto;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class OriginCountDtoMapper {

    /**
     * Create a {@link OriginCountDto} from a ResultSet
     *
     * @param resultSet containing values from ds_records table
     * @return OriginCountDto populated with data
     * @throws SQLException
     */
    public OriginCountDto map(ResultSet resultSet) throws SQLException {
        OriginCountDto originCountDto = new OriginCountDto();

        originCountDto.setOrigin(resultSet.getString("origin"));
        originCountDto.setCount(resultSet.getLong("count"));
        originCountDto.setDeleted(resultSet.getLong("deleted"));
        originCountDto.setLatestMTime(resultSet.getLong("max"));
        originCountDto.setLastMTimeHuman(convertToHumanDate(originCountDto.getLatestMTime()));

        return originCountDto;
    }

    /**
     * Method is synchronized because simple dateformat is not thread safe. Faster to reuse synchronized than to construct new every time.
     */
    private static synchronized String convertToHumanDate(long millis_time_1000) {
        return OffsetDateTime.ofInstant(Instant.ofEpochMilli(millis_time_1000 / 1000), ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ssZ", Locale.ROOT));
    }
}