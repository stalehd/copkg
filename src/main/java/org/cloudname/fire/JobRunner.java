package org.cloudname.fire;

import org.cloudname.copkg.Configuration;
import org.cloudname.copkg.PackageCoordinate;

import java.io.File;
import java.util.logging.Logger;

/**
 *
 * @author borud
 */
public final class JobRunner {
    private static final Logger log = Logger.getLogger(JobRunner.class.getName());

    public static final String SCRIPT_DIR = "script.d";
    public static final String START_SCRIPT = "start.py";

    private final Configuration config;

    /**
     * @param config the copkg configuration.
     */
    public JobRunner(final Configuration config) {
        this.config = config;
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

        return null;
    }

    /**
     * Figure out what the path of the start script is and return it.
     *
     * @return the File object pointing to the start script or
     *   {@code null} if it was not found.
     */
    public File startScriptForJob(final Job job) {
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
