package org.cloudname.fire;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

import java.util.Map;
import java.util.HashMap;

/**
 * Main class for Fire.  Fire is a facility for starting services that
 * have been packaged using copkg.  You can run Fire on the command
 * line in order to Fire up (heh heh) services or you can run it as a
 * daemon with a REST API.
 *
 * TODO(borud): support starting and stopping packages on remote hosts
 *   (using REST API behind the scenes)
 *
 * @author borud
 */
public class Main {
    private static Pattern paramPattern = Pattern.compile("--([a-zA-Z0-9][a-zA-Z0-9-]*)(=(.*))?");

    public static void main(String[] args) {
        if (args.length < 1) {
            displayHelp();
            return;
        }

        Main m = new Main();

        if ("daemon".equals(args[0])) {
            m.doDaemon(args);
        }

        if ("start".equals(args[0])) {
            m.doStart(args);
        }

        if ("stop".equals(args[0])) {
            m.doStop(args);
        }
    }

    /**
     * Display command line help.  Does not exit like some might
     * expect.
     */
    public static void displayHelp() {
        System.out.println(
            "\n"
            + "  fire start <service> <package> [--param=\"...\" ...]\n"
            + "  fire stop <service>\n"
            + "  fire daemon\n"
        );
    }


    /**
     * Get parameters from array of strings.
     *
     * @param from the index from which we will start to parse command line options.
     * @param a the array of command line arguments
     */
    public static Map<String, String> getParams(int from, String[] a) {
        Map<String, String> map = new HashMap<String, String>();

        for (int i = from; i < a.length; i++) {
            Matcher m = paramPattern.matcher(a[i]);
            if (m.matches()) {
                String key = m.group(1);
                String val = m.group(3);

                // val is allowed to be null.  This indicates a param
                // with no args.J
                map.put(key, val);
            }
        }

        return map;
    }

    /**
     * Start Fire in daemon mode.  Starts up server and presents REST
     * API to the world.
     */
    public void doDaemon(String[] args) {
        throw new RuntimeException("daemon not implemented yet");
    }

    /**
     * Start package on local host.
     *
     */
    public void doStart(String[] args) {
        if (args.length < 3) {
            System.out.println("\nstart: too few arguments");
            displayHelp();
            return;
        }

        String serviceCoordinate = args[1];
        String packageCoordinate = args[2];

        Map<String, String> params = getParams(3, args);

        Job job = new Job(serviceCoordinate, packageCoordinate, params);
        System.out.println(job);
    }

    /**
     * Stop package
     */
    public void doStop(String[] args) {
        if (args.length < 2) {
            System.out.println("\nstop: too few arguments");
            displayHelp();
            return;
        }

        String coordinate = args[1];
    }

}
