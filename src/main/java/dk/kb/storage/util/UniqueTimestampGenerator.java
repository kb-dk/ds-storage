package dk.kb.storage.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generate a new unique timestamp. The format is System.currentTimeMillis() with 3 added digits.
 * If systemtime is : 1637057234458 the timestamp will be 1637057234458000
 * If another timestamp is required at same millis it will 1637057234458001 etc.
 * <p>
 * To convert a timestamp to a date in millis just remove the last 0 digits.
 * <p>
 *
 * If the maximum of 999 is reached for the additional nanos, it will trigger a 1 millis sleep. But this will likely never happen.
 * Stress test of just this method could not generate more than 200 uniquestamps within the same millis. 
 *
 */
public class UniqueTimestampGenerator {

	private static long lastUsedTimestamp=System.currentTimeMillis();
	private static int lastUsedAdditionalNanos = 0;

	private static final Logger log = LoggerFactory.getLogger(UniqueTimestampGenerator.class);
		
    //Force use of the static method.
	private UniqueTimestampGenerator() {      
	}

	synchronized static public long next() {
		long sysTime = System.currentTimeMillis();

		if (lastUsedAdditionalNanos == 999) { //Will never happen in theory by hardware limitation. 
			try { 
				Thread.sleep(1L);
			}
			catch(Exception e) {				
				log.error("Error in generating timestamp: '{}'", e.getMessage());
			}
			lastUsedAdditionalNanos=0;//Reset to next milis
			sysTime = System.currentTimeMillis();
		}

		if (sysTime == lastUsedTimestamp) {
			lastUsedAdditionalNanos++;        	        	
		}
		else {
			lastUsedAdditionalNanos=0; //Reset nanos
		}

		long next = sysTime*1000+lastUsedAdditionalNanos;
		lastUsedTimestamp=sysTime;			
		return next;
	}

}


