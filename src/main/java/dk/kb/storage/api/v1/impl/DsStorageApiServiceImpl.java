package dk.kb.storage.api.v1.impl;

import dk.kb.storage.api.v1.DsStorageApi;
import dk.kb.storage.config.ServiceConfig;
import dk.kb.storage.facade.DsStorageFacade;
import dk.kb.storage.model.v1.DsRecordDto;
import dk.kb.storage.model.v1.DsRecordMinimalDto;
import dk.kb.storage.model.v1.MappingDto;
import dk.kb.storage.model.v1.OriginCountDto;
import dk.kb.storage.model.v1.OriginDto;
import dk.kb.storage.model.v1.RecordTypeDto;
import dk.kb.util.Pair;
import dk.kb.util.webservice.ImplBase;
import dk.kb.util.webservice.stream.ExportWriter;
import dk.kb.util.webservice.stream.ExportWriterFactory;
import dk.kb.util.webservice.stream.ContinuationUtil;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
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
    public List<OriginDto> getOriginConfiguration() {
        try {
            log.debug("getOriginConfiguration() called with call details: {}", getCallDetails());

            //TODO MOVE TO FACEDE
            List<OriginDto> originList = new ArrayList<OriginDto>();
            HashMap<String, OriginDto> allowedOrigins = ServiceConfig.getAllowedOrigins();
            for (String originName : allowedOrigins.keySet()) {
                originList.add(allowedOrigins.get(originName));
            }
            return originList;
        } catch (Exception e) {
            throw handleException(e);
        }

    }
    @Override
    public StreamingOutput getRecordsModifiedAfter(String origin,RecordTypeDto recordType, Long mTime, Long maxRecords) {
        if (recordType != null) {
            return getRecordsByRecordTypeModifiedAfterLocalTree(origin,recordType, mTime,  maxRecords);
        }
        else {
            return getRecordsModifiedAfterNoLocalTree( origin, mTime, maxRecords);   
            
        }       
    }
    
    private StreamingOutput getRecordsModifiedAfterNoLocalTree(String origin, Long mTime, Long maxRecords) {
        try {
            log.debug("getRecordsModifiedAfter(origin='{}', mTime={}, maxRecords={}) with batchSize={} " +
                      "called with call details: {}",
                      origin, mTime, maxRecords, ServiceConfig.getDBBatchSize(), getCallDetails());
            // Both mTime and maxRecords defaults should be set in the OpenAPI YAML, but the current version of
            // the OpenAPI generator does not support defaults for longs (int64)
            long finalMTime = mTime == null ? 0L : mTime;
            long finalMaxRecords = maxRecords == null ? 1000L : maxRecords;

            // Count records in the origin we are extracting from
            long recordsInOrigin = DsStorageFacade.countRecordsInOrigin(origin, finalMTime);
            setHeaders(finalMTime, finalMaxRecords, DsStorageFacade.getMaxMtimeAfter(origin, finalMTime, finalMaxRecords), recordsInOrigin);

            return output -> {
                try (ExportWriter writer = ExportWriterFactory.wrap(
                        output, httpServletResponse, ExportWriterFactory.FORMAT.json, false, "records")) {
                    DsStorageFacade.getRecordsModifiedAfter(writer, origin, finalMTime, finalMaxRecords, ServiceConfig.getDBBatchSize());
                }
            };
        } catch (Exception e){
            throw handleException(e);
        }
    }


    private StreamingOutput getRecordsByRecordTypeModifiedAfterLocalTree(String origin, RecordTypeDto recordType, Long mTime, Long maxRecords) {
        try {
            log.debug(" getRecordsByRecordTypeModifiedAfterLocalTree(origin='{}', recordtype='{}', mTime={}, maxRecords={}) with batchSize={} " +
                      "called with call details: {}",
                      origin, recordType, mTime, maxRecords, ServiceConfig.getDBBatchSize(), getCallDetails());
            // Both mTime and maxRecords defaults should be set in the OpenAPI YAML, but the current version of
            // the OpenAPI generator does not support defaults for longs (int64)
            long finalMTime = mTime == null ? 0L : mTime;
            long finalMaxRecords = maxRecords == null ? 1000L : maxRecords;

            long recordsInOrigin = DsStorageFacade.countRecordsInOrigin(origin, finalMTime); //TODO Victor. Shouldnt this also use recordType when counting?
            setHeaders(finalMTime, finalMaxRecords, DsStorageFacade.getMaxMtimeAfter(origin, finalMTime, finalMaxRecords), recordsInOrigin);

            return output -> {
                try (ExportWriter writer = ExportWriterFactory.wrap(
                        output, httpServletResponse, ExportWriterFactory.FORMAT.json, false, "records")) {
                    DsStorageFacade.getRecordsByRecordTypeModifiedAfterWithLocalTree(writer, origin, recordType, finalMTime, finalMaxRecords, ServiceConfig.getDBBatchSize());
                }
            };
        } catch (Exception e){
            throw handleException(e);
        }

    }

    /**
     * Set headers for the response delivered through the API endpoint. The method tries to set the following headers
     * explicitly: Content-Disposition, Paging-Continuation-Token, Paging-Has-More and Paging-Record-Count.
     * @param finalMTime is used to determine how to set the Content-Disposition header.
     * @param finalMaxRecords is used to determine how to set the Content-Disposition header.
     * @param continuationPair contains the values for the Paging-Continuation-Token and Paging-Has-More headers.
     *                         See {@link DsStorageFacade#getMaxMtimeAfter(String, long, long)} and
     *                         {@link DsStorageFacade#getMaxMtimeAfter(String, RecordTypeDto, long, long)} for explanation.
     * @param recordsInOrigin the amount of records available from the backing DsStorage during the call.
     */
    private void setHeaders(long finalMTime, long finalMaxRecords, Pair<Long, Boolean> continuationPair, long recordsInOrigin) {
        setContentDispositionHeader(finalMTime, finalMaxRecords);
        ContinuationUtil.setHeaders(httpServletResponse, continuationPair);

        // Figure which value is the correct amount to be used for the Paging-Record-Count header.
        long returnedRecords = Math.min(finalMaxRecords, recordsInOrigin);
        if (finalMaxRecords == -1L){
            returnedRecords = recordsInOrigin;
        }

        ContinuationUtil.setHeaderRecordCount(httpServletResponse, returnedRecords);
    }

    /**
     * Determines the value for the Content-Disposition header by looking at the value of finalMaxRecords.
     * @param finalMTime value used to construct the filename used on in header.
     * @param finalMaxRecords amount of records being requested. If this value is more than 2, then the response is
     *                        shown inline.
     */
    private void setContentDispositionHeader(long finalMTime, long finalMaxRecords) {
        String filename = "records_" + finalMTime + ".json";
        if (finalMaxRecords < 2) { // The Swagger GUI is extremely sluggish for inline rendering
            // A few records is ok to show inline in the Swagger GUI:
            // Show inline in Swagger UI, inline when opened directly in browser
            httpServletResponse.setHeader("Content-Disposition", "inline; filename=\"" + filename + "\"");
        } else {
            // When there are a lot of records, they should not be displayed inline in the OpenAPI GUI:
            // Show download link in Swagger UI, inline when opened directly in browser
            // https://github.com/swagger-api/swagger-ui/issues/3832
            httpServletResponse.setHeader("Content-Disposition", "inline; swaggerDownload=\"attachment\"; filename=\"" + filename + "\"");
        }
    }

    @Override
    public void recordPost(DsRecordDto dsRecordDto) {
        try {
            log.debug("recordPost(Origin='{}', record.id='{}', ...) called with call details: {}",
                      dsRecordDto.getOrigin(), dsRecordDto.getId(), getCallDetails());
            DsStorageFacade.createOrUpdateRecord(dsRecordDto);
        } catch (Exception e) {
            throw handleException(e);
        }

    }

    @Override
    public DsRecordDto getRecord(String id, Boolean includeLocalTree) {
        try {
            log.debug("getRecord(id='{}') called with call details: {}", id, getCallDetails());
            DsRecordDto record= DsStorageFacade.getRecord(id,includeLocalTree);                      
            return record;
        } catch (Exception e) {
            throw handleException(e);
        }

    }
    //@Override Not overwrite. Method removed from openAPI due to cyclic loop
    /*
    public DsRecordDto getRecordTree(String id) {
        try {
            log.debug("getRecordTree(id='{}') called with call details: {}", id, getCallDetails());
            DsRecordDto record= DsStorageFacade.getRecordTree(id);       
            
            return record;
        } catch (Exception e) {
            throw handleException(e);
        }

    }
    */
     
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
    public Integer deleteMarkedForDelete(String origin) {
        try {
            log.debug("deleteMarkedForDelete(origin'{}') called with call details: {}",
                      origin, getCallDetails());
            return DsStorageFacade.deleteMarkedForDelete(origin);
        } catch (Exception e) {
            throw handleException(e);
        }
    }
    
    @Override
    public  List<OriginCountDto> getOriginStatistics() {
        try {
            log.debug("getOriginStatistics() called with call details: {}", getCallDetails());
            return DsStorageFacade.getOriginStatistics();
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @Override
    public Integer deleteRecordsForOrigin(String origin, Long mTimeFrom, Long mTimeTo) {
        try {
	    log.debug("deleteRecordsForOrigin() called with call details: {}", getCallDetails());
	    return DsStorageFacade.deleteRecordsForOrigin(origin,mTimeFrom,mTimeTo);
        } catch (Exception e) {
	    throw handleException(e);
        }
    }
    
    @Override
    public void updateKalturaIdForRecord(String referenceId, String kalturaId) {
        try {
            log.debug("updateKalturaId() called with call details: {}", getCallDetails());
            DsStorageFacade.updateKalturaIdForRecord(referenceId, kalturaId);               
        } catch (Exception e) {
            throw handleException(e);
        }
    }
    
    @Override
    public void updateReferenceIdForRecord(String recordId, String referenceId) {
        try {
            log.debug("updateReferenceIdForRecord() called with call details: {}", getCallDetails());
            DsStorageFacade.updateReferenceIdForRecord(recordId,referenceId);               
        } catch (Exception e) {
            throw handleException(e);
        }
    }
    
      
    @Override
    public void mappingPost(MappingDto mappingDto) {
        try {
            log.debug("updateKalturaId() called with call details: {}", getCallDetails());
            DsStorageFacade.createOrUpdateMapping(mappingDto);               
        } catch (Exception e) {
            throw handleException(e);
        }        
    }
    @Override
    public MappingDto getMapping(String referenceId) {
        try {
            log.debug("getMapping() called with call details: {}", getCallDetails());
            return DsStorageFacade.getMapping(referenceId);
            
        } catch (Exception e) {
            throw handleException(e);
        }        
    }
    @Override
    public Integer updateKalturaIdForRecords() {
        log.debug("updateKalturaIdForRecords() called with call details: {}", getCallDetails());
        return DsStorageFacade.updateKalturaIdForRecords();        
    }
    
        
    
    /**
    * <p>
    * Get a list of records after a given mTime. The records will only have fields
    * id,mTime,referenceid and kalturaid defined 
    * </p>
    *
    * @param origin The origin to fetch records drom    
    * @param maxRecords Number of maximum records to return
    * @param mTime only fetch records with mTime larger that this
    *
    * @return List of records only have fields id,mTime,referenceid and kalturaid
    */
    @Override
    public StreamingOutput getMinimalRecords(String origin, Integer maxRecords, Long mTime) {
        log.debug("referenceIds called with call details: {}", getCallDetails());

        // Both mTime and maxRecords defaults should be set in the OpenAPI YAML, but the current version of
        // the OpenAPI generator does not support defaults for longs (int64)
        long finalMTime = mTime == null ? 0L : mTime;
        long finalMaxRecords = maxRecords == null ? 1000L : maxRecords;

        long recordsInOrigin = DsStorageFacade.countRecordsInOrigin(origin, finalMTime); //TODO Victor. Shouldnt this also use recordType when counting?
        setHeaders(finalMTime, finalMaxRecords, DsStorageFacade.getMaxMtimeAfter(origin, finalMTime, finalMaxRecords), recordsInOrigin);

        return output -> {
            try (ExportWriter writer = ExportWriterFactory.wrap(
                    output, httpServletResponse, ExportWriterFactory.FORMAT.json, false, "records")) {
                DsStorageFacade.getMinimalRecordsModifiedAfter(writer, origin, finalMTime, finalMaxRecords, ServiceConfig.getDBBatchSize());
            }
        };
    }
    

}
