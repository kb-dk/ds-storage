package dk.kb.storage.storage;

import dk.kb.storage.mapper.RerunClusterResponseDtoMapper;
import dk.kb.storage.model.v1.RerunClusterRequestDto;
import dk.kb.storage.model.v1.RerunClusterResponseDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.UUID;

import static java.time.ZoneOffset.UTC;

public class RerunClusterStorage extends BaseModuleStorage {
    private static final Logger log = LoggerFactory.getLogger(RerunClusterStorage.class);
    private final static RerunClusterResponseDtoMapper rerunClusterResponseDtoMapper = new RerunClusterResponseDtoMapper();

    private static String getRerunClusterByFileIdStatement = """
            SELECT
                id,
                file_id,
                rerun_cluster_id,
                cluster_id_creation_date,
                created_time,
                modified_time
            FROM
                rerun_clusters
            WHERE
                file_id = ?
            """;

    private static String createRerunClusterStatement = """
            INSERT INTO rerun_clusters(
                file_id,
                rerun_cluster_id,
                cluster_id_creation_date,
                created_time,
                modified_time
            )
            VALUES(
                ?,
                ?,
                ?,
                ?,
                ?
            )
            """;

    private static String updateRerunClusterStatement = """
            UPDATE
                rerun_clusters
            SET
                rerun_cluster_id = ?,
                cluster_id_creation_date = ?,
                modified_time = ?
            WHERE
                file_id = ?
            """;

    public RerunClusterStorage() throws SQLException {
        super();
    }

    /**
     * Get a rerun cluster by fileId
     *
     * @param fileId
     * @return
     * @throws Exception
     */
    public RerunClusterResponseDto getRerunClusterByFileId(UUID fileId) throws Exception {
        try (PreparedStatement stmt = connection.prepareStatement(getRerunClusterByFileIdStatement)) {
            stmt.setObject(1, fileId);
            ResultSet resultSet = stmt.executeQuery();

            while (resultSet.next()) {
                return rerunClusterResponseDtoMapper.map(resultSet);
            }

            return null;
        } catch (SQLException e) {
            String message = "SQL Exception in getRerunClusterByFileId with fileId:'" + fileId + "' error: " + e.getMessage();
            log.error(message);
            throw new SQLException(message, e);
        }
    }

    /**
     * Create a rerun cluster
     *
     * @param rerunClusterRequestDto
     * @throws Exception
     */
    public void createRerunCluster(RerunClusterRequestDto rerunClusterRequestDto) throws Exception {
        try (PreparedStatement stmt = connection.prepareStatement(createRerunClusterStatement)) {
            stmt.setObject(1, rerunClusterRequestDto.getFileId());
            stmt.setObject(2, rerunClusterRequestDto.getRerunClusterId());
            stmt.setObject(3, rerunClusterRequestDto.getClusterIdCreationDate());
            stmt.setObject(4, OffsetDateTime.now(UTC));
            stmt.setObject(5, OffsetDateTime.now(UTC));
            stmt.executeUpdate();
        } catch (SQLException e) {
            String message = "SQL Exception in createRerunCluster with fileId:'" + rerunClusterRequestDto.getFileId() + "' error: " + e.getMessage();
            log.error(message);
            throw new SQLException(message, e);
        }
    }

    /**
     * Update a rerun cluster
     *
     * @param rerunClusterRequestDto
     * @throws Exception
     */
    public void updateRerunCluster(RerunClusterRequestDto rerunClusterRequestDto) throws Exception {
        try (PreparedStatement stmt = connection.prepareStatement(updateRerunClusterStatement)) {
            stmt.setObject(1, rerunClusterRequestDto.getRerunClusterId());
            stmt.setObject(2, rerunClusterRequestDto.getClusterIdCreationDate());
            stmt.setObject(3, OffsetDateTime.now(UTC));
            stmt.setObject(4, rerunClusterRequestDto.getFileId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            String message = "SQL Exception in updateRerunCluster with fileId:'" + rerunClusterRequestDto.getFileId() + "' error: " + e.getMessage();
            log.error(message);
            throw new SQLException(message, e);
        }
    }
}
