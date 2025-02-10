package dk.kb.storage.config;

import dk.kb.util.Resolver;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

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
class ServiceConfigTest {

  
    private static final Logger log = LoggerFactory.getLogger(ServiceConfigTest.class);
    
    /*
     * This unit-test probably fails when the template is applied and a proper project is taking form.
     * That is okay. It is only here to serve as a temporary demonstration of unit-testing and configuration.
     */
    @Test
    void sampleConfigTest() throws IOException {
        // Pretty hacky, but it is only a sample unit test
        Path knownFile = Path.of(Resolver.resolveURL("logback-test.xml").getPath());
        String projectRoot = knownFile.getParent().getParent().getParent().toString();

        Path sampleEnvironmentSetup = Path.of(projectRoot, "conf/ds-storage-environment.yaml");
        
        
        
        if(!Files.exists(sampleEnvironmentSetup)) {
            log.warn("You must create a local yaml-file: 'ds-storage-environment.yaml' with local values if you want to start up jetty");
            
        }
        

        ServiceConfig.initialize(projectRoot + File.separator + "conf" + File.separator + "ds-storage*.yaml");

     
    }
    
    
    
}