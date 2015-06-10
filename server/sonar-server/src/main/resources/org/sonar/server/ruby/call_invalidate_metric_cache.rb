# this script defines a method which calls the class method "clear_cache" of the Metric class defined
# in /server/sonar-web/src/main/webapp/WEB-INF/app/models/metric.rb

class RbCallInvalidateMetricCache
  include Java::org.sonar.server.ruby.CallInvalidateMetricCache
  def call_invalidate
    Metric.clear_cache
  end
end
RbCallInvalidateMetricCache.new
