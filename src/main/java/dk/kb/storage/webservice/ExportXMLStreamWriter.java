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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Writer;

/**
 * Wrapper that handles streamed output of a entries as XML. This differs from the standard XMLStreamWriter by
 * supporting the Jackson annotated POJOs as input.
 *
 * Use the method {@link #write(Object)} and remember to call {@link #close} when finished.
 */
public class ExportXMLStreamWriter extends ExportWriter {
    private static final Logger log = LoggerFactory.getLogger(ExportXMLStreamWriter.class);
    private final XmlMapper xmlMapper = new XmlMapper();

    private final boolean writeNulls;
    private final String wrapperElement;
    private boolean first = true;
    private boolean isclosing = false; // If the writer is in the process of closing (breaks infinite recursion)

    /**
     * Wrap the given inner Writer in the ExportXMLStreamWriter. Calls to {@link #write(Object)} writes directly to
     * inner, so the ExportXMLStreamWriter holds no cached data. The inner {@link Writer#flush()} is not called during
     * write.
     * null-values in objects given to {@link #write(Object)} will not be written. To control this, use
     * the {@link ExportXMLStreamWriter (Writer, FORMAT, boolean)} constructor.
     * @param rootElement the name of the element that wraps XML for the given Jackson annotated Objects.
     * @param inner  the Writer to send the result to.
     */
    public ExportXMLStreamWriter(Writer inner, String rootElement) {
        this(inner, rootElement, false);
    }

    /**
     * Wrap the given inner Writer in the ExportXMLStreamWriter. Calls to {@link #write(Object)} writes directly to
     * inner, so the ExportXMLStreamWriter holds no cached data. The inner {@link Writer#flush()} is not called during
     * write.
     * @param inner  the Writer to send the result to.
     * @param rootElement the name of the element that wraps XML for the given Jackson annotated Objects.
     * @param writeNulls if true, null values are written as {@code "key" : null}, if false they are skipped.
     */
    public ExportXMLStreamWriter(Writer inner, String rootElement, boolean writeNulls) {
        super(inner);
        this.writeNulls = writeNulls;
        this.wrapperElement = rootElement;

        xmlMapper.setSerializationInclusion(writeNulls ? JsonInclude.Include.ALWAYS : JsonInclude.Include.NON_NULL);
        xmlMapper.enable(SerializationFeature.INDENT_OUTPUT);
//        xmlWriter = mapper.writer(new MinimalPrettyPrinter()).withoutRootName();
        
    }

    /**
     * Write an XML expression that has already been serialized to String.
     * It is the responsibility of the caller to ensure that the XML in xmlStr is valid.
     * @param xmlStr a valid XML represented as a String.
     */
    @Override
    public void write(String xmlStr) {
        if (first) {
            super.write("<" + wrapperElement + ">");
            first = false;
        } else {
            super.write("\n");
        }
        super.write(xmlStr);
    }

    /**
     * Use {@link #xmlMapper} to serialize the given object to String XML and write the result.
     * @param annotatedObject a Jackson annotated Object.
     */
    @Override
    public void write(Object annotatedObject) {
        if (annotatedObject == null) {
            log.warn("Internal inconsistency: write(null) called. This should not happen");
            return;
        }
        try {
            write(xmlMapper.writeValueAsString(annotatedObject));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JsonProcessingException attempting to write " + annotatedObject, e);
        } catch (IOException e) {
            throw new RuntimeException("IOException attempting to write " + annotatedObject, e);
        }
    }

    /**
     * Finishes the XML stream by writing closing statements (if needed).
     */
    @Override
    public void close() {
        if (isclosing) {
            return; // Avoid infinite recursion
        }
        isclosing = true;
        if (first) {
            super.write("<" + wrapperElement + ">");
        }
        super.write("\n</" + wrapperElement + ">\n");
        super.close();
    }
}
