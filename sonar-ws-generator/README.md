# What is this project used for?

The *sonar-ws-generator* must be used to generate the java sources used to access the Web API (HTTP) of SonarQube.
The generated sources are not compilable on their own, but require to be copied in the `sonar-ws` module.

# How do I use it?

* Make your changes on the SonarQube Web API (protobuf specification of responses in sonar-ws or `WebService` implementations in sonar-server)
* Build SonarQube (for example with `./quick-build.sh`)
* Run this generator: `sonar-ws-generator/run.sh`
* Check your git status: the generated sources are copied in `sonar-ws/src/main/java`. Please double check the changed code.
* Now you can rebuild sonar-ws and use it in integration tests

# Hints
* A request parameter has type `List<String>` if its description contains "comma-separated" or "list of" (case insensitive). Examples:
  * "Comma-separated list of metric keys"
  * "List of metric keys"
