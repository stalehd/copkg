# copkg Maven Assembly Descriptor 

copkg is a simple package format for packaging software for use in a
cloud computing environment.  The basic idea is that the package
contains everything the software needs to run and comes with a defined
way to start and stop the software.

When a copkg is unpacked, the resulting directory structure is meant
to be immutable.  Meaning that when the software is started any
runtime state will need to live in a runtime directory.  The main
motivation for this is to keep software management simple.

## Usage

In order to use the `copkg` assembly descriptor you need to make sure
that you have added the appropriate directories under
**src/main/copkg/** (see further down) in your project and you add the
following to your `pom.xml` file:

    <plugin>
      <artifactId>maven-assembly-plugin</artifactId>
        <dependencies>
          <dependency>
            <groupId>org.cloudname</groupId>
            <artifactId>copkg-assembly</artifactId>
            <version>1.0.0</version>
          </dependency>
        </dependencies>
        <executions>
          <execution>
            <id>assemble</id>
            <phase>package</phase>
            <goals>
              <goal>single</goal>
            </goals>
            <configuration>
              <descriptorRefs>
                <descriptorRef>copkg</descriptorRef>
              </descriptorRefs>
              <includeEmptyDirs>true</includeEmptyDirs>
            </configuration>
          </execution>
        </executions>
      </plugin>

## Structure of a copkg package

A valid copkg package has the following directory structure.

### `bin/`

This directory will contain the binaries.  Some projects have a single
binary while other projects require multiple binaries.  The binaries
may be anything from Java binaries, Python scripts, ELF binaries.  At
this point we do not suggest or impose any platform support.

**Includes: target/*.jar**

### `etc/`

Any static configuration goes into this directory.  Static
configuration is the sort of configuration that will not vary between
multiple instances of the same software running on the same machine.

**Includes: src/main/copkg/etc**

### `lib/`

Anything that does not fit in `bin/` or `etc/` should go into lib.

**Includes: src/main/copkg/lib**

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

**Includes: src/main/copkg/script.d**

## Distributing packages

`copkg` borrows its naming scheme from Maven, meaning that packages
are given *coordinates* that follow the model used in Maven.  This
means that a package is identified by three pieces of information:

* a `groupId`
* an `artifactId`
* a `version`

Usually these are expressed as colon-separated (:) fields like so:
`org.cloudname:timber:1.2.3`.  Two packages with the same groupId,
artifactId and version _must be bit-for-bit identical_.

Packages are distributed using a web server that serves a filesystem
with package files.  The download URL for a package can be calculated
given the coordinate:

The package coordinate `org.cloudname:timber:1.2.3` thus becomes:

    http://copkg.example.com/org/cloudname/timber/1.2.3/timber-1.2.3-copkg.zip
	
## Installing packages

Installing a package is trivial.  The package is downloaded to a
download area and then unpacked into a directory structure that
mimicks the way we calculate distribution URLs from coordinates.

    /copkg/org/cloudname/timber/1.2.3
	
Note that once the package has been unpacked it is verboten to modify
any of the files or add new files.  Each installed package can be in
use by multiple running instances.

## Starting and stopping

As mentioned above, a valid `copkg` package needs at minimum a
`start.sh` and a `stop.sh` script to manage the lifecycle of the
software.  This hides any complexity in starting the application from
the surrounding systems -- all `copkg` packages are started in the
same manner.

In order to start a package you will need to provide a runtime
directory.

### Generic runtime directory

If you are not using the `copkg` with Cloudname you can use the
generic way of specifying runtime directory.

    start.sh --run=/my/runtime/directory
	
You need to supply the `stop.sh` script with the runtime directory as
well.  This in case the startup script leaves PID files or state files
indicating which host:port the service is listening to.

    stop.sh --run=/my/runtime/directory

### Running in conjunction with Cloudname

If the software is supposed to run as a service under Cloudname it
would make sense to use the service coordinate to calculate the path
for the runtime directory and use this.  In this case you need to
specify a `--coordinate` command line option that gives the `start.sh`
script a *Cloudname coordinate*.

    start.sh --coordinate=1.idee.prod.ie
	
Likewise the `stop.sh` script needs the coordinate to be able to stop
the process:

    stop.sh --coordinate=1.idee.prod.ie

