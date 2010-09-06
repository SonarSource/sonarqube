set MAVEN_OPTS=-Xmx768m -XX:MaxPermSize=256m
mvn clean install -Dtest=false -DfailIfNoTests=false