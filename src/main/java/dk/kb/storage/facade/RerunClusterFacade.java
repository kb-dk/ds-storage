package dk.kb.storage.facade;

import dk.kb.storage.model.v1.RecordsCountDto;
import dk.kb.storage.model.v1.RerunClusterDto;
import dk.kb.storage.storage.BaseModuleStorage;
import dk.kb.storage.storage.RerunClusterStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.NotFoundException;
import java.util.UUID;

public class RerunClusterFacade {

    private static final Logger log = LoggerFactory.getLogger(RerunClusterFacade.class);

    /**
     * Fetch new rows from remote rerun clusters table, save it to our rerun_cluster table, update mtime in ds_records
     * table and return number of rows inserted or updated in rerun_clusters table.
     *
     * @return RecordsCountDto number of rows inserted or updated
     */
    public static RecordsCountDto updateRerunClustersTable() {
        return BaseModuleStorage.performStorageAction("updateRerunClustersTable()", RerunClusterStorage.class, storage -> {
            RecordsCountDto recordsCountDto = ((RerunClusterStorage) storage).updateRerunClustersTable();
            return recordsCountDto;
        });
    }

    /**
     * Return rerunCluster from fileId
     *
     * @param fileId UUID of fileId.
     * @return RerunClusterDto
     */
    public static RerunClusterDto getRerunClusterByFileId(UUID fileId) {
        return BaseModuleStorage.performStorageAction("getRerunClusterByFileId(" + fileId + ")", RerunClusterStorage.class, storage -> {
            RerunClusterDto rerunClusterDto = ((RerunClusterStorage) storage).getRerunClusterByFileId(fileId);

            if (rerunClusterDto == null) {
                final String errorMessage = "rerunCluster fileId='" + fileId + "' not found";
                log.error(errorMessage);
                throw new NotFoundException(errorMessage);
            }

            return rerunClusterDto;
        });
    }
}
