set MAVEN_OPTS='-Xmx512m -XX:MaxPermSize=160m'
mvn org.apache.tomcat.maven:tomcat7-maven-plugin::run -Pstart-dev-server,postgresql %*
