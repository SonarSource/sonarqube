set MAVEN_OPTS=-Xmx256m
mvn clean install -Dtest=false -DfailIfNoTests=false %*
