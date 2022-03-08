package dk.kb.storage.api.v1.impl;

import dk.kb.storage.api.v1.DsStorageApi;
import dk.kb.storage.config.ServiceConfig;
import dk.kb.storage.facade.DsStorageFacade;
import dk.kb.storage.model.v1.DsRecordDto;
import dk.kb.storage.model.v1.RecordBaseCountDto;
import dk.kb.storage.model.v1.RecordBaseDto;
import dk.kb.storage.webservice.ExportWriter;
import dk.kb.storage.webservice.ExportWriterFactory;
import dk.kb.storage.webservice.exception.InternalServiceException;
import dk.kb.storage.webservice.exception.ServiceException;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.*;
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
public class DsStorageApiServiceImpl implements DsStorageApi {
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
            log.info("getRecordsModifiedAfter called with parameters recordBase:{} mTime:{} maxRecords:{} batchSize:{}",
                     recordBase, mTime, maxRecords, ServiceConfig.getDBBatchSize());

            String filename = "records_" + mTime + ".json";
            if (maxRecords <= 2) { // The Swagger GUI is extremely sluggish for inline rendering
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
                    DsStorageFacade.getRecordsModifiedAfter(writer, recordBase, mTime, maxRecords, ServiceConfig.getDBBatchSize());
                }
            };
        } catch (Exception e){
            throw handleException(e);
        }
    }

    @Override
    public void recordCreateOrUpdateRecordPost(DsRecordDto dsRecordDto) {
        try {
            DsStorageFacade.createOrUpdateRecord(dsRecordDto);
            
        
        } catch (Exception e) {
            throw handleException(e);
        }

    }

    @Override
    public DsRecordDto getRecord(String id) {
        try {
            return DsStorageFacade.getRecord(id);
        } catch (Exception e) {
            throw handleException(e);
        }

    }

    @Override
    public Integer markRecordForDelete(String id) {
        try {
            return DsStorageFacade.markRecordForDelete(id);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    
    @Override
    public Integer deleteMarkedForDelete(String recordBase) {
        try {
            return DsStorageFacade.deleteMarkedForDelete(recordBase);
        } catch (Exception e) {
            throw handleException(e);
        }
    }
    
    @Override
    public  List<RecordBaseCountDto> getRecordBaseStatistics() {
        try {
         return DsStorageFacade.getRecordBaseStatistics();
        } catch (Exception e) {
            throw handleException(e);
        }
    }
    
    
    /**
     * This method simply converts any Exception into a Service exception
     * 
     * @param e: Any kind of exception
     * @return A ServiceException
     * @see dk.kb.storage.webservice.ServiceExceptionMapper
     */
    private ServiceException handleException(Exception e) {
        if (e instanceof ServiceException) {
            return (ServiceException) e; // Do nothing - this is a declared ServiceException from within module.
        } else {// Unforseen exception (should not happen). Wrap in internal service exception
            log.error("ServiceException(HTTP 500):", e); // You probably want to log this.
            return new InternalServiceException(e.getMessage());
        }
    }



}
