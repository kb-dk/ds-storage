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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * HTTP specific {@link InputStream} wrapper that keeps track of the headers returned by the server.
 */
public class HeaderInputStream extends FilterInputStream implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(HeaderInputStream.class);

    private final Map<String, List<String>> headers;

    /**
     * Establish a connection to the given {@code uri}, extract all headers and construct a
     * {@link HeaderInputStream} with the headers and response stream from the {@code uri}.
     * @param uri full URI for a call to a webservice.
     * @return an {@code InputStream} with the response.
     * @throws IOException if the connection failed.
     */
    public static HeaderInputStream from(URI uri) throws IOException {
        log.debug("Opening streaming connection to '{}'", uri);
        HttpURLConnection con = (HttpURLConnection) uri.toURL().openConnection();
        con.setInstanceFollowRedirects(true);
        int status = con.getResponseCode();
        if (status < 200 || status > 299) {
            throw new IOException("Got HTTP " + status + " establishing connection to '" + uri + "'");
            // TODO: Consider if the error stream should be logged. It can be arbitrarily large
        }
        Map<String, List<String>> headers = con.getHeaderFields();
        return new HeaderInputStream(headers, con.getInputStream());
    }

    private HeaderInputStream(Map<String, List<String>> headers, InputStream in) {
        super(in);
        this.headers = headers;
    }

    /**
     * @return headers from the established connection.
     */
    public Map<String, List<String>> getHeaders() {
        return headers;
    }
}
