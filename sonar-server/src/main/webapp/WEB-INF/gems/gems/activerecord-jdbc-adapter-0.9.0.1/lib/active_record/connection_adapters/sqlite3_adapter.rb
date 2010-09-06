tried_gem = false
begin
  require "jdbc/sqlite3"
rescue LoadError
  unless tried_gem
    require 'rubygems'
    gem "jdbc-sqlite3"
    tried_gem = true
    retry
  end
  # trust that the sqlite jar is already present
end
require 'active_record/connection_adapters/jdbc_adapter'
