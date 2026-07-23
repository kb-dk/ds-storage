package dk.kb.storage.mapper;

import dk.kb.storage.model.v1.RecordsCountDto;

import java.sql.SQLException;

public class RecordsCountDtoMapper {

    /**
     * Create a {@link RecordsCountDto} from an int
     *
     * @param rows number of inserted, updated or deleted
     * @return RecordsCountDto populated with count
     * @throws SQLException
     */
    public RecordsCountDto map(int rows) throws SQLException {
        RecordsCountDto output = new RecordsCountDto();

        output.setCount(rows);

        return output;
    }
}
