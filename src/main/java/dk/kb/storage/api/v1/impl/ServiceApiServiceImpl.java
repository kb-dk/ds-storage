package dk.kb.storage.api.v1.impl;

import dk.kb.storage.api.v1.*;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;
import dk.kb.storage.model.v1.StatusDto;

import dk.kb.util.BuildInfoManager;
import dk.kb.util.webservice.exception.ServiceException;
import dk.kb.util.webservice.exception.InternalServiceException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.io.File;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dk.kb.util.webservice.ImplBase;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Providers;
import javax.ws.rs.core.MediaType;
import org.apache.cxf.jaxrs.model.wadl.Description;
import org.apache.cxf.jaxrs.model.wadl.DocTarget;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.jaxrs.ext.multipart.*;

import io.swagger.annotations.Api;

/**
 * ds-storage
 *
 * <p># Ds-storage(Digitale Samlinger) by the Royal Danish Library.      ## A metadata storage. Ds-storage is a storage for metadata to describe objects in collections.   The metadata format is UTF-8 encoded but format can be anything from text,XML,JSON etc. depending on the metadata format for that collection. The basic idea behind ds-storage is a single access point to metadata describing objects belonging to many different collections. Having a simple API to  store and retrieve the records. Instead of integrating to wide range of different APIs located on different servers, the ds-storage offers a unified API for all of them and is optimized for retrieving a specific record very fast.  ## Records Objects in ds-storage are called records. A record has a metadata field that is UTF-8 encoded. The value of the metadata will typical be same as in the originating collection if harvested with OAI-PMH etc.       A record must has an origin that is prefined in the ds-storage configuration. This is the name of the collection in ds-storage. The recordId must also have the origin as prefix so it is easy from a recordid to see where it belongs. When a record is created it will be given a creation time (cTime) that will never be changed and a modification time (mTime). If a record is later update only the mTime will be updated. The timeformat of cTime and mTime is system milis with 3 added digits and is guaranteed to be unique values.                        ## Naming convention for origin and id The origin must only contain lowercase letters and dot (.) as separator.      The id must start with the origin followed by colon (:). The part after origin must only contain of upper of lower case letters, digits and the following characters:  : . _ -      Regexp for origin: ([a-z0-9.]+)   Regexp for id: ([a-z0-9.]+):([a-zA-Z0-9:._-]+)  ## ID normalisation                      If a record contains a invalid character after the origin part, it will be normalised and the invalid characters will be replaced. The original (invalid) id will be stored in the 'orgid' field and flagged for invalid id. Having the original id will make it possible to track it back to the collection it came from. The record can still be retrieved and updated using the invalid id, but also by the normalised id.                                                              ## Record hierarchy                  The datamodel allows a single optional parent and unlimited number of children. Every origin is configured with a transitive  update strategy that makes sense for the origin. When a record is created or update it can update the mTime of parent and all children if defined for the origin. The possible update strategies are: NONE, ALL, PARENT, CHILDREN.  (see #updatestrategy schema)      ## API    Records can be extracted by recordId or as a list by specified origin and last modification time (mTime). The uniqueness of mTime will ensure batching through the records using mTime will not return same record twice. 
 *
 */
public class ServiceApiServiceImpl extends ImplBase implements ServiceApi {
    private Logger log = LoggerFactory.getLogger(this.toString());



    /**
     * Ping the server to check if the server is reachable.
     * 
     * @return <ul>
      *   <li>code = 200, message = "OK", response = String.class</li>
      *   </ul>
      * @throws ServiceException when other http codes should be returned
      *
      * @implNote return will always produce a HTTP 200 code. Throw ServiceException if you need to return other codes
     */
    @Override
    public String ping() throws ServiceException {
        try{
            return "Pong";
        } catch (Exception e){
            throw handleException(e);
        }
    }

    /**
     * Detailed status / health check for the service
     * 
     * @return <ul>
      *   <li>code = 200, message = "OK", response = StatusDto.class</li>
      *   <li>code = 500, message = "Internal Error", response = String.class</li>
      *   </ul>
      * @throws ServiceException when other http codes should be returned
      *
      * @implNote return will always produce a HTTP 200 code. Throw ServiceException if you need to return other codes
     */
    @Override
    public StatusDto status() throws ServiceException {
        log.debug("status() called with call details: {}", getCallDetails());
        String host = "N/A";
        try {
            host = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            log.warn("Exception resolving hostname", e);
        }
        return new StatusDto()
                .application(BuildInfoManager.getName())
                .version(BuildInfoManager.getVersion())
                .build(BuildInfoManager.getBuildTime())
                .java(System.getProperty("java.version"))
                .heap(Runtime.getRuntime().maxMemory()/1048576L)
                .server(host)
                .health("ok");
    }


}
