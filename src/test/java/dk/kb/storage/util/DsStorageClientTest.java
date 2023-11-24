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

import dk.kb.storage.model.v1.DsRecordDto;
import dk.kb.storage.model.v1.RecordTypeDto;
import dk.kb.util.yaml.YAML;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.CharSequenceInputStream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Simple verification of client code generation.
 */
public class DsStorageClientTest {
    private static final Logger log = LoggerFactory.getLogger(DsStorageClientTest.class);

    public static final String TEST_CONF = "internal-test-setup.yaml";

    public static final String RECORD1 = "{\"id\": \"id1\", \"mTime\": \"123\"}";
    public static final String RECORD2 = "{\"id\": \"id2\", \"mTime\": \"124\"}";
    public static final String RECORDS0 = "[]";
    public static final String RECORDS1 = "[" + RECORD1 + "]";
    public static final String RECORDS2 = "[" + RECORD1 + ", " + RECORD2 + "]";

    private static DsStorageClient remote = getRemote();

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
        try (HeaderInputStream recordsIS = remote.getRecordsModifiedAfterRaw(
                "ds.radiotv", 0L, 3L)) {
            String recordsStr = IOUtils.toString(recordsIS, StandardCharsets.UTF_8);
            assertTrue(recordsStr.contains("\"id\":\"ds.radiotv:oai"),
                    "At least 1 JSON block for a record should be returned");
            assertNotNull(recordsIS.getHeaders().get(DsStorageClient.HEADER_HIGHEST_MTIME),
                    "The continuation header '" + DsStorageClient.HEADER_HIGHEST_MTIME + "' should be present");
        }
    }

    @Test
    public void testRemoteRecordsTreeRaw() throws IOException {
        if (remote == null) {
            return;
        }
        try (HeaderInputStream recordsIS = remote.getRecordsByRecordTypeModifiedAfterLocalTreeRaw(
                             "ds.radiotv", RecordTypeDto.DELIVERABLEUNIT,  0L, 3L)) {
            String recordsStr = IOUtils.toString(recordsIS, StandardCharsets.UTF_8);
            assertTrue(recordsStr.contains("\"id\":\"ds.radiotv:oai"),
                    "At least 1 JSON block for a record should be returned");
            assertNotNull(recordsIS.getHeaders().get(DsStorageClient.HEADER_HIGHEST_MTIME),
                    "The continuation header '" + DsStorageClient.HEADER_HIGHEST_MTIME + "' should be present");
        }
    }

    @Test
    public void testRemoteRecordsStream() throws IOException {
        if (remote == null) {
            return;
        }
        try (DsStorageClient.RecordStream records = remote.getRecordsModifiedAfterStream(
                "ds.radiotv", 0L, 3L)) {
            List<DsRecordDto> recordList = records.collect(Collectors.toList());
            assertEquals(3, recordList.size(), "The requested number of records should be received");
            assertNotNull(records.getHighestModificationTime(),
                    "The highest modification time should be present");
            log.debug("Stated highest modification time was " + records.getHighestModificationTime());
            assertEquals(recordList.get(recordList.size()-1).getmTime(),
                    Long.valueOf(records.getHighestModificationTime()),
                    "Received highest mTime should match stated highest mTime");
        }
    }

    @Test
    public void testRemoteRecordsTreeStream() throws IOException {
        if (remote == null) {
            return;
        }
        try (DsStorageClient.RecordStream records = remote.getRecordsByRecordTypeModifiedAfterLocalTreeStream(
                "ds.radiotv", RecordTypeDto.DELIVERABLEUNIT, 0L, 3L)) {
            long count = records.count();
            assertEquals(3L, count, "The requested number of records should be received");
            assertNotNull(records.getHighestModificationTime(),
                    "The highest modification time should be present");
        }
    }

    @Test
    public void testRecords0() throws IOException {
        try (Stream<DsRecordDto> deserialized = DsStorageClient.bytesToRecordStream(
                new CharSequenceInputStream(RECORDS0, StandardCharsets.UTF_8, 1024))) {
            List<DsRecordDto> records = deserialized.collect(Collectors.toList());
            assertTrue(records.isEmpty(), "There should be no records, but there were " + records.size());
        }
    }

    @Test
    public void testRecords1() throws IOException {
        try (Stream<DsRecordDto> deserialized = DsStorageClient.bytesToRecordStream(
                new CharSequenceInputStream(RECORDS1, StandardCharsets.UTF_8, 1024))) {
            List<DsRecordDto> records = deserialized.collect(Collectors.toList());
            assertEquals(1, records.size(), "There should be the right number of records");
            assertEquals("id1", records.get(0).getId(), "The first record should have the expected ID");
        }
    }

    @Test
    public void testRecords2() throws IOException {
        try (Stream<DsRecordDto> deserialized = DsStorageClient.bytesToRecordStream(
                new CharSequenceInputStream(RECORDS2, StandardCharsets.UTF_8, 1024))) {
            List<DsRecordDto> records = deserialized.collect(Collectors.toList());
            assertEquals(2, records.size(), "There should be the right number of records");
            assertEquals("id1", records.get(0).getId(), "The first record should have the expected ID");
            assertEquals(123L, records.get(0).getmTime(), "The first record should have the expected mTime");
            assertEquals("id2", records.get(1).getId(), "The second record should have the expected ID");
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
