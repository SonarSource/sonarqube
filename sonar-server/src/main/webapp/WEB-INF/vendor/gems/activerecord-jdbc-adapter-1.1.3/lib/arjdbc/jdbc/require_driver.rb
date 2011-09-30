module Kernel
  # load a JDBC driver library/gem, failing silently. If failed, trust
  # that the driver jar is already present through some other means
  def jdbc_require_driver(path, gem_name = nil)
    gem_name ||= path.sub('/', '-')
    2.times do
      begin
        require path
        break
      rescue LoadError
        require 'rubygems'
        begin; gem gem_name; rescue LoadError; end
      end
    end
  end
end
