#!/usr/bin/expect

set timeout 1800

set BITBUCKET_VERSION [lindex $argv 0]

if { $BITBUCKET_VERSION == "LATEST" } {
    spawn bash -c "JAVA_HOME=/usr/lib/jvm/temurin-8-jdk-amd64 /opt/atlassian-plugin-sdk/bin/atlas-run-standalone -DskipAllPrompts=true --product bitbucket"
} else {
    spawn bash -c "JAVA_HOME=/usr/lib/jvm/temurin-8-jdk-amd64 /opt/atlassian-plugin-sdk/bin/atlas-run-standalone -DskipAllPrompts=true --product bitbucket --version $BITBUCKET_VERSION --data-version $BITBUCKET_VERSION"
}

expect "dummy string to let the script not terminate"
