tried_gem = false
begin
  require "jdbc/derby"
rescue LoadError
  unless tried_gem
    require 'rubygems'
    gem "jdbc-derby"
    tried_gem = true
    retry
  end
  # trust that the derby jar is already present
end
require 'active_record/connection_adapters/jdbc_adapter'