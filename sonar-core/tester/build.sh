#!/bin/sh

rm *.jar

javac a/*.java
jar cvf a.jar -C a/ .

javac b/*.java
jar cvf b.jar -C b/ .

javac c/*.java
jar cvf c.jar -C c/ .

javac a_v2/*.java
jar cvf a_v2.jar -C a_v2 .

