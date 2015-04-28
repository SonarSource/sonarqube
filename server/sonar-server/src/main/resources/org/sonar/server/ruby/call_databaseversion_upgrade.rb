# this script defines a method which calls the class method "upgrade" of the DatabaseVersion class defined
# in /server/sonar-web/src/main/webapp/WEB-INF/lib/database_version.rb

require 'database_version'

class RbCallUpgrade
  include Java::org.sonar.server.ruby.CallDatabaseVersionUpgrade
  def call_upgrade
    DatabaseVersion.upgrade
  end
end
RbCallUpgrade.new