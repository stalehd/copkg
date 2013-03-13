package org.cloudname.fire;

import static com.google.common.base.Preconditions.checkNotNull;
import com.google.common.base.Objects;

/**
 * Class that represents the result of running a Job.
 *
 * @author borud
 */
public class Result {
    private final String stdout;
    private final String stderr;
    private final Status status;
    private final String message;
    private final int exitValue;

    public enum Status {
        SCRIPT_NOT_FOUND,
        SCRIPT_NOT_EXECUTABLE,
        OTHER,
    }

    /**
     * Result from running the process.
     *
     * @param stdout the output on stdout from the process
     * @param stderr the output on stderr from the process
     * @return exitValue the exit value of the process
     */
    public Result(final String stdout,
                  final String stderr,
                  final Status status,
                  final String message,
                  final int exitValue) {
        this.stdout = checkNotNull(stdout);
        this.stderr = checkNotNull(stderr);
        this.status = status;
        this.message = checkNotNull(message);
        this.exitValue = exitValue;
    }

    /**
     * Create an error result.
     *
     * @param status what kind of error we encountered
     * @param message a human consumable message explaining what went wrong
     * @return a Result with status and message set and everything else set to
     *   "neutral" values.
     */
    public static Result makeError(final Status status, final String message) {
        return new Result("", "", status, message, 0);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
            .add("stdout", stdout)
            .add("stderr", stderr)
            .add("status", status)
            .add("message", message)
            .add("exitValue", exitValue)
            .toString();
    }
}

