require 'active_record/connection_adapters/abstract/schema_definitions'

module ::JdbcSpec
  # Don't need to load native mysql adapter
  $LOADED_FEATURES << "active_record/connection_adapters/mysql_adapter.rb"

  module ActiveRecordExtensions
    def mysql_connection(config)
      config[:port] ||= 3306
      if config[:url]
        config[:url] = config[:url]['?'] ? "#{config[:url]}&#{MySQL::URL_OPTIONS}" : "#{config[:url]}?#{MySQL::URL_OPTIONS}"
      else
        config[:url] = "jdbc:mysql://#{config[:host]}:#{config[:port]}/#{config[:database]}?#{MySQL::URL_OPTIONS}"
      end
      config[:driver] = "com.mysql.jdbc.Driver"
      jdbc_connection(config)
    end
  end

  module MySQL
    URL_OPTIONS = "zeroDateTimeBehavior=convertToNull&jdbcCompliantTruncation=false&useUnicode=true&characterEncoding=utf8"
    def self.column_selector
      [/mysql/i, lambda {|cfg,col| col.extend(::JdbcSpec::MySQL::Column)}]
    end

    def self.adapter_selector
      [/mysql/i, lambda {|cfg,adapt| adapt.extend(::JdbcSpec::MySQL)}]
    end

    def self.extended(adapter)
      adapter.execute("SET SQL_AUTO_IS_NULL=0")
    end

    module Column
      TYPES_ALLOWING_EMPTY_STRING_DEFAULT = Set.new([:binary, :string, :text])

      def simplified_type(field_type)
        return :boolean if field_type =~ /tinyint\(1\)|bit/i
        return :string  if field_type =~ /enum/i
        super
      end

      def init_column(name, default, *args)
        @original_default = default
        @default = nil if missing_default_forged_as_empty_string?
      end

      # MySQL misreports NOT NULL column default when none is given.
      # We can't detect this for columns which may have a legitimate ''
      # default (string, text, binary) but we can for others (integer,
      # datetime, boolean, and the rest).
      #
      # Test whether the column has default '', is not null, and is not
      # a type allowing default ''.
      def missing_default_forged_as_empty_string?
        !null && @original_default == '' && !TYPES_ALLOWING_EMPTY_STRING_DEFAULT.include?(type)
      end
    end

    def modify_types(tp)
      tp[:primary_key] = "int(11) DEFAULT NULL auto_increment PRIMARY KEY"
      tp[:decimal] = { :name => "decimal" }
      tp[:timestamp] = { :name => "datetime" }
      tp[:datetime][:limit] = nil
      tp
    end

    # QUOTING ==================================================

    def quote(value, column = nil)
      return value.quoted_id if value.respond_to?(:quoted_id)

      if column && column.type == :primary_key
        value.to_s
      elsif column && String === value && column.type == :binary && column.class.respond_to?(:string_to_binary)
        s = column.class.string_to_binary(value).unpack("H*")[0]
        "x'#{s}'"
      elsif BigDecimal === value
        "'#{value.to_s("F")}'"
      else
        super
      end
    end

    def quote_column_name(name) #:nodoc:
        "`#{name}`"
    end

    def quote_table_name(name) #:nodoc:
      quote_column_name(name).gsub('.', '`.`')
    end

    def quoted_true
        "1"
    end

    def quoted_false
        "0"
    end

    def begin_db_transaction #:nodoc:
      @connection.begin
    rescue Exception
      # Transactions aren't supported
    end

    def commit_db_transaction #:nodoc:
      @connection.commit
    rescue Exception
      # Transactions aren't supported
    end

    def rollback_db_transaction #:nodoc:
      @connection.rollback
    rescue Exception
      # Transactions aren't supported
    end

    def disable_referential_integrity(&block) #:nodoc:
      old = select_value("SELECT @@FOREIGN_KEY_CHECKS")
      begin
        update("SET FOREIGN_KEY_CHECKS = 0")
        yield
      ensure
        update("SET FOREIGN_KEY_CHECKS = #{old}")
      end
    end

    # SCHEMA STATEMENTS ========================================

    def structure_dump #:nodoc:
      if supports_views?
        sql = "SHOW FULL TABLES WHERE Table_type = 'BASE TABLE'"
      else
        sql = "SHOW TABLES"
      end

      select_all(sql).inject("") do |structure, table|
        table.delete('Table_type')

        hash = select_one("SHOW CREATE TABLE #{quote_table_name(table.to_a.first.last)}")

        if(table = hash["Create Table"])
          structure += table + ";\n\n"
        elsif(view = hash["Create View"])
          structure += view + ";\n\n"
        end
      end
    end

    def recreate_database(name) #:nodoc:
      drop_database(name)
      create_database(name)
    end

    def create_database(name, options = {}) #:nodoc:
      if options[:collation]
        execute "CREATE DATABASE `#{name}` DEFAULT CHARACTER SET `#{options[:charset] || 'utf8'}` COLLATE `#{options[:collation]}`"
      else
        execute "CREATE DATABASE `#{name}` DEFAULT CHARACTER SET `#{options[:charset] || 'utf8'}`"
      end
    end

    def drop_database(name) #:nodoc:
      execute "DROP DATABASE IF EXISTS `#{name}`"
    end

    def current_database
      select_one("SELECT DATABASE() as db")["db"]
    end

    def create_table(name, options = {}) #:nodoc:
      super(name, {:options => "ENGINE=InnoDB CHARACTER SET utf8 COLLATE utf8_bin"}.merge(options))
    end

    def rename_table(name, new_name)
      execute "RENAME TABLE #{quote_table_name(name)} TO #{quote_table_name(new_name)}"
    end

    def change_column_default(table_name, column_name, default) #:nodoc:
      current_type = select_one("SHOW COLUMNS FROM #{quote_table_name(table_name)} LIKE '#{column_name}'")["Type"]

      execute("ALTER TABLE #{quote_table_name(table_name)} CHANGE #{quote_column_name(column_name)} #{quote_column_name(column_name)} #{current_type} DEFAULT #{quote(default)}")
    end

    def change_column(table_name, column_name, type, options = {}) #:nodoc:
      unless options_include_default?(options)
        if column = columns(table_name).find { |c| c.name == column_name.to_s }
          options[:default] = column.default
        else
          raise "No such column: #{table_name}.#{column_name}"
        end
      end

      change_column_sql = "ALTER TABLE #{quote_table_name(table_name)} CHANGE #{quote_column_name(column_name)} #{quote_column_name(column_name)} #{type_to_sql(type, options[:limit], options[:precision], options[:scale])}"
      add_column_options!(change_column_sql, options)
      execute(change_column_sql)
    end

    def rename_column(table_name, column_name, new_column_name) #:nodoc:
      cols = select_one("SHOW COLUMNS FROM #{quote_table_name(table_name)} LIKE '#{column_name}'")
      current_type = cols["Type"] || cols["COLUMN_TYPE"]
      execute "ALTER TABLE #{quote_table_name(table_name)} CHANGE #{quote_table_name(column_name)} #{quote_column_name(new_column_name)} #{current_type}"
    end

    def add_limit_offset!(sql, options) #:nodoc:
      if limit = options[:limit]
        unless offset = options[:offset]
          sql << " LIMIT #{limit}"
        else
          sql << " LIMIT #{offset}, #{limit}"
        end
      end
    end

    def show_variable(var)
      res = execute("show variables like '#{var}'")
      row = res.detect {|row| row["Variable_name"] == var }
      row && row["Value"]
    end

    def charset
      show_variable("character_set_database")
    end

    def collation
      show_variable("collation_database")
    end

    private
    def supports_views?
      false
    end
  end
end
