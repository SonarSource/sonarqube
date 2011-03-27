tried_gem = false
begin
  require "jdbc/mysql"
rescue LoadError
  unless tried_gem
    require 'rubygems'
    gem "jdbc-mysql"
    tried_gem = true
    retry
  end
  # trust that the mysql jar is already present
end
require 'active_record/connection_adapters/jdbc_adapter'
