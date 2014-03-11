package org.cloudname.copkg;

import com.google.common.testing.EqualsTester;

import java.io.File;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for the Configuration class.
 *
 * @author borud
 */
public class ConfigurationTest {
    private static final String packageDir = "/some/root";
    private static final String downloadDir = packageDir + File.separatorChar + Configuration.DOWNLOAD_DIR;
    private static final String baseUrl = "http://localhost/packages";
    private static final String username = "foo";
    private static final String password = "bar";
    private static final String runtimeDir = "/var/for/runtime";

    private static final PackageCoordinate coordinate = new PackageCoordinate(
        "org.cloudname",
        "something",
        "1.2.3"
    );

    Configuration config = new Configuration(packageDir, baseUrl, username, password, runtimeDir);

    /**
     * Test that the paths turn out as expected.
     */
    @Test
    public void testPaths() throws Exception {
        assertEquals(packageDir, config.getPackageDir());
        assertEquals(downloadDir, config.getDownloadDir());
        assertEquals(baseUrl, config.getPackageBaseUrl());
    }

    /**
     * Verify the the download directory and file for a given
     * coordinate.
     */
    @Test
    public void testDownloadFilePath() throws Exception {
        final String expected = downloadDir
            + File.separatorChar
            + coordinate.getPathFragment()
            + File.separatorChar
            + coordinate.getFilename();

        assertEquals(expected, config.downloadFilenameForCoordinate(coordinate));
    }

    @Test
    public void testPackageDirectoryForCoordinate() throws Exception {
        final String expected = packageDir + File.separatorChar + coordinate.getPathFragment();
        assertEquals(expected, config.packageDirectoryForCoordinate(coordinate));
    }

    /**
     * Ensure that mapping to/from JSON works.
     */
    @Test
    public void testJson() throws Exception {
        String json = config.toJson();
        assertNotNull(json);

        Configuration parsedConfig = Configuration.fromJson(json);
        assertNotNull(parsedConfig);

        assertEquals(config, parsedConfig);

        // Test explicitly in case we have fucked up equals()
        assertEquals(config.getPackageDir(), parsedConfig.getPackageDir());
        assertEquals(config.getPackageBaseUrl(), parsedConfig.getPackageBaseUrl());

        // TODO(borud): add missing unit test for fromFile() method.
    }

    @Test
    public void testEquals() throws Exception {
        // I'm not sure of the quality of this test.  It should have a
        // negative as well.
        new EqualsTester()
            .addEqualityGroup(new Configuration(packageDir, baseUrl, username, password, runtimeDir),
                              new Configuration(packageDir, baseUrl, username, password, runtimeDir))
            .testEquals();
    }
}
