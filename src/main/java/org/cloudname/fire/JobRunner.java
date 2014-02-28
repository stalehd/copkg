package org.cloudname.fire;

import static com.google.common.base.Preconditions.checkNotNull;

import org.cloudname.copkg.Configuration;
import org.cloudname.copkg.PackageCoordinate;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

/**
 * This class takes care of running a single job which should
 * terminate within a reasonable amount of time.  It is not designed
 * to support long-running jobs.
 *
 * @author borud
 */
public final class JobRunner {
    private static final Logger log = Logger.getLogger(JobRunner.class.getName());

    public static final String SCRIPT_DIR = "script.d";
    public static final String START_SCRIPT = "start.py";
    public static final String STATUS_SCRIPT = "status.py";

    private final Configuration config;

    /**
     * @param config the copkg configuration.
     */
    public JobRunner(final Configuration config) {
        this.config = checkNotNull(config);
    }

    private Runnable createStreamReader(final InputStream inputStream, final List<String> output) {
        return new Runnable() {
            @Override
            public void run() {
                try {
                    final LineNumberReader reader= new LineNumberReader(new InputStreamReader(inputStream));
                    String line = reader.readLine();
                    while (line != null) {
                        output.add(line);
                        line = reader.readLine();
                    }
                    reader.close();
                } catch (IOException ioe) {
                    output.add(ioe.getMessage());
                }
            }
        };
    }
    /**
     * Run a Job.  The job is expected to just start the service and
     * then terminate.  If this job hangs for an unacceptably long
     * time or it produces exorbitant amounts of output, we
     * unceremoniously terminate the process.
     *
     * @return a Result instance.
     */
    public Result runJob(final Job job) {
        File startScript = startScriptForJob(job);
        log.info("start script: " + startScript.getAbsolutePath());

        if (! startScript.exists()) {
            return Result.makeError(Result.Status.SCRIPT_NOT_FOUND,
                                    "Script does not exist: " + startScript.getAbsolutePath());
        }

        if (! startScript.canExecute()) {
            return Result.makeError(Result.Status.SCRIPT_NOT_EXECUTABLE,
                                    "Script is not executable: " + startScript.getAbsolutePath());
        }

        // Launch job. The first entry is the script name, the 2nd and 3rd element
        // is the working directory parameter
        final List<String> command = new ArrayList<>();
        command.add(startScript.getAbsolutePath());
        command.add("--working-directory");
        command.add(job.getRuntimeDirectory());

        // Add the rest of the parameters
        for (final Map.Entry<String, String> param : job.getParams().entrySet()) {
            command.add(param.getKey());
            command.add(param.getValue());
        }

        // Finally, launch the startup script and capture all stdout and stderr
        // data in separate threads.
        try {
            final List<String> stderr = new ArrayList<>();
            final List<String> stdout = new ArrayList<>();
            final Process process = Runtime.getRuntime().exec(command.toArray(new String[command.size()]));

            final ExecutorService outputReader = Executors.newFixedThreadPool(3);
            outputReader.execute(createStreamReader(process.getInputStream(), stdout));
            outputReader.execute(createStreamReader(process.getErrorStream(), stderr));
            final AtomicBoolean killedProcess = new AtomicBoolean(false);
            // Create a thread to kill off the process if it won't start within 180 seconds
            outputReader.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(180000L);
                    } catch (InterruptedException ie) {
                        // Just ignore it; nothing to do.
                        return;
                    }
                    // Check if the process has completed
                    try {
                        process.exitValue();
                    } catch (IllegalThreadStateException ise) {
                        System.out.println("Won't wait for the process any longer, killing it.");
                        process.destroy();
                        killedProcess.set(true);
                    }
                }
            });
            try {
                process.waitFor();
            } catch (InterruptedException ie) {
                return Result.makeError(
                        Result.Status.OTHER,
                        "Got exception when waiting for the process to end: " + ie.getMessage());
            }
            outputReader.shutdownNow();

            if (killedProcess.get()) {
                return Result.makeError(Result.Status.OTHER, "Process timed out.");
            }
            // Return regardless of the exit value; if the exit value is other than 1 it'll
            // say so on the command line.
            return new Result(
                    joinLines(stdout),
                    joinLines(stderr),
                    Result.Status.SUCCESS,
                    "Script executed",
                    process.exitValue());

        } catch (IOException ioe) {
            return Result.makeError(
                    Result.Status.OTHER,
                    "Got exception running the process: " + ioe.getMessage());
        }
    }

    private String joinLines(final List<String> lines) {
        final StringBuilder sb = new StringBuilder();
        for (final String str : lines) {
            sb.append(str).append("\n");
        }
        return sb.toString();
    }

    /**
     * Figure out what the path of the start script is and return it.
     *
     * @return the File object pointing to the start script or
     *   {@code null} if it was not found.
     */
    public File startScriptForJob(final Job job) {
        checkNotNull(job);
        // There is a good chance you came to this method because you
        // wanted to add support for more script types.  And now you
        // are happy because you realized that I made this easy for
        // you.  This is the point where I tell you to reconsider.
        // Whatever you add support for, you are going to have to live
        // with for a very long time.  And if it isn't portable across
        // platforms you just became part of the problem.  Don't be
        // part of the problem.
        final PackageCoordinate packageCoordinate = PackageCoordinate.parse(job.getPackageCoordinate());
        return new File(
            config.packageDirectoryForCoordinate(packageCoordinate)
            + File.separatorChar
            + SCRIPT_DIR
            + File.separatorChar
            + START_SCRIPT);
    }
}
