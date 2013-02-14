package org.cloudname.copkg;

import org.cloudname.copkg.util.LogSetup;

import org.cloudname.flags.Flag;
import org.cloudname.flags.Flags;

import java.io.File;
import java.io.IOException;

import java.util.logging.Logger;

/**
 * Main class for the copkg package manager command line utility.
 *
 * This is a command line utility so throwing exceptions willy-nilly
 * is just fine.
 *
 * @author borud
 */
public class Main {
    private static final Logger log = Logger.getLogger(Main.class.getName());

    public static final String COPKG_CONFIG_FILE = "config.json";
    public static final String COPKG_USER_DIR = ".copkg";
    public static final String COPKG_ETC_DIR = "copkg";

    // Last ditch defaults
    public static final String COPKG_HOME_PACKAGE_DIR = "packages";
    public static final String COPKG_DEFAULT_PACKAGE_URL = "http://packages.skunk-works.no/copkg";

    // Flags
    @Flag(name = "repos", description = "Package Repository URL")
    public static String optRepos = null;

    @Flag(name = "dir", description = "Package install directory")
    public static String optDir = null;

    @Flag(name = "install", description = "Install package given by coordinate")
    public static String optInstall = null;

    @Flag(name = "uninstall", description = "Uninstall package given by coordinate")
    public static String optUninstall = null;

    @Flag(name = "list", description="List installed package coordinates")
    public static boolean optList = false;

    @Flag(name = "resolve", description = "Resolve coordinate into path and URL")
    public static String optResolve = null;

    private static Flags flags;

    private Manager manager;
    private Configuration config;

    /**
     * Set up Main with appropriate configuration.
     */
    public Main(Configuration config) {
        this.config = config;
        manager = new Manager(config);
    }

    /**
     * Main entry point.
     */
    public static void main(String[] args) throws Exception {
        // Parse flags
        flags = new Flags()
            .loadOpts(Main.class)
            .parse(args);

        if (flags.helpFlagged()) {
            flags.printHelp(System.out);
            return;
        }

        // Make logging usable for interactive users
        LogSetup.setup();

        // TODO(borud): add explicit overrides to config so that we
        // can specify config on the command line which will override
        // all other config.
        Configuration config = makeOrFindConfiguration();

        Main m = new Main(config);
        m.dispatch();
    }

    /**
     * Dispatch commands
     */
    private void dispatch() throws Exception {

        if (optInstall != null) {
            install(optInstall);
            return;
        }

        if (optUninstall != null) {
            uninstall(optUninstall);
            return;
        }


        if (optList) {
            list();
            return;
        }

        if (optResolve != null) {
            resolve(optResolve);
            return;
        }

        flags.printHelp(System.out);
    }


    /**
     * Install package.
     *
     * @param coordinateString the coordinate string of the package we
     *   wish to install.
     */
    private void install(String coordinateString) throws Exception {
        PackageCoordinate coordinate = PackageCoordinate.parse(coordinateString);
        manager.install(coordinate);
    }

    /**
     * Uninstall package.
     *
     * @param coordinateString the coordinate string of the package we
     *   wish to uninstall.
     */
    private void uninstall(String coordinateString) {
        // TODO(borud): implement
        log.warning("Uninstall not implemented yet");
    }

    /**
     * List installed packages.  Lists package coordinates.
     */
    private void list() {
        // TODO(borud): implement
        log.warning("List not implemented yet");
    }

    /**
     * Given a coordinate, output the install path, the download path
     * etc given the current configuration.
     */
    private void resolve(String coordinateString) {
        PackageCoordinate coordinate = PackageCoordinate.parse(coordinateString);
        System.out.println("installDir = " + config.getPackageDir() + File.separatorChar + coordinate.getPathFragment());
        System.out.println("downloadFilename = " + config.downloadFilenameForCoordinate(coordinate));
        System.out.println("downloadUrl = " + coordinate.toUrl(config.getPackageBaseUrl()));
    }

    /**
     * Find configuration or make an appropriate default
     * configuration.  Will look in /etc and the user's home directory
     * as well as take command line options into consideration.
     *
     */
    private static Configuration makeOrFindConfiguration() throws IOException {
        String installDir = System.getProperty("user.home") + File.separatorChar + COPKG_HOME_PACKAGE_DIR;
        String reposUrl = COPKG_DEFAULT_PACKAGE_URL;
        Configuration c = new Configuration(installDir, reposUrl);

        File etc = new File("/etc/" + COPKG_ETC_DIR + "/" + COPKG_CONFIG_FILE);
        if (etc.exists()) {
            log.info("Getting configuration from " + etc.getAbsolutePath());
            c = Configuration.fromFile(etc);
        }

        File home = new File(
            System.getProperty("user.home")
            + File.separatorChar
            + COPKG_USER_DIR
            + File.separatorChar
            + COPKG_CONFIG_FILE);
        if (home.exists()) {
            log.info("Getting configuration from " + home.getAbsolutePath());
            c = Configuration.fromFile(home);
        }

        if (optDir != null) {
            c = new Configuration(optDir, c.getPackageBaseUrl());
            log.info("Set package dir to " + optDir);
        }

        if (optRepos != null) {
            c = new Configuration(c.getPackageDir(), optRepos);
            log.info("Set repos URL to " + optRepos);
        }


        return c;
    }
}
