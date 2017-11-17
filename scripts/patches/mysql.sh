set -euo pipefail

source scripts/property_utils.sh

SQ_HOME=$1

echo "configuring mysql"
set_property sonar.jdbc.url "jdbc:mysql://localhost:3306/sonarqube?useUnicode=true&characterEncoding=utf8&rewriteBatchedStatements=true&useConfigs=maxPerformance" $SQ_HOME/conf/sonar.properties
set_property sonar.jdbc.username sonarqube $SQ_HOME/conf/sonar.properties
set_property sonar.jdbc.password sonarqube $SQ_HOME/conf/sonar.properties
