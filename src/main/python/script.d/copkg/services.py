import argparse
import glob
import logging
import os
import os.path
import subprocess
import sys
import time

class BaseService:
    """
    The base service. Implement a new class inheriting from this
    class to support additional service types.
    """

    def check_dependencies(self, install_dir):
        """
        Check basic dependencies if required. Return True if everything
        seems OK, False otherwise.
        """
        return False

    def get_command(self, working_dir, additional_args):
        """
        Get the command as an array, first element is the executable,
        the parameters and arguments as the following elements. The addditional
        arguments are parameters from the command line that should be passed
        on to the executable.
        """
        return []

class JavaService(BaseService):
    """
    A java service implementation. Checks for JAVA_HOME, locates the jar file to use
    in the install dir and launches the process.
    """

    stdout = None
    stdin = None
    java_executable = 'java'
    java_command = java_executable
    jar_file = None

    def __locate_java_executable_(self):
        """
        Try to locate the Java executable in the path.
        """
        # Note: This won't work for Windows installations but I can live with that.
        for path in os.environ["PATH"].split(":"):
            filename = os.path.join(path, self.java_executable)
            if os.path.exists(filename):
                self.java_command = filename
                logging.debug('Found java executable at %s', self.java_command)
                return True
        return False

    def check_dependencies(self, install_dir):
        """
        Check basic software dependencies before launching the service. If this method
        returns False the start operation will fail.
        """
        if not os.environ['JAVA_HOME']:
            logging.warning('JAVA_HOME isn''t set. The system might not have a working JVM installed.')
            if not self.__locate_java_executable_():
                logging.error('Could not locate a java executable somewhere on the path.')
                return False
            logging.debug('Java executable is %s', self.java_command)
        else:
            logging.debug('JAVA_HOME is set to %s', os.environ['JAVA_HOME'])
            self.java_command = os.path.join(os.environ['JAVA_HOME'], 'bin/java')
            if not os.path.isfile(self.java_command):
                logging.error('Could not find java command at %s', self.java_command)
                return False

        # locate the jar file
        fullpath = os.path.join(install_dir, 'bin/*-jar-with-dependencies.jar')
        candidates = glob.glob(fullpath)
        if len(candidates) < 1:
            logging.error('Could not find a suitable jar file to use anywhere in  %s/bin',self.working_dir)
            return False

        self.jar_file = candidates[0]
        logging.debug('Using jar file: %s', self.jar_file)
        return True

    def get_command(self, working_dir, additional_args):
        """
        Return the command to launch the service (java -jar <jar file> <parameters>)
        """
        ret = [ self.java_command , '-jar', self.jar_file ]
        if len(additional_args) > 0:
            ret.extend(additional_args)
        return ret



