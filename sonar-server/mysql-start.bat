set MAVEN_OPTS='-Xmx768m -XX:MaxPermSize=128m'
mvn clean jetty:run -Pstart-dev-server,mysql %*
