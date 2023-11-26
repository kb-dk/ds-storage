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

import dk.kb.util.Pair;

import javax.servlet.http.HttpServletResponse;
import java.util.Objects;
import java.util.Optional;

/**
 * Support methods for handling continuation headers for webservices.
 */
public class ContinuationSupport {

    /**
     * Set as header by record streaming endpoints to communicate the highest mTime that any records will contain.
     * This always means the mTime for the last record in the stream.
     * <p>
     * Note that there is no preceeding {@code X-} as this is discouraged by
     * <a href="https://www.rfc-editor.org/rfc/rfc6648">rfc6648</a>.
     */
    public static final String HEADER_PAGING_CONTINUATION_TOKEN = "Paging-Continuation-Token";
    public static final String HEADER_PAGING_HAS_MORE = "Paging-Has-More";

    /**
     * Extract the {@code #HEADER_PAGING_CONTINUATION_TOKEN} from the given {@code headerInputStream} and return it.
     * <p>
     * Note: The header might be undefined.
     * @param headerInputStream provides the headers to check.
     * @return an optional continuation.
     */
    public static Optional<String> getContinuationToken(HeaderInputStream headerInputStream) {
        return headerInputStream.getHeaders().get(HEADER_PAGING_CONTINUATION_TOKEN) == null ?
                Optional.empty() :
                Optional.of(headerInputStream.getHeaders().get(HEADER_PAGING_CONTINUATION_TOKEN).get(0));
    }

    /**
     * Extract the {@code #HEADER_PAGING_HAS_MORE} from the given {@code headerInputStream} and return it.
     * <p>
     * Note: The header might be undefined.
     * @param headerInputStream provides the headers to check.
     * @return an optional hasMore, signalling whether subsequent calls are likely to provide more data.
     */
    public static Optional<Boolean> getHasMore(HeaderInputStream headerInputStream) {
        return headerInputStream.getHeaders().get(HEADER_PAGING_HAS_MORE) == null ?
                Optional.empty() :
                Optional.of(Boolean.parseBoolean(headerInputStream.getHeaders().get(HEADER_PAGING_HAS_MORE).get(0)));
    }

    /**
     * Use {@link Pair#getLeft()} as {@code continuationToken} for {@link #HEADER_PAGING_CONTINUATION_TOKEN}.
     * Use {@link Pair#getRight()} as {@code hasMore} for {@link #HEADER_PAGING_HAS_MORE}.
     * @param httpServletResponse headers are assigned with {@link HttpServletResponse#setHeader(String, String)}.
     * @param continuationAndHasMore pair containing the continuation token and the has more boolean.
     */
    public static void setHeaders(HttpServletResponse httpServletResponse, Pair<Long, Boolean> continuationAndHasMore) {
        setHeaderContinuation(httpServletResponse, continuationAndHasMore.getLeft());
        setHeaderHasMore(httpServletResponse, continuationAndHasMore.getRight());
    }

    /**
     * Set {@code continuationToken} as the value for {@link #HEADER_PAGING_CONTINUATION_TOKEN} if it exists.
     * @param httpServletResponse header is assigned with {@link HttpServletResponse#setHeader(String, String)}.
     * @param continuationToken the continuation token or {@code null} is none exists.
     */
    private static void setHeaderContinuation(HttpServletResponse httpServletResponse, Object continuationToken) {
        if (continuationToken == null) {
            return;
        }
        httpServletResponse.setHeader(HEADER_PAGING_CONTINUATION_TOKEN, Objects.toString(continuationToken));
    }

    /**
     * Set {@code hasMore} as the value for {@link #HEADER_PAGING_HAS_MORE} if it exists.
     * @param httpServletResponse header is assigned with {@link HttpServletResponse#setHeader(String, String)}.
     * @param hasMore the has more token or {@code null} is none exists.
     */
    private static void setHeaderHasMore(HttpServletResponse httpServletResponse, Boolean hasMore) {
        if (hasMore == null) {
            return;
        }
        httpServletResponse.setHeader(HEADER_PAGING_HAS_MORE, Boolean.toString(hasMore));
    }

}
