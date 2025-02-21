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
package dk.kb.storage.integration;

import dk.kb.storage.config.ServiceConfig;
import dk.kb.storage.invoker.v1.ApiException;
import dk.kb.storage.model.v1.DsRecordDto;
import dk.kb.storage.model.v1.DsRecordMinimalDto;
import dk.kb.storage.model.v1.MappingDto;
import dk.kb.storage.model.v1.OriginCountDto;
import dk.kb.storage.model.v1.OriginDto;
import dk.kb.storage.model.v1.RecordTypeDto;
import dk.kb.storage.model.v1.RecordsCountDto;
import dk.kb.storage.util.DsStorageClient;
import dk.kb.util.webservice.stream.ContinuationInputStream;
import dk.kb.util.webservice.stream.ContinuationStream;
import dk.kb.util.webservice.stream.ContinuationUtil;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
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
 * Integration test on class level, will not be run by automatic build flow.
 * Call 'kb init' to fetch YAML property file with server urls
 * 
 */
@Tag("integration")
public class DsStorageClientTest {
    private static final Logger log =  LoggerFactory.getLogger(DsStorageClientTest.class);

    private static DsStorageClient remote = null;
    private static String dsStorageDevel=null;  

    @BeforeAll
    static void setup() {
        try {
            ServiceConfig.initialize("conf/ds-storage-behaviour.yaml","ds-storage-integration-test.yaml"); 
            dsStorageDevel= ServiceConfig.getConfig().getString("integration.devel.storage"); 
            remote = new DsStorageClient(dsStorageDevel);
        } catch (IOException e) { 
            e.printStackTrace();
            log.error("Integration yaml 'ds-storage-integration-test.yaml' file most be present. Call 'kb init'"); 
            fail();

        }
    }
    
    @Test
    public void testGetOriginConfiguration() throws ApiException {      

         List<OriginDto> originConfiguration = remote.getOriginConfiguration(); 
         assertTrue(originConfiguration.size() > 0);         
    }
    
    @Test
    public void testGetOriginStatistics() throws ApiException {      
         List<OriginCountDto> originStatistics = remote.getOriginStatistics(); 
         assertTrue(originStatistics.size() > 0);
         
    }
    
    
    
    @Test
    public void testGetRecord() throws ApiException {      
        String id = "kb.image.luftfo.luftfoto:oai:kb.dk:images:luftfo:2011:maj:luftfoto:object187744";
        DsRecordDto record = remote.getRecord(id,false); 
        log.info("Loaded record from storage with id: '{}'", record.getId());
        assertEquals(id, "kb.image.luftfo.luftfoto:oai:kb.dk:images:luftfo:2011:maj:luftfoto:object187744"); 
    }

    @Test
    public void testMarkRecordForDelete() throws ApiException {              
         String id="ds.radio:oai:io:8f8f2da9-98e3-4ba2-aa6c-XXXXXX";  //does not exist
         RecordsCountDto  marked = remote.markRecordForDelete(id); 
         assertEquals(0,marked.count(null));
         
    }
    
    
    @Test
    public void testUpdateReferenceId() throws ApiException {              
         String recordId="ds.radio:oai:io:8f8f2da9-98e3-4ba2-aa6c-XXXXX";
         String refId="1234";
         remote.updateReferenceIdForRecord(recordId, refId);          
    }
    
    
    @Test
    public void testUpdateKalturaIdForRecord() throws ApiException {                       
         String refId="1234";
         String kalturaId="1234";
         remote.updateKalturaIdForRecord(refId,kalturaId);         
    }
    
    @Test
    public void testMappingPost() throws ApiException {                       
         String refId="1234";
         String kalturaId="1234";
         MappingDto dto = new MappingDto();
         dto.setKalturaId(kalturaId);
         dto.setReferenceId(refId);
         remote.mappingPost(dto);         
    }
        
    
    @Test
    public void testGetMapping() throws ApiException {                       
         String refId="d24be758-c6fe-4f58-ae6d-164125b78a8b"; //This is exist now                                   
         MappingDto mapping = remote.getMapping(refId);
         //System.out.println(mapping);
    }
    
