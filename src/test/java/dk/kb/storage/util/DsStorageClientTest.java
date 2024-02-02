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

import dk.kb.storage.invoker.v1.ApiException;
import dk.kb.storage.model.v1.DsRecordDto;
import dk.kb.storage.model.v1.OriginCountDto;
import dk.kb.storage.model.v1.RecordTypeDto;
import dk.kb.util.webservice.stream.ContinuationInputStream;
import dk.kb.util.webservice.stream.ContinuationStream;
import dk.kb.util.webservice.stream.ContinuationUtil;
import dk.kb.util.yaml.YAML;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Simple verification of client code generation.
 */
public class DsStorageClientTest {
    private static final Logger log =  LoggerFactory.getLogger(DsStorageClientTest.class);

    public static final String TEST_CONF = "internal-test-setup.yaml";

    private static DsStorageClient remote = null;
    private static DsStorageClient local = new DsStorageClient("http://localhost:9072/ds-storage/v1/");


    @BeforeAll
    public static void beforeClass() {
        remote = getRemote();
    }

    @Test
    public void testInstantiation() {
        String backendURIString = "htp://example.com/ds-storage/v1";
        log.debug("Creating inactive client for ds-storage with URI '{}'", backendURIString);
        new DsStorageClient(backendURIString);
    }

    @Test
    public void testRemoteRecordsRaw() throws IOException {
        if (remote == null) {
            return;
        }
        try (ContinuationInputStream<Long> recordsIS = remote.getRecordsModifiedAfterJSON(
                "ds.radiotv", 0L, 3L)) {
            String recordsStr = IOUtils.toString(recordsIS, StandardCharsets.UTF_8);
            assertTrue(recordsStr.contains("\"id\":\"ds.radiotv:oai"),
                    "At least 1 JSON block for a record should be returned");
            assertNotNull(recordsIS.getContinuationToken(),
                    "The continuation header '" + ContinuationUtil.HEADER_PAGING_CONTINUATION_TOKEN +
                            "' should be present");
            assertTrue(ContinuationUtil.getHasMore(recordsIS).isPresent(),
                       "The continuation header '" + ContinuationUtil.HEADER_PAGING_HAS_MORE + "' should be present");
        }
    }

    // Combined unit test and demonstration of paging with continuation, OAI-PMH-like
    @Test
    public void testRemotePaging() throws IOException {
         long numberOfRecords=200L;
    	
    	if (remote == null) {
            return;
        }

        Long lastMTime;
        boolean hasMore;

        List<DsRecordDto> batch1;
        List<DsRecordDto> batch2;

        // First paging
        try (ContinuationStream<DsRecordDto, Long> recordStream =
                     remote.getRecordsModifiedAfterStream("ds.radiotv", 0L, numberOfRecords)) {
            batch1 = recordStream.collect(Collectors.toList());
            lastMTime = recordStream.getContinuationToken();
            hasMore = recordStream.hasMore();
            DsRecordDto lastRecord = batch1.get(batch1.size()-1);

            assertEquals(lastMTime, lastRecord.getmTime(),
                    "Continuation Token should match mTime for last record in batch 1");
            assertTrue(hasMore, "There should be more records after batch 1");
        }

        // Second paging
        try (ContinuationStream<DsRecordDto, Long> recordStream =
                     remote.getRecordsModifiedAfterStream("ds.radiotv", lastMTime, numberOfRecords)) {
            batch2 = recordStream.collect(Collectors.toList());
            lastMTime = recordStream.getContinuationToken();
            hasMore = recordStream.hasMore();
            DsRecordDto lastRecord = batch2.get(batch2.size()-1);

            assertEquals(lastMTime, lastRecord.getmTime(),
                    "Continuation Token should match mTime for last record in batch 2");
            assertTrue(hasMore, "There should be more records after batch 2");
        }

        // Verify batch1 + batch2
        try (ContinuationStream<DsRecordDto, Long> recordStream =
                     remote.getRecordsModifiedAfterStream("ds.radiotv", 0L, 2L*numberOfRecords)) {
            List<DsRecordDto> batchAll = recordStream.collect(Collectors.toList());
            List<DsRecordDto> batch1plus2 = new ArrayList<>(batch1);
            batch1plus2.addAll(batch2);
            for (int i = 0 ; i < batchAll.size() ; i++) {
                assertEquals(batchAll.get(i), batch1plus2.get(i),
                        "ID #" + i + " should match for batch1+2");
            }
        }
    }

