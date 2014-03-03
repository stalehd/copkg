package org.cloudname.fire;

import static com.google.common.base.Preconditions.checkNotNull;
import com.google.common.base.Objects;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.SerializationFeature;


import java.util.List;
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
 * @author borud
 */
public final class Job {
    // These are strings because this class is JSON serializable and
    // we need to do a bit of work before we can replace these with
    // the proper types.
    private final String runtimeDirectory;
    private final String packageCoordinate;
    private final List<String> params;

    /**
     * @param runtimeDirectory the service coordinate of the job
     * @param packageCoordinate the package coordinate of the job
     * @param params List of parameters to pass on to script
     */
    @JsonCreator
    public Job(@JsonProperty("runtimeDirectory") String runtimeDirectory,
               @JsonProperty("packageCoordinate") String packageCoordinate,
               @JsonProperty("params") List<String> params) {
        checkNotNull(runtimeDirectory);
        checkNotNull(packageCoordinate);
        checkNotNull(params);
        this.runtimeDirectory = runtimeDirectory;
        this.packageCoordinate = packageCoordinate;
        this.params = params;
    }

    public String getRuntimeDirectory() {
        return runtimeDirectory;
    }

    public String getPackageCoordinate() {
        return packageCoordinate;
    }

    public List<String> getParams() {
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
     * @return the parameters formatted as a command line option
     *   array.
     */
    @JsonIgnore // derived property
    public String[] getOptionArray() {
        return params.toArray(new String[] {});
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(runtimeDirectory, packageCoordinate, params);
    }

    @Override
    public boolean equals(final Object obj){
        if(! (obj instanceof Job)) {
            return false;
        }

        final Job other = (Job) obj;
        return Objects.equal(packageCoordinate, other.packageCoordinate)
            && Objects.equal(runtimeDirectory, other.runtimeDirectory)
            && Objects.equal(params, other.params);
    }

    @Override
    public String toString() {
        return Job.class.getName() + " " + toJson();
    }
}
