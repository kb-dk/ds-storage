package dk.kb.storage.config;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import dk.kb.storage.model.v1.RecordBaseDto;
import dk.kb.storage.model.v1.UpdateStrategyDto;
import dk.kb.storage.util.IdNormaliser;
import dk.kb.util.yaml.YAML;

/**
 * Sample configuration class using the Singleton pattern.
 * This should work well for most projects with non-dynamic properties.
 */
public class ServiceConfig {

	  private static final Logger log = LoggerFactory.getLogger(ServiceConfig.class);
	
	//key is basename
	private static final HashMap<String,RecordBaseDto> allowedBases = new HashMap<String,RecordBaseDto>();
    
	
	/**
	 * Besides parsing of YAML files using SnakeYAML, the YAML helper class provides convenience
	 * methods like {@code getInteger("someKey", defaultValue)} and {@code getSubMap("config.sub1.sub2")}.
	 */
	private static YAML serviceConfig;

	/**
	 * Initialized the configuration from the provided configFile.
	 * This should normally be called from {@link dk.kb.storage.webservice.ContextListener} as
	 * part of web server initialization of the container.
	 * @param configFile the configuration to load.
	 * @throws IOException if the configuration could not be loaded or parsed.
	 */
	public static synchronized void initialize(String configFile) throws IOException {
		serviceConfig = YAML.resolveLayeredConfigs(configFile);
		loadAllowedBases();
	}

	/**
	 * Demonstration of a first-class property, meaning that an explicit method has been provided.
	 * @see #getConfig() for alternative.
	 * @return the "Hello World" lines defined in the config file.
	 */
	public static List<String> getHelloLines() {
		List<String> lines = serviceConfig.getList("config.helloLines");
		return lines;
	}

	private static void loadAllowedBases() throws IOException{

		List<YAML> bases = serviceConfig.getYAMLList("config.allowed_bases");
		//Load updtateStategy for each
		for (YAML base: bases) {
			String name = base.getString("name");
			if (!IdNormaliser.validateRecordBase(name)) {
			    throw new IOException("Configured recordBase: '"+name+"' does not validate to regexp for recordbase");			    
			}
			
			String updateStrategy = base.getString("update_strategy");        
			RecordBaseDto recordBase = new RecordBaseDto();
			recordBase.setName(name);
			recordBase.setUpdateStrategy(UpdateStrategyDto.valueOf(updateStrategy));                	
			allowedBases.put(name, recordBase);
            log.info("Updatestrategy loaded for recordbase:"+recordBase.getName()  +" with update strategy:"+recordBase.getUpdateStrategy());
		}

		log.info("Allowed bases loaded from config. Number of bases:"+allowedBases.size());
		
	}


	public static  String getDBDriver() {
		String dbDriver= serviceConfig.getString("config.db.driver");
		return dbDriver;
	}

	public static  String getDBUrl() {
		String dbUrl= serviceConfig.getString("config.db.url");
		return dbUrl;
	}

	public static  String getDBUserName() {
		String dbUserName= serviceConfig.getString("config.db.username");
		return dbUserName;
	}

	public static  String getDBPassword() {
		String dbPassword= serviceConfig.getString("config.db.password");
		return dbPassword;
	}
		
	public static HashMap<String, RecordBaseDto> getAllowedBases() {
		return allowedBases;
	}

	
	/**
	 * Direct access to the backing YAML-class is used for configurations with more flexible content
	 * and/or if the service developer prefers key-based property access.
	 * @see #getHelloLines() for alternative.
	 * @return the backing YAML-handler for the configuration.
	 */
	public static YAML getConfig() {
		if (serviceConfig == null) {
			throw new IllegalStateException("The configuration should have been loaded, but was not");
		}
		return serviceConfig;
	}

}
