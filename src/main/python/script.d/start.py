#!/usr/bin/env python
import copkg
import copkg.services
import logging
import sys

if __name__ == "__main__":
    java_service = copkg.services.JavaService()

    #
    # Set up default arguments
    #
    launcher = copkg.services.ServiceLauncher(java_service)
    if not launcher.create_default_arguments():
        sys.exit(1)

    if launcher.pidfile_exists():
        logging.warning('Found existing pidfile at %s; process is already running',
            launcher.get_pidfile())
        sys.exit(2)

    if not launcher.check_dependencies():
        sys.exit(3)

    if not copkg.start_service(launcher):
        sys.exit(4)