    @Test
    public void testGetMinimalRecords() throws ApiException {                       
        String origin="ds.radio";
        int maxRecords=10;
        long mTime=0;
         
         List<DsRecordMinimalDto> minimalRecords = remote.getMinimalRecords(origin, maxRecords,mTime);
         assertEquals(10,minimalRecords.size());         
    }
    
    
    @Test
    public void testRemoteRecordsRaw() throws IOException {       
        try (ContinuationInputStream<Long> recordsIS = remote.getRecordsModifiedAfterJSON(
                "ds.radio", 0L, 3L)) {
            String recordsStr = IOUtils.toString(recordsIS, StandardCharsets.UTF_8);
            assertTrue(recordsStr.contains("\"id\":\"ds.radio:oai"),
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
    	    	
        Long lastMTime;
        boolean hasMore;

        List<DsRecordDto> batch1;
        List<DsRecordDto> batch2;

        // First paging
        try (ContinuationStream<DsRecordDto, Long> recordStream =
                     remote.getRecordsModifiedAfterStream("ds.radio", 0L, numberOfRecords)) {
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
                     remote.getRecordsModifiedAfterStream("ds.radio", lastMTime, numberOfRecords)) {
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
                     remote.getRecordsModifiedAfterStream("ds.radio", 0L, 2L*numberOfRecords)) {
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
    public void testRemotePagingCount() throws IOException {

        try (ContinuationInputStream<Long> recordsIS = remote.getRecordsModifiedAfterJSON(
                "ds.tv", 0L, 500L)) {
            assertEquals(500L, recordsIS.getRecordCount());
        }
    }

    @Test
    public void testRemotePageLast() throws ApiException, IOException {        
        Long lastMTime = null;
        for (OriginCountDto originCount: remote.getOriginStatistics()) {
            if ("ds.radio".equals(originCount.getOrigin())) {
                lastMTime = originCount.getLatestMTime();
            }
        }
        assertNotNull(lastMTime, "lastMTime should be extractable for 'ds.radiotv'");

        // Decrement with 1 to be sure to match the latest record
        lastMTime -= 1;

        try (ContinuationStream<DsRecordDto, Long> recordStream =
                     remote.getRecordsModifiedAfterStream("ds.radio", lastMTime, 10L)) {
            assertEquals(1, recordStream.count(), "There should be a single record after " + lastMTime);
            assertFalse(recordStream.hasMore(), "The hasMore flag should be false");
        }
    }

    @Test
    public void testRemoteRecordsTreeRaw() throws IOException {
  
        try (ContinuationInputStream<Long> recordsIS = remote.getRecordsByRecordTypeModifiedAfterLocalTreeJSON(
                             "ds.radio", RecordTypeDto.DELIVERABLEUNIT,  0L, 3L)) {
            String recordsStr = IOUtils.toString(recordsIS, StandardCharsets.UTF_8);
            assertTrue(recordsStr.contains("\"id\":\"ds.radio:oai"),
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
     
        try (ContinuationStream<DsRecordDto, Long> records = remote.getRecordsModifiedAfterStream(
                "ds.radio", 0L,numberOfRecords)) {
            List<DsRecordDto> recordList = records.collect(Collectors.toList());
            
            
            assertEquals(numberOfRecords, recordList.size(), "The requested number of records should be received");
            assertNotNull(records.getContinuationToken(),
                    "The highest modification time should be present");
            log.debug("Stated highest modification time was: '{}'", records.getContinuationToken());
            assertEquals(recordList.get(recordList.size()-1).getmTime(),
                         records.getContinuationToken(),
                    "Received highest mTime should match stated highest mTime");
        }
    }

    @Test
    public void testRemoteRecordsTreeStream() throws IOException {
      
        try (ContinuationStream<DsRecordDto, Long> records = remote.getRecordsByRecordTypeModifiedAfterLocalTreeStream(
                "ds.radio", RecordTypeDto.DELIVERABLEUNIT, 0L, 3L)) {
            long count = records.count();
            System.out.println(records.getResponseHeaders());
            assertEquals(3L, count, "The requested number of records should be received");
            assertNotNull(records.getContinuationToken(),"The highest modification time should be present");
        }
    }

    @Test
    public void testRemoteMinimalRecordsStream() throws IOException {
        try (ContinuationStream<DsRecordMinimalDto, Long> records = remote.getDsRecordsMinimalModifiedAfterStream(
                "ds.tv", 5, 0L)){
            long count = records.count();

            assertEquals(5L, count, "The requested number of records should be received");
            assertNotNull(records.getContinuationToken(),"The highest modification time should be present");

        }

    }

    @Test
    public void testRemoteMinimalRecordsContent() throws IOException {
        try (ContinuationInputStream<Long> recordsIS = remote.getMinimalRecordsModifiedAfterJSON(
                "ds.tv", 0L, 10L)) {
            String recordsStr = IOUtils.toString(recordsIS, StandardCharsets.UTF_8);
            assertTrue(recordsStr.contains("\"id\":\"ds.tv:oai"),
                    "At least 1 JSON block for a record should be returned");
            // Minimal records shouldn't contain data
            assertFalse(recordsStr.contains("\"data\":"));
        }
    }

}
