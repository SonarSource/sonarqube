module ArJdbc
  module DB2
    def self.column_selector
      [ /(db2|as400)/i,
        lambda { |cfg, column| column.extend(::ArJdbc::DB2::Column) } ]
    end

    def self.jdbc_connection_class
      ::ActiveRecord::ConnectionAdapters::DB2JdbcConnection
    end

    module Column
      def type_cast(value)
        return nil if value.nil? || value =~ /^\s*null\s*$/i
        case type
        when :string    then value
        when :integer   then defined?(value.to_i) ? value.to_i : (value ? 1 : 0)
        when :primary_key then defined?(value.to_i) ? value.to_i : (value ? 1 : 0)
        when :float     then value.to_f
        when :datetime  then ArJdbc::DB2::Column.cast_to_date_or_time(value)
        when :date      then ArJdbc::DB2::Column.cast_to_date_or_time(value)
        when :timestamp then ArJdbc::DB2::Column.cast_to_time(value)
        when :time      then ArJdbc::DB2::Column.cast_to_time(value)
        # TODO AS400 stores binary strings in EBCDIC (CCSID 65535), need to convert back to ASCII
        else
          super
        end
      end

      def type_cast_code(var_name)
        case type
        when :datetime  then "ArJdbc::DB2::Column.cast_to_date_or_time(#{var_name})"
        when :date      then "ArJdbc::DB2::Column.cast_to_date_or_time(#{var_name})"
        when :timestamp then "ArJdbc::DB2::Column.cast_to_time(#{var_name})"
        when :time      then "ArJdbc::DB2::Column.cast_to_time(#{var_name})"
        else
          super
        end
      end

      def self.cast_to_date_or_time(value)
        return value if value.is_a? Date
        return nil if value.blank?
        guess_date_or_time((value.is_a? Time) ? value : cast_to_time(value))
      end

      def self.cast_to_time(value)
        return value if value.is_a? Time
        # AS400 returns a 2 digit year, LUW returns a 4 digit year, so comp = true to help out AS400
        time_array = ParseDate.parsedate(value, true)
        time_array[0] ||= 2000; time_array[1] ||= 1; time_array[2] ||= 1;
        Time.send(ActiveRecord::Base.default_timezone, *time_array) rescue nil
      end

      def self.guess_date_or_time(value)
        (value.hour == 0 and value.min == 0 and value.sec == 0) ?
        Date.new(value.year, value.month, value.day) : value
      end

      private
      # <b>DEPRECATED:</b> SMALLINT is now used for boolean field types. Please
      # convert your tables using DECIMAL(5) for boolean values to SMALLINT instead.
      def use_decimal5_for_boolean
        warn "[DEPRECATION] using DECIMAL(5) for boolean is deprecated. Convert your columns to SMALLINT instead."
        :boolean
      end

      # http://publib.boulder.ibm.com/infocenter/db2luw/v9r7/topic/com.ibm.db2.luw.apdv.java.doc/doc/rjvjdata.html
      def simplified_type(field_type)
        case field_type
        # old jdbc_db2.rb used decimal(5,0) as boolean
        when /^smallint/i           then :boolean
        when /^decimal\(5\)$/i      then use_decimal5_for_boolean
        when /^real/i               then :float
        when /^timestamp/i          then :datetime
        else
          super
        end
      end

      # Post process default value from JDBC into a Rails-friendly format (columns{-internal})
      def default_value(value)
        # IBM i (AS400) will return an empty string instead of null for no default
        return nil if value.blank?

        # string defaults are surrounded by single quotes
        return $1 if value =~ /^'(.*)'$/

        value
      end
    end

    def _execute(sql, name = nil)
      if ActiveRecord::ConnectionAdapters::JdbcConnection::select?(sql)
        @connection.execute_query(sql)
      elsif ActiveRecord::ConnectionAdapters::JdbcConnection::insert?(sql)
        (@connection.execute_insert(sql) or last_insert_id(sql)).to_i
      else
        @connection.execute_update(sql)
      end
    end

    # holy moly batman! all this to tell AS400 "yes i am sure"
    def execute_and_auto_confirm(sql)
      begin
        @connection.execute_update "call qsys.qcmdexc('QSYS/CHGJOB INQMSGRPY(*SYSRPYL)',0000000031.00000)"
        @connection.execute_update "call qsys.qcmdexc('ADDRPYLE SEQNBR(9876) MSGID(CPA32B2) RPY(''I'')',0000000045.00000)"
      rescue Exception => e
        raise "Could not call CHGJOB INQMSGRPY(*SYSRPYL) and ADDRPYLE SEQNBR(9876) MSGID(CPA32B2) RPY('I').\n" +
          "Do you have authority to do this?\n\n" + e.to_s
      end

      r = execute sql

      begin
        @connection.execute_update "call qsys.qcmdexc('QSYS/CHGJOB INQMSGRPY(*DFT)',0000000027.00000)"
        @connection.execute_update "call qsys.qcmdexc('RMVRPYLE SEQNBR(9876)',0000000021.00000)"
      rescue Exception => e
        raise "Could not call CHGJOB INQMSGRPY(*DFT) and RMVRPYLE SEQNBR(9876).\n" +
          "Do you have authority to do this?\n\n" + e.to_s
      end
      r
    end

    def last_insert_id(sql)
      table_name = sql.split(/\s/)[2]
      result = select(ActiveRecord::Base.send(:sanitize_sql,
          %[select IDENTITY_VAL_LOCAL() as last_insert_id from #{table_name}],
          nil))
      result.last['last_insert_id']
    end

    def modify_types(tp)
      tp[:primary_key] = 'int not null generated by default as identity (start with 1) primary key'
      tp[:string][:limit] = 255
      tp[:integer][:limit] = nil
      tp[:boolean] = {:name => "smallint"}
      tp
    end

    def type_to_sql(type, limit = nil, precision = nil, scale = nil)
      limit = nil if type.to_sym == :integer
      super(type, limit, precision, scale)
    end

    def adapter_name
      'DB2'
    end

    def arel2_visitors
      require 'arel/visitors/db2'
      {'db2' => ::Arel::Visitors::DB2, 'as400' => ::Arel::Visitors::DB2}
    end

    def add_limit_offset!(sql, options)
      replace_limit_offset!(sql, options[:limit], options[:offset])
    end

    def replace_limit_offset!(sql, limit, offset)
      if limit
        limit = limit.to_i
        if !offset
          if limit == 1
            sql << " FETCH FIRST ROW ONLY"
          else
            sql << " FETCH FIRST #{limit} ROWS ONLY"
          end
        else
          offset = offset.to_i
          sql.gsub!(/SELECT/i, 'SELECT B.* FROM (SELECT A.*, row_number() over () AS internal$rownum FROM (SELECT')
          sql << ") A ) B WHERE B.internal$rownum > #{offset} AND B.internal$rownum <= #{limit + offset}"
        end
      end
      sql
    end

    def pk_and_sequence_for(table)
      # In JDBC/DB2 side, only upcase names of table and column are handled.
      keys = super(table.upcase)
      if keys && keys[0]
        # In ActiveRecord side, only downcase names of table and column are handled.
        keys[0] = keys[0].downcase
      end
      keys
    end

    def quote_column_name(column_name)
      column_name
    end

    def quote(value, column = nil) # :nodoc:
      if column && column.respond_to?(:primary) && column.primary && column.klass != String
        return value.to_i.to_s
      end
      if column && (column.type == :decimal || column.type == :integer) && value
        return value.to_s
      end
      case value
      when String
        if column && column.type == :binary
          "BLOB('#{quote_string(value)}')"
        else
          "'#{quote_string(value)}'"
        end
      else super
      end
    end

    def quote_string(string)
      string.gsub(/'/, "''") # ' (for ruby-mode)
    end

    def quoted_true
      '1'
    end

    def quoted_false
      '0'
    end

    def reorg_table(table_name)
      unless as400?
        @connection.execute_update "call sysproc.admin_cmd ('REORG TABLE #{table_name}')"
      end
    end

    def recreate_database(name)
      tables.each {|table| drop_table("#{db2_schema}.#{table}")}
    end

    def remove_index(table_name, options = { })
      execute "DROP INDEX #{quote_column_name(index_name(table_name, options))}"
    end

    # http://publib.boulder.ibm.com/infocenter/db2luw/v9r7/topic/com.ibm.db2.luw.admin.dbobj.doc/doc/t0020130.html
    # ...not supported on IBM i, so we raise in this case
    def rename_column(table_name, column_name, new_column_name) #:nodoc:
      if as400?
        raise NotImplementedError, "rename_column is not supported on IBM i"
      else
        execute "ALTER TABLE #{table_name} RENAME COLUMN #{column_name} TO #{new_column_name}"
        reorg_table(table_name)
      end
    end

    def change_column_null(table_name, column_name, null)
      if null
        execute_and_auto_confirm "ALTER TABLE #{table_name} ALTER COLUMN #{column_name} DROP NOT NULL"
      else
        execute_and_auto_confirm "ALTER TABLE #{table_name} ALTER COLUMN #{column_name} SET NOT NULL"
      end
      reorg_table(table_name)
    end

    def change_column_default(table_name, column_name, default)
      if default.nil?
        execute_and_auto_confirm "ALTER TABLE #{table_name} ALTER COLUMN #{column_name} DROP DEFAULT"
      else
        execute_and_auto_confirm "ALTER TABLE #{table_name} ALTER COLUMN #{column_name} SET WITH DEFAULT #{quote(default)}"
      end
      reorg_table(table_name)
    end

    def change_column(table_name, column_name, type, options = {})
      data_type = type_to_sql(type, options[:limit], options[:precision], options[:scale])
      sql = "ALTER TABLE #{table_name} ALTER COLUMN #{column_name} SET DATA TYPE #{data_type}"
      as400? ? execute_and_auto_confirm(sql) : execute(sql)
      reorg_table(table_name)

      if options.include?(:default) and options.include?(:null)
        # which to run first?
        if options[:null] or options[:default].nil?
          change_column_null(table_name, column_name, options[:null])
          change_column_default(table_name, column_name, options[:default])
        else
          change_column_default(table_name, column_name, options[:default])
          change_column_null(table_name, column_name, options[:null])
        end
      elsif options.include?(:default)
        change_column_default(table_name, column_name, options[:default])
      elsif options.include?(:null)
        change_column_null(table_name, column_name, options[:null])
      end
    end

    # http://publib.boulder.ibm.com/infocenter/db2luw/v9r7/topic/com.ibm.db2.luw.admin.dbobj.doc/doc/t0020132.html
    def remove_column(table_name, column_name) #:nodoc:
      sql = "ALTER TABLE #{table_name} DROP COLUMN #{column_name}"

      as400? ? execute_and_auto_confirm(sql) : execute(sql)
      reorg_table(table_name)
    end

    # http://publib.boulder.ibm.com/infocenter/db2luw/v9r7/topic/com.ibm.db2.luw.sql.ref.doc/doc/r0000980.html
    def rename_table(name, new_name) #:nodoc:
      execute "RENAME TABLE #{name} TO #{new_name}"
      reorg_table(new_name)
    end

    def tables
      @connection.tables(nil, db2_schema, nil, ["TABLE"])
    end

    # only record precision and scale for types that can set
    # them via CREATE TABLE:
    # http://publib.boulder.ibm.com/infocenter/db2luw/v9r7/topic/com.ibm.db2.luw.sql.ref.doc/doc/r0000927.html
    HAVE_LIMIT = %w(FLOAT DECFLOAT CHAR VARCHAR CLOB BLOB NCHAR NCLOB DBCLOB GRAPHIC VARGRAPHIC) #TIMESTAMP
    HAVE_PRECISION = %w(DECIMAL NUMERIC)
    HAVE_SCALE = %w(DECIMAL NUMERIC)

    def columns(table_name, name = nil)
      cols = @connection.columns(table_name, name, db2_schema)

      # scrub out sizing info when CREATE TABLE doesn't support it
      # but JDBC reports it (doh!)
      for col in cols
        base_sql_type = col.sql_type.sub(/\(.*/, "").upcase
        col.limit = nil unless HAVE_LIMIT.include?(base_sql_type)
        col.precision = nil unless HAVE_PRECISION.include?(base_sql_type)
        #col.scale = nil unless HAVE_SCALE.include?(base_sql_type)
      end

      cols
    end

    def jdbc_columns(table_name, name = nil)
      columns(table_name, name)
    end

    def indexes(table_name, name = nil)
      @connection.indexes(table_name, name, db2_schema)
    end

    def add_quotes(name)
      return name unless name
      %Q{"#{name}"}
    end

    def strip_quotes(str)
      return str unless str
      return str unless /^(["']).*\1$/ =~ str
      str[1..-2]
    end

    def expand_double_quotes(name)
      return name unless name && name['"']
      name.gsub(/"/,'""')
    end

    def structure_dump #:nodoc:
      definition=""
      rs = @connection.connection.meta_data.getTables(nil,db2_schema.upcase,nil,["TABLE"].to_java(:string))
      while rs.next
        tname = rs.getString(3)
        definition << "CREATE TABLE #{tname} (\n"
        rs2 = @connection.connection.meta_data.getColumns(nil,db2_schema.upcase,tname,nil)
        first_col = true
        while rs2.next
          col_name = add_quotes(rs2.getString(4));
          default = ""
          d1 = rs2.getString(13)
          # IBM i (as400 toolbox driver) will return an empty string if there is no default
          if @config[:url] =~ /^jdbc:as400:/
            default = !d1.blank? ? " DEFAULT #{d1}" : ""
          else
            default = d1 ? " DEFAULT #{d1}" : ""
          end

          type = rs2.getString(6)
          col_precision = rs2.getString(7)
          col_scale = rs2.getString(9)
          col_size = ""
          if HAVE_SCALE.include?(type) and col_scale
            col_size = "(#{col_precision},#{col_scale})"
          elsif (HAVE_LIMIT + HAVE_PRECISION).include?(type) and col_precision
            col_size = "(#{col_precision})"
          end
          nulling = (rs2.getString(18) == 'NO' ? " NOT NULL" : "")
          create_col_string = add_quotes(expand_double_quotes(strip_quotes(col_name))) +
            " " +
            type +
            col_size +
            "" +
            nulling +
            default
          if !first_col
            create_col_string = ",\n #{create_col_string}"
          else
            create_col_string = " #{create_col_string}"
          end

          definition << create_col_string

          first_col = false
        end
        definition << ");\n\n"
      end
      definition
    end

    private
    def as400?
        @config[:url] =~ /^jdbc:as400:/
    end

    def db2_schema
      if @config[:schema].blank?
        if as400?
          # AS400 implementation takes schema from library name (last part of url)
          schema = @config[:url].split('/').last.strip
          (schema[-1..-1] == ";") ? schema.chop : schema
        else
          # LUW implementation uses schema name of username by default
          @config[:username] or ENV['USER']
        end
      else
        @config[:schema]
      end
    end
  end
end
