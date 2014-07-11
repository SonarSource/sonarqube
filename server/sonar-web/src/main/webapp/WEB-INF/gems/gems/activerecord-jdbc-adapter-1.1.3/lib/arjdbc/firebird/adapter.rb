module ::ArJdbc
  module FireBird

    def self.extended(mod)
      unless @lob_callback_added
        ActiveRecord::Base.class_eval do
          def after_save_with_firebird_blob
            self.class.columns.select { |c| c.sql_type =~ /blob/i }.each do |c|
              value = self[c.name]
              value = value.to_yaml if unserializable_attribute?(c.name, c)
              next if value.nil?
              connection.write_large_object(c.type == :binary, c.name, self.class.table_name, self.class.primary_key, quote_value(id), value)
            end
          end
        end

        ActiveRecord::Base.after_save :after_save_with_firebird_blob
        @lob_callback_added = true
      end
    end

    def adapter_name
      'Firebird'
    end

    def arel2_visitors
      require 'arel/visitors/firebird'
      {'firebird' => ::Arel::Visitors::Firebird, 'firebirdsql' => ::Arel::Visitors::Firebird}
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

      # BLOBs are updated separately by an after_save trigger.
      return value.nil? ? "NULL" : "'#{quote_string(value[0..1])}'" if column && [:binary, :text].include?(column.type)

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
