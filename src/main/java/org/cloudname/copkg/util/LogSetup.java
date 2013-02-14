package org.cloudname.copkg.util;

import java.util.logging.Logger;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;
import java.util.logging.ConsoleHandler;

/**
 * Do log setup for command line utility.  Do NOT use this except in
 * the command line utility.
 *
 * @author borud
 */
public class LogSetup {

    private static class InteractiveFormatter extends SimpleFormatter {
        @Override
        public String format (LogRecord r) {
            String name = r.getLoggerName();
            if (name.startsWith("org.cloudname.copkg.")) {
                name = name.replaceFirst("org.cloudname.copkg.", "");
            }
            return "*** " + name + " : " + r.getMessage() + "\n";
        }
    }

    /**
     * Remove all loggers
     */
    public static void setup() {
        // First we remove all log handlers
        final Logger rootLogger = Logger.getLogger("");
        for (final Handler handler : rootLogger.getHandlers()) {
            rootLogger.removeHandler(handler);
        }

        // Then add back the console handler with our trivial formatter
        ConsoleHandler h = new ConsoleHandler();
        h.setFormatter(new InteractiveFormatter());
        rootLogger.addHandler(h);
    }
}