# a dummy class Bootstrap that "mocks" the Bootstrap class of the platform by defining a class method called upgrade_and_start
# unit test only makes sure the wrapping and method forwarding provided by JRuby works so providing an empty method is enough as
# it would otherwise raise an exception
class Bootstrap

  def self.load_java_web_services

  end

end

class Metric

  def self.clear_cache

  end

end
