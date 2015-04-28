# this script defines a method which calls the class method "load_java_web_services" of the DatabaseVersion class
# definedin /server/sonar-web/src/main/webapp/WEB-INF/lib/database_version.rb

require 'database_version'

class RbCallLoadJavaWebServices
  include Java::org.sonar.server.ruby.CallLoadJavaWebServices
  def call_load_java_web_services
    DatabaseVersion.load_java_web_services
  end
end
RbCallLoadJavaWebServices.new