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

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.kb.storage.client.v1.DsStorageApi;
import dk.kb.storage.invoker.v1.ApiClient;
import dk.kb.storage.invoker.v1.Configuration;
import dk.kb.storage.model.v1.DsRecordDto;
import dk.kb.storage.model.v1.RecordTypeDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

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
     * Set as header by record streaming endpoints to communicate the highest mTime that any records will contain.
     * This always means the mTime for the last record in the stream.
     * <p>
     * Note that there is no preceeding {@code X-} as this is discouraged by
     * <a href="https://www.rfc-editor.org/rfc/rfc6648">rfc6648</a>.
     */
    // This is a duplicate of the same field in DsStorageApiServiceImpl.
    // This is on purpose as the DsStorageClient is packed as a separate JAR.
    public static final String HEADER_HIGHEST_MTIME = "Highest-mTime";

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
    public DsStorageClient(String serviceURI) {
        super(createClient(serviceURI));
        this.serviceURI = serviceURI;
        log.info("Created OpenAPI client for '" + serviceURI + "'");
    }

    /**
     * Delivers a stream of records from a call to a remote ds-storage {@link #getRecordsModifiedAfter}.
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
    public RecordStream getRecordsModifiedAfterStream(String origin, Long mTime, Long maxRecords)
            throws IOException {
        HeaderInputStream headerStream = getRecordsModifiedAfterRaw(origin, mTime, maxRecords);
        String highestModificationTime = headerStream.getHeaders().get(HEADER_HIGHEST_MTIME) == null ? null :
                headerStream.getHeaders().get(HEADER_HIGHEST_MTIME).get(0);
        return new RecordStream(highestModificationTime, bytesToRecordStream(headerStream));
    }

    /**
     * Delivers a stream of records from a call to a remote ds-storage
     * {@link #getRecordsByRecordTypeModifiedAfterLocalTree}.
     * The stream is unbounded by memory and gives access to the highest modification time (microseconds since
     * Epoch 1970) for any record that will be delivered by the stream.
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
    public RecordStream getRecordsByRecordTypeModifiedAfterLocalTreeStream(
            String origin, RecordTypeDto recordType, Long mTime, Long maxRecords) throws IOException {
        HeaderInputStream headerStream = getRecordsByRecordTypeModifiedAfterLocalTreeRaw(
                origin, recordType, mTime, maxRecords);
        String highestModificationTime = headerStream.getHeaders().get(HEADER_HIGHEST_MTIME) == null ? null :
                headerStream.getHeaders().get(HEADER_HIGHEST_MTIME).get(0);
        return new RecordStream(highestModificationTime, bytesToRecordStream(headerStream));
    }

    /**
     * Delivers the raw bytestream from a call to a remote ds-storage {@link #getRecordsModifiedAfter}.
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
     * Delivers the raw bytestream from a call to a remote ds-storage
     * {@link #getRecordsByRecordTypeModifiedAfterLocalTree}.
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
     * Convert the given {@code byteStream} to a {@link Stream} of {@link DsRecordDto}.
     * <p>
     * The {@code byteStream} is expected to be a JSON response from a {@code records}-endpoint for a ds-storage.
     * <p>
     * The conversion happens lazily and an arbitrarily large {@code byteStream} can be given.
     * <p>
     * Important: Ensure that the returned stream is closed to avoid resource leaks.
     * @param byteStream JSON response with records from a ds-storage.
     * @return a stream of {@link DsRecordDto} de-serialized from the {@code byteStream}.
     * @throws IOException if the Stream could not be initialized from the {@code byteStream}.
     */
    public static Stream<DsRecordDto> bytesToRecordStream(InputStream byteStream) throws IOException {
        Iterator<DsRecordDto> iRecords = bytesToRecordIterator(byteStream);
        return StreamSupport.stream(
                        Spliterators.spliteratorUnknownSize(iRecords, Spliterator.ORDERED),
                        false) // iterator -> stream
                .onClose(() -> {
                    try {
                        log.debug("bytesToRecordStream: Closing source byteStream");
                        byteStream.close();
                    } catch (IOException e) {
                        // Not critical but should generally not happen so we warn
                        log.warn("bytesToRecordStream: IOException attempting to close InputStream", e);
                    }
                });
    }

    /**
     * Convert the given {@code byteStream} to an {@link Iterator} of {@link DsRecordDto}.
     * <p>
     * The {@code byteStream} is expected to be a JSON response from a {@code records}-endpoint for a ds-storage.
     * <p>
     * The conversion happens lazily and an arbitrarily large {@code byteStream} can be given.
     * <p>
     * Note: This method is not public as the returned {@code Iterator} is not closeable, which might lead to
     * resource leaks in case of problems such as a remote caller disconnecting. This is to be handled outside
     * of this context, i.e. in {@link #bytesToRecordStream}.
     * @param byteStream JSON response with records from a ds-storage.
     * @return an iterator of {@link DsRecordDto} de-serialized from the {@code byteStream}.
     * @throws IOException if the {@code byteStream} could not be read.
     */
    static Iterator<DsRecordDto> bytesToRecordIterator(InputStream byteStream) throws IOException {
        JsonFactory jFactory = new JsonFactory();
        ObjectMapper mapper = new ObjectMapper();
        jFactory.setCodec(mapper);
        JsonParser jParser = jFactory.createParser(byteStream);

        if (jParser.nextToken() != JsonToken.START_ARRAY) {
            throw new IllegalStateException("Expected JSON START_ARRAY but got " + jParser.currentToken());
        }

        return new Iterator<>() {
            private DsRecordDto nextRecord = null;
            private boolean eolReached = false;

            @Override
            public boolean hasNext(){
                ensureNext();
                return nextRecord != null;
            }

            @Override
            public DsRecordDto next() {
                if (!hasNext()) {
                    throw new IllegalStateException("next() called with hasNext() == false");
                }
                DsRecordDto record = nextRecord;
                nextRecord = null;
                return record;
            }

            /**
             * Move to next JSON token. If it is an END_ARRAY, processing is stopped, else a DsRecordDto is read
             */
            private void ensureNext() {
                if (nextRecord != null || eolReached) {
                    return;
                }
                try {
                    if (jParser.nextToken() == JsonToken.END_ARRAY) {
                        eolReached = true;
                        jParser.close();
                        byteStream.close();
                        return;
                    }
                } catch (IOException e) {
                    throw new RuntimeException("Unable to read next JSON token", e);
                }
                try {
                    nextRecord = jParser.readValueAs(DsRecordDto.class);
                } catch (IOException e) {
                    throw new RuntimeException("Unable to read DsRecordDto Object from JSON stream", e);
                }
            }
        };
    }

    /**
     * {@link DsRecordDto} specific stream that gives access to the highest modification time for any record
     * that will be in the stream (not just up to the current point). The highest modification time will always
     * belong to the last record in the stream.
     */
    public static class RecordStream extends FilterStream<DsRecordDto> implements AutoCloseable {
        private final String highestModificationTime;

        public RecordStream(String highestModificationTime, Stream<DsRecordDto> inner) {
            super(inner);
            this.highestModificationTime = highestModificationTime;
            log.debug("Creating RecordStream with highestModificationTime='{}'", highestModificationTime);
        }

        /**
         * @return the highest modification time for any record that has already been delivered by the stream or will
         *         be delivered later in the stream. If unknown, this will be null.
         */
        public String getHighestModificationTime() {
            return highestModificationTime;
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
