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
import dk.kb.storage.model.v1.RecordTypeDto;
import dk.kb.util.webservice.exception.InternalServiceException;
import dk.kb.util.webservice.stream.ContinuationInputStream;
import dk.kb.util.webservice.stream.ContinuationStream;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Locale;

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
        log.info("Created OpenAPI client for '{}'", serviceURI);
    }

    
    /**
     * <p>
     * If the mapping does not exist a new entry will be created in the mapping table.<br>
     * The referenceid can not be null, but kalturaId can be null.<br>
     * If the mapping already exist for the referenceid, the kalturaId value will be updated
     * </p>
     * 
     * @param mapping The mapping entry to be created or updated
     * 
     */
    public void updateMappings(MappingDto mapping) throws ApiException {               
       super.mappingPost(mapping);                       
    }

    /**
     * This method is deprecated. See: {@link #getDsRecordsMinimalModifiedAfterStream(String, int, long)}
     * Get a list of records having a referenceId after a given lastModified time
     * Extract a list of records with a given batch size by origin and mTime larger than input.
     * The records will only have the id, mTime, referenceId and kalturaId fields set.
     * <p/>
     *  
     *  @param origin The Origin to extract records from
     *  @param batchSize How many records to fetch.
     *  @param mTimeFrom Only retrieve records after mTimeFrom
     * 
     * @throws ApiException  
     * 
     */
    public List<DsRecordMinimalDto> getDsRecordsReferenceIdModifiedAfter(String origin,int batchSize,long mTimeFrom) throws ApiException {
        return super.getMinimalRecords(origin, batchSize, mTimeFrom);
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
     * The referenceId is an id in the external system for the record. <br>
     * For preservica records the referenceId is the name of the stream file.
     *  
     *  @param recordId of the record to update referenceId for
     *  @param referenceId The referenceId to set for the record
     * 
     * @throws ApiException  
     * 
     */
    public void updateReferenceIdForRecord(String recordId,String referenceId) throws ApiException {
        super.updateReferenceIdForRecord(recordId, referenceId);
    }
    
    /**
     * Deconstruct the given URI and use the components to create an ApiClient.
     * @param serviceURIString a URI to a service.
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
