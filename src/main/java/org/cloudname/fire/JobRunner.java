package org.cloudname.fire;

import static com.google.common.base.Preconditions.checkNotNull;

import org.cloudname.copkg.Configuration;
import org.cloudname.copkg.PackageCoordinate;
import org.cloudname.copkg.util.StreamConsumer;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
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
    public static final String STOP_SCRIPT = "stop.py";

    private final Configuration config;

    /**
     * @param config the copkg configuration.
     */
    public JobRunner(final Configuration config) {
        this.config = checkNotNull(config);
    }

    /**
     * The maximum number of bytes to read from the process before
     * bailing out.
     */
    private final int MAX_BYTES_TO_READ = 1024*64;
    /**
     * The maximum number of seconds to wait for the process to start.
     */
    private final int MAX_SECONDS_TO_WAIT = 180;

    /**
     * Run a Job.  The job is expected to just execute the script and
     * then terminate.  If this job hangs for an unacceptably long
     * time or it produces exorbitant amounts of output, we
     * unceremoniously terminate the process.
     *
     * @return a Result instance.
     */
    public Result runJob(final Job job, final String scriptFile) {
        File script = getScriptForJob(job, scriptFile);
        log.info("Script: " + script.getAbsolutePath());

        if (! script.exists()) {
            return Result.makeError(Result.Status.SCRIPT_NOT_FOUND,
                                    "Script does not exist: " + script.getAbsolutePath());
        }

        if (! script.canExecute()) {
            return Result.makeError(Result.Status.SCRIPT_NOT_EXECUTABLE,
                                    "Script is not executable: " + script.getAbsolutePath());
        }

        // Launch job. The first entry is the script name, the 2nd and 3rd element
        // is the working directory parameter
        final List<String> command = new ArrayList<>();
        command.add(script.getAbsolutePath());
        command.add("--working-directory");
        command.add(job.getRuntimeDirectory());

        command.addAll(job.getParams());

        // Finally, launch the startup script and capture all stdout and stderr
        // data in separate threads.

        final ExecutorService executor = Executors.newFixedThreadPool(3);
        try {
            final Process process = Runtime.getRuntime().exec(command.toArray(new String[command.size()]));

            final ByteArrayOutputStream stderrOutput = new ByteArrayOutputStream();
            final ByteArrayOutputStream stdoutOutput = new ByteArrayOutputStream();

            final StreamConsumer stderrConsumer = new StreamConsumer(process.getErrorStream(), stderrOutput, MAX_BYTES_TO_READ);
            final StreamConsumer stdoutConsumer = new StreamConsumer(process.getInputStream(), stdoutOutput, MAX_BYTES_TO_READ);
            final CountDownLatch completeLatch = new CountDownLatch(2);
            final AtomicBoolean hasOverflow = new AtomicBoolean(false);
            final StreamConsumer.Listener listener = new StreamConsumer.Listener() {
                @Override
                public void onNotify(StreamConsumer consumer, Status status) {
                    if (status == Status.DONE || status == Status.GOT_EXCEPTION) {
                        completeLatch.countDown();
                    }
                    if (status == Status.MAX_READ) {
                        hasOverflow.set(true);
                    }
                }
            };
            stderrConsumer.addListener(listener);
            stdoutConsumer.addListener(listener);

            executor.execute(stderrConsumer);
            executor.execute(stdoutConsumer);

            try {
                completeLatch.await(MAX_SECONDS_TO_WAIT, TimeUnit.SECONDS);
            } catch (InterruptedException ie) {
                throw new RuntimeException("Got InterruptedException waiting for process", ie);
            }

            if (hasOverflow.get()) {
                process.destroy();
                return Result.makeError(
                        Result.Status.OTHER,
                        "Process has dumped excessive amounts of data (> " + MAX_BYTES_TO_READ + " bytes) has been terminated ");
            }
            if (completeLatch.getCount() > 0) {
                // The process is still running - kill it.
                process.destroy();
                return Result.makeError(Result.Status.OTHER, "Process did not start in " + MAX_SECONDS_TO_WAIT + " seconds.");
            }

            if (process.exitValue() != 0) {
                return new Result(
                        stdoutOutput.toString("UTF-8"),
                        stderrOutput.toString("UTF-8"),
                        Result.Status.ERROR_CODE_RETURNED,
                        "Script executed but returned with error code",
                        process.exitValue());
            }
            return new Result(
                    stdoutOutput.toString("UTF-8"),
                    stderrOutput.toString("UTF-8"),
                    Result.Status.SUCCESS,
                    "Script executed",
                    process.exitValue());

        } catch (IOException ioe) {
            return Result.makeError(
                    Result.Status.OTHER,
                    "Got exception running the process: " + ioe.getMessage());
        } finally {
            executor.shutdown();
        }
    }

    /**
     * Figure out what the path of the start script is and return it.
     *
     * @return the File object pointing to the start script or
     *   {@code null} if it was not found.
     */
    public File getScriptForJob(final Job job, final String scriptName) {
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
                        + scriptName);
    }


}
