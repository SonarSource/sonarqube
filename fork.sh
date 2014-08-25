#!/bin/sh

mvn clean install -DskipTests -pl :sonar-server,:sonar-search,:sonar-process -amd

if [[ "$OSTYPE" == "darwin"* ]]; then
  OS='macosx-universal-64'
else
  OS='linux-x86-64'
fi

cd sonar-application/target/
if ! ls sonarqube-*/bin/$OS/sonar.sh &> /dev/null; then
  unzip sonarqube-*.zip
fi

cd sonarqube-*
touch logs/application.log
touch logs/search.log
touch logs/sonar.log

tmux new-session "tmux split-window -v 'tail -f logs/sonar.log'; tmux split-window -h 'tail -f logs/search.log'; java -jar lib/sonar-application*.jar -Dsonar.node.name=forky -Dsonar.cluster.master=10.0.1.1:2222"
#tmux new-session "tmux split-window -h 'tail -f logs/search.log'; java -jar lib/sonar-application*.jar"

#tmux new-session "tmux split-window -v 'tail -f logs/sonar.log'; tmux split-window -h 'tail -f logs/search.log'; tail -f logs/application.log"
