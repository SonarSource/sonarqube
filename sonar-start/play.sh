# killall -9 java;

cd /Volumes/data/sonar/sonarqube/server/sonar-process
mvn clean package install -DskipTests;
#
cd /Volumes/data/sonar/sonarqube/server/sonar-search
mvn clean package install -DskipTests;
#
# cd /Volumes/data/sonar/sonarqube/sonar-application
# mvn clean package install -DskipTests;

cd /Volumes/data/sonar/sonarqube/sonar-start;
mvn clean package install -DskipTests; 

cd target;
unzip sonarqube-4.5-SNAPSHOT.zip; 
cd sonarqube-4.5-SNAPSHOT; 
java -jar start.jar --debug