class ServiceLauncher:
    """
    Service launcher class; does most of the glue work. Normally you don't have
    to modify this class; just implement a suitable BaseService implementation.
    """
    parser = None
    args = None
    service = None
    stdout = None
    stderr = None
    process = None

    def __init__(self, service):
        """
        Initialize; parameters:
          - service: the service class to use when launching
        """
        self.service = service

    def __configure_logging_(self):
        """
        Configure logging with appropriate format and level
        """
        logformat='%(levelname)-8s %(message)s'
        if self.args.silent:
            logging.basicConfig(level=logging.WARNING, format=logformat)
        elif self.args.verbose:
            logging.basicConfig(level=logging.DEBUG, format=logformat)
        else:
            logging.basicConfig(level=logging.INFO, format=logformat)
        logging.debug('arguments are %s', self.args)


    def create_default_arguments(self):
        """
        Set up the default arguments for the script. There's basically
        three arguments needed: Working directory (install dir is inferred by
        the location of the script) and verbosity.
        """
        self.parser = argparse.ArgumentParser(description="copkg start script")
        self.parser.add_argument('-w', '--working-directory',
            help='The working directory for the process', required=False)
        self.parser.add_argument('-v', '--verbose',
            help='Verbose output', required=False, action='store_true')
        self.parser.add_argument('-s', '--silent',
            help='Silent (mute everything except warnings and alerts',
                required=False, action='store_true')
        (self.args, self.args.additional_args) = self.parser.parse_known_args()

        self.__configure_logging_()

        #
        # Do a sanity check on the arguments.
        #
        if not 'working_dir' in self.args:
            self.args.working_dir = os.getcwd()
            logging.warning('Using %s as working directory', self.args.working_dir)

        if not 'install_dir' in self.args:
            self.args.install_dir = os.getcwd()
            logging.warning('Using %s as install directory', self.args.install_dir)

        if not os.path.isdir(self.args.working_dir):
            logging.error('Working directory (%s) not found.', self.args.working_dir)
            return False

        if not os.path.isdir(self.args.install_dir):
            logging.error('Install directory (%s) not found.', self.args.install_dir)
            return False

        self.args.install_dir = os.path.abspath(
            os.path.join(
                os.path.dirname(sys.argv[0]), '../'))

        self.args.run_dir = os.path.join(self.args.working_dir, 'run/')
        self.args.log_dir = os.path.join(self.args.working_dir, 'logs/')
        self.args.stderr_file = os.path.join(self.args.working_dir, 'logs/stderr')
        self.args.stdout_file = os.path.join(self.args.working_dir, 'logs/stdout')
        self.args.pid_file = os.path.join(self.args.working_dir, 'run/process.pid')

        return True


    def pidfile_exists(self):
        """
        Check if the pidfile exists. Returns True if it does.
        """
        if os.path.isfile(self.get_pidfile()):
            return True
        return False

    def check_dependencies(self):
        """
        Do a dependency check.
        """
        return self.service.check_dependencies(self.args.install_dir)


    def get_pidfile(self):
        """
        Get the name of the pidfile
        """
        return self.args.pid_file

    def clean_up(self):
        """
        Clean up files if the service can't start; typically just the pidfile.
        """
        if self.pidfile_exists():
            os.unlink(self.get_pidfile())

    def make_log_dir(self):
        """
        Create log directory in the runtime directory
        """
        if not os.path.isdir(self.args.log_dir):
            os.mkdir(self.args.log_dir)

    def make_run_dir(self):
        """
        Create the run/ directory in the runtime directory
        """
        if not os.path.isdir(self.args.run_dir):
            os.mkdir(self.args.run_dir)

    def get_working_dir(self):
        """
        Return the working directory; where the service will run.
        """
        return self.args.working_dir

    def __dump_logfile_(self, filename):
        """
        Dump a file to the log.
        """
        logging.debug('Dumping %s from process', filename)
        if os.file.isfile(filename):
            with open(filename, 'r') as fh:
                for line in fh:
                    logging.info('<%s>: %s', os.path.basename(filename), line.rstrip())


    def dump_stderr(self):
        """
        Dump stderr from the process. (if it exists)
        """
        self.__dump_logfile_(self.args.stderr_file)

    def dump_stdout(self):
        """
        Dump stdout from the process. (if it exists)
        """
        self.__dump_logfile_(self.args.stdout_file)

    def start(self):
        """
        Start the service.
        """
        logging.debug('stdout to %s, stderr to %s', self.args.stdout_file, self.args.stderr_file)

        self.stdout = open(self.args.stdout_file, 'w')
        self.stderr = open(self.args.stderr_file, 'w')

        start_command = self.service.get_command(self.args.working_dir, self.args.additional_args)
        logging.debug('Launching %s', start_command)

        self.process = subprocess.Popen(start_command,
            stdout=self.stdout, stderr=self.stderr, stdin=None)

        logging.debug('Process is launched with pid %d', self.process.pid)

        with open(self.get_pidfile(), 'w') as fh:
            fh.write(str(self.process.pid))

        return True

    def process_has_stopped(self):
        """
        Check if the service has stopped.
        """
        if not self.process:
            return False

        result = self.process.poll()
        if self.process.returncode or not (result is None):
            return True

        return False
