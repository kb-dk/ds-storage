package dk.kb.storage.webservice;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import dk.kb.storage.api.v1.impl.DsStorageApiServiceImpl;
import dk.kb.storage.api.v1.impl.ServiceApiServiceImpl;
import dk.kb.util.webservice.exception.ServiceExceptionMapper;


public class Application_v1 extends javax.ws.rs.core.Application {

    @Override
    public Set<Class<?>> getClasses() {
        return new HashSet<>(Arrays.asList(
                JacksonJsonProvider.class,
                DsStorageApiServiceImpl.class,
                ServiceApiServiceImpl.class,
                ServiceExceptionMapper.class,
                OpenApiResource.class
        ));
    }


}
