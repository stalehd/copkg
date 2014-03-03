#!/usr/bin/env python
#
# A sample stop.py that uses the copkg and copkg.services libraries.
# There's not a lot of exciting things going on; just check for
# a pid file, stop the service if it is found and return the proper
# error code.
#
import copkg
import copkg.services
import sys
import logging

if __name__ == "__main__":
    java_service= copkg.services.JavaService()
    launcher = copkg.services.ServiceLauncher(java_service)

    if not launcher.create_default_arguments():
        sys.exit(1)

    if not launcher.pidfile_exists():
        logging.warning('No pidfile found at %s',
            launcher.get_pidfile())
        sys.exit(2)

    if not copkg.stop_service(launcher):
        logging.error('Got error stopping process.')
        sys.exit(3)
