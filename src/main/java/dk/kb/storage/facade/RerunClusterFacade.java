package dk.kb.storage.facade;

import dk.kb.storage.model.v1.RerunClusterRequestDto;
import dk.kb.storage.model.v1.RerunClusterResponseDto;
import dk.kb.storage.storage.BaseModuleStorage;
import dk.kb.storage.storage.DsStorage;
import dk.kb.storage.storage.RerunClusterStorage;
import dk.kb.util.webservice.exception.InternalServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.NotFoundException;
import java.sql.SQLException;
import java.util.UUID;

public class RerunClusterFacade {

    private static final Logger log = LoggerFactory.getLogger(RerunClusterFacade.class);

    /**
     * Return rerunCluster from fileId
     *
     * @param fileId UUID of fileId.
     * @return RerunClusterResponseDto
     */
    public static RerunClusterResponseDto getRerunClusterByFileId(UUID fileId) {
        return BaseModuleStorage.performStorageAction("getRerunClusterByFileId(" + fileId + ")", RerunClusterStorage.class, storage -> {
            RerunClusterResponseDto rerunClusterResponseDto = ((RerunClusterStorage) storage).getRerunClusterByFileId(fileId);

            if (rerunClusterResponseDto == null) {
                final String errorMessage = "rerunCluster fileId='" + fileId + "' not found";
                log.error(errorMessage);
                throw new NotFoundException(errorMessage);
            }

            return rerunClusterResponseDto;
        });
    }

    /**
     * Create or update a rerun cluster.
     *
     * @param rerunClusterRequestDto The entry to be created or updated
     * @return RerunClusterResponseDto
     */
    public static RerunClusterResponseDto createOrUpdateRerunCluster(RerunClusterRequestDto rerunClusterRequestDto) {
        // Try to create a rerun cluster
        try {
            createRerunCluster(rerunClusterRequestDto);
        } catch (InternalServiceException exception) {
            // If the root exception is SQLException, then update the rerun cluster instead
            if (exception.getCause().getCause() instanceof SQLException) {
                updateRerunCluster(rerunClusterRequestDto);
            } else {
                throw exception;
            }
        }
        RerunClusterResponseDto rerunClusterResponseDto = getRerunClusterByFileId(rerunClusterRequestDto.getFileId());
        return rerunClusterResponseDto;
    }

    /**
     * Create a rerun cluster.
     *
     * @param rerunClusterRequestDto The entry to be created
     * @return RerunClusterResponseDto
     */
    public static void createRerunCluster(RerunClusterRequestDto rerunClusterRequestDto) {
        BaseModuleStorage.performStorageAction("createRerunCluster(" + rerunClusterRequestDto.getFileId() + ")", RerunClusterStorage.class, storage -> {
            ((RerunClusterStorage) storage).createRerunCluster(rerunClusterRequestDto);
            // Update modified time on record(s) matching with fileId in ds_records table so the information about rerun cluster gets
            // picked up in the next indexing job.
            int touched = ((RerunClusterStorage) storage).updateMTimeForRecordByFileId(rerunClusterRequestDto.getFileId().toString());
            log.info("Created rerun cluster with fileId='{}'. Number of records touched in ds_records='{}'", rerunClusterRequestDto.getFileId(), touched);
            return null;
        });
    }

    /**
     * Update a rerun cluster.
     *
     * @param rerunClusterRequestDto The entry to be updated
     * @return RerunClusterResponseDto
     */
    public static void updateRerunCluster(RerunClusterRequestDto rerunClusterRequestDto) {
        BaseModuleStorage.performStorageAction("updateRerunCluster(" + rerunClusterRequestDto.getFileId() + ")", RerunClusterStorage.class, storage -> {
            ((RerunClusterStorage) storage).updateRerunCluster(rerunClusterRequestDto);
            RerunClusterResponseDto rerunClusterResponseDto = ((RerunClusterStorage) storage).getRerunClusterByFileId(rerunClusterRequestDto.getFileId());
            // Update modified time on record(s) matching with fileId in ds_records table so the information about rerun cluster gets
            // picked up in the next indexing job.
            int touched = ((RerunClusterStorage) storage).updateMTimeForRecordByFileId(rerunClusterRequestDto.getFileId().toString());
            log.info("Updated rerun cluster with fileId='{}'. Number of records touched in ds_records='{}'", rerunClusterRequestDto.getFileId(), touched);
            return null;
        });
    }
}
