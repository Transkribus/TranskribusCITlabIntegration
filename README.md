# TranskribusCITlabIntegration

A project that imports both Transkribus packages and the CITlabModule.
It includes tests that check interoperability of both.

## Building
Here is a short guide with steps that need to be performed
to build the project.

### Requirements
- Java >= version 8
- Maven
- All further dependencies should be gathered via Maven

### Build steps
In case this is not yet done, install planet.jar via Maven:

```
mvn install:install-file -Dfile=/path/to/planet/jar/planet_jar-2.1.jar -DgroupId=de.planet -DartifactId=planet_jar -Dversion=2.1 -Dpackaging=jar
```
Clone project and run maven:

```
git clone https://github.com/Transkribus/TranskribusCITlabIntegration
cd TranskribusCITlabIntegration
mvn install
```
This will build the project and run the tests included. If you run the tests in an IDE, make sure to set the Java VM arguments as follows (e.g. in Eclipse via the "Run Configurations..." setting):
```
-Xss64m -Xms128m -Xmx4g
```