require 'jdbc_adapter/tsql_helper'

module ::JdbcSpec
  module ActiveRecordExtensions
    def cachedb_connection( config )
      config[:port] ||= 1972
      config[:url] ||= "jdbc:Cache://#{config[:host]}:#{config[:port]}/#{ config[:database]}"
      config[:driver] ||= "com.intersys.jdbc.CacheDriver"
      jdbc_connection( config )
    end
  end

  module CacheDB
    include TSqlMethods

    def self.column_selector
      [ /cache/i, lambda {  | cfg, col | col.extend( ::JdbcSpec::CacheDB::Column ) } ]
    end

    def self.adapter_selector
      [ /cache/i, lambda {  | cfg, adapt | adapt.extend( ::JdbcSpec::CacheDB ) } ]
    end

    module Column
    end
    
    def create_table(name, options = { })
      super(name, options)
      primary_key = options[:primary_key] || "id"
      execute "ALTER TABLE #{name} ADD CONSTRAINT #{name}_PK PRIMARY KEY(#{primary_key})" unless options[:id] == false
    end
  end
end