    @Test
    public void testRemotePagingCount() throws IOException, ApiException {
        try (ContinuationInputStream<Long> recordsIS = remote.getRecordsModifiedAfterJSON(
                "ds.tv", 0L, 500L)) {
            assertEquals(500L, recordsIS.getRecordCount());
        }
    }

    @Test
    public void testRemotePageLast() throws ApiException, IOException {
        if (remote == null) {
            return;
        }
        Long lastMTime = null;
        for (OriginCountDto originCount: remote.getOriginStatistics()) {
            if ("ds.radiotv".equals(originCount.getOrigin())) {
                lastMTime = originCount.getLatestMTime();
            }
        }
        assertNotNull(lastMTime, "lastMTime should be extractable for 'ds.radiotv'");

        // Decrement with 1 to be sure to match the latest record
        lastMTime -= 1;

        try (ContinuationStream<DsRecordDto, Long> recordStream =
                     remote.getRecordsModifiedAfterStream("ds.radiotv", lastMTime, 10L)) {
            assertEquals(1, recordStream.count(), "There should be a single record after " + lastMTime);
            assertFalse(recordStream.hasMore(), "The hasMore flag should be false");
        }
    }

    @Test
    public void testRemoteRecordsTreeRaw() throws IOException {
        if (remote == null) {
            return;
        }
        try (ContinuationInputStream recordsIS = remote.getRecordsByRecordTypeModifiedAfterLocalTreeJSON(
                             "ds.radiotv", RecordTypeDto.DELIVERABLEUNIT,  0L, 3L)) {
            String recordsStr = IOUtils.toString(recordsIS, StandardCharsets.UTF_8);
            assertTrue(recordsStr.contains("\"id\":\"ds.radiotv:oai"),
                    "At least 1 JSON block for a record should be returned");
            assertNotNull(recordsIS.getContinuationToken(),
                       "The continuation header '" + ContinuationUtil.HEADER_PAGING_CONTINUATION_TOKEN +
                       "' should be present");
            assertTrue(ContinuationUtil.getHasMore(recordsIS).isPresent(),
                       "The continuation header '" + ContinuationUtil.HEADER_PAGING_HAS_MORE + "' should be present");
        }
    }

    @Test
    public void testRemoteRecordsStream() throws IOException {
       long numberOfRecords=300L;
       if (remote == null) {
            return;
        }
        try (ContinuationStream<DsRecordDto, Long> records = remote.getRecordsModifiedAfterStream(
                "ds.radiotv", 0L,numberOfRecords)) {
            List<DsRecordDto> recordList = records.collect(Collectors.toList());

            System.out.println(records.getResponseHeaders());
            
            assertEquals(numberOfRecords, recordList.size(), "The requested number of records should be received");
            assertNotNull(records.getContinuationToken(),
                    "The highest modification time should be present");
            log.debug("Stated highest modification time was " + records.getContinuationToken());
            assertEquals(recordList.get(recordList.size()-1).getmTime(),
                         records.getContinuationToken(),
                    "Received highest mTime should match stated highest mTime");
        }
    }

    @Test
    public void testRemoteRecordsTreeStream() throws IOException {
        if (remote == null) {
            return;
        }
        try (ContinuationStream<DsRecordDto, Long> records = remote.getRecordsByRecordTypeModifiedAfterLocalTreeStream(
                "ds.radiotv", RecordTypeDto.DELIVERABLEUNIT, 0L, 3L)) {
            long count = records.count();
            System.out.println(records.getResponseHeaders());
            assertEquals(3L, count, "The requested number of records should be received");
            assertNotNull(records.getContinuationToken(),
                    "The highest modification time should be present");
        }
    }

    /**
     * @return a {@link DsStorageClient} if a KB-internal remote storage is specified and is available.
     */
    private static DsStorageClient getRemote() {
        YAML config;
        try {
            config = YAML.resolveLayeredConfigs(TEST_CONF);
        } catch (Exception e) {
            log.info("Unable to resolve '{}' (try running 'kb init'). Skipping test", TEST_CONF);
            return null;
        }
        String storageURL = config.getString(DsStorageClient.STORAGE_SERVER_URL_KEY, null);
        if (storageURL == null) {
            log.info("Resolved internal config '{}' but could not retrieve a value for key '{}'. Skipping test",
                    TEST_CONF, DsStorageClient.STORAGE_SERVER_URL_KEY);
            return null;
        }
        DsStorageClient client = new DsStorageClient(storageURL);
        try {
            client.getOriginConfiguration();
        } catch (Exception e) {
           log.info("Found ds-storage address '{}' but could not establish contact. Skipping test", storageURL);
            return null;
        }
        log.debug("Established connection to storage at '{}'", storageURL);
        return client;
    }

}
