#!/usr/bin/python
import argparse
import os
import os.path
import logging
import signal
import sys
import time
import utils

def check_args(args):
    """
    Do a sanity check on the arguments.
    """

    if not args.working_dir:
        args.working_dir = os.getcwd()
        logging.warning('Using %s as working directory', args.working_dir)

    if not os.path.isdir(args.working_dir):
        logging.error('Working directory (%s) not found.', args.working_dir)
        sys.exit(1)


# ---------------------------------------------------------------------------
if __name__ == "__main__":

    #
    # Set up the arguments
    #
    parser = argparse.ArgumentParser(description="copkg start script")
    parser.add_argument('-w', '--working-directory',
        help='The working directory for the process', required=False)
    parser.add_argument('-v', '--verbose',
        help='Verbose output', required=False, action='store_true')
    parser.add_argument('-s', '--silent',
        help='Silent (mute everything except warnings and alerts',
            required=False, action='store_true')
    args = parser.parse_args()


    if args.silent:
        logging.basicConfig(level=logging.WARNING)
    elif args.verbose:
        logging.basicConfig(level=logging.DEBUG)
    else:
        logging.basicConfig(level=logging.INFO)

    logging.debug('arguments are %s',args)

    check_args(args)

    pidfile = utils.get_pidfile(args.working_dir)
    logging.debug('Pidfile is at %s', pidfile)

    if not os.path.isfile(pidfile):
        logging.warning('No running process (did not find a pidfile). Aborting.')
        sys.exit(1)

    pid = None
    with open(pidfile, 'r') as fh:
        lines = fh.readlines()
        if len(lines) == 0:
            logging.warning('pidfile is empty.')
            sys.exit(1)
        pid = int(lines[0])

    logging.debug('sending SIGTERM to pid %d', pid)

    # Ask the process to terminate politely. We could use psutil to check the
    # process list but that introduces external dependencies. And that sucks.
    try:
        os.kill(pid, signal.SIGTERM)
    except OSError as oe:
        logging.error('Got error trying to send SIGTERM to process. Is it running? (%s)', oe)
        sys.exit(2)

    seconds = 0
    while (seconds < 60):
        try:
            os.kill(pid, 0)
        except OSError as oe:
            # got exception - PID doesn't exist
            logging.debug('Got error trying to send signal to pid %d, assuming it has stopped: %s', pid, oe)
            os.unlink(pidfile)
            sys.exit(0)
        seconds = seconds + 1
        time.sleep(1)

    logging.warning('Process is still running after 60 seconds. Terminating it with SIGKILL')
    try:
        os.kill(pid, signal.SIGKILL)
    except OSError as oe:
        logging.warning('Got exception %s trying to terminate process')
        sys.exit(3)

    os.unlink(pidfile)
    sys.exit(0)
