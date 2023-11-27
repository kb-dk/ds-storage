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
import dk.kb.storage.invoker.v1.Configuration;
import dk.kb.storage.model.v1.DsRecordDto;
import dk.kb.storage.model.v1.RecordTypeDto;
import dk.kb.storage.webservice.ContinuationStream;
import dk.kb.storage.webservice.ContinuationUtil;
import dk.kb.storage.webservice.HeaderInputStream;
import dk.kb.storage.webservice.JSONStreamUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.net.URI;

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

    public static final String STORAGE_SERVER_URL_KEY = ".config.storage.url";
    
    /**
     * Creates a client for the remote ds-storage service.
     * <p>
     * When working with YAML configs, it is suggested to define the storage URI as the structure
     * <pre>
     * config:
     *   storage:
     *     url: 'http://localhost:9072/ds-storage/v1'
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
        HeaderInputStream headerStream = getRecordsModifiedAfterRaw(origin, mTime, maxRecords);
        return toContinuationStream(headerStream);
    }

    /**
     * Call the remote ds-storage {@link #getRecordsByRecordTypeModifiedAfterLocalTree} and return the response
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
        HeaderInputStream headerStream = getRecordsByRecordTypeModifiedAfterLocalTreeRaw(
                origin, recordType, mTime, maxRecords);
        return toContinuationStream(headerStream);
    }

    /**
     * Call the remote ds-storage {@link #getRecordsModifiedAfter} and return the response unchanged as a wrapped
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
    public HeaderInputStream getRecordsModifiedAfterRaw(String origin, Long mTime, Long maxRecords) throws IOException {
        URI uri = UriBuilder.fromUri(serviceURI)
                .path("records")
                .queryParam("origin", origin)
                .queryParam("mTime", mTime == null ? 0L : mTime)
                .queryParam("maxRecords", maxRecords == null ? 10 : maxRecords)
                .build();
        log.debug("Opening streaming connection to '{}'", uri);
        return HeaderInputStream.from(uri);
    }

    /**
     * Call the remote ds-storage {@link #getRecordsByRecordTypeModifiedAfterLocalTree} and return the response 
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
    public HeaderInputStream getRecordsByRecordTypeModifiedAfterLocalTreeRaw(
            String origin, RecordTypeDto recordType, Long mTime, Long maxRecords) throws IOException {
        URI uri = UriBuilder.fromUri(serviceURI)
                .path("recordsByRecordTypeLocalTree")
                .queryParam("origin", origin)
                .queryParam("recordType", recordType)
                .queryParam("mTime", mTime == null ? 0L : mTime)
                .queryParam("maxRecords", maxRecords == null ? 10 : maxRecords)
                .build();
        log.debug("Opening streaming connection to '{}'", uri);
        return HeaderInputStream.from(uri);
    }

    /**
     * Convert the raw {@code jsonResponse} to a stream of {@link DsRecordDto}s.
     * @param jsonResponse a JSON array of serialized {@link DsRecordDto}s.
     * @return a stream of objects created from the {@code jsonResponse}.
     * @throws IOException if {@code jsonResponse} could not be read or converted to objects.
     */
    private ContinuationStream<DsRecordDto, Long> toContinuationStream(HeaderInputStream jsonResponse) throws IOException {
        Long highestModificationTime =
                ContinuationUtil.getContinuationToken(jsonResponse).map(Long::parseLong).orElse(null);
        Boolean hasMore = ContinuationUtil.getHasMore(jsonResponse).orElse(null);
        return new ContinuationStream<>(JSONStreamUtil.jsonToObjectsStream(jsonResponse, DsRecordDto.class),
                                        highestModificationTime, hasMore);
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
