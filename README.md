# copkg

**copkg** is an extremely simple way to package software artifacts in a ZIP file and does not attempt to be a complete package management system.  Rather, it represents the simplest possible way to package code, some config and a few scripts to start and stop the package in a standardized manner so that **automation becomes easier**.

An important motivation for copkg is that it should be usable on developer workstations as well.  This will help get iteration times down as it is very important that developers be able to install components from other projects (everyone knows how to install and run their own components -- this isn't necessarily true for other projects components).

It does not, and will never, deal with dependencies.  copkg packages have to include whatever dependencies they have in the package.  A copkg should be self-contained.

### Installing the copkg utility

- Brew install: `brew install https://raw.github.com/borud/homebrew/master/copkg.rb`
- Debian package

### Configuring copkg

You can put config in `~/.copkg/config.json` or `/etc/copkg/config.json`.  Here is a sample configuration:

    {
      "packageDir" : "/path/to/your/package/dir",
      "packageBaseUrl" : "http://your.repository/",
	  "username" : "the username",
	  "password" : "the password"
    }


### Installing copkg packages

You can install a copkg by issuing the following command:

    copkg install <coordinate>
    
This will download and install the package if it is available.  It will not start the service in question.

### Removing copkg packages

    copkg uninstall <coordinate>

### Resolving

When you are playing around with copkg packages you sometimes need to figure out what URLs you end up downloading from, what directories you have configured etc.  The `resolve` command takes care of this:

    copkg resolve <coordinate>

---

# How copkg works

A copkg is identified by a **package coordinate**.  The copkg package coordinates are loosely based on Maven coordinates and have three parts: groupId, artifactId and version.  Their canonical form is the three fields separated by ":" (colon) character.  Here are a few examples of valid copkg package coordinates:

    com.comoyo:james:1.2.1
    org.mongodb:mongodb:2.3.0
    
## Structure of a copkg package

A valid copkg package has the following directory structure.

### bin/

This directory will contain the binaries.  Some projects have a single
binary while other projects require multiple binaries.  The binaries
may be anything from Java binaries, Python scripts, ELF binaries.  At
this point we do not suggest or impose any platform support.

*Includes: target/*.jar*

### etc/

Any static configuration goes into this directory.  Static
configuration is the sort of configuration that will not vary between
multiple instances.

*Includes: src/main/copkg/etc*

### lib/

Anything that does not fit in `bin/` or `etc/` should go into lib.

*Includes: src/main/copkg/lib*

### script.d/

This directory will contain the scripts for starting and stopping the
software.  Currently a valid `copkg` package requires two scripts:

* start.py
* stop.py

*Includes: src/main/copkg/script.d*

## Distributing packages

*Can use any facility capable of serving static HTTP resources as long as directory structure can be achieved*.

## Package directory and runtime directory

**copkg** packages are downloaded and unpacked into a *package directory*.  The *package directory* is a path somewhere on the filesystem into which packages are installed -- for instance `/usr/share/copkg`. 

So when the package `example.org:myservice:1.2.3` is installed it would be installed under `/usr/share/copkg/org/example/myservice/1.2.3`.


### Runtime directory 

In order to run `myservice` in the above example we need a *runtime directory*.  This allows us to de-couple the runtime state of the service from the software artifact. It makes sense to name the runtime directory after something that identifies the service instance.

We can imagine that our runtime directories are rooted at `/usr/local/services`.  Let's imagine we identify my service as `1.myservice.borud.trd`.  Then its runtime directory would be `/usr/local/services/1.myservice.borud.trd`.

## Runtime directory structure

### logs/

Directory used for all logs.

### data/

The main directory for where the service will store its runtime state.  For instance, if the service has a on-disk database, this is where the database would go.  (*Lifecycle management of ephemeral state is a future concern*).

### run/

Various runtime metadata such as a `service.pid` file which contains the PID(s) of the process (*if multiple PIDs then one on each line*).  Also where we store the `start.json` file.

### etc/

Per instance configuration.  This is where configuration files that are specific to the instance go.



## Building copkg packages

When a copkg is unpacked, the resulting directory structure is meant
to be immutable.  Meaning that when the software is started any
runtime state will need to live in a runtime directory.  The main
motivation for this is to keep software management simple.

### Usage

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

