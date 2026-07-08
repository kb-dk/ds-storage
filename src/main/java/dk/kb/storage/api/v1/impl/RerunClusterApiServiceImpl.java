package dk.kb.storage.api.v1.impl;

import dk.kb.storage.api.v1.RerunClusterApi;
import dk.kb.storage.facade.RerunClusterFacade;
import dk.kb.storage.model.v1.RecordsCountDto;
import dk.kb.storage.model.v1.RerunClusterDto;
import dk.kb.util.webservice.ImplBase;
import org.apache.cxf.interceptor.InInterceptors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * ds-storage
 *
 * <p># Ds-storage(Digitale Samlinger) by the Royal Danish Library.      ## A metadata storage. Ds-storage is a storage for metadata to describe objects in collections.   The metadata format is UTF-8 encoded but format can be anything from text,XML,JSON etc. depending on the metadata format for that collection. The basic idea behind ds-storage is a single access point to metadata describing objects belonging to many different collections. Having a simple API to  store and retrieve the records. Instead of integrating to wide range of different APIs located on different servers, the ds-storage offers a unified API for all of them and is optimized for retrieving a specific record very fast.  ## Records Objects in ds-storage are called records. A record has a metadata field that is UTF-8 encoded. The value of the metadata will typical be same as in the originating collection if harvested with OAI-PMH etc.       ## cTime and mTime format The format of cTime and mTime is milliseconds since Epoch (1970) with 3 added digits and is guaranteed to be unique values. The additional  3 digits is used to ensure uniqueness. If multiple records are created/updated within same millis the last 3 digits will be consecutive.      ## Creating and updating records     A record must has a origin that is prefined in the ds-storage configuration. This is the name of the collection in ds-storage. The recordId must also have the origin as prefix so it is easy from a recordid to see where it belongs. When a record is created it will be given a creation time (cTime) that will never be changed and a modification time (mTime). If a record is later update only the mTime will be updated.   ## Naming convention for origin and id The origin must only contain lowercase letters and dot (.) as separator.      The id must start with the origin followed by colon (:). The part after origin must only contain of upper of lower case letters, digits and the following characters:  : . _ -      Regexp for origin: ([a-z0-9.]+)   Regexp for id: ([a-z0-9.]+):([a-zA-Z0-9:._-]+)  ## ID normalisation                      If a record contains a invalid character after the origin part, it will be normalised and the invalid characters will be replaced. The original (invalid) id will be stored in the 'orgid' field and flagged for invalid id. Having the original id will make it possible to track it back to the collection it came from. The record can still be retrieved and updated using the invalid id, but also by the normalised id.                                                              ## Record hierarchy                  The datamodel is a tree structure with a single parent but no limit on number of children. The tree can be several levels deep (tree depth). Due to the tree structre, there can not exist a cycle in the graph.     Every origin is configured with a transitive  update strategy that makes sense for the origin. When a record is created or update it can update the mTime of parent and all children if defined for the origin. The possible updatestrategies are: NONE, ALL, PARENT, CHILDREN.  (see #updatestrategy schema)      ## Record type Records must have one of the 3 enum types define in RecordTypeDto. The recordtype is an easy way to determine depth in the hierachy and can be collection specific. COLLECTION This is top parent (root) with information about the collection.  DELIVERABLEUNIT Parent for a manifestation. MANIFESTATION A record that has metadata that relates to single digital preservation unit (image, video, audio etc.).                ## API    Records can be extracted by recordId or as a list by specified origin and last modification time (mTime). The uniqueness of mTime will ensure batching through the records using mTime will not return same record twice.
 *
 */
@InInterceptors(interceptors = "dk.kb.storage.webservice.KBAuthorizationInterceptor")
public class RerunClusterApiServiceImpl extends ImplBase implements RerunClusterApi {
    private static final Logger log = LoggerFactory.getLogger(RerunClusterApiServiceImpl.class);

    /**
     * Fetch new rows from remote p3rerun database in table clusters table, save it to our rerun_clusters table,
     * update mtime in ds_records table and return number of rows inserted or updated in rerun_clusters table.
     *
     * @return RecordsCountDto number of rows inserted or updated
     */
    @Override
    public RecordsCountDto updateRerunClustersTable() {
        return RerunClusterFacade.updateRerunClustersTable();
    }

    /**
     * Get a Rerun Cluster from fileId.
     *
     * @param fileId:
     * @return RerunClusterDto
     */
    @Override
    public RerunClusterDto getRerunClusterByFileId(UUID fileId) {
        return RerunClusterFacade.getRerunClusterByFileId(fileId);
    }
}
