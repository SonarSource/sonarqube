module ::JdbcSpec
  # Don't need to load native postgres adapter
  $LOADED_FEATURES << "active_record/connection_adapters/postgresql_adapter.rb"

  module ActiveRecordExtensions
    def postgresql_connection(config)
      config[:host] ||= "localhost"
      config[:port] ||= 5432
      config[:url] ||= "jdbc:postgresql://#{config[:host]}:#{config[:port]}/#{config[:database]}"
      config[:url] << config[:pg_params] if config[:pg_params]
      config[:driver] ||= "org.postgresql.Driver"
      jdbc_connection(config)
    end
  end

  module PostgreSQL
    def self.column_selector
      [/postgre/i, lambda {|cfg,col| col.extend(::JdbcSpec::PostgreSQL::Column)}]
    end

    def self.adapter_selector
      [/postgre/i, lambda {|cfg,adapt| adapt.extend(::JdbcSpec::PostgreSQL)}]
    end

    module Column
      def type_cast(value)
        case type
        when :boolean then cast_to_boolean(value)
        else super
        end
      end

      def simplified_type(field_type)
        return :integer if field_type =~ /^serial/i
        return :string if field_type =~ /\[\]$/i || field_type =~ /^interval/i
        return :string if field_type =~ /^(?:point|lseg|box|"?path"?|polygon|circle)/i
        return :datetime if field_type =~ /^timestamp/i
        return :float if field_type =~ /^real|^money/i
        return :binary if field_type =~ /^bytea/i
        return :boolean if field_type =~ /^bool/i
        super
      end

      def cast_to_boolean(value)
        if value == true || value == false
          value
        else
          %w(true t 1).include?(value.to_s.downcase)
        end
      end

      def cast_to_date_or_time(value)
        return value if value.is_a? Date
        return nil if value.blank?
        guess_date_or_time((value.is_a? Time) ? value : cast_to_time(value))
      end

      def cast_to_time(value)
        return value if value.is_a? Time
        time_array = ParseDate.parsedate value
        time_array[0] ||= 2000; time_array[1] ||= 1; time_array[2] ||= 1;
        Time.send(ActiveRecord::Base.default_timezone, *time_array) rescue nil
      end

      def guess_date_or_time(value)
        (value.hour == 0 and value.min == 0 and value.sec == 0) ?
        Date.new(value.year, value.month, value.day) : value
      end

      def default_value(value)
        # Boolean types
        return "t" if value =~ /true/i
        return "f" if value =~ /false/i

        # Char/String/Bytea type values
        return $1 if value =~ /^'(.*)'::(bpchar|text|character varying|bytea)$/

        # Numeric values
        return value if value =~ /^-?[0-9]+(\.[0-9]*)?/

        # Fixed dates / timestamp
        return $1 if value =~ /^'(.+)'::(date|timestamp)/

        # Anything else is blank, some user type, or some function
        # and we can't know the value of that, so return nil.
        return nil
      end
    end

    def modify_types(tp)
      tp[:primary_key] = "serial primary key"
      tp[:string][:limit] = 255
      tp[:integer][:limit] = nil
      tp[:boolean][:limit] = nil
      tp
    end

    def default_sequence_name(table_name, pk = nil)
      default_pk, default_seq = pk_and_sequence_for(table_name)
      default_seq || "#{table_name}_#{pk || default_pk || 'id'}_seq"
    end

    # Resets sequence to the max value of the table's pk if present.
    def reset_pk_sequence!(table, pk = nil, sequence = nil)
      unless pk and sequence
        default_pk, default_sequence = pk_and_sequence_for(table)
        pk ||= default_pk
        sequence ||= default_sequence
      end
      if pk
        if sequence
          select_value <<-end_sql, 'Reset sequence'
            SELECT setval('#{sequence}', (SELECT COALESCE(MAX(#{pk})+(SELECT increment_by FROM #{sequence}), (SELECT min_value FROM #{sequence})) FROM #{table}), false)
          end_sql
        else
          @logger.warn "#{table} has primary key #{pk} with no default sequence" if @logger
        end
      end
    end

    # Find a table's primary key and sequence.
    def pk_and_sequence_for(table)
      # First try looking for a sequence with a dependency on the
      # given table's primary key.
        result = select(<<-end_sql, 'PK and serial sequence')[0]
          SELECT attr.attname AS nm, name.nspname AS nsp, seq.relname AS rel
          FROM pg_class      seq,
               pg_attribute  attr,
               pg_depend     dep,
               pg_namespace  name,
               pg_constraint cons
          WHERE seq.oid           = dep.objid
            AND seq.relnamespace  = name.oid
            AND seq.relkind       = 'S'
            AND attr.attrelid     = dep.refobjid
            AND attr.attnum       = dep.refobjsubid
            AND attr.attrelid     = cons.conrelid
            AND attr.attnum       = cons.conkey[1]
            AND cons.contype      = 'p'
            AND dep.refobjid      = '#{table}'::regclass
        end_sql

        if result.nil? or result.empty?
          # If that fails, try parsing the primary key's default value.
          # Support the 7.x and 8.0 nextval('foo'::text) as well as
          # the 8.1+ nextval('foo'::regclass).
          # TODO: assumes sequence is in same schema as table.
          result = select(<<-end_sql, 'PK and custom sequence')[0]
            SELECT attr.attname AS nm, name.nspname AS nsp, split_part(def.adsrc, '\\\'', 2) AS rel
            FROM pg_class       t
            JOIN pg_namespace   name ON (t.relnamespace = name.oid)
            JOIN pg_attribute   attr ON (t.oid = attrelid)
            JOIN pg_attrdef     def  ON (adrelid = attrelid AND adnum = attnum)
            JOIN pg_constraint  cons ON (conrelid = adrelid AND adnum = conkey[1])
            WHERE t.oid = '#{table}'::regclass
              AND cons.contype = 'p'
              AND def.adsrc ~* 'nextval'
          end_sql
        end
        # check for existence of . in sequence name as in public.foo_sequence.  if it does not exist, join the current namespace
        result['rel']['.'] ? [result['nm'], result['rel']] : [result['nm'], "#{result['nsp']}.#{result['rel']}"]
      rescue
        nil
      end

    def insert(sql, name = nil, pk = nil, id_value = nil, sequence_name = nil) #:nodoc:
      execute(sql, name)
      table = sql.split(" ", 4)[2]
      id_value || pk && last_insert_id(table, sequence_name || default_sequence_name(table, pk))
    end

    def columns(table_name, name=nil)
      schema_name = "public"
      if table_name =~ /\./
        parts = table_name.split(/\./)
        table_name = parts.pop
        schema_name = parts.join(".")
      end
      @connection.columns_internal(table_name, name, schema_name)
    end

    # From postgresql_adapter.rb
    def indexes(table_name, name = nil)
      result = select_rows(<<-SQL, name)
        SELECT i.relname, d.indisunique, a.attname
          FROM pg_class t, pg_class i, pg_index d, pg_attribute a
         WHERE i.relkind = 'i'
           AND d.indexrelid = i.oid
           AND d.indisprimary = 'f'
           AND t.oid = d.indrelid
           AND t.relname = '#{table_name}'
           AND a.attrelid = t.oid
           AND ( d.indkey[0]=a.attnum OR d.indkey[1]=a.attnum
              OR d.indkey[2]=a.attnum OR d.indkey[3]=a.attnum
              OR d.indkey[4]=a.attnum OR d.indkey[5]=a.attnum
              OR d.indkey[6]=a.attnum OR d.indkey[7]=a.attnum
              OR d.indkey[8]=a.attnum OR d.indkey[9]=a.attnum )
        ORDER BY i.relname
      SQL

      current_index = nil
      indexes = []

      result.each do |row|
        if current_index != row[0]
          indexes << ::ActiveRecord::ConnectionAdapters::IndexDefinition.new(table_name, row[0], row[1] == "t", [])
          current_index = row[0]
        end

        indexes.last.columns << row[2]
      end

      indexes
    end

    def last_insert_id(table, sequence_name)
      Integer(select_value("SELECT currval('#{sequence_name}')"))
    end

    def recreate_database(name)
      drop_database(name)
      create_database(name)
    end

    def create_database(name, options = {})
      execute "CREATE DATABASE \"#{name}\" ENCODING='#{options[:encoding] || 'utf8'}'"
    end

    def drop_database(name)
      execute "DROP DATABASE \"#{name}\""
    end

    def structure_dump
      database = @config[:database]
      if database.nil?
        if @config[:url] =~ /\/([^\/]*)$/
          database = $1
        else
          raise "Could not figure out what database this url is for #{@config["url"]}"
        end
      end

      ENV['PGHOST']     = @config[:host] if @config[:host]
      ENV['PGPORT']     = @config[:port].to_s if @config[:port]
      ENV['PGPASSWORD'] = @config[:password].to_s if @config[:password]
      search_path = @config[:schema_search_path]
      search_path = "--schema=#{search_path}" if search_path

      @connection.connection.close
      begin
        file = "db/#{RAILS_ENV}_structure.sql"
        `pg_dump -i -U "#{@config[:username]}" -s -x -O -f #{file} #{search_path} #{database}`
        raise "Error dumping database" if $?.exitstatus == 1

        # need to patch away any references to SQL_ASCII as it breaks the JDBC driver
        lines = File.readlines(file)
        File.open(file, "w") do |io|
          lines.each do |line|
            line.gsub!(/SQL_ASCII/, 'UNICODE')
            io.write(line)
          end
        end
      ensure
        reconnect!
      end
    end

    def _execute(sql, name = nil)
        case sql.strip
        when /\A\(?\s*(select|show)/i:
          @connection.execute_query(sql)
        else
          @connection.execute_update(sql)
        end
    end

    # SELECT DISTINCT clause for a given set of columns and a given ORDER BY clause.
    #
    # PostgreSQL requires the ORDER BY columns in the select list for distinct queries, and
    # requires that the ORDER BY include the distinct column.
    #
    #   distinct("posts.id", "posts.created_at desc")
    def distinct(columns, order_by)
      return "DISTINCT #{columns}" if order_by.blank?

      # construct a clean list of column names from the ORDER BY clause, removing
      # any asc/desc modifiers
      order_columns = order_by.split(',').collect { |s| s.split.first }
      order_columns.delete_if(&:blank?)
      order_columns = order_columns.zip((0...order_columns.size).to_a).map { |s,i| "#{s} AS alias_#{i}" }

      # return a DISTINCT ON() clause that's distinct on the columns we want but includes
      # all the required columns for the ORDER BY to work properly
      sql = "DISTINCT ON (#{columns}) #{columns}, "
      sql << order_columns * ', '
    end

    # ORDER BY clause for the passed order option.
    #
    # PostgreSQL does not allow arbitrary ordering when using DISTINCT ON, so we work around this
    # by wrapping the sql as a sub-select and ordering in that query.
    def add_order_by_for_association_limiting!(sql, options)
      return sql if options[:order].blank?

      order = options[:order].split(',').collect { |s| s.strip }.reject(&:blank?)
      order.map! { |s| 'DESC' if s =~ /\bdesc$/i }
      order = order.zip((0...order.size).to_a).map { |s,i| "id_list.alias_#{i} #{s}" }.join(', ')

      sql.replace "SELECT * FROM (#{sql}) AS id_list ORDER BY #{order}"
    end

    def quote(value, column = nil)
      return value.quoted_id if value.respond_to?(:quoted_id)

      if value.kind_of?(String) && column && column.type == :binary
        "'#{escape_bytea(value)}'"
      elsif column && column.type == :primary_key
        return value.to_s
      else
        super
      end
    end

    def escape_bytea(s)
      if s
        result = ''
        s.each_byte { |c| result << sprintf('\\\\%03o', c) }
        result
      end
    end

    def quote_column_name(name)
      %("#{name}")
    end

    def quoted_date(value)
      value.strftime("%Y-%m-%d %H:%M:%S")
    end

    def disable_referential_integrity(&block) #:nodoc:
      execute(tables.collect { |name| "ALTER TABLE #{quote_table_name(name)} DISABLE TRIGGER ALL" }.join(";"))
      yield
    ensure
      execute(tables.collect { |name| "ALTER TABLE #{quote_table_name(name)} ENABLE TRIGGER ALL" }.join(";"))
    end

    def rename_table(name, new_name)
      execute "ALTER TABLE #{name} RENAME TO #{new_name}"
    end

    def add_column(table_name, column_name, type, options = {})
      execute("ALTER TABLE #{table_name} ADD #{column_name} #{type_to_sql(type, options[:limit])}")
      change_column_default(table_name, column_name, options[:default]) unless options[:default].nil?
      if options[:null] == false
        execute("UPDATE #{table_name} SET #{column_name} = '#{options[:default]}'") if options[:default]
        execute("ALTER TABLE #{table_name} ALTER #{column_name} SET NOT NULL")
      end
    end

    def change_column(table_name, column_name, type, options = {}) #:nodoc:
      begin
        execute "ALTER TABLE #{table_name} ALTER  #{column_name} TYPE #{type_to_sql(type, options[:limit])}"
      rescue ActiveRecord::StatementInvalid
        # This is PG7, so we use a more arcane way of doing it.
        begin_db_transaction
        add_column(table_name, "#{column_name}_ar_tmp", type, options)
        execute "UPDATE #{table_name} SET #{column_name}_ar_tmp = CAST(#{column_name} AS #{type_to_sql(type, options[:limit])})"
        remove_column(table_name, column_name)
        rename_column(table_name, "#{column_name}_ar_tmp", column_name)
        commit_db_transaction
      end
      change_column_default(table_name, column_name, options[:default]) unless options[:default].nil?
    end

    def change_column_default(table_name, column_name, default) #:nodoc:
      execute "ALTER TABLE #{table_name} ALTER COLUMN #{column_name} SET DEFAULT '#{default}'"
    end

    def rename_column(table_name, column_name, new_column_name) #:nodoc:
      execute "ALTER TABLE #{table_name} RENAME COLUMN #{column_name} TO #{new_column_name}"
    end

    def remove_index(table_name, options) #:nodoc:
      execute "DROP INDEX #{index_name(table_name, options)}"
    end

    def type_to_sql(type, limit = nil, precision = nil, scale = nil) #:nodoc:
      return super unless type.to_s == 'integer'

      if limit.nil? || limit == 4
        'integer'
      elsif limit < 4
        'smallint'
      else
        'bigint'
      end
    end

    def tables
      @connection.tables(database_name, nil, nil, ["TABLE"])
    end
  end
end
