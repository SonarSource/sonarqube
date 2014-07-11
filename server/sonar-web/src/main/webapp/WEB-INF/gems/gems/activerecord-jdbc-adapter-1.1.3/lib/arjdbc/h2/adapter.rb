require 'arjdbc/hsqldb/adapter'

module ArJdbc
  module H2
    include HSQLDB

    def self.jdbc_connection_class
      ::ActiveRecord::ConnectionAdapters::H2JdbcConnection
    end

    def adapter_name #:nodoc:
      'H2'
    end

    def arel2_visitors
      super.merge 'h2' => ::Arel::Visitors::HSQLDB, 'jdbch2' => ::Arel::Visitors::HSQLDB
    end

    def h2_adapter
      true
    end

    def tables
      @connection.tables(nil, h2_schema)
    end

    def columns(table_name, name=nil)
      @connection.columns_internal(table_name.to_s, name, h2_schema)
    end

    private
    def h2_schema
      @config[:schema] || ''
    end
  end
end
