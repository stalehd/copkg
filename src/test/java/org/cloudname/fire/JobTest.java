package org.cloudname.fire;

import java.util.Map;
import java.util.HashMap;

import org.junit.*;
import static org.junit.Assert.*;

/**
 * Unit tests for Job class.
 *
 * @author borud
 */
public class JobTest {
    private static final String serviceCoordinate = "1.service.user.dc";
    private static final String packageCoordinate = "com.example:artifact:2.2.1";
    private static final Map params = new HashMap() {{
        put("alpha-key", "alpha value");
        put("beta-key", "beta value");
        put("gamma-key", "gamma value");
        // option that takes parameter but is empty
        put("delta-key", "");
        // option that does not take parameter but is present
        put("9", null);
    }};

    /**
     * Just a very simple test that proves we can serialize and then
     * parse what we have just serialized.
     */
    @Test
    public void testJson() throws Exception {
        Job job = new Job(serviceCoordinate, packageCoordinate, params);
        Job job2 = Job.parse(job.toJson());

        assertNotNull(job2);

        // We are cheating a bit to not have to implement equals() :-P
        assertEquals(job.toJson(), job2.toJson());
    }

    /**
     * Make sure the keys are in the predicted order and everything is
     * formatted as expected.
     */
    @Test
    public void testParams() throws Exception {
        Job job = new Job(serviceCoordinate, packageCoordinate, params);
        assertEquals("--9 --alpha-key=\"alpha value\" --beta-key=\"beta value\" --delta-key=\"\" --gamma-key=\"gamma value\"",
                     job.paramsAsOptions());
    }
}
