To update the archetype project :

1. update sources of the initial project (/project directory)
2. /project$ mvn install -DsonarTargetVersion=<version>
3. copy the content of /project/target/generated-sources/archetype/src/main/resources to src/main/resources. Be careful with .svn files !
