=== How to install a plugin

1. Build plugin with Maven : mvn clean install
2. Copy the JAR file generated into target/ to the directory extensions/plugins/ of the Sonar server 
3. Restart the Sonar server


=== How to activate Checkstyle extensions

Install the plugin "checkstyle-extensions" then search for the rule "Methods count"  in the administration console of Quality profiles.