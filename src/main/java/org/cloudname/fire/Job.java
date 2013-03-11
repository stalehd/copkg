package org.cloudname.fire;

import static com.google.common.base.Preconditions.checkNotNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.util.Map;
import java.util.TreeSet;
import java.util.Collections;
import java.io.IOException;

/**
 * Job description.  A job consists of:
 *
 * <ul>
 *   <li> service coordinate
 *   <li> package coordinate
 *   <li> a key/value map of parameters
 * </ul>
 *
 * To indicate that a parameter that takes no value is present a key
 * pointing to {@code null} is added to the map.
 *
 * TODO(borud): implement hashCode() and equals() if needed.
 *
 * @author borud
 */
public final class Job {
    private final String serviceCoordinate;
    private final String packageCoordinate;
    private final Map<String,String> params;

    /**
     * @param serviceCoordinate the service coordinate of the job
     * @param packageCoordinate the package coordinate of the job
     * @param map holding key-value pairs that represent the parameters of the job
     */
    @JsonCreator
    public Job(@JsonProperty("serviceCoordinate") String serviceCoordinate,
               @JsonProperty("packageCoordinate") String packageCoordinate,
               @JsonProperty("params") Map<String, String> params) {
        checkNotNull(serviceCoordinate);
        checkNotNull(packageCoordinate);
        checkNotNull(params);
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
        checkNotNull(json);
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

    /**
     * Get the parameters formatted as command line options.  In order
     * to get the parameters predictably, the parameters will be
     * sorted in lexical order.
     *
     * @return the parameters formatted as command line options.
     */
    public String paramsAsOptions() {
        StringBuilder buff = new StringBuilder();
        boolean first = true;

        for (String key : new TreeSet<String>(params.keySet())) {
            String value = params.get(key);
            buff.append((first?"--":" --"))
                .append(key);

            // A null value means that the flag was present but had no
            // options.
            if (value != null) {
                buff.append("=\"")
                    .append(value)
                    .append("\"");
            }

            first = false;
        }
        return buff.toString();
    }

    @Override
    public String toString() {
        return toJson();
    }
}
