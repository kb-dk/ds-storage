package dk.kb.storage.util;

import java.util.ArrayList;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public class UniqueTimestampGeneratorTest {

    private static final Logger log = LoggerFactory.getLogger(UniqueTimestampGeneratorTest.class);


    @Test
    public void testUniqueTimestamps() {

        int numberOfTimeStamps =10000; // Need at least 1000 to test nano overflow
        ArrayList<Long> stamps = new ArrayList<> ();

        long start=System.currentTimeMillis();

        for (int i = 0;i<numberOfTimeStamps;i++) { 	    		
            stamps.add(UniqueTimestampGenerator.next());	    		
        }
        log.info("Generated '{}' in '{}' millis", numberOfTimeStamps, (System.currentTimeMillis()-start));

        //Test they are different and increasing
        for (int i = 0;i<numberOfTimeStamps-1;i++) { 	    		              
            Assertions.assertTrue(stamps.get(i)<stamps.get(i+1));	    			    	       
        }


    }	    

}
