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

import java.util.stream.Stream;

/**
 * Stream wrapper with first class support for continuation token, used for requesting extra data that follows
 * logically after the last element in the current stream.
 * <p>
 * This is akin to Solr's {@code cursorMark} and OAI-PMH's {@code resumptionToken}.
 * 
 * @param <T> the class of objects for the stream.
 * @param <C> the class for the {@code continuationToken}, typically {@code String>} or {@code Long}.
 */
public class ContinuationStream<T, C> extends FilterStream<T> implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(ContinuationStream.class);

    private final C continuationToken;
    private final boolean hasMore;

    /**
     * Create a stream.
     *
     * @param inner             the provider of the elements.
     * @param continuationToken used for requesting a new stream that continues after the last element of the
     *                          current stream. If {@code null}, no continuation information is available.
     * @param hasMore           whether or not a subsequent request for a stream is likely to produce any elements.
     */
    public ContinuationStream(Stream<T> inner, C continuationToken, boolean hasMore) {
        super(inner);
        this.continuationToken = continuationToken;
        this.hasMore = hasMore;
        log.debug("Creating ContinuationStream with continuationToken='{}', hasMore={}",
                  continuationToken, hasMore);
    }

    /**
     * @return continuation token intended for requesting a new stream that delivers from the point where the
     * current stream stops. If {@code null}, no continuation information is available.
     * @see #hasMore()
     */
    public C getContinuationToken() {
        return continuationToken;
    }

    /**
     * Non-authoritative indicator for whether or not an immediate request for a new stream using
     * {@link #getContinuationToken()} will return any results.
     *
     * @return true if a request for a new stream using {@link #getContinuationToken()} will result in data,
     * false if such a call will <em>probably</em> not give any data at the current time, but might
     * result is extra data at a later point in time.
     * @see #getContinuationToken()
     */
    public boolean hasMore() {
        return hasMore;
    }
}
