# this script defines a method which calls the class method "load_java_web_services" of the Bootstrap class
# definedin /server/sonar-web/src/main/webapp/WEB-INF/config/environment.rb

# this essentially makes UT work, must be a file that actually exists in production
require 'database_version'

class RbCallLoadJavaWebServices
  include Java::org.sonar.server.ruby.CallLoadJavaWebServices
  def call_load_java_web_services
    Bootstrap.load_java_web_services
  end
end
RbCallLoadJavaWebServices.new
