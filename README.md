# copkg - a simple software packaging format.

copkg is a simple package format for packaging software for use in a
cloud computing environment.  The basic idea is that the package
contains everything the software needs to run and comes with a defined
way to start and stop the software.

When a copkg is unpacked, the resulting directory structure is meant
to be immutable.  Meaning that when the software is started any
runtime state will need to live in a runtime directory.  The main
motivation for this is to keep software management simple.

## Structure of a copkg package

A valid copkg package has the following directory structure.

### `bin/`

This directory will contain the binaries.  Some projects have a single
binary while other projects require multiple binaries.  The binaries
may be anything from Java binaries, Python scripts, ELF binaries.  At
this point we do not suggest or impose any platform support.

### `etc/`

Any static configuration goes into this directory.  Static
configuration is the sort of configuration that will not vary between
multiple instances of the same software running on the same machine.

### `lib/`

Anything that does not fit in `bin/` or `etc/` should go into lib.

### `script.d/`

This directory will contain the scripts for starting and stopping the
software.  Currently a valid `copkg` package requires two scripts:

* start.sh
* stop.sh

*We will most likely be defining more optional scripts at a later
point.  For instance we may need scripts for managing state that has
been persisted to disk (typical use-case is moving it to a durable
store and having a way to thaw the data from durable store at a later
point).*

## Starting and stopping

As mentioned above, a valid `copkg` package needs at minimum a
`start.sh` and a `stop.sh` script to manage the lifecycle of the
software.  This hides any complexity in starting the application from
the surrounding systems -- all `copkg` packages are started in the
same manner.
