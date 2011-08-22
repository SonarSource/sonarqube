module ::JdbcSpec
  module ActiveRecordExtensions
    def sqlite3_connection(config)
      config[:url] ||= "jdbc:sqlite:#{config[:database]}"
      config[:driver] ||= "org.sqlite.JDBC"
      jdbc_connection(config)
    end
  end

  module SQLite3
    def self.column_selector
      [/sqlite/i, lambda {|cfg,col| col.extend(::JdbcSpec::SQLite3::Column)}]
    end

    def self.adapter_selector
      [/sqlite/i, lambda {|cfg,adapt| adapt.extend(::JdbcSpec::SQLite3)}]
    end

    module Column

      private
      def simplified_type(field_type)
        case field_type
        when /^integer\(1\)$/i                  then :boolean
        when /text/i                           then :string
        when /int/i                            then :integer
        when /real/i                   then @scale == 0 ? :integer : :decimal
        when /date|time/i                      then :datetime
        when /blob/i                           then :binary
        end
      end

      def self.cast_to_date_or_time(value)
        return value if value.is_a? Date
        return nil if value.blank?
        guess_date_or_time((value.is_a? Time) ? value : cast_to_time(value))
      end

      def self.cast_to_time(value)
        return value if value.is_a? Time
        Time.at(value) rescue nil
      end

      def self.guess_date_or_time(value)
        (value.hour == 0 and value.min == 0 and value.sec == 0) ?
        Date.new(value.year, value.month, value.day) : value
      end
    end

    def type_cast(value)
      return nil if value.nil?
      case type
      when :string   then value
      when :integer  then defined?(value.to_i) ? value.to_i : (value ? 1 : 0)
      when :primary_key then defined?(value.to_i) ? value.to_i : (value ? 1 : 0)
      when :float    then value.to_f
      when :datetime then JdbcSpec::SQLite3::Column.cast_to_date_or_time(value)
      when :time     then JdbcSpec::SQLite3::Column.cast_to_time(value)
      when :decimal   then self.class.value_to_decimal(value)
      when :boolean   then self.class.value_to_boolean(value)
      else value
      end
    end

    def modify_types(tp)
      tp[:primary_key] = "INTEGER PRIMARY KEY AUTOINCREMENT"
      tp[:float] = { :name => "REAL" }
      tp[:decimal] = { :name => "REAL" }
      tp[:datetime] = { :name => "INTEGER" }
      tp[:timestamp] = { :name => "INTEGER" }
      tp[:time] = { :name => "INTEGER" }
      tp[:date] = { :name => "INTEGER" }
      tp[:boolean] = { :name => "INTEGER", :limit => 1}
      tp
    end

    def quote(value, column = nil) # :nodoc:
      return value.quoted_id if value.respond_to?(:quoted_id)

      case value
      when String
        if column && column.type == :binary
          "'#{quote_string(value).unpack("C*").collect {|v| v.to_s(16)}.join}'"
        else
          "'#{quote_string(value)}'"
        end
      else super
      end
    end

    def quote_string(str)
      str.gsub(/'/, "''")
    end

    def quoted_true
      '1'
    end

    def quoted_false
      '0'
    end

    def add_column(table_name, column_name, type, options = {})
      if option_not_null = options[:null] == false
        option_not_null = options.delete(:null)
      end
      add_column_sql = "ALTER TABLE #{quote_table_name(table_name)} ADD #{quote_column_name(column_name)} #{type_to_sql(type, options[:limit], options[:precision], options[:scale])}"
      add_column_options!(add_column_sql, options)
      execute(add_column_sql)
      if option_not_null
        alter_column_sql = "ALTER TABLE #{quote_table_name(table_name)} ALTER #{quote_column_name(column_name)} NOT NULL"
      end
    end

    def remove_column(table_name, column_name) #:nodoc:
      cols = columns(table_name).collect {|col| col.name}
      cols.delete(column_name)
      cols = cols.join(', ')
      table_backup = table_name + "_backup"

      @connection.begin

      execute "CREATE TEMPORARY TABLE #{table_backup}(#{cols})"
      insert "INSERT INTO #{table_backup} SELECT #{cols} FROM #{table_name}"
      execute "DROP TABLE #{table_name}"
      execute "CREATE TABLE #{table_name}(#{cols})"
      insert "INSERT INTO #{table_name} SELECT #{cols} FROM #{table_backup}"
      execute "DROP TABLE #{table_backup}"

      @connection.commit
    end

    def change_column(table_name, column_name, type, options = {}) #:nodoc:
      execute "ALTER TABLE #{table_name} ALTER COLUMN #{column_name} #{type_to_sql(type, options[:limit])}"
    end

    def change_column_default(table_name, column_name, default) #:nodoc:
      execute "ALTER TABLE #{table_name} ALTER COLUMN #{column_name} SET DEFAULT #{quote(default)}"
    end

    def rename_column(table_name, column_name, new_column_name) #:nodoc:
      execute "ALTER TABLE #{table_name} ALTER COLUMN #{column_name} RENAME TO #{new_column_name}"
    end

    def rename_table(name, new_name)
      execute "ALTER TABLE #{name} RENAME TO #{new_name}"
    end

    def insert(sql, name = nil, pk = nil, id_value = nil, sequence_name = nil) #:nodoc:
      log(sql,name) do
        @connection.execute_update(sql)
      end
      table = sql.split(" ", 4)[2]
      id_value || last_insert_id(table, nil)
    end

    def last_insert_id(table, sequence_name)
      Integer(select_value("SELECT SEQ FROM SQLITE_SEQUENCE WHERE NAME = '#{table}'"))
    end

    def add_limit_offset!(sql, options) #:nodoc:
      if options[:limit]
        sql << " LIMIT #{options[:limit]}"
        sql << " OFFSET #{options[:offset]}" if options[:offset]
      end
    end

    def tables
      @connection.tables.select {|row| row.to_s !~ /^sqlite_/i }
    end

    def remove_index(table_name, options = {})
      execute "DROP INDEX #{quote_column_name(index_name(table_name, options))}"
    end

    def indexes(table_name, name = nil)
      result = select_rows("SELECT name, sql FROM sqlite_master WHERE tbl_name = '#{table_name}' AND type = 'index'", name)

      result.collect do |row|
        name = row[0]
        index_sql = row[1]
        unique = (index_sql =~ /unique/i)
        cols = index_sql.match(/\((.*)\)/)[1].gsub(/,/,' ').split
        ::ActiveRecord::ConnectionAdapters::IndexDefinition.new(table_name, name, unique, cols)
      end
    end
  end
end
