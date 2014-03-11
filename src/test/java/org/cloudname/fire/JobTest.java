package org.cloudname.fire;

import com.google.common.testing.EqualsTester;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import org.junit.*;
import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.is;
/**
 * Unit tests for Job class.
 *
 * @author borud
 */
public class JobTest {
    private static final String runtimeDirectory = "/home/foo/bar";
    private static final String packageCoordinate = "com.example:artifact:2.2.1";
    private static final String[] PARAMS_AS_OPTION_ARRAY = {
            "--9",
            "--alpha-key=alpha value",
            "--beta-key=beta value",
            "--delta-key=",
            "--gamma-key=gamma value"
    };
    private static final List<String> params = Arrays.asList(PARAMS_AS_OPTION_ARRAY);


    /**
     * Just a very simple test that proves we can serialize and then
     * parse what we have just serialized.
     */
    @Test
    public void testJson() throws Exception {
        Job job = new Job(runtimeDirectory, packageCoordinate, params);
        Job job2 = Job.parse(job.toJson());

        assertNotNull(job2);

        // We are cheating a bit to not have to implement equals() :-P
        assertEquals(job.toJson(), job2.toJson());
    }

    @Test
    public void testParamArray() throws Exception {
        Job job = new Job(runtimeDirectory, packageCoordinate, params);

        assertThat(PARAMS_AS_OPTION_ARRAY, is(job.getOptionArray()));
    }

    @Test(expected = NullPointerException.class)
    public void testConstructNull() throws Exception {
        new Job(null, null, null);
    }

    @Test(expected = NullPointerException.class)
    public void testParseNull() throws Exception {
        Job.parse(null);
    }

    @Test
    public void testEquals() throws Exception {
        new EqualsTester()
            .addEqualityGroup(new Job("/the/working/directory/too", "group:artifact:1.2.3", params),
                              new Job("/the/working/directory/too", "group:artifact:1.2.3", params))
            .testEquals();


    }
}
