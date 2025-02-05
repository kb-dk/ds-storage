package dk.kb.storage.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.kb.util.webservice.exception.InvalidArgumentServiceException;

public class IdNormaliser {

    private static final Logger log = LoggerFactory.getLogger(IdNormaliser.class);    
    
    private static final String regexpIdPattern="([a-z0-9.]+):([a-zA-Z0-9:._-]+)";
    private static final Pattern idPattern = Pattern.compile(regexpIdPattern);
    
    private static final String regexpOrigin="([a-z0-9.]+)";
    private static final Pattern originPattern = Pattern.compile(regexpOrigin);
    
    
    private static final Pattern NO_GO = Pattern.compile("[^a-zA-Z0-9:._-]");
    
    /**
     * Normalise the ID. Invalid characters will be replaced.
     * @param id to normalise.
     */
    public static String normaliseId(String id)  throws Exception{
     String orgId= id;
     
       String[][] replaces= new String[][]{
            {"æ", "ae"},
            {"ä", "ae"},
            {"Æ", "Ae"},
            {"Ä", "Ae"},
            {"ø", "oe"},
            {"ö", "oe"},
            {"Ø", "Oe"},
            {"Ö", "Oe"},
            {"å", "aa"},
            {"Å", "Aa"},
            {" ", "-"},
            {"/", "-"},
            {"~", "-"}
            };
    

        // Note: Not a proper id as the collection is not added        
        for (String[] subst: replaces) {
            id = id.replace(subst[0], subst[1]);
        }
        id = NO_GO.matcher(id).replaceAll(".");

        if (!validateID(id)) { //If this happen we probably have to fix this method
            log.error("Unable to normalize id:"+orgId);      
            throw new InvalidArgumentServiceException("ID  syntax was not valid normalisation could not correct it:"+orgId);
        }
                
        return id;         
    }
    
    
    public static boolean validateID(String recordId) {
        Matcher m = idPattern.matcher(recordId);      
        return m.matches();
    }
    
    public static boolean validateOrigin(String origin) {
        Matcher m = originPattern.matcher(origin);      
        return m.matches();
    }
    
    
}
