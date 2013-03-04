package org.cloudname.fire;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.util.Map;
import java.io.IOException;

/**
 * Job description.
 *
 * @author borud
 */
public class Job {
    private String serviceCoordinate;
    private String packageCoordinate;
    private Map<String,String> params;

    /**
     * @param serviceCoordinate the service coordinate of the job
     * @param packageCoordinate the package coordinate of the job
     * @param map holding key-value pairs that represent the parameters of the job
     */
    @JsonCreator
    public Job(@JsonProperty("serviceCoordinate") String serviceCoordinate,
               @JsonProperty("packageCoordinate") String packageCoordinate,
               @JsonProperty("params") Map<String, String> params) {
        this.serviceCoordinate = serviceCoordinate;
        this.packageCoordinate = packageCoordinate;
        this.params = params;
    }

    public String getServiceCoordinate() {
        return serviceCoordinate;
    }

    public String getPackageCoordinate() {
        return packageCoordinate;
    }

    public Map<String, String> getParams() {
        return params;
    }

    /**
     * Parse a JSON blob and return a Job instance.
     * @return a Job instance.
     */
    public static Job parse(String json) throws IOException {
        return new ObjectMapper().readValue(json, Job.class);
    }

    /**
     * Transform this instance to a JSON blob.
     */
    public String toJson() {
        try {
            final ObjectMapper mapper = new ObjectMapper();
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
            return mapper.writeValueAsString(this);
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public String toString() {
        return toJson();
    }
}
