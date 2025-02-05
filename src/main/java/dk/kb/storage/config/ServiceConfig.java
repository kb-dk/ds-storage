package dk.kb.storage.config;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.kb.storage.model.v1.OriginDto;
import dk.kb.storage.model.v1.UpdateStrategyDto;
import dk.kb.storage.util.IdNormaliser;
import dk.kb.util.yaml.YAML;

/**
 * Sample configuration class using the Singleton pattern.
 * This should work well for most projects with non-dynamic properties.
 */
public class ServiceConfig {

	  private static final Logger log = LoggerFactory.getLogger(ServiceConfig.class);

	  public static final int DB_BATCH_SIZE_DEFAULT = 100;

	//key is origin
	private static final HashMap<String,OriginDto> allowedOrigins = new HashMap<>();
    
	
	/**
	 * Besides parsing of YAML files using SnakeYAML, the YAML helper class provides convenience
	 * methods like {@code getInteger("someKey", defaultValue)} and {@code getSubMap("config.sub1.sub2")}.
	 */
	private static YAML serviceConfig;

	/**
	 * Initialized the configuration from the provided configFile.
	 * This should normally be called from {@link dk.kb.storage.webservice.ContextListener} as
	 * part of web server initialization of the container.
	 * @param configFiles the YAML files which the configuration is loaded from.
	 * @throws IOException if the configuration could not be loaded or parsed.
	 */
	public static synchronized void initialize(String... configFiles) throws IOException {
		serviceConfig = YAML.resolveLayeredConfigs(configFiles);
		serviceConfig.setExtrapolate(true);
		loadAllowedOrigins();
	}

	private static void loadAllowedOrigins() throws IOException{

		List<YAML> origins = serviceConfig.getYAMLList("origins");
		//Load updtateStategy for each
		for (YAML origin: origins) {
			String name = origin.getString("name");
			if (!IdNormaliser.validateOrigin(name)) {
			    throw new IOException("Configured origin: '"+name+"' does not validate to regexp for origin");			    
			}
			
			String updateStrategy =origin.getString("updateStrategy");        
			OriginDto originDto = new OriginDto();
			originDto.setName(name);
			originDto.setUpdateStrategy(UpdateStrategyDto.valueOf(updateStrategy));                	
			allowedOrigins.put(name, originDto);
            log.info("Updatestrategy loaded for origin: '{}' with update strategy: '{}'", originDto.getName(), originDto.getUpdateStrategy());
		}

		log.info("Allowed origin loaded from config. Number of origins: '{}'", allowedOrigins.size());
		
	}


	public static  String getDBDriver() {
		String dbDriver= serviceConfig.getString("db.driver");
		return dbDriver;
	}

	public static  String getDBUrl() {
		String dbUrl= serviceConfig.getString("db.url");
		return dbUrl;
	}

	public static  String getDBUserName() {
		String dbUserName= serviceConfig.getString("db.username");
		return dbUserName;
	}

	public static  String getDBPassword() {
		String dbPassword= serviceConfig.getString("db.password");
		return dbPassword;
	}

	public static int getConnectionPoolSize() {
	   int connectionPoolSize= serviceConfig.getInteger("db.connectionPoolSize",10); //Default 10
	    return connectionPoolSize;
	}
	
	public static int getDBBatchSize() {
		return serviceConfig.getInteger("db.batch.size", DB_BATCH_SIZE_DEFAULT);
	}

	public static HashMap<String, OriginDto> getAllowedOrigins() {
		return allowedOrigins;
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
