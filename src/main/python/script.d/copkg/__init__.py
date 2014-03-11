import os
import time
import logging
import signal

def start_service(launcher):
    """
    Launch service. The service launch changes to the working directory and
    will launch the service in a new process. If it fails during start for
    some reason it will dump the output on stdout and stderr from the process
    in the log.

    Once the service has started it will wait for 2 seconds to see if it terminates
    if it does; dump output and return error.
    """
    try:
        launcher.make_log_dir()
        launcher.make_run_dir()

        os.chdir(launcher.get_working_dir())
        if not launcher.start():
            launcher.dump_stderr()
            launcher.dump_stdout()
            launcher.clean_up()
            return False

        # Wait 2 seconds for program to launch. If it has terminated, return error
        # The most typical issue is misconfigured command line parameters; if
        # a required parameter is missing or there's some other issue it terminates
        # really fast.
        time.sleep(2)

        if launcher.process_has_stopped():
            launcher.dump_stderr()
            launcher.dump_stdout()
            launcher.clean_up()
            logging.error('Process did NOT start')
            return False

        # All OK. Return true
        return True
    except Exception as ex:
        logging.error('Got error launching service: %s', ex)
        launcher.clean_up()
        return False


def stop_service(launcher):
    """
    Stop service. The first thing checked is the pidfile. If found, it will send a
    SIGTERM to the process and check that it terminates. If it doesn't terminate
    within 60 seconds a SIGKILL is sent to the process.
    """
    pid = None
    with open(launcher.get_pidfile(), 'r') as fh:
        lines = fh.readlines()
        if len(lines) == 0:
            logging.warning('pidfile is empty.')
            launcher.clean_up()
            return False
        pid = int(lines[0])

    logging.debug('sending SIGTERM to pid %d', pid)

    # Ask the process to terminate politely. We could use psutil to check the
    # process list but that introduces external dependencies. And that sucks.
    try:
        os.kill(pid, signal.SIGTERM)
    except OSError as oe:
        logging.error('Got error trying to send SIGTERM to process. Is it running? (%s)', oe)
        return False

    seconds = 0
    while (seconds < 60):
        try:
            os.kill(pid, 0)
        except OSError as oe:
            # got exception - PID doesn't exist
            logging.debug('Got error trying to send signal to pid %d, assuming it has stopped: %s', pid, oe)
            launcher.clean_up()
            return True
        seconds = seconds + 1
        time.sleep(1)

    logging.warning('Process is still running after 60 seconds. Terminating it with SIGKILL')
    try:
        os.kill(pid, signal.SIGKILL)
    except OSError as oe:
        logging.warning('Got exception %s trying to terminate process')
        return False

    os.unlink(pidfile)
    launcher.clean_up()
    return True
