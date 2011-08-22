tried_gem = false
begin
  require "jdbc/postgres"
rescue LoadError
  unless tried_gem
    require 'rubygems'
    gem "jdbc-postgres"
    tried_gem = true
    retry
  end
  # trust that the postgres jar is already present
end
require 'active_record/connection_adapters/jdbc_adapter'