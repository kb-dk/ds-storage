/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package dk.kb.storage.util;

import dk.kb.storage.client.v1.DsStorageApi;
import dk.kb.storage.invoker.v1.ApiClient;
import dk.kb.storage.invoker.v1.ApiException;
import dk.kb.storage.invoker.v1.Configuration;
import dk.kb.storage.model.v1.DsRecordDto;
import dk.kb.storage.model.v1.DsRecordMinimalDto;
import dk.kb.storage.model.v1.MappingDto;
import dk.kb.storage.model.v1.OriginCountDto;
import dk.kb.storage.model.v1.OriginDto;
import dk.kb.storage.model.v1.RecordTypeDto;
import dk.kb.storage.model.v1.RecordsCountDto;
import dk.kb.storage.webservice.KBAuthorizationInterceptor;
import dk.kb.util.webservice.Service2ServiceRequest;
import dk.kb.util.webservice.exception.InternalServiceException;
import dk.kb.util.webservice.stream.ContinuationInputStream;
import dk.kb.util.webservice.stream.ContinuationStream;


import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;


/**
 * Client for the service. Intended for use by other projects that calls this service.
 * See the {@code README.md} for details on usage.
 * </p>
 * This class is not used internally.
 * </p>
 * The client is Thread safe and handles parallel requests independently.
 * It is recommended to persist the client and to re-use it between calls.
 */
public class DsStorageClient extends DsStorageApi {
    private static final Logger log = LoggerFactory.getLogger(DsStorageClient.class);
    private final String serviceURI;

    public static final String STORAGE_SERVER_URL_KEY = ".storage.url";

    /**
     * Creates a client for the remote ds-storage service.
     * <p>
     * When working with YAML configs, it is suggested to define the storage URI as the structure
     * <pre>
     * storage:
     *   url: 'http://localhost:9072/ds-storage/v1'
     * </pre>
     * Then use the path {@link #STORAGE_SERVER_URL_KEY} to extract the URL.
     * @param serviceURI the URI for the service, e.g. {@code https://example.com/ds-license/v1}.
     */
    @SuppressWarnings("JavadocLinkAsPlainText")
    public DsStorageClient(String serviceURI) {
        super(createClient(serviceURI));
        this.serviceURI = serviceURI;
        log.info("Created OpenAPI client for '" + serviceURI + "'");
    }

    /**
     * Retrieve a list of configured origins with their respective update strategy
     * This endpoint delivers a list of all configured origins. An origin defines which collection data comes from. This could for instance be the Radio &amp; TV collection at The Royal Danish Library, which has the origin defined as &#39;ds.radiotv&#39;. The update strategy defines how data from the specific origin is updated, when a record is added, modified or deleted. 
     * @return List&lt;OriginDto&gt;
     * @throws ApiException if fails to make API call
     */
    @Override
    public List<OriginDto> getOriginConfiguration () throws ApiException{       
        try {
            URI uri = new URIBuilder(serviceURI + "origin/config")                                                                
                    .build();
            return Service2ServiceRequest.httpCallWithOAuthToken(uri,"GET",new ArrayList<OriginDto>(),null);              
        }
        catch(Exception e) {
            e.printStackTrace();
            throw new ApiException(e);
        }                        

    }

    /**
     * Show amount of records in each origin
     * 
     * @return List&lt;OriginCountDto&gt;
     * @throws ApiException if fails to make API call
     */
    @Override
    public List<OriginCountDto> getOriginStatistics() throws ApiException {
        try {
            URI uri = new URIBuilder(serviceURI + "/origin/stats")                                                                
                    .build();
            return Service2ServiceRequest.httpCallWithOAuthToken(uri,"GET",new ArrayList<OriginCountDto>(),null);              
        }
        catch(Exception e) {
            e.printStackTrace();
            throw new ApiException(e);
        }                                
    }


    /**
     * <p>
     * This method can not be called on ds-storage client. Must be called directly on ds-storage
     * <p>
     * 
     * Delete all records for an origin that has deleted flag set.
     * Notice that applications retrieving records from the deleted origin never will know that the records were deleted unless the application retrieves the records after they have been marked with the delete flag. 
     * @param origin The origin to delete records from. (required)
     * @return Integer
     * @throws ApiException if fails to make API call
     */
    @Override
    public RecordsCountDto deleteMarkedForDelete(String origin) throws ApiException {        
        throw new ApiException(403, "Method deleteMarkedForDelete not allowed to be called on StorageClient");
    }


