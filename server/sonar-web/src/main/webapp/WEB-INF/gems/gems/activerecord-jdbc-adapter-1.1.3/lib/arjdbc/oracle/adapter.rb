module ActiveRecord::ConnectionAdapters
  OracleAdapter = Class.new(AbstractAdapter) unless const_defined?(:OracleAdapter)
end

module ::ArJdbc
  module Oracle
    def self.extended(mod)
      unless @lob_callback_added
        ActiveRecord::Base.class_eval do
          def after_save_with_oracle_lob
            self.class.columns.select { |c| c.sql_type =~ /LOB\(|LOB$/i }.each do |c|
              value = self[c.name]
              value = value.to_yaml if unserializable_attribute?(c.name, c)
              next if value.nil?  || (value == '')

              connection.write_large_object(c.type == :binary, c.name, self.class.table_name, self.class.primary_key, quote_value(id), value)
            end
          end
        end

        ActiveRecord::Base.after_save :after_save_with_oracle_lob
        @lob_callback_added = true
      end
      require 'arjdbc/jdbc/quoted_primary_key'
      ActiveRecord::Base.extend ArJdbc::QuotedPrimaryKeyExtension
      (class << mod; self; end).class_eval do
        alias_chained_method :insert, :query_dirty, :ora_insert
        alias_chained_method :columns, :query_cache, :ora_columns
      end
    end

    def self.column_selector
      [/oracle/i, lambda {|cfg,col| col.extend(::ArJdbc::Oracle::Column)}]
    end

    def self.jdbc_connection_class
      ::ActiveRecord::ConnectionAdapters::OracleJdbcConnection
    end

    module Column
      def primary=(val)
        super
        if val && @sql_type =~ /^NUMBER$/i
          @type = :integer
        end
      end

      #sonar
      def type_cast(value)
        return nil if value.nil?
        case type
          when :datetime then ArJdbc::Oracle::Column.string_to_time(value, self.class)
        else
          super
        end
      end

      def self.string_to_time(string, klass)
        time = klass.string_to_time(string)
        guess_date_or_time(time)
      end

      def self.guess_date_or_time(value)
        return value if Date === value
        (value && value.hour == 0 && value.min == 0 && value.sec == 0) ?
            Date.new(value.year, value.month, value.day) : value
      end

      def self.string_to_date(string) #:nodoc:
        return string.to_date if string.is_a?(Time) || string.is_a?(DateTime)
        super
      end
      #/sonar

      private
      def simplified_type(field_type)
        case field_type
        when /^number\(1\)$/i                  then :boolean
        when /char/i                           then :string
        when /float|double/i                   then :float
        when /int/i                            then :integer
        when /num|dec|real/i                   then extract_scale(field_type) == 0 ? :integer : :decimal
        when /date|time/i                      then :datetime
        when /clob/i                           then :text
        when /blob/i                           then :binary
        end
      end

      # Post process default value from JDBC into a Rails-friendly format (columns{-internal})
      def default_value(value)
        return nil unless value

        # Not sure why we need this for Oracle?
        value = value.strip

        return nil if value == "null"

        # sysdate default should be treated like a null value
        return nil if value.downcase == "sysdate"

        # jdbc returns column default strings with actual single quotes around the value.
        return $1 if value =~ /^'(.*)'$/

        value
      end
    end

    def adapter_name
      'Oracle'
    end

    def arel2_visitors
      { 'oracle' => Arel::Visitors::Oracle }
    end

    # TODO: use this instead of the QuotedPrimaryKey logic and execute_id_insert?
    # def prefetch_primary_key?(table_name = nil)
    #   columns(table_name).detect {|c| c.primary } if table_name
    # end

    def table_alias_length
      30
    end

    def default_sequence_name(table, column = nil) #:nodoc:
      "#{table}_seq"
    end

    def create_table(name, options = {}) #:nodoc:
      super(name, options)
      seq_name = options[:sequence_name] || default_sequence_name(name)
      start_value = options[:sequence_start_value] || 10000
      raise ActiveRecord::StatementInvalid.new("name #{seq_name} too long") if seq_name.length > table_alias_length
      execute "CREATE SEQUENCE #{seq_name} START WITH #{start_value}" unless options[:id] == false
    end

    def rename_table(name, new_name) #:nodoc:
      execute "RENAME #{name} TO #{new_name}"
      execute "RENAME #{name}_seq TO #{new_name}_seq" rescue nil
    end

    def drop_table(name, options = {}) #:nodoc:
      super(name) rescue nil
      seq_name = options[:sequence_name] || default_sequence_name(name)
      execute "DROP SEQUENCE #{seq_name}" rescue nil
    end

    def recreate_database(name)
      tables.each{ |table| drop_table(table) }
    end

    def drop_database(name)
      recreate_database(name)
    end

    def next_sequence_value(sequence_name)
      # avoid #select or #select_one so that the sequence values aren't cached
      execute("select #{sequence_name}.nextval id from dual").first['id'].to_i
    end

    def sql_literal?(value)
      # SonarQube
      # See http://jira.sonarsource.com/browse/SONAR-4994
      # This code always returns false (we use ActiveRecord 2.3) and is heavy-CPU consuming
      # because of raised exceptions.
      #defined?(::Arel::SqlLiteral) && ::Arel::SqlLiteral === value
      false
      # /SonarQube
    end

    def ora_insert(sql, name = nil, pk = nil, id_value = nil, sequence_name = nil) #:nodoc:
      if (id_value && !sql_literal?(id_value)) || pk.nil?
        # Pre-assigned id or table without a primary key
        # Presence of #to_sql means an Arel literal bind variable
        # that should use #execute_id_insert below
        execute sql, name
      else
        # Assume the sql contains a bind-variable for the id
        # Extract the table from the insert sql. Yuck.
        table = sql.split(" ", 4)[2].gsub('"', '')
        sequence_name ||= default_sequence_name(table)
        id_value = next_sequence_value(sequence_name)
        log(sql, name) do
          @connection.execute_id_insert(sql,id_value)
        end
      end
      id_value
    end

    def indexes(table, name = nil)
      @connection.indexes(table, name, @connection.connection.meta_data.user_name)
    end

    def _execute(sql, name = nil)
      case sql.strip
        when /\A\(?\s*(select|show)/i then
          @connection.execute_query(sql)
        else
          @connection.execute_update(sql)
        end
    end

    def modify_types(tp)
      tp[:primary_key] = "NUMBER(38) NOT NULL PRIMARY KEY"
      tp[:integer] = { :name => "NUMBER", :limit => 38 }
      tp[:time] = { :name => "DATE" }
      tp[:date] = { :name => "DATE" }

      # sonar
      tp[:datetime]={ :name => "TIMESTAMP" }
      tp[:timestamp]={ :name => "TIMESTAMP" }
      tp[:boolean]={ :name => "NUMBER", :limit => 1 }
      tp[:big_integer]={ :name => "NUMBER", :limit => 38 }
      #/sonar
      tp
    end

    def add_limit_offset!(sql, options) #:nodoc:
      offset = options[:offset] || 0

      if limit = options[:limit]
        sql.replace "select * from (select raw_sql_.*, rownum raw_rnum_ from (#{sql}) raw_sql_ where rownum <= #{offset+limit}) where raw_rnum_ > #{offset}"
      elsif offset > 0
        sql.replace "select * from (select raw_sql_.*, rownum raw_rnum_ from (#{sql}) raw_sql_) where raw_rnum_ > #{offset}"
      end
    end

    def current_database #:nodoc:
      select_one("select sys_context('userenv','db_name') db from dual")["db"]
    end

    def remove_index(table_name, options = {}) #:nodoc:
      execute "DROP INDEX #{index_name(table_name, options)}"
    end

    def change_column_default(table_name, column_name, default) #:nodoc:
      execute "ALTER TABLE #{table_name} MODIFY #{column_name} DEFAULT #{quote(default)}"
    end

    def add_column_options!(sql, options) #:nodoc:
      # handle case  of defaults for CLOB columns, which would otherwise get "quoted" incorrectly
      if options_include_default?(options) && (column = options[:column]) && column.type == :text
        sql << " DEFAULT #{quote(options.delete(:default))}"
      end
      super
    end

    def change_column(table_name, column_name, type, options = {}) #:nodoc:
      change_column_sql = "ALTER TABLE #{table_name} MODIFY #{column_name} #{type_to_sql(type, options[:limit])}"
      add_column_options!(change_column_sql, options)
      execute(change_column_sql)
    end

    def rename_column(table_name, column_name, new_column_name) #:nodoc:
      execute "ALTER TABLE #{table_name} RENAME COLUMN #{column_name} to #{new_column_name}"
    end

    def remove_column(table_name, column_name) #:nodoc:
      execute "ALTER TABLE #{table_name} DROP COLUMN #{column_name}"
    end

    def structure_dump #:nodoc:
      s = select_all("select sequence_name from user_sequences").inject("") do |structure, seq|
        structure << "create sequence #{seq.to_a.first.last};\n\n"
      end

      select_all("select table_name from user_tables").inject(s) do |structure, table|
        ddl = "create table #{table.to_a.first.last} (\n "
        cols = select_all(%Q{
              select column_name, data_type, data_length, data_precision, data_scale, data_default, nullable
              from user_tab_columns
              where table_name = '#{table.to_a.first.last}'
              order by column_id
            }).map do |row|
          row = row.inject({}) do |h,args|
            h[args[0].downcase] = args[1]
            h
          end
          col = "#{row['column_name'].downcase} #{row['data_type'].downcase}"
          if row['data_type'] =='NUMBER' and !row['data_precision'].nil?
            col << "(#{row['data_precision'].to_i}"
            col << ",#{row['data_scale'].to_i}" if !row['data_scale'].nil?
            col << ')'
          elsif row['data_type'].include?('CHAR')
            col << "(#{row['data_length'].to_i})"
          end
          col << " default #{row['data_default']}" if !row['data_default'].nil?
          col << ' not null' if row['nullable'] == 'N'
          col
        end
        ddl << cols.join(",\n ")
        ddl << ");\n\n"
        structure << ddl
      end
    end

    def structure_drop #:nodoc:
      s = select_all("select sequence_name from user_sequences").inject("") do |drop, seq|
        drop << "drop sequence #{seq.to_a.first.last};\n\n"
      end

      select_all("select table_name from user_tables").inject(s) do |drop, table|
        drop << "drop table #{table.to_a.first.last} cascade constraints;\n\n"
      end
    end

    # SELECT DISTINCT clause for a given set of columns and a given ORDER BY clause.
    #
    # Oracle requires the ORDER BY columns to be in the SELECT list for DISTINCT
    # queries. However, with those columns included in the SELECT DISTINCT list, you
    # won't actually get a distinct list of the column you want (presuming the column
    # has duplicates with multiple values for the ordered-by columns. So we use the
    # FIRST_VALUE function to get a single (first) value for each column, effectively
    # making every row the same.
    #
    #   distinct("posts.id", "posts.created_at desc")
    def distinct(columns, order_by)
      return "DISTINCT #{columns}" if order_by.blank?

      # construct a valid DISTINCT clause, ie. one that includes the ORDER BY columns, using
      # FIRST_VALUE such that the inclusion of these columns doesn't invalidate the DISTINCT
      order_columns = order_by.split(',').map { |s| s.strip }.reject(&:blank?)
      order_columns = order_columns.zip((0...order_columns.size).to_a).map do |c, i|
        "FIRST_VALUE(#{c.split.first}) OVER (PARTITION BY #{columns} ORDER BY #{c}) AS alias_#{i}__"
      end
      sql = "DISTINCT #{columns}, "
      sql << order_columns * ", "
    end

    # ORDER BY clause for the passed order option.
    #
    # Uses column aliases as defined by #distinct.
    def add_order_by_for_association_limiting!(sql, options)
      return sql if options[:order].blank?

      order = options[:order].split(',').collect { |s| s.strip }.reject(&:blank?)
      order.map! {|s| $1 if s =~ / (.*)/}
      order = order.zip((0...order.size).to_a).map { |s,i| "alias_#{i}__ #{s}" }.join(', ')

      sql << "ORDER BY #{order}"
    end

    def tables
      @connection.tables(nil, oracle_schema)
    end

    def ora_columns(table_name, name=nil)
      @connection.columns_internal(table_name, name, oracle_schema)
    end

    # QUOTING ==================================================
    #
    # see: abstract/quoting.rb

    # See ACTIVERECORD_JDBC-33 for details -- better to not quote
    # table names, esp. if they have schemas.
    def quote_table_name(name) #:nodoc:
      name.to_s
    end

    # Camelcase column names need to be quoted.
    # Nonquoted identifiers can contain only alphanumeric characters from your
    # database character set and the underscore (_), dollar sign ($), and pound sign (#).
    # Database links can also contain periods (.) and "at" signs (@).
    # Oracle strongly discourages you from using $ and # in nonquoted identifiers.
    # Source: http://download.oracle.com/docs/cd/B28359_01/server.111/b28286/sql_elements008.htm
    def quote_column_name(name) #:nodoc:
      name.to_s =~ /^[a-z0-9_$#]+$/ ? name.to_s : "\"#{name}\""
    end

    def quote_string(string) #:nodoc:
      string.gsub(/'/, "''")
    end

    def quote(value, column = nil) #:nodoc:
      # Arel 2 passes SqlLiterals through
      return value if sql_literal?(value)

      if column && [:text, :binary].include?(column.type)
        if /(.*?)\([0-9]+\)/ =~ column.sql_type
          %Q{empty_#{ $1.downcase }()}
        else
          %Q{empty_#{ column.sql_type.downcase rescue 'blob' }()}
        end
      else
        if column.respond_to?(:primary) && column.primary && column.klass != String
          return value.to_i.to_s
        end
        quoted = super
        #sonar
        if value.acts_like?(:date)
          quoted = "TO_DATE('#{value.strftime('%Y-%m-%d')}','YYYY-MM-DD')"
        elsif value.acts_like?(:time) || value.acts_like?(:timestamp)
          quoted = "TO_TIMESTAMP('#{value.strftime('%Y-%m-%d %H:%M:%S')}:#{("%.6f"%value.to_f).split('.')[1]}','YYYY-MM-DD HH24:MI:SS:FF6')"
        end
        #/sonar
        quoted
      end
    end

    def quoted_true #:nodoc:
      '1'
    end

    def quoted_false #:nodoc:
      '0'
    end

    private
    # In Oracle, schemas are usually created under your username:
    # http://www.oracle.com/technology/obe/2day_dba/schema/schema.htm
    # But allow separate configuration as "schema:" anyway (GH #53)
    def oracle_schema
      if @config[:schema]
        @config[:schema].to_s
      elsif @config[:username]
        @config[:username].to_s
      end
    end

    def select(sql, name=nil)
      records = execute(sql,name)
      records.each do |col|
          col.delete('raw_rnum_')
      end
      records
    end
  end
end

