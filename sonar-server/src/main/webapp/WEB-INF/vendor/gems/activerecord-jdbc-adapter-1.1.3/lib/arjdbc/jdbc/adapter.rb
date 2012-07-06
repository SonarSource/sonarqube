require 'active_record/version'
require 'active_record/connection_adapters/abstract_adapter'
require 'arjdbc/version'
require 'arjdbc/jdbc/require_driver'
require 'arjdbc/jdbc/connection_methods'
require 'arjdbc/jdbc/compatibility'
require 'arjdbc/jdbc/core_ext'
require 'arjdbc/jdbc/java'
require 'arjdbc/jdbc/type_converter'
require 'arjdbc/jdbc/driver'
require 'arjdbc/jdbc/column'
require 'arjdbc/jdbc/connection'
require 'arjdbc/jdbc/callbacks'
require 'arjdbc/jdbc/extension'
require 'bigdecimal'

module ActiveRecord
  module ConnectionAdapters
    class JdbcAdapter < AbstractAdapter
      extend ShadowCoreMethods
      include CompatibilityMethods if CompatibilityMethods.needed?(self)
      include JdbcConnectionPoolCallbacks if JdbcConnectionPoolCallbacks.needed?

      attr_reader :config

      def initialize(connection, logger, config)
        @config = config
        spec = adapter_spec config
        unless connection
          connection_class = jdbc_connection_class spec
          connection = connection_class.new config
        end
        super(connection, logger)
        extend spec if spec
        configure_arel2_visitors(config)
        connection.adapter = self
        JndiConnectionPoolCallbacks.prepare(self, connection)
      end

      def jdbc_connection_class(spec)
        connection_class = spec.jdbc_connection_class if spec && spec.respond_to?(:jdbc_connection_class)
        connection_class = ::ActiveRecord::ConnectionAdapters::JdbcConnection unless connection_class
        connection_class
      end

      def jdbc_column_class
        ActiveRecord::ConnectionAdapters::JdbcColumn
      end

      # Retrieve the raw java.sql.Connection object.
      def jdbc_connection
        raw_connection.connection
      end

      # Locate specialized adapter specification if one exists based on config data
      def adapter_spec(config)
        2.times do
          dialect = (config[:dialect] || config[:driver]).to_s
          ::ArJdbc.constants.map { |name| ::ArJdbc.const_get name }.each do |constant|
            if constant.respond_to? :adapter_matcher
              spec = constant.adapter_matcher(dialect, config)
              return spec if spec
            end
          end

          # If nothing matches and we're using jndi, try to automatically detect the database.
          break unless config[:jndi] and !config[:dialect]
          begin
            conn = Java::javax.naming.InitialContext.new.lookup(config[:jndi]).getConnection
            config[:dialect] = conn.getMetaData.getDatabaseProductName

            # Derby-specific hack
            if ::ArJdbc::Derby.adapter_matcher(config[:dialect], config)
              # Needed to set the correct database schema name
              config[:username] ||= conn.getMetaData.getUserName
            end
          rescue
            conn.close if conn
          end
        end
        nil
      end

      def modify_types(tp)
        tp
      end

      def adapter_name #:nodoc:
        'JDBC'
      end

      def arel2_visitors
        {}
      end

      def configure_arel2_visitors(config)
        if defined?(::Arel::Visitors::VISITORS)
          visitors = ::Arel::Visitors::VISITORS
          visitor = nil
          arel2_visitors.each do |k,v|
            visitor = v
            visitors[k] = v
          end
          if visitor && config[:adapter] =~ /^(jdbc|jndi)$/
            visitors[config[:adapter]] = visitor
          end
        end
      end

      def is_a?(klass)          # :nodoc:
        # This is to fake out current_adapter? conditional logic in AR tests
        if Class === klass && klass.name =~ /#{adapter_name}Adapter$/i
          true
        else
          super
        end
      end

      def supports_migrations?
        true
      end

      def native_database_types #:nodoc:
        @connection.native_database_types
      end

      def database_name #:nodoc:
        @connection.database_name
      end

      def native_sql_to_type(tp)
        if /^(.*?)\(([0-9]+)\)/ =~ tp
          tname = $1
          limit = $2.to_i
          ntype = native_database_types
          if ntype[:primary_key] == tp
            return :primary_key,nil
          else
            ntype.each do |name,val|
              if name == :primary_key
                next
              end
              if val[:name].downcase == tname.downcase && (val[:limit].nil? || val[:limit].to_i == limit)
                return name,limit
              end
            end
          end
        elsif /^(.*?)/ =~ tp
          tname = $1
          ntype = native_database_types
          if ntype[:primary_key] == tp
            return :primary_key,nil
          else
            ntype.each do |name,val|
              if val[:name].downcase == tname.downcase && val[:limit].nil?
                return name,nil
              end
            end
          end
        else
          return :string,255
        end
        return nil,nil
      end

      def active?
        @connection.active?
      end

      def reconnect!
        @connection.reconnect!
        @connection
      end

      def disconnect!
        @connection.disconnect!
      end

      def execute(sql, name = nil)
        if name == :skip_logging
          _execute(sql)
        else
          log(sql, name) { _execute(sql) }
        end
      end

      # we need to do it this way, to allow Rails stupid tests to always work
      # even if we define a new execute method. Instead of mixing in a new
      # execute, an _execute should be mixed in.
      def _execute(sql, name = nil)
        @connection.execute(sql)
      end

      def jdbc_insert(sql, name = nil, pk = nil, id_value = nil, sequence_name = nil)
        insert_sql(sql, name, pk, id_value, sequence_name)
      end

      def jdbc_update(sql, name = nil) #:nodoc:
        execute(sql, name)
      end
      def jdbc_select_all(sql, name = nil)
        select(sql, name)
      end

      # Allow query caching to work even when we override alias_method_chain'd methods
      alias_chained_method :select_all, :query_cache, :jdbc_select_all
      alias_chained_method :update, :query_dirty, :jdbc_update
      alias_chained_method :insert, :query_dirty, :jdbc_insert

      # Do we need this? Not in AR 3.
      def select_one(sql, name = nil)
        select(sql, name).first
      end

      def select_rows(sql, name = nil)
        rows = []
        select(sql, name).each {|row| rows << row.values }
        rows
      end

      def insert_sql(sql, name = nil, pk = nil, id_value = nil, sequence_name = nil)
        id = execute(sql, name = nil)
        id_value || id
      end

      def jdbc_columns(table_name, name = nil)
        # sonar
        return @connection.columns(table_name.to_s).reject { |c| (c.name=='admin' || c.name=='remarks') } if table_name=='users'
        @connection.columns(table_name.to_s)
      end
      alias_chained_method :columns, :query_cache, :jdbc_columns

      def tables(name = nil)
        @connection.tables
      end

      def table_exists?(name)
        jdbc_columns(name) rescue nil
      end

      def indexes(table_name, name = nil, schema_name = nil)
        @connection.indexes(table_name, name, schema_name)
      end

      def begin_db_transaction
        @connection.begin
      end

      def commit_db_transaction
        @connection.commit
      end

      def rollback_db_transaction
        @connection.rollback
      end

      def write_large_object(*args)
        @connection.write_large_object(*args)
      end

      def pk_and_sequence_for(table)
        key = primary_key(table)
        [key, nil] if key
      end

      def primary_key(table)
        primary_keys(table).first
      end

      def primary_keys(table)
        @connection.primary_keys(table)
      end

      def select(*args)
        execute(*args)
      end

      def translate_exception(e, message)
        puts e.backtrace if $DEBUG || ENV['DEBUG']
        super
      end
      protected :translate_exception
    end
  end
end
