require 'arjdbc/jdbc/missing_functionality_helper'

module ActiveRecord::ConnectionAdapters
  Sqlite3Adapter = Class.new(AbstractAdapter) unless const_defined?(:Sqlite3Adapter)
end

module ::ArJdbc
  module SQLite3
    def self.column_selector
      [/sqlite/i, lambda {|cfg,col| col.extend(::ArJdbc::SQLite3::Column)}]
    end

    def self.jdbc_connection_class
      ::ActiveRecord::ConnectionAdapters::Sqlite3JdbcConnection
    end

    module Column
      def init_column(name, default, *args)
        @default = '' if default =~ /NULL/
      end

      def type_cast(value)
        return nil if value.nil?
        case type
        when :string   then value
        when :primary_key then defined?(value.to_i) ? value.to_i : (value ? 1 : 0)
        when :float    then value.to_f
        when :decimal  then self.class.value_to_decimal(value)
        when :boolean  then self.class.value_to_boolean(value)
        else super
        end
      end

      private
      def simplified_type(field_type)
        case field_type
        when /boolean/i                        then :boolean
        when /text/i                           then :text
        when /varchar/i                        then :string
        when /int/i                            then :integer
        when /float/i                          then :float
        when /real|decimal/i                   then @scale == 0 ? :integer : :decimal
        when /datetime/i                       then :datetime
        when /date/i                           then :date
        when /time/i                           then :time
        when /blob/i                           then :binary
        end
      end

      def extract_limit(sql_type)
        return nil if sql_type =~ /^(real)\(\d+/i
        super
      end

      def extract_precision(sql_type)
        case sql_type
          when /^(real)\((\d+)(,\d+)?\)/i then $2.to_i
          else super
        end
      end

      def extract_scale(sql_type)
        case sql_type
          when /^(real)\((\d+)\)/i then 0
          when /^(real)\((\d+)(,(\d+))\)/i then $4.to_i
          else super
        end
      end

      # Post process default value from JDBC into a Rails-friendly format (columns{-internal})
      def default_value(value)
        # jdbc returns column default strings with actual single quotes around the value.
        return $1 if value =~ /^'(.*)'$/

        value
      end
    end

    def adapter_name #:nodoc:
      'SQLite'
    end

    def arel2_visitors
      {'jdbcsqlite3' => ::Arel::Visitors::SQLite}
    end

    def supports_ddl_transactions?
      true # sqlite_version >= '2.0.0'
    end

    def supports_add_column?
      sqlite_version >= '3.1.6'
    end

    def supports_count_distinct? #:nodoc:
      sqlite_version >= '3.2.6'
    end

    def supports_autoincrement? #:nodoc:
      sqlite_version >= '3.1.0'
    end

    def sqlite_version
      @sqlite_version ||= select_value('select sqlite_version(*)')
    end

    def modify_types(tp)
      tp[:primary_key] = "integer primary key autoincrement not null"
      tp[:string] = { :name => "varchar", :limit => 255 }
      tp[:text] = { :name => "text" }
      tp[:float] = { :name => "float" }
      tp[:decimal] = { :name => "decimal" }
      tp[:datetime] = { :name => "datetime" }
      tp[:timestamp] = { :name => "datetime" }
      tp[:time] = { :name => "time" }
      tp[:date] = { :name => "date" }
      tp[:boolean] = { :name => "boolean" }
      tp[:binary] = { :name => "blob" }
      tp
    end

    def quote_column_name(name) #:nodoc:
      %Q("#{name}")
    end

    def quote_string(str)
      str.gsub(/'/, "''")
    end

    def quoted_true
      %Q{'t'}
    end

    def quoted_false
      %Q{'f'}
    end

    # Quote date/time values for use in SQL input. Includes microseconds
    # if the value is a Time responding to usec.
    def quoted_date(value) #:nodoc:
      if value.respond_to?(:usec)
        "#{super}.#{sprintf("%06d", value.usec)}"
      else
        super
      end
    end

    def insert_sql(sql, name = nil, pk = nil, id_value = nil, sequence_name = nil) #:nodoc:
      @connection.execute_update(sql)
      id_value || last_insert_id
    end

    def last_insert_id
      Integer(select_value("SELECT last_insert_rowid()"))
    end

    def tables(name = nil) #:nodoc:
      sql = <<-SQL
        SELECT name
        FROM sqlite_master
        WHERE type = 'table' AND NOT name = 'sqlite_sequence'
      SQL

      select_rows(sql, name).map do |row|
        row[0]
      end
    end

    def indexes(table_name, name = nil)
      result = select_rows("SELECT name, sql FROM sqlite_master WHERE tbl_name = #{quote_table_name(table_name)} AND type = 'index'", name)

      result.collect do |row|
        name = row[0]
        index_sql = row[1]
        unique = (index_sql =~ /unique/i)
        cols = index_sql.match(/\((.*)\)/)[1].gsub(/,/,' ').split.map do |c|
          match = /^"(.+)"$/.match(c); match ? match[1] : c
        end
        ::ActiveRecord::ConnectionAdapters::IndexDefinition.new(table_name, name, unique, cols)
      end
    end

    def primary_key(table_name) #:nodoc:
      column = table_structure(table_name).find {|field| field['pk'].to_i == 1}
      column ? column['name'] : nil
    end

    def recreate_database(name)
      tables.each{ |table| drop_table(table) }
    end

    def _execute(sql, name = nil)
      result = super
      ActiveRecord::ConnectionAdapters::JdbcConnection::insert?(sql) ? last_insert_id : result
    end

    def select(sql, name=nil)
      execute(sql, name).map do |row|
        record = {}
        row.each_key do |key|
          if key.is_a?(String)
            record[key.sub(/^"?\w+"?\./, '')] = row[key]
          end
        end
        record
      end
    end

    def table_structure(table_name)
      structure = @connection.execute_query("PRAGMA table_info(#{quote_table_name(table_name)})")
      raise ActiveRecord::StatementInvalid, "Could not find table '#{table_name}'" if structure.empty?
      structure
    end

    def jdbc_columns(table_name, name = nil) #:nodoc:
      table_structure(table_name).map do |field|
        ::ActiveRecord::ConnectionAdapters::SQLite3Column.new(@config, field['name'], field['dflt_value'], field['type'], field['notnull'] == 0)
      end
    end

    def primary_key(table_name) #:nodoc:
      column = table_structure(table_name).find { |field|
        field['pk'].to_i == 1
      }
      column && column['name']
    end

    def remove_index!(table_name, index_name) #:nodoc:
      execute "DROP INDEX #{quote_column_name(index_name)}"
    end

    def rename_table(name, new_name)
      execute "ALTER TABLE #{quote_table_name(name)} RENAME TO #{quote_table_name(new_name)}"
    end

    # See: http://www.sqlite.org/lang_altertable.html
    # SQLite has an additional restriction on the ALTER TABLE statement
    def valid_alter_table_options( type, options)
      type.to_sym != :primary_key
    end

    def add_column(table_name, column_name, type, options = {}) #:nodoc:
      if supports_add_column? && valid_alter_table_options( type, options )
        super(table_name, column_name, type, options)
      else
        alter_table(table_name) do |definition|
          definition.column(column_name, type, options)
        end
      end
    end

    def remove_column(table_name, *column_names) #:nodoc:
      raise ArgumentError.new("You must specify at least one column name.  Example: remove_column(:people, :first_name)") if column_names.empty?
      column_names.flatten.each do |column_name|
        alter_table(table_name) do |definition|
          definition.columns.delete(definition[column_name])
        end
      end
    end
    alias :remove_columns :remove_column

    def change_column_default(table_name, column_name, default) #:nodoc:
      alter_table(table_name) do |definition|
        definition[column_name].default = default
      end
    end

    def change_column_null(table_name, column_name, null, default = nil)
      unless null || default.nil?
        execute("UPDATE #{quote_table_name(table_name)} SET #{quote_column_name(column_name)}=#{quote(default)} WHERE #{quote_column_name(column_name)} IS NULL")
      end
      alter_table(table_name) do |definition|
        definition[column_name].null = null
      end
    end

    def change_column(table_name, column_name, type, options = {}) #:nodoc:
      alter_table(table_name) do |definition|
        include_default = options_include_default?(options)
        definition[column_name].instance_eval do
          self.type    = type
          self.limit   = options[:limit] if options.include?(:limit)
          self.default = options[:default] if include_default
          self.null    = options[:null] if options.include?(:null)
        end
      end
    end

    def rename_column(table_name, column_name, new_column_name) #:nodoc:
      unless columns(table_name).detect{|c| c.name == column_name.to_s }
        raise ActiveRecord::ActiveRecordError, "Missing column #{table_name}.#{column_name}"
      end
      alter_table(table_name, :rename => {column_name.to_s => new_column_name.to_s})
    end

     # SELECT ... FOR UPDATE is redundant since the table is locked.
    def add_lock!(sql, options) #:nodoc:
      sql
    end

    def empty_insert_statement_value
      "VALUES(NULL)"
    end

    protected
    include ArJdbc::MissingFunctionalityHelper

    def translate_exception(exception, message)
      case exception.message
      when /column(s)? .* (is|are) not unique/
        ActiveRecord::RecordNotUnique.new(message, exception)
      else
        super
      end
    end
  end
end

module ActiveRecord::ConnectionAdapters
  remove_const(:SQLite3Adapter) if const_defined?(:SQLite3Adapter)
  remove_const(:SQLiteAdapter) if const_defined?(:SQLiteAdapter)

  class SQLite3Column < JdbcColumn
    include ArJdbc::SQLite3::Column

    def initialize(name, *args)
      if Hash === name
        super
      else
        super(nil, name, *args)
      end
    end

    def call_discovered_column_callbacks(*)
    end

    def self.string_to_binary(value)
      "\000b64" + [value].pack('m*').split("\n").join('')
    end

    def self.binary_to_string(value)
      if value.respond_to?(:force_encoding) && value.encoding != Encoding::ASCII_8BIT
        value = value.force_encoding(Encoding::ASCII_8BIT)
      end

      if value[0..3] == "\000b64"
        value[4..-1].unpack('m*').first
      else
        value
      end
    end
  end

  class SQLite3Adapter < JdbcAdapter
    include ArJdbc::SQLite3

    def adapter_spec(config)
      # return nil to avoid extending ArJdbc::SQLite3, which we've already done
    end

    def jdbc_connection_class(spec)
      ::ArJdbc::SQLite3.jdbc_connection_class
    end

    def jdbc_column_class
      ActiveRecord::ConnectionAdapters::SQLite3Column
    end

    alias_chained_method :columns, :query_cache, :jdbc_columns
  end

  SQLiteAdapter = SQLite3Adapter
end

# Fake out sqlite3/version driver for AR tests
$LOADED_FEATURES << 'sqlite3/version.rb'
module SQLite3
  module Version
    VERSION = '1.2.6' # query_cache_test.rb requires SQLite3::Version::VERSION > '1.2.5'
  end
end
