import os.path

def get_pidfile(working_dir):
    return os.path.join(working_dir, 'run/process.pid')


def dump_logfile(filename):
    with open(filename, 'r') as fh:
        for line in fh:
            logging.info('%s -- %s', os.path.basename(filename), line)
