# this script defines a method which calls the class method "clear_cache" of the Metric class defined
# in /server/sonar-web/src/main/webapp/WEB-INF/app/models/metric.rb

# this essentially makes UT work, must be a file that actually exists in production
require 'database_version'

class RbCallInvalidateMetricCache
  include Java::org.sonar.server.ruby.CallInvalidateMetricCache
  def call_invalidate
    Metric.clear_cache
  end
end
RbCallInvalidateMetricCache.new
