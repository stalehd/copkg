#!/usr/bin/python
import argparse
import os
import os.path
import sys
import logging
import subprocess
import time
import configs
import utils

def check_arguments(args):
    """
    Do a sanity check on the arguments.
    """

    if not args.working_dir:
        args.working_dir = os.getcwd()
        logging.warning('Using %s as working directory', args.working_dir)

    if not args.install_dir:
        args.install_dir = os.getcwd()
        logging.warning('Using %s as install directory', args.install_dir)

    if not os.path.isdir(args.working_dir):
        logging.error('Working directory (%s) not found.', args.working_dir)
        sys.exit(1)

    if not os.path.isdir(args.install_dir):
        logging.error('Install directory (%s) not found.', args.install_dir)
        sys.exit(1)

# ---------------------------------------------------------------------------


class IdeeLaunchConfig(configs.JavaLaunchConfig):

    def command(self):
        """
        Return the command required to start the service. Uses string expansion for the
        defaults.
        """
        return [
            self.java_command,
            '-jar', os.path.join(self.install_dir, 'bin/id-core-2.2.0-jar-with-dependencies.jar'),
            '--coordinate=1.idee.test.local',
            '--zookeeper=localhost:8100',
            '--logdir=',os.path.join(self.working_dir, 'logs/')
            ]

# ---------------------------------------------------------------------------

def get_config(working_dir, install_dir):
    """
    Get the custom configuration from the copkg internals; load it and run it
    """
    return IdeeLaunchConfig(working_dir=working_dir, install_dir=install_dir)


def check_dirs(working_dir):
    log_dir = os.path.join(args.working_dir, 'logs')
    if not os.path.isdir(log_dir):
        os.mkdir(log_dir)

    pid_dir = os.path.join(args.working_dir, 'run')
    if not os.path.isdir(pid_dir):
        os.mkdir(pid_dir)

def start_process(launch_config):
    """
    Start the process
    """
    check_dirs(launch_config.working_dir)

    logdir = os.path.join(launch_config.working_dir, 'logs/')
    stdout_file = os.path.join(logdir, 'stdout')
    stderr_file = os.path.join(logdir, 'stderr')

    logging.debug('stdout to %s, stderr to %s', stdout_file, stderr_file)

    launch_config.stdout = open(stdout_file, 'w')
    launch_config.stderr = open(stderr_file, 'w')

    try:
        launch_config.process = subprocess.Popen(launch_config.command(),
            stdout=launch_config.stdout, stderr=launch_config.stderr, stdin=None)

        logging.debug('New process id is %d', launch_config.process.pid)

    except Exception as ex:
        logging.error('Exception launching process: %s', ex)
        return False

    with open(utils.get_pidfile(launch_config.working_dir), 'w') as fh:
        fh.write(str(launch_config.process.pid))

    # Wait 1 second for program to launch. If it has terminated, return error
    # The most typical issue is misconfigured command line parameters; if
    # a required parameter is missing or there's some other issue it terminates
    # really fast.
    logging.debug('Waiting for process to start...')
    time.sleep(1)
    launch_config.process.poll()
    if launch_config.process.returncode:
        logging.error('Process (%s) terminated when launching with return code %d',
            launch_config.command(), launch_config.process.returncode)
        utils.dump_logfile(stderr_file)
        utils.dump_logfile(stdout_file)
        return False

    logging.debug('Process is running - return succees')
    return True

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

    # the script is running in the install directory; infer the path from the script
    args.install_dir = os.path.abspath(
        os.path.join(
            os.path.dirname(sys.argv[0]), '../'))

    if args.silent:
        logging.basicConfig(level=logging.WARNING)
    elif args.verbose:
        logging.basicConfig(level=logging.DEBUG)
    else:
        logging.basicConfig(level=logging.INFO)

    logging.debug('arguments are %s',args)
    logging.debug('install dir = %s', args.install_dir)

    #
    # Start launching the service
    #

    check_arguments(args)

    if (os.path.isfile(utils.get_pidfile(args.working_dir))):
        logging.warning('Found pidfile at %s, process is already running.', utils.get_pidfile(args.working_dir))
        sys.exit(1)

    logging.debug('chdir to working directory (%s)', args.working_dir)
    os.chdir(args.working_dir)

    launch_config = get_config(working_dir=args.working_dir, install_dir=args.install_dir)

    if not launch_config.check_dependencies():
        sys.exit(1)

    if not start_process(launch_config):
        sys.exit(2)