    /** 
     * <p>
     * This method can not be called on ds-storage client. Must be called directly on ds-storage
     * <p>
     * 
     * Delete records from the storage for the origin within the timeframe.
     * Delete records from the storage for the origin within the timeframe. Records will be deleted and not just marked as deleted. mTimeFrom and mTimeTo will be included in the deletion range. 
     * @param origin The origin to delete records from. (required)
     * @param mTimeFrom Format is milliseconds since Epoch with 3 added digits. Value is included in the deletion (required)
     * @param mTimeTo Format is milliseconds since Epoch with 3 added digits. Value is included in the deletion (required)
     * @return Integer
     * @throws ApiException if fails to make API call
     */
    @Override
    public RecordsCountDto deleteRecordsForOrigin(String origin, Long mTimeFrom, Long mTimeTo) throws ApiException {
        throw new ApiException(403, "Method deleteRecordsForOrigin not allowed to be called on StorageClient");          
    }

    
    /**
     * <p>
     * This method can not be called on ds-storage client. Must be called directly on ds-storage
     * <p>
     * 
     * 
     * Update all records having a referenceIf with the matching Kaltura, if the mapping can be found in the mapping table
     * Update all records having a referenceId with the matching kalturaId, if the mapping can be found in the mapping table.  If many records needs to be updated this can take some time. Estimated 15 minutes for 1M records. It is possible to update in batches if the mapping table is also updated in batches.                 
     * @return RecordsCountDto
     * @throws ApiException if fails to make API call
     */
    @Override
    public RecordsCountDto updateKalturaIdForRecords() throws ApiException {
        throw new ApiException(403, "Method updateKalturaIdForRecords not allowed to be called on StorageClient");          
    }
    
    /**
     * Read a specific record by ID.
     * Extract a specific record by ID. Parent and children recordIds will also be include. If setting includeLocalTree&#x3D;true the local recordTree with parent record and children records will also be loaded as objects.   A record marked with delete flag will also be returned. If a record is not found in ds-storage, the endpoint will throw an exception. 
     * @param id Record ID (required)
     * @param includeLocalTree Also load parent and direct children as objects (optional, default to false)
     * @return DsRecordDto
     * @throws ApiException if fails to make API call
     */
    @Override
    public DsRecordDto getRecord(String id, Boolean includeLocalTree) throws ApiException{       
        try {
            URI uri = new URIBuilder(serviceURI + "record/"+id) //Set the full path, then add parameters. Id is part of url and not parameter                                                
                    .addParameter("includeLocalTree",""+includeLocalTree)               
                    .build();
            return Service2ServiceRequest.httpCallWithOAuthToken(uri,"GET",new DsRecordDto(),null);              
        }
        catch(Exception e) {
            throw new ApiException(e);
        }                        
    }


    /**
     * Create a new record or update an existing record.
     * A record can have a single optional parent and may have multiple children
     * @param dsRecordDto  (optional)
     * @throws ApiException if fails to make API call
     */
    @Override
    public void recordPost(DsRecordDto dsRecordDto) throws ApiException {
        try {
            URI uri = new URIBuilder(serviceURI + "record") //Set the full path, then add parameters.                                                               
                    .build();
            Service2ServiceRequest.httpCallWithOAuthToken(uri,"POST", null, dsRecordDto);              
        }
        catch(Exception e) {
            throw new ApiException(e);
        }                        
    }

    /**
     * Mark a record with delete flag.
     * This will not delete the record in the database but only mark it as deleted. 
     * @param id Record ID (required)
     * @return Integer
     * @throws ApiException if fails to make API call
     */
    @Override
    public RecordsCountDto markRecordForDelete (String id) throws ApiException {
        try {                          
            URI uri = new URIBuilder(serviceURI + "record/"+id)                                                                
                    .build();
            return Service2ServiceRequest.httpCallWithOAuthToken(uri,"DELETE",new RecordsCountDto(),null);              
        }
        catch(Exception e) {
            throw new ApiException(e);
        }                    
    }

    /**
     * Create a new mapping or update a mapping .
     * Create a new mapping or update mapping if referenceId exists. Each record with a stream will have a referenceId (file-id) and needs to be mapped to the KalturaId
     * @param mappingDto  (optional)
     * @throws ApiException if fails to make API call
     */
    @Override
    public void mappingPost(MappingDto mapping) throws ApiException {               
        try {
            URI uri = new URIBuilder(serviceURI + "mapping") //Set the full path, then add parameters.                                                               
               .build();
            Service2ServiceRequest.httpCallWithOAuthToken(uri,"POST", null, mapping);              
        }
        catch(Exception e) {
            throw new ApiException(e);
        }                               
    }

