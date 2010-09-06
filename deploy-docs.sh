#!/bin/sh

mvn clean javadoc:aggregate -Prelease

tar -cf docs.tar -C target/site/ .

scp -i ~/.ssh/id_rsa-sonar-keypair -C -r docs.tar root@ec2-75-101-133-159.compute-1.amazonaws.com:/tmp

ssh -i ~/.ssh/id_rsa-sonar-keypair root@ec2-75-101-133-159.compute-1.amazonaws.com 'tar -xf /tmp/docs.tar -C /vol/www/sonar.codehaus.org/docs'