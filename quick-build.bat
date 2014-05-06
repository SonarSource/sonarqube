set MAVEN_OPTS=-Xmx256m
mvn install -Dtest=false -DfailIfNoTests=false %*
