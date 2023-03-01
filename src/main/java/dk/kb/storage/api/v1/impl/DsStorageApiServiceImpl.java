package dk.kb.storage.api.v1.impl;

import dk.kb.storage.api.v1.DsStorageApi;
import dk.kb.storage.config.ServiceConfig;
import dk.kb.storage.facade.DsStorageFacade;
import dk.kb.storage.model.v1.DsRecordDto;
import dk.kb.storage.model.v1.RecordBaseCountDto;
import dk.kb.storage.model.v1.RecordBaseDto;
import dk.kb.storage.webservice.ExportWriter;
import dk.kb.storage.webservice.ExportWriterFactory;
import dk.kb.util.webservice.ImplBase;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Providers;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * ds-storage
 *
 * <p>
 * ds-storage by the Royal Danish Library
 *
 */
public class DsStorageApiServiceImpl extends ImplBase implements DsStorageApi {
    private Logger log = LoggerFactory.getLogger(this.toString());

    /*
     * How to access the various web contexts. See
     * https://cxf.apache.org/docs/jax-rs-basics.html#JAX-RSBasics-
     * Contextannotations
     */

    @Context
    private transient UriInfo uriInfo;

    @Context
    private transient SecurityContext securityContext;

    @Context
    private transient HttpHeaders httpHeaders;

    @Context
    private transient Providers providers;

    @Context
    private transient Request request;

    // Disabled as it is always null? TODO: Investigate when it can be not-null,
    // then re-enable with type
    // @Context
    // private transient ContextResolver contextResolver;

    @Context
    private transient HttpServletRequest httpServletRequest;

    @Context
    private transient HttpServletResponse httpServletResponse;

    @Context
    private transient ServletContext servletContext;

    @Context
    private transient ServletConfig servletConfig;

    @Context
    private transient MessageContext messageContext;

    @Override
    public List<RecordBaseDto> getBasesConfiguration() {
        try {
            log.debug("getBasesConfiguration() called with call details: {}", getCallDetails());

            //TODO MOVE TO FACEDE
            List<RecordBaseDto> basesList = new ArrayList<RecordBaseDto>();
            HashMap<String, RecordBaseDto> allowedBases = ServiceConfig.getAllowedBases();
            for (String baseName : allowedBases.keySet()) {
                basesList.add(allowedBases.get(baseName));
            }
            return basesList;
        } catch (Exception e) {
            throw handleException(e);
        }

    }

    @Override
    public StreamingOutput getRecordsModifiedAfter(String recordBase, Long mTime, Long maxRecords) {
        try {
            log.debug("getRecordsModifiedAfter(recordBase='{}', mTime={}, maxRecords={}) with batchSize={} " +
                      "called with call details: {}",
                      recordBase, mTime, maxRecords, ServiceConfig.getDBBatchSize(), getCallDetails());
            // Both mTime and maxRecords defaults should be set in the OpenAPI YAML, but the current version of
            // the OpenAPI generator does not support defaults for longs (int64)
            long finalMTime = mTime == null ? 0L : mTime;
            long finalMaxRecords = maxRecords == null ? 1000L : maxRecords;

            String filename = "records_" + finalMTime + ".json";
            if (finalMaxRecords <= 2) { // The Swagger GUI is extremely sluggish for inline rendering
                // A few records is ok to show inline in the Swagger GUI:
                // Show inline in Swagger UI, inline when opened directly in browser
                httpServletResponse.setHeader("Content-Disposition", "inline; filename=\"" + filename + "\"");
            } else {
                // When there are a lot of records, they should not be displayed inline in the OpenAPI GUI:
                // Show download link in Swagger UI, inline when opened directly in browser
                // https://github.com/swagger-api/swagger-ui/issues/3832
                httpServletResponse.setHeader("Content-Disposition", "inline; swaggerDownload=\"attachment\"; filename=\"" + filename + "\"");
            }

            return output -> {
                try (ExportWriter writer = ExportWriterFactory.wrap(
                        output, httpServletResponse, ExportWriterFactory.FORMAT.json, false, "records")) {
                    DsStorageFacade.getRecordsModifiedAfter(writer, recordBase, finalMTime, finalMaxRecords, ServiceConfig.getDBBatchSize());
                }
            };
        } catch (Exception e){
            throw handleException(e);
        }
    }

    @Override
    public void recordPost(DsRecordDto dsRecordDto) {
        try {
            log.debug("recordPost(record.base='{}', record.id='{}', ...) called with call details: {}",
                      dsRecordDto.getBase(), dsRecordDto.getId(), getCallDetails());
            DsStorageFacade.createOrUpdateRecord(dsRecordDto);
        } catch (Exception e) {
            throw handleException(e);
        }

    }

    @Override
    public DsRecordDto getRecord(String id) {
        try {
            log.debug("getRecord(id='{}') called with call details: {}", id, getCallDetails());
            return DsStorageFacade.getRecord(id);
        } catch (Exception e) {
            throw handleException(e);
        }

    }

    @Override
    public Integer markRecordForDelete(String id) {
        try {
            log.debug("markRecordForDelete(id='{}') called with call details: {}", id, getCallDetails());
            return DsStorageFacade.markRecordForDelete(id);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @Override
    public Integer deleteMarkedForDelete(String recordBase) {
        try {
            log.debug("deleteMarkedForDelete(recordBase='{}') called with call details: {}",
                      recordBase, getCallDetails());
            return DsStorageFacade.deleteMarkedForDelete(recordBase);
        } catch (Exception e) {
            throw handleException(e);
        }
    }
    
    @Override
    public  List<RecordBaseCountDto> getRecordBaseStatistics() {
        try {
            log.debug("getRecordBaseStatistics() called with call details: {}", getCallDetails());
            return DsStorageFacade.getRecordBaseStatistics();
        } catch (Exception e) {
            throw handleException(e);
        }
    }

}