    /**
     * Get mapping for a specific referenceId
     * Get entry from the mapping table for the referenceId. If the entry is not found null will be returned. It is not guarantees the entry if it exists, will have the kalturaId set yet.
     * @param referenceId  (required)
     * @return MappingDto
     * @throws ApiException if fails to make API call
     */
    @Override
    public MappingDto getMapping(String referenceId) throws ApiException {        
        try {
            URI uri = new URIBuilder(serviceURI + "mapping") //Set the full path, then add parameters.                                                                
                    .addParameter("referenceId",referenceId)                       
                    .build();
            return Service2ServiceRequest.httpCallWithOAuthToken(uri,"GET", new MappingDto(), null);              
        }
        catch(Exception e) {
            throw new ApiException(e);
        }                                                
    }
        
    /**
     * Get a list of minimal records having a referenceId after a given lastModified time
     * Extract a list of records with a given batch size by origin and mTime larger than input. 
     * The records will only have the id, mTime, referenceId and kalturaId fields. This means that no actual data can be retrieved through this endpoint. It can however be used  for operations where the data from the record isn&#39;t needed. Such as updating Kaltura IDs for records, which is done with referenceId and kalturaId only. 
     * @param origin The origin to extract records for (required)
     * @param maxRecords Number of records to extract. (required)
     * @param mTime Only extract records after this mTime. (optional, default to 0l)
     * @return List&lt;DsRecordMinimalDto&gt;
     * @throws ApiException if fails to make API call
     */
    @Override
    public List<DsRecordMinimalDto> getMinimalRecords (String origin, Integer maxRecords, Long mTime) throws ApiException {   
        try {
            URI uri = new URIBuilder(serviceURI + "records/minimal") //Set the full path, then add parameters.                                                               
                    .addParameter("origin",origin)
                    .addParameter("maxRecords",""+maxRecords)
                    .addParameter("mTime",""+mTime)                    
                    .build();
            return Service2ServiceRequest.httpCallWithOAuthToken(uri,"GET", new ArrayList<DsRecordMinimalDto>(), null);              
        }
        catch(Exception e) {
            throw new ApiException(e);
        }                                                        
    }

    /**
     * Call the remote ds-storage {@link #getRecordsModifiedAfter} and return the response in the form of a Stream 
     * of records.
     * <p>
     * The stream is unbounded by memory and gives access to the highest modification time (microseconds since
     * Epoch 1970) for any record that will be delivered by the stream.
     * <p>
     * Important: Ensure that the returned stream is closed to avoid resource leaks.
     * @param origin     the origin for the records.
     * @param mTime      Exclusive start time for records to deliver:
     *                   Epoch time in microseconds (milliseconds times 1000).
     * @param maxRecords the maximum number of records to deliver. -1 means no limit.
     * @return a stream of records from the remote ds-storage.
     * @throws IOException if the connection to the remote ds-storage failed.
     */
    public ContinuationStream<DsRecordDto, Long> getRecordsModifiedAfterStream(String origin, Long mTime, Long maxRecords)
            throws IOException {
        return getRecordsModifiedAfterJSON(origin, mTime, maxRecords)
                .stream(DsRecordDto.class);
    }

    /**
     * Call the remote ds-storage {@link #getRecordsByRecordTypeModifiedAfterLocalTreeJSON} and return the response
     * in the form of a Stream of records.
     * <p>
     * The stream is unbounded by memory and gives access to the highest modification time (microseconds since
     * Epoch 1970) for any record that will be delivered by the stream {@link ContinuationStream#getContinuationToken}.
     * <p>
     * Important: Ensure that the returned stream is closed to avoid resource leaks.
     * @param origin     the origin for the records.
     * @param recordType valid values {@code COLLECTION}, {@code DELIVERABLEUNIT}, {@code MANIFESTATION}.
     * @param mTime      Exclusive start time for records to deliver:
     *                   Epoch time in microseconds (milliseconds times 1000).
     * @param maxRecords the maximum number of records to deliver. -1 means no limit.
     * @return a stream of records from the remote ds-storage.
     * @throws IOException if the connection to the remote ds-storage failed.
     */
    public ContinuationStream<DsRecordDto, Long> getRecordsByRecordTypeModifiedAfterLocalTreeStream(
            String origin, RecordTypeDto recordType, Long mTime, Long maxRecords) throws IOException {
        return getRecordsByRecordTypeModifiedAfterLocalTreeJSON(origin, recordType, mTime, maxRecords)
                .stream(DsRecordDto.class);
    }

