module ArJdbc
  module Mimer
    def self.extended(mod)
      require 'arjdbc/jdbc/quoted_primary_key'
      ActiveRecord::Base.extend ArJdbc::QuotedPrimaryKeyExtension
    end

    def modify_types(tp)
      tp[:primary_key] = "INTEGER NOT NULL PRIMARY KEY"
      tp[:boolean][:limit] = nil
      tp[:string][:limit] = 255
      tp[:binary] = {:name => "BINARY VARYING", :limit => 4096}
      tp[:text] = {:name => "VARCHAR", :limit => 4096}
      tp[:datetime] = { :name => "TIMESTAMP" }
      tp[:timestamp] = { :name => "TIMESTAMP" }
      tp[:time] = { :name => "TIMESTAMP" }
      tp[:date] = { :name => "TIMESTAMP" }
      tp
    end

    def default_sequence_name(table, column) #:nodoc:
      "#{table}_seq"
    end

    def create_table(name, options = {}) #:nodoc:
      super(name, options)
      execute "CREATE SEQUENCE #{name}_seq" unless options[:id] == false
    end

    def drop_table(name, options = {}) #:nodoc:
      super(name) rescue nil
      execute "DROP SEQUENCE #{name}_seq" rescue nil
    end

    def change_column(table_name, column_name, type, options = {}) #:nodoc:
      execute "ALTER TABLE #{table_name} ALTER COLUMN #{column_name} #{type_to_sql(type, options[:limit])}"
    end

    def change_column_default(table_name, column_name, default) #:nodoc:
      execute "ALTER TABLE #{table_name} ALTER COLUMN #{column_name} SET DEFAULT #{quote(default)}"
    end

    def remove_index(table_name, options = {}) #:nodoc:
      execute "DROP INDEX #{index_name(table_name, options)}"
    end

    def insert(sql, name = nil, pk = nil, id_value = nil, sequence_name = nil) #:nodoc:
      if pk.nil? # Who called us? What does the sql look like? No idea!
        execute sql, name
      elsif id_value # Pre-assigned id
        log(sql, name) { @connection.execute_insert sql,pk }
      else # Assume the sql contains a bind-variable for the id
        id_value = select_one("SELECT NEXT_VALUE OF #{sequence_name} AS val FROM MIMER.ONEROW")['val']
        log(sql, name) {
          execute_prepared_insert(sql,id_value)
        }
      end
      id_value
    end

    def execute_prepared_insert(sql, id)
      @stmts ||= {}
      @stmts[sql] ||= @connection.ps(sql)
      stmt = @stmts[sql]
      stmt.setLong(1,id)
      stmt.executeUpdate
      id
    end

    def quote(value, column = nil) #:nodoc:
      return value.quoted_id if value.respond_to?(:quoted_id)

      if String === value && column && column.type == :binary
        return "X'#{quote_string(value.unpack("C*").collect {|v| v.to_s(16)}.join)}'"
      end
      case value
      when String
        %Q{'#{quote_string(value)}'}
      when NilClass
        'NULL'
      when TrueClass
        '1'
      when FalseClass
        '0'
      when Numeric
        value.to_s
      when Date, Time
        %Q{TIMESTAMP '#{value.strftime("%Y-%m-%d %H:%M:%S")}'}
      else
        %Q{'#{quote_string(value.to_yaml)}'}
      end
    end

    def quoted_true
      '1'
    end

    def quoted_false
      '0'
    end

    def add_limit_offset!(sql, options) # :nodoc:
      @limit = options[:limit]
      @offset = options[:offset]
    end

    def select_all(sql, name = nil)
      @offset ||= 0
      if !@limit || @limit == -1
        range = @offset..-1
      else
        range = @offset...(@offset+@limit)
      end
      select(sql, name)[range]
    ensure
      @limit = @offset = nil
    end

    def select_one(sql, name = nil)
      @offset ||= 0
      select(sql, name)[@offset]
    ensure
      @limit = @offset = nil
    end

    def _execute(sql, name = nil)
        if sql =~ /^select/i
          @offset ||= 0
          if !@limit || @limit == -1
            range = @offset..-1
          else
            range = @offset...(@offset+@limit)
          end
          @connection.execute_query(sql)[range]
        else
          @connection.execute_update(sql)
        end
    ensure
      @limit = @offset = nil
    end
  end
end
