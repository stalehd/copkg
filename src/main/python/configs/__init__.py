import os
import os.path
import logging

class BaseLaunchConfig:
    """
    The basic launch config class. This is a genering (non-working) configuration
    for all services.
    """

    stdout = None
    stderr = None
    working_dir = None
    install_dir = None

    def __init__(self, working_dir, install_dir):
        self.working_dir = working_dir
        self.install_dir = install_dir

    def check_dependencies(self):
        return False

    def command(self):
        return None

# ---------------------------------------------------------------------------
class JavaLaunchConfig(BaseLaunchConfig):
    """
    A generic launch configuration for java processes. the dependency check
    basically checks if JAVA_HOME is set and if the java executable exists
    somewhere on the path.
    """
    java_executable = 'java'
    java_command = java_executable

    def locate_java_executable(self):
        """
        Note: This won't work for Windows installations but I can live with that.
        """
        for path in os.environ["PATH"].split(":"):
            filename = os.path.join(path, self.java_executable)
            if os.path.exists(filename):
                self.java_command = filename
                logging.debug('Found java executable at %s', self.java_command)
                return True

        return False

    def check_dependencies(self):
        logging.debug('Checking dependencies')
        if not os.environ['JAVA_HOME']:
            logging.warning('JAVA_HOME isn''t set. The system might not have a working JVM installed.')
            if not self.locate_java_executable():
                logging.error('Could not locate a java executable somewhere on the path.')
                return False
        else:
            logging.debug('JAVA_HOME is set to %s', os.environ['JAVA_HOME'])
            self.java_command = os.path.join(os.environ['JAVA_HOME'], 'bin/java')
            if not os.path.isfile(self.java_command):
                logging.error('Could not find java command at %s', self.java_command)
                return False

        return True
