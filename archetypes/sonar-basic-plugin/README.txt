To update the archetype project :

1. update sources of the initial project (/project directory)
2. /project$ mvn install -DsonarTargetVersion=<version>
3. copy the content of /project/target/generated-sources/archetype/src/main/resources to src/main/resources. Be careful with .svn files !

To execute the archetype :
mvn archetype:generate -B -DarchetypeGroupId=org.codehaus.sonar.archetypes -DarchetypeArtifactId=sonar-basic-plugin-archetype -DarchetypeVersion=<SONAR VERSION> -DgroupId=com.mycompany.sonar -DartifactId=sonar-basic-sample-plugin -Dversion=0.1-SNAPSHOT