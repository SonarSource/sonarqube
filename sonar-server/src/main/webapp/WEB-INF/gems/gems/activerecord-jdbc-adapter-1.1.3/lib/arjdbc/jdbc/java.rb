require 'java'
require 'arjdbc/jdbc/adapter_java'

module ActiveRecord
  module ConnectionAdapters
    module Jdbc
      Mutex = java.lang.Object.new
      DriverManager = java.sql.DriverManager
      Types = java.sql.Types
    end

    java_import "arjdbc.jdbc.JdbcConnectionFactory"
  end
end
