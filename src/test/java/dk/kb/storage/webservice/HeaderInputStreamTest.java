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
package dk.kb.storage.webservice;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class HeaderInputStreamTest {

    @Test
    public void testBasicStream() throws IOException {
        URI uri = UriBuilder.fromUri("https://www.kb.dk/").build();

        String plainStr = IOUtils.toString(uri, StandardCharsets.UTF_8);

        String headerString;
        try (HeaderInputStream headerStream = HeaderInputStream.from(uri)) {
            headerString = IOUtils.toString(headerStream, StandardCharsets.UTF_8);
            assertEquals("bytes", headerStream.getHeaders().get("Accept-Ranges").get(0),
                    "The header 'Accept-Ranges: bytes' should be present");
        }

        assertEquals(plainStr, headerString, "Plain InputStream and HeaderInputStream should deliver the same content");
    }
}