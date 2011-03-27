module ::JdbcSpec
  module FireBird
    def self.adapter_selector
      [/firebird/i, lambda{|cfg,adapt| adapt.extend(::JdbcSpec::FireBird)}]
    end

    def modify_types(tp)
      tp[:primary_key] = 'INTEGER NOT NULL PRIMARY KEY'
      tp[:string][:limit] = 252
      tp[:integer][:limit] = nil
      tp
    end
    
    def insert(sql, name = nil, pk = nil, id_value = nil, sequence_name = nil) # :nodoc:
      execute(sql, name)
      id_value
    end

    def add_limit_offset!(sql, options) # :nodoc:
      if options[:limit]
        limit_string = "FIRST #{options[:limit]}"
        limit_string << " SKIP #{options[:offset]}" if options[:offset]
        sql.sub!(/\A(\s*SELECT\s)/i, '\&' + limit_string + ' ')
      end
    end

    def prefetch_primary_key?(table_name = nil)
      true
    end

    def default_sequence_name(table_name, primary_key) # :nodoc:
      "#{table_name}_seq"
    end
    
    def next_sequence_value(sequence_name)
      select_one("SELECT GEN_ID(#{sequence_name}, 1 ) FROM RDB$DATABASE;")["gen_id"]
    end
    
    def create_table(name, options = {}) #:nodoc:
      super(name, options)
      execute "CREATE GENERATOR #{name}_seq"
    end

    def rename_table(name, new_name) #:nodoc:
      execute "RENAME #{name} TO #{new_name}"
      execute "UPDATE RDB$GENERATORS SET RDB$GENERATOR_NAME='#{new_name}_seq' WHERE RDB$GENERATOR_NAME='#{name}_seq'" rescue nil
    end  

    def drop_table(name, options = {}) #:nodoc:
      super(name)
      execute "DROP GENERATOR #{name}_seq" rescue nil
    end

    def change_column(table_name, column_name, type, options = {}) #:nodoc:
      execute "ALTER TABLE #{table_name} ALTER  #{column_name} TYPE #{type_to_sql(type, options[:limit])}"
    end

    def rename_column(table_name, column_name, new_column_name)
      execute "ALTER TABLE #{table_name} ALTER  #{column_name} TO #{new_column_name}"
    end

    def remove_index(table_name, options) #:nodoc:
      execute "DROP INDEX #{index_name(table_name, options)}"
    end
    
    def quote(value, column = nil) # :nodoc:
      return value.quoted_id if value.respond_to?(:quoted_id)
      
      if [Time, DateTime].include?(value.class)
        "CAST('#{value.strftime("%Y-%m-%d %H:%M:%S")}' AS TIMESTAMP)"
      else
        if column && column.type == :primary_key
          return value.to_s
        end
        super
      end
    end

    def quote_string(string) # :nodoc:
      string.gsub(/'/, "''")
    end
    
    def quote_column_name(column_name) # :nodoc:
      %Q("#{ar_to_fb_case(column_name)}")
    end
    
    def quoted_true # :nodoc:
      quote(1)
    end
    
    def quoted_false # :nodoc:
      quote(0)
    end

    private
    
    # Maps uppercase Firebird column names to lowercase for ActiveRecord;
    # mixed-case columns retain their original case.
    def fb_to_ar_case(column_name)
      column_name =~ /[[:lower:]]/ ? column_name : column_name.to_s.downcase
    end
    
    # Maps lowercase ActiveRecord column names to uppercase for Fierbird;
    # mixed-case columns retain their original case.
    def ar_to_fb_case(column_name)
      column_name =~ /[[:upper:]]/ ? column_name : column_name.to_s.upcase
    end
  end
end
