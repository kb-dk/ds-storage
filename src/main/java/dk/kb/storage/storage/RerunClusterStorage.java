package dk.kb.storage.storage;

import dk.kb.storage.mapper.RecordsCountDtoMapper;
import dk.kb.storage.mapper.RerunClusterDtoMapper;
import dk.kb.storage.model.v1.RecordsCountDto;
import dk.kb.storage.model.v1.RerunClusterDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class RerunClusterStorage extends BaseModuleStorage {
    private static final Logger log = LoggerFactory.getLogger(RerunClusterStorage.class);

    private final static RecordsCountDtoMapper recordsCountDtoMapper = new RecordsCountDtoMapper();
    private final static RerunClusterDtoMapper rerunClusterDtoMapper = new RerunClusterDtoMapper();

    private static String updateRerunClustersTableStatement = """
            WITH newest_created_rerun_clusters AS (
            	SELECT
            		max(rc.created) AS newest_created -- find the newest created date
            	FROM
            		rerun_clusters rc
            ),
            insert_update_rerun_clusters AS (
            	INSERT INTO rerun_clusters (
            		id,
            		file_id,
            		rerun_cluster_id,
            		created,
            		job_id,
            		inserted,
            		updated
            	)
                SELECT DISTINCT ON (rrc.file_id) -- there can be multiple of the same file_id (history) and we want the newest inserted file_id
            		rrc.id,
            		rrc.file_id,
            		rrc.rerun_cluster_id,
            		rrc.created,
            		rrc.job_id,
            		transaction_timestamp(), -- fixed at transactions start
            		transaction_timestamp() -- fixed at transactions start
            	FROM
            		remote_rerun_clusters rrc
            	INNER JOIN
            		newest_created_rerun_clusters ncrc ON
            		(
            			ncrc.newest_created IS NULL -- takes care if the rerun_clusters table is empty so we still get rows to insert
            			OR rrc.created > ncrc.newest_created
            		)
            	ORDER BY
            		rrc.file_id ASC,
            		rrc.created DESC
            	ON CONFLICT (file_id)
            	DO UPDATE SET
            		id = EXCLUDED.id,
            		rerun_cluster_id = EXCLUDED.rerun_cluster_id,
            		created = EXCLUDED.created,
            		job_id = EXCLUDED.job_id,
            		updated = transaction_timestamp() -- fixed at transactions start
            	RETURNING
            		file_id -- what rows need to be updated in ds_records and used in count(*)
            ),
            update_mtime_ds_records AS (
            	UPDATE
            		ds_records dr
            	SET
            		mtime = (EXTRACT(EPOCH FROM transaction_timestamp()) * 1000000)::bigint -- get unix timestamp in nano seconds
            	FROM
            		insert_update_rerun_clusters iurc
            	WHERE
            		dr.referenceid = iurc.file_id::TEXT
            	RETURNING
            		mtime -- used in count(*)
            ),
            inserted_updated_rerun_clusters AS (
                SELECT
            		count(*) AS rerun_clusters_count
            	FROM
            		insert_update_rerun_clusters
            ),
            updated_ds_records AS (
            	SELECT
            		count(*) AS ds_records_count
            	FROM
            		update_mtime_ds_records
            )
            SELECT
            	iurc.rerun_clusters_count,
            	udr.ds_records_count
            FROM
            	inserted_updated_rerun_clusters iurc
            CROSS JOIN
            	updated_ds_records udr
            """;

    private static String getRerunClusterByFileIdStatement = """
            SELECT
                rc.id,
                rc.file_id,
                rc.rerun_cluster_id,
                rc.created,
                rc.job_id,
                rc.inserted,
                rc.updated,
                (SELECT COUNT(*) FROM rerun_clusters WHERE rerun_cluster_id = rc.rerun_cluster_id) as rerun_cluster_id_count
            FROM
                rerun_clusters rc
            WHERE
                file_id = ?
            """;

    public RerunClusterStorage() throws SQLException {
        super();
    }

    /**
     * Fetch new rows from remote p3rerun database in table clusters table, save it to our rerun_clusters table,
     * update mtime in ds_records table and return number of rows inserted or updated in rerun_clusters table.
     *
     * @return RecordsCountDto number of rows inserted or updated
     * @throws Exception
     */
    public RecordsCountDto updateRerunClustersTable() throws Exception {
        try (PreparedStatement stmt = connection.prepareStatement(updateRerunClustersTableStatement)) {
            ResultSet resultSet = stmt.executeQuery();

            log.info("Inserted/updated rows in rerun_clusters:'{}'. Updated rows in ds_records:'{}'",
                    resultSet.getInt("rerun_clusters_count"),
                    resultSet.getInt("ds_records_count")
            );

            return recordsCountDtoMapper.map(resultSet.getInt("rerun_clusters_count"));
        } catch (SQLException e) {
            String message = "SQL Exception in updateRerunClustersTable: " + e.getMessage();
            log.error(message);
            throw new SQLException(message, e);
        }
    }

    /**
     * Get a rerun cluster by fileId
     *
     * @param fileId
     * @return
     * @throws Exception
     */
    public RerunClusterDto getRerunClusterByFileId(UUID fileId) throws Exception {
        try (PreparedStatement stmt = connection.prepareStatement(getRerunClusterByFileIdStatement)) {
            stmt.setObject(1, fileId);
            ResultSet resultSet = stmt.executeQuery();

            while (resultSet.next()) {
                return rerunClusterDtoMapper.map(resultSet);
            }

            return null;
        } catch (SQLException e) {
            String message = "SQL Exception in getRerunClusterByFileId with fileId:'" + fileId + "' error: " + e.getMessage();
            log.error(message);
            throw new SQLException(message, e);
        }
    }
}
