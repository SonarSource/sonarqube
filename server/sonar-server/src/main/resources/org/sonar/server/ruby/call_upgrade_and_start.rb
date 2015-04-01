# this script defines a method which calls the class method "upgrade_and_start" of the DatabaseVersion class defined
# in /server/sonar-web/src/main/webapp/WEB-INF/lib/database_version.rb

require 'database_version'

def call_upgrade_and_start
  DatabaseVersion.upgrade_and_start
end