    /**
     * Call the remote ds-storage {@link #getRecordsModifiedAfter} and return the JSON response unchanged as a wrapped
     * bytestream.
     * <p>
     * Important: Ensure that the returned stream is closed to avoid resource leaks.
     * @param origin     the origin for the records.
     * @param mTime      exclusive start time for records to deliver:
     *                   Epoch time in microseconds (milliseconds times 1000).
     * @param maxRecords the maximum number of records to deliver. -1 means no limit.
     * @return a raw bytestream with the response from the remote ds-storage.
     * @throws IOException if the connection to the remote ds-storage failed.
     */
    public ContinuationInputStream<Long> getRecordsModifiedAfterJSON(String origin, Long mTime, Long maxRecords)
            throws IOException {
        URI uri;
        try {
            uri = new URIBuilder(serviceURI + "records")
                    // setPath overwrites paths given in serviceURI
                    // .setPath("records")
                    .addParameter("origin", origin)
                    .addParameter("mTime", Long.toString(mTime == null ? 0L : mTime))
                    .addParameter("maxRecords", Long.toString(maxRecords == null ? 10 : maxRecords))
                    .build();
        } catch (URISyntaxException e) {
            String message = String.format(Locale.ROOT,
                    "getRecordsModifiedAfterJSON(origin='%s', mTime=%d, maxRecords=%d): Unable to construct URI",
                    origin, mTime, maxRecords);
            log.warn(message, e);
            throw new InternalServiceException(message);
        }

        log.debug("Opening streaming connection to '{}'", uri);
        return ContinuationInputStream.from(uri, Long::valueOf);
    }

    /**
     * Call the remote ds-storage {@link #getRecordsByRecordTypeModifiedAfterLocalTreeJSON} and return the JSON response
     * unchanged as a wrapped bytestream.
     * <p>
     * Important: Ensure that the returned stream is closed to avoid resource leaks.
     * @param origin     the origin for the records.
     * @param recordType valid values {@code COLLECTION}, {@code DELIVERABLEUNIT}, {@code MANIFESTATION}.
     * @param mTime      exclusive start time for records to deliver:
     *                   Epoch time in microseconds (milliseconds times 1000).
     * @param maxRecords the maximum number of records to deliver. -1 means no limit.
     * @return a raw bytestream with the response from the remote ds-storage.
     * @throws IOException if the connection to the remote ds-storage failed.
     */
    public ContinuationInputStream<Long> getRecordsByRecordTypeModifiedAfterLocalTreeJSON(
            String origin, RecordTypeDto recordType, Long mTime, Long maxRecords) throws IOException {
        URI uri;
        try {

            uri = new URIBuilder(serviceURI + "records")
                    // setPath overwrites paths given in serviceURI
                    //.setPath("records")
                    .addParameter("origin", origin)
                    .addParameter("recordType", recordType.toString())
                    .addParameter("mTime", Long.toString(mTime == null ? 0L : mTime))
                    .addParameter("maxRecords", Long.toString(maxRecords == null ? 10 : maxRecords))
                    .build();
        } catch (URISyntaxException e) {
            String message = String.format(Locale.ROOT,
                    "getRecordsModifiedAfterLocalTreeJSON(origin='%s', recordType='%s', mTime=%d, maxRecords=%d): " +
                            "Unable to construct URI",
                            origin, recordType, mTime, maxRecords);
            log.warn(message, e);
            throw new InternalServiceException(message);
        }

        log.debug("Opening streaming connection to '{}'", uri);
        Map<String, String> requestHeaders = new HashMap<String, String>();
        String token= (String) JAXRSUtils.getCurrentMessage().get(KBAuthorizationInterceptor.ACCESS_TOKEN_STRING);
        log.info("Calling ds-storage from continuation stream with 'Authorization'-parameter header token:"+token);
        if (token != null) {                                          
            requestHeaders.put("Authorization","Bearer "+token);
            log.info("setting token:"+token);
        }

        return ContinuationInputStream.from(uri, Long::valueOf);       
    }

    /**
     * Call the remote ds-storage {@link #getMinimalRecords(String, Integer, Long)} and return the response in the form of a Stream
     * of records. Get a stream of minimal records having a referenceId after a given lastModified time. The records will only have the id, mTime, referenceId and kalturaId fields
     * available.
     * <p>
     * The stream is unbounded by memory and gives access to the highest modification time (microseconds since
     * Epoch 1970) for any record that will be delivered by the stream.
     * <p>
     * Important: Ensure that the returned stream is closed to avoid resource leaks.
     * @param origin     the origin for the records.
     * @param mTimeFrom      Exclusive start time for records to deliver:
     *                   Epoch time in microseconds (milliseconds times 1000).
     * @param maxRecords the maximum number of records to deliver. -1 means no limit.
     * @return a stream of records from the remote ds-storage.
     * @throws IOException if the connection to the remote ds-storage failed.
     */
    public ContinuationStream<DsRecordMinimalDto, Long> getDsRecordsMinimalModifiedAfterStream(String origin, int maxRecords, long mTimeFrom) throws IOException {
        return getMinimalRecordsModifiedAfterJSON(origin, mTimeFrom, (long) maxRecords)
                .stream(DsRecordMinimalDto.class);
    }

