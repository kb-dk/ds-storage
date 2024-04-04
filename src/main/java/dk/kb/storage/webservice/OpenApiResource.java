package dk.kb.storage.webservice;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import dk.kb.storage.config.ServiceConfig;
import dk.kb.util.Resolver;
import dk.kb.util.webservice.exception.NotFoundServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OpenApiResource {
    private static final Logger log = LoggerFactory.getLogger(OpenApiResource.class);


    public static final String APPLICATION_YAML = "application/yaml";

    /**
     * Pattern to allow search-replace for variabels defined as ${config.yaml.path} in OpenAPI specifications.
     * Everything after 'config.' is treated as a path to an entry in the backing configuration.
     */
    private static final Pattern CONFIG_REPLACEMENT= Pattern.compile("\\$\\{config:([^}]+)\\}");

    /**
     * Deliver the OpenAPI specification with substituted configuration values as a YAML file.
     */
    @GET
    @Produces(APPLICATION_YAML)
    @Path("/{path}.yaml")
    public Response getYamlSpec(@PathParam("path") String path) {
        try {
            path = new File(path).getName();
            String inputYaml = Resolver.readFileFromClasspath(path + ".yaml");

            if (inputYaml == null){
                throw new NotFoundServiceException("No OpenAPI specification with path '" + path + ".yaml' was found.");
            }

            String replacedText = replaceConfigPlaceholders(inputYaml);

            Response.ResponseBuilder builder = Response.ok(replacedText)
                    .header("Content-Disposition", "inline; filename=" + path + ".yaml");

            return builder.build();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Deliver the OpenAPI specification with substituted configuration values as a JSON file.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{path}.json")
    public Response getJsonSpec(@PathParam("path") String path){
        try {
            return createJson(path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Response createJson(String path) throws JsonProcessingException {
        try {
            path = new File(path).getName();
            String inputYaml = Resolver.readFileFromClasspath(path + ".yaml");
            String correctString = OpenApiResource.replaceConfigPlaceholders(inputYaml);

            String jsonString = getJsonString(correctString);

            Response.ResponseBuilder builder = Response.ok(jsonString).header("Content-Disposition", "inline; filename=" + path + ".json");
            return builder.build();
        } catch (YAMLException | IOException  | NullPointerException e){
            throw new NotFoundServiceException("No OpenAPI specification with path '" + path + ".yaml' was found.");
        }
    }

    /*@GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/openapi.json")
    public Response getJsonShorthand(){
        try {
            return createJson("ds-discover-openapi_v1");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }*/

    /**
     * Replace placeholders in the original OpenAPI YAML specification. These placeholders have the format ${config.yamlpath},
     * where the value inside the '{}' and after 'config.' is treated as a YAML path which is used to find the
     * replacement value in the backing configuration files.
     * @param originalApiSpec the content of the original YAML specification
     * @return an updated YAML string, where config placeholders have been replaced.
     */
    private static String replaceConfigPlaceholders(String originalApiSpec) {
        Matcher matcher = CONFIG_REPLACEMENT.matcher(originalApiSpec);

        StringBuilder replacedText = new StringBuilder();
        while (matcher.find()){
           String replacementPath = matcher.group(1);
           String replacement = getReplacementForMatch(replacementPath);
           matcher.appendReplacement(replacedText, replacement);
        }
        matcher.appendTail(replacedText);
        return replacedText.toString();
    }

    /**
     * Resolve the value for the given YAML path in the configuration files for the project.
     * @param yPath to extract value from.
     * @return the value at the given path in the configuration files.
     */
    private static String getReplacementForMatch(String yPath) {
        return ServiceConfig.getConfig().get(yPath).toString();
    }

    /**
     * Convert a YAML string to a JSON string
     * @param yamlString which is to be converted to JSON.
     * @return JSON representation of the input YAML.
     */
    private static String getJsonString(String yamlString) throws JsonProcessingException {
        Yaml yaml = new Yaml();
        Object yamlObject = yaml.load(yamlString);
        ObjectMapper jsonMapper = new ObjectMapper();
        return jsonMapper.enable(SerializationFeature.INDENT_OUTPUT).writeValueAsString(yamlObject);
    }
}



