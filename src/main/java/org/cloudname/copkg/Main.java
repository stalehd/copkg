package org.cloudname.copkg;

import org.cloudname.fire.Job;
import org.cloudname.fire.JobRunner;
import org.cloudname.fire.Result;

import org.cloudname.copkg.util.LogSetup;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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
    public static final String COPKG_RUNTIME_DIR = "runtime";
    public static final String COPKG_DEFAULT_PACKAGE_URL = "http://packages.skunk-works.no/copkg";

    // Command line options
    private static OptionParser optionParser = new OptionParser();

    private static OptionSpec<String> repository =
        optionParser.accepts("repository").withRequiredArg().ofType(String.class);

    private static OptionSpec<String> packageDir =
        optionParser.accepts("package-dir").withRequiredArg().ofType(String.class);

    private static OptionSpec<String> username =
        optionParser.accepts("username").withRequiredArg().ofType(String.class);

    private static OptionSpec<String> password =
        optionParser.accepts("password").withRequiredArg().ofType(String.class);

    private static OptionSpec<String> runtimeBaseDir =
        optionParser.accepts("runtime-base-dir").withRequiredArg().ofType(String.class);

    private static OptionSpec<Void> help = optionParser.accepts("help").forHelp();

    private static OptionSet optionSet;

    private Manager manager;
    private Configuration config;

    private final List<String> scriptParameters;

    /**
     * Set up Main with appropriate configuration.
     */
    public Main(Configuration config, final List<String> scriptParameters) {
        this.config = config;
        manager = new Manager(config);
        this.scriptParameters = scriptParameters;
    }

    /**
     * Extract command and service arguments into two separate arrays.
     */
    private static void splitParameters(final String[] args,
                                        final List<String> commandParameters,
                                        final List<String> serviceParameters) {
        boolean otherParams = false;
        for (final String parameter : args) {
            if ("--".equals(parameter)) {
                otherParams = true;
                continue;
            }
            if (otherParams) {
                serviceParameters.add(parameter);
                continue;
            }
            commandParameters.add(parameter);
        }
    }

    /**
     * Main entry point.
     */
    public static void main(String[] args) throws Exception {
        // Strip off the service's arguments. This is separated by a double dash and
        // everything after the double dash will be passed on to the service as parameters
        final List<String> scriptParameters = new ArrayList<>();
        final List<String> commandParameters = new ArrayList<>();
        splitParameters(args, commandParameters, scriptParameters);

        optionSet = optionParser.parse(commandParameters.toArray(new String[]{}));

        // if --help then bail at once
        if (optionSet.has(help)) {
            printHelp();
            return;
        }

        // Make logging usable for interactive users
        LogSetup.setup();

        final Configuration config = makeOrFindConfiguration();

        if (!doSanityCheckOnConfig(config)) {
            return;
        }
        final Main m = new Main(config, scriptParameters);

        final List<String> parameters = optionSet.nonOptionArguments();
        if (parameters.size() == 0) {
            // No commands and no parameters -- print help
            printHelp();
            return;
        }

        // Pop off the command, package name and runtimeId from the arguments.
        // The order will always be the same.
        final String command = parameters.get(0);
        final String packageName = parameters.size() > 1 ? parameters.get(1) : null;
        final String runtimeId = parameters.size() > 2 ? parameters.get(2) : null;
        m.dispatch(command, packageName, runtimeId);
    }

    /**
     * The configuration object *might* have null values if the configuration file
     * is out of date and we'd rather have error messages explaining the missing
     * parameter than a nondescript NPE thrown somewhere in the code. Whenever a
     * new parameter is added these checks must be updated.
     */
    private static boolean doSanityCheckOnConfig(final Configuration config) {
        if (config.getUsername() == null) {
            System.err.println("Missing user name in configuration.");
            return false;
        }
        if (config.getPassword() == null) {
            System.err.println("Missing password in configuration.");
            return false;
        }
        if (config.getPackageBaseUrl() == null) {
            System.err.println("Missing package base URL in configuration.");
            return false;
        }
        if (config.getPackageDir() == null) {
            System.err.println("Missing package directory in configuration.");
            return false;
        }
        if (config.getDownloadDir() == null) {
            System.err.println("Missing download directory in configuration.");
            return false;
        }
        if (config.getRuntimeBaseDir() == null) {
            System.err.println("Missing runtime base directory in configuration.");
            return false;
        }
        return true;
    }

    /**
     * We could have used the built-in help display in jopt-simple,
     * but this looks a bit clearer.
     */
    private static void printHelp() {
        System.out.println(
            "\n"
            + "Flags:\n"
            + "----------------------------------------------------------------------------------------------------\n"
            + "    --package-dir=<dir>       : where to install packages\n"
            + "    --repository=<dir>        : where to fetch packages from\n"
            + "    --runtime-base-dir=<dir>  : where to to keep state when daemon\n"
            + "    --username=<username>     : username used for BASIC auth at repository\n"
            + "    --password=<password>     : password used for BASUC auth at repository\n"
            + "\n"
            + "Package commands:\n"
            + "----------------------------------------------------------------------------------------------------\n"
            + "  copkg [flags] install <package coordinate>    : install the package\n"
            + "  copkg [flags] uninstall <package coordinate>  : uninstall the package\n"
            + "  copkg [flags] resolve <package coordinate>    : print paths and URLs for a given package coordinate\n"
            + "\n"
            + "Service lifecycle management:\n"
            + "----------------------------------------------------------------------------------------------------\n"
            + "  copkg [flags] start <package coordinate> <id> -- [start params]\n"
            + "      : Start service with package coordinate with a given runtime id.  Parameters\n"
            + "        to service are added after the \"--\" marker\n"
            + "\n"
            + "  copkg [flags] stop <package coordinate> <id>\n"
            + "      : Stop service with a given runtime id.\n"
            + "\n"
        );
    }

    /**
     * Dispatch commands.
     */
    private void dispatch(final String command, final String packageName, final String runtimeId) throws Exception {

        if ("install".equals(command)) {
            if (packageName == null) {
                System.err.println("\ninstall error: expected package coordinate as argument");
                return;
            }
            install(packageName);
            return;
        }

        if ("uninstall".equals(command)) {
            if (packageName == null) {
                System.err.println("\nuninstall error: expected package coordinate as argument");
                return;
            }

            uninstall(packageName);
            return;
        }

        if ("resolve".equals(command)) {
            if (packageName == null) {
                System.err.println("\nresolve error: expected coordinate as argument");
                return;
            }
            resolve(packageName);
            return;
        }

        if ("start".equals(command)) {
            if (packageName == null || runtimeId == null) {
                System.err.println("\nstart: expected package coordinate and runtime id");
                return;
            }

            start(packageName, runtimeId);
            return;
        }

        if ("stop".equals(command)) {
            if (packageName == null || runtimeId == null) {
                System.err.println("\nstop: expected package coordinate and runtime id");
                return;
            }

            stop(packageName, runtimeId);
            return;
        }

        if ("status".equals(command)) {
            System.out.println("\nNot implemented yet: " + command);
            printHelp();
            return;
        }

        if ("daemon".equals(command)) {
            System.out.println("\nNot implemented yet: " + command);
            printHelp();
            return;
        }

        System.err.println("\nUnknown command: " + command);

        printHelp();
    }

    /**
     * Stop the service.
     */
    private void stop(final String packageCoordinate, final String runtimeId) {
        final File rtDir = new File(config.getRuntimeBaseDir() + File.pathSeparator + runtimeId);
        if (!rtDir.isDirectory()) {
            if (!rtDir.mkdir()) {
                System.err.println("\nCould create runtime directory " + rtDir.getAbsolutePath() + ".");
                return;
            }
        }

        final Job job = new Job(
                rtDir.getAbsolutePath(),
                packageCoordinate,
                scriptParameters);

        // Run the job!
        final Result result = new JobRunner(config).runJob(job, JobRunner.STOP_SCRIPT);
        System.out.println(result.toString());
    }
    /**
     * Start the service.
     */
    private void start(final String packageCoordinate, final String runtimeId) {
        final File rtDir = new File(config.getRuntimeBaseDir() + File.pathSeparator + runtimeId);
        if (!rtDir.isDirectory()) {
            // Create the runtime directory if it doesn't exist
            if (!rtDir.mkdir()) {
                System.err.println("\nCould not create runtime directory " + rtDir.getAbsolutePath() + ".");
                return;
            }
        }

        final Job job = new Job(
                rtDir.getAbsolutePath(),
                packageCoordinate,
                scriptParameters);

        // Run the job!
        final Result result = new JobRunner(config).runJob(job, JobRunner.START_SCRIPT);
        System.out.println(result.toString());
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
    private void uninstall(String coordinateString) throws Exception {
        PackageCoordinate coordinate = PackageCoordinate.parse(coordinateString);
        manager.uninstall(coordinate);
    }

    /**
     * Given a coordinate, output the install path, the download path
     * etc given the current configuration.
     */
    private void resolve(String coordinateString) {
        PackageCoordinate coordinate = PackageCoordinate.parse(coordinateString);
        System.out.println("");
        System.out.println("installDir       = "
                + config.getPackageDir() + File.separatorChar + coordinate.getPathFragment());
        System.out.println("downloadUrl      = " + coordinate.toUrl(config.getPackageBaseUrl()));
        System.out.println("downloadFilename = " + config.downloadFilenameForCoordinate(coordinate));
        System.out.println("");
    }

    /**
     * Find configuration or make an appropriate default
     * configuration.  Will look in /etc and the user's home directory
     * as well as take command line options into consideration.
     *
     */
    private static Configuration makeOrFindConfiguration() throws IOException {
        final String defaultInstallDir
                = System.getProperty("user.home") + File.separatorChar + COPKG_HOME_PACKAGE_DIR;
        final String defaultReposUrl = COPKG_DEFAULT_PACKAGE_URL;
        final String defaultUserName = "";
        final String defaultPassword = "";
        final String defaultRuntimeBaseDir
                = System.getProperty("user.home") + File.separatorChar + COPKG_RUNTIME_DIR;

        Configuration c = new Configuration(
                defaultInstallDir,
                defaultReposUrl,
                defaultUserName,
                defaultPassword,
                defaultRuntimeBaseDir);

        final File etc = new File("/etc/" + COPKG_ETC_DIR + "/" + COPKG_CONFIG_FILE);
        if (etc.exists()) {
            log.fine("Getting configuration from " + etc.getAbsolutePath());
            c = Configuration.fromFile(etc);
        }

        final File home = new File(
            System.getProperty("user.home")
            + File.separatorChar
            + COPKG_USER_DIR
            + File.separatorChar
            + COPKG_CONFIG_FILE);
        if (home.exists()) {
            log.fine("Getting configuration from " + home.getAbsolutePath());
            c = Configuration.fromFile(home);
        }

        // The following is not the most elegant code, but it was
        // quick to write.  The Configuration class should have a
        // cascade/merge method instead.

        if (optionSet.has(packageDir)) {
            c = new Configuration(optionSet.valueOf(packageDir),
                                  c.getPackageBaseUrl(),
                                  c.getUsername(),
                                  c.getPassword(),
                                  c.getRuntimeBaseDir());
        }

        if (optionSet.has(repository)) {
            c = new Configuration(c.getPackageDir(),
                                  optionSet.valueOf(repository),
                                  c.getUsername(),
                                  c.getPassword(),
                                  c.getRuntimeBaseDir());
        }

        if (optionSet.has(username)) {
            c = new Configuration(c.getPackageDir(),
                                  c.getPackageBaseUrl(),
                                  optionSet.valueOf(username),
                                  c.getPassword(),
                                  c.getRuntimeBaseDir());
        }

        if (optionSet.has(password)) {
            c = new Configuration(c.getPackageDir(),
                                  c.getPackageBaseUrl(),
                                  c.getUsername(),
                                  optionSet.valueOf(password),
                                  c.getRuntimeBaseDir());
        }

        if (optionSet.has(runtimeBaseDir)) {
            c = new Configuration(c.getPackageDir(),
                    c.getPackageBaseUrl(),
                    c.getUsername(),
                    c.getPassword(),
                    optionSet.valueOf(runtimeBaseDir));
        }
        return c;
    }
}
