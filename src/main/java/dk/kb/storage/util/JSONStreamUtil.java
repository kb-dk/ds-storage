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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Helpers for handling streaming deserialization of JSON to Java objects.
 */
public class JSONStreamUtil {
    private static final Logger log = LoggerFactory.getLogger(JSONStreamUtil.class);

    /**
     * Convert the given {@code jsonStream} to a {@link Stream} of objects of the given {@code clazz}.
     * <p>
     * The {@code jsonStream} is expected to be a JSON array containing only JSON representations of {@code clazz}.
     * The conversion happens lazily and an arbitrarily large {@code jsonStream} can be given.
     * <p>
     * Important: Ensure that the returned stream is closed to avoid resource leaks.
     * @param jsonStream JSON response with {@code clazz} objects.
     * @param clazz the class to deserialize the JSON to.
     * @return a stream of {@code clazz} de-serialized from the {@code jsonStream}.
     * @throws IOException if the {@code jsonStream} could not be read.
     */
    public static <T> Stream<T> jsonToObjectsStream(InputStream jsonStream, Class<T> clazz) throws IOException {
        Iterator<T> iRecords = jsonToObjectsIterator(jsonStream, clazz);
        return StreamSupport.stream(
                        Spliterators.spliteratorUnknownSize(iRecords, Spliterator.ORDERED),
                        false) // iterator -> stream
                .onClose(() -> {
                    try {
                        log.debug("jsonToObjectsStream: Closing source InputStream");
                        jsonStream.close();
                    } catch (IOException e) {
                        // Not critical but should generally not happen so we warn
                        log.warn("jsonToObjectsStream: IOException attempting to close InputStream", e);
                    }
                });
    }

    /**
     * Convert the given {@code jsonStream} to an {@link Iterator} of objects of the given {@code clazz}.
     * <p>
     * The {@code jsonStream} is expected to be a JSON array containing only JSON representations of {@code clazz}.
     * The conversion happens lazily and an arbitrarily large {@code jsonStream} can be given.
     * <p>
     * Note: The returned {@code Iterator} is not closeable, which might lead to resource leaks in case of problems
     * such as a remote caller disconnecting. This is to be handled outside of this context.
     * @param jsonStream JSON response with {@code clazz} objects.
     * @param clazz the class to deserialize the JSON to.
     * @return an iterator of {@code clazz} de-serialized from the {@code jsonStream}.
     * @throws IOException if the {@code jsonStream} could not be read.
     * @see #jsonToObjectsStream(InputStream, Class) 
     */
    public static <T> Iterator<T> jsonToObjectsIterator(InputStream jsonStream, Class<T> clazz) throws IOException {
        JsonFactory jFactory = new JsonFactory();
        ObjectMapper mapper = new ObjectMapper();
        jFactory.setCodec(mapper);
        JsonParser jParser = jFactory.createParser(jsonStream);

        if (jParser.nextToken() != JsonToken.START_ARRAY) {
            throw new IllegalStateException("Expected JSON START_ARRAY but got " + jParser.currentToken());
        }
        // In principle we could just use jParser.readValuesAs(clazz); but that will not close the jsonStream
        // as it stops at the end-array token
        return new Iterator<>() {
            private T nextRecord = null;
            private boolean eolReached = false;

            @Override
            public boolean hasNext(){
                ensureNext();
                return nextRecord != null;
            }

            @Override
            public T next() {
                if (!hasNext()) {
                    throw new IllegalStateException("next() called with hasNext() == false");
                }
                T record = nextRecord;
                nextRecord = null;
                return record;
            }

            /**
             * Move to next JSON token. If it is an END_ARRAY, processing is stopped, else a clazz-object is read
             */
            private void ensureNext() {
                if (nextRecord != null || eolReached) {
                    return;
                }
                try {
                    if (jParser.nextToken() == JsonToken.END_ARRAY) {
                        eolReached = true;
                        jParser.close();
                        jsonStream.close();
                        return;
                    }
                } catch (IOException e) {
                    throw new RuntimeException("Unable to read next JSON token", e);
                }
                try {
                    // Unfortunately we need to use the clazz instead of T
                    // https://stackoverflow.com/questions/28895088/how-to-deserialize-a-generic-type-with-objectmapper-jackson
                    nextRecord = jParser.readValueAs(clazz);
                } catch (IOException e) {
                    throw new RuntimeException("Unable to read " + clazz.getName() + " Object from JSON stream", e);
                }
            }
        };
    }

}