    /**
     * Call the remote ds-storage {@link #getMinimalRecords} and return the JSON response unchanged as a wrapped bytestream.
     * <p>
     * Important: Ensure that the returned stream is closed to avoid resource leaks.
     * @param origin     the origin for the records.
     * @param mTime      exclusive start time for records to deliver:
     *                   Epoch time in microseconds (milliseconds times 1000).
     * @param maxRecords the maximum number of records to deliver. -1 means no limit.
     * @return a raw bytestream with the response from the remote ds-storage.
     * @throws IOException if the connection to the remote ds-storage failed.
     */
    public ContinuationInputStream<Long> getMinimalRecordsModifiedAfterJSON(String origin, Long mTime, Long maxRecords)
            throws IOException {
        URI uri;
        try {
            uri = new URIBuilder(serviceURI + "records/minimal")
                    // setPath overwrites paths given in serviceURI
                    // .setPath("records")
                    .addParameter("origin", origin)
                    .addParameter("mTime", Long.toString(mTime == null ? 0L : mTime))
                    .addParameter("maxRecords", Long.toString(maxRecords == null ? 10 : maxRecords))
                    .build();
        } catch (URISyntaxException e) {
            String message = String.format(Locale.ROOT,
                    "getMinimalRecordsModifiedAfterJSON(origin='%s', mTime=%d, maxRecords=%d): Unable to construct URI",
                    origin, mTime, maxRecords);
            log.warn(message, e);
            throw new InternalServiceException(message);
        }

        log.debug("Opening streaming connection to '{}'", uri);
        return ContinuationInputStream.from(uri, Long::valueOf);
    }


    /**
     * Update the referenceId for a record <br>
     * The referenceId is a id in the external system for the record. <br>
     * For preservica records the referenceId is the name of the stream file.
     *  
     *  @param recordId Id of the record to update referenceId for
     *  @param referenceId The referenceId to set for the record
     * 
     * @throws ApiException  
     * 
     */
    @Override
    public void updateReferenceIdForRecord(String recordId,String referenceId) throws ApiException {       
        try {
            URI uri = new URIBuilder(serviceURI + "record/updateReferenceId") //Set the full path, then add parameters.                                                                
                    .addParameter("recordId",""+recordId)               
                    .addParameter("referenceId",""+referenceId)
                    .build();            
            Service2ServiceRequest.httpCallWithOAuthToken(uri,"POST", null,null);              
        }
        catch(Exception e) {
            throw new ApiException(e);
        }                        

    }

    /**
     * Update a record with the Kaltura id. 
     * Update a record with the Kaltura id. The record was uploaded to Kaltura with the referenceId as metadata. Knowing the kalturaId is important to find the record(stream) in Kaltura later for update or deletetion etc.
     * @param referenceId  (required)
     * @param kalturaId  (required)
     * @throws ApiException if fails to make API call
     */
    @Override
    public void updateKalturaIdForRecord (String referenceId, String kalturaId) throws ApiException {

        try {
            URI uri = new URIBuilder(serviceURI + "record/updateKalturaId") //Set the full path, then add parameters.                                                                
                    .addParameter("referenceId",referenceId)               
                    .addParameter(" kalturaId",kalturaId)
                    .build();            
            Service2ServiceRequest.httpCallWithOAuthToken(uri,"POST", null,null);              
        }
        catch(Exception e) {
            throw new ApiException(e);
        }                      
        
        
    }

    /**
     * Deconstruct the given URI and use the components to create an ApiClient.
     * @param serviceURIString an URI to a service.
     * @return an ApiClient constructed from the serviceURIString.
     */
    private static ApiClient createClient(String serviceURIString) {
        log.debug("Creating OpenAPI client with URI '{}'", serviceURIString);

        URI serviceURI = URI.create(serviceURIString);
        // No mechanism for just providing the full URI. We have to deconstruct it
        return Configuration.getDefaultApiClient().
                setScheme(serviceURI.getScheme()).
                setHost(serviceURI.getHost()).
                setPort(serviceURI.getPort()).
                setBasePath(serviceURI.getRawPath());
    }
   
}
