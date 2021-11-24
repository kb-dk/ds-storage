package dk.kb.storage.api.v1.impl;

import dk.kb.storage.api.v1.*;
import dk.kb.storage.model.v1.ErrorDto;
import java.io.File;
import dk.kb.storage.model.v1.HelloReplyDto;
import dk.kb.storage.storage.DsStorage;



import dk.kb.storage.webservice.exception.ServiceException;
import dk.kb.storage.webservice.exception.InternalServiceException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



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
 * <p>ds-storage by the Royal Danish Library 
 *
 */
public class DsStorageApiServiceImpl implements DsStorageApi {
    private Logger log = LoggerFactory.getLogger(this.toString());



    /* How to access the various web contexts. See https://cxf.apache.org/docs/jax-rs-basics.html#JAX-RSBasics-Contextannotations */

    @Context
    private transient UriInfo uriInfo;

    @Context
    private transient SecurityContext securityContext;

    @Context
    private transient HttpHeaders httpHeaders;

    @Context
    private transient Providers providers;

    @Context
    private transient Request request;

    // Disabled as it is always null? TODO: Investigate when it can be not-null, then re-enable with type
    //@Context
    //private transient ContextResolver contextResolver;

    @Context
    private transient HttpServletRequest httpServletRequest;

    @Context
    private transient HttpServletResponse httpServletResponse;

    @Context
    private transient ServletContext servletContext;

    @Context
    private transient ServletConfig servletConfig;

    @Context
    private transient MessageContext messageContext;


    
    @Override
    public HelloReplyDto getGreeting(String alternateHello) throws ServiceException {
        // TODO: Implement...    	
        
        try{ 
        	
        	DsStorage storage = new DsStorage();
        
        	
        	storage.createNewDatabase("/home/teg/workspace/ds-storage/src/test/resources/ddl/create_ds_storage.ddl");
        	
        	String id ="id1";
	    	String base="base_test";	    	
	    	String data = "Hello";
	    	String parentId="id_1_parent";
	  	    /*	
	    	DsRecord record = new DsRecord(id, base,data, parentId);
            storage.createNewRecord(record );
            */  
            HelloReplyDto response = new HelloReplyDto();
            
        response.setMessage("KEG7Z6");
        return response;
        } catch (Exception e){
            throw handleException(e);
        }
    
    }

  
    /**
    * This method simply converts any Exception into a Service exception
    * @param e: Any kind of exception
    * @return A ServiceException
    * @see dk.kb.storage.webservice.ServiceExceptionMapper
    */
    private ServiceException handleException(Exception e) {
        if (e instanceof ServiceException) {
            return (ServiceException) e; // Do nothing - this is a declared ServiceException from within module.
        } else {// Unforseen exception (should not happen). Wrap in internal service exception
            log.error("ServiceException(HTTP 500):", e); //You probably want to log this.
            return new InternalServiceException(e.getMessage());
        }
    }

}
