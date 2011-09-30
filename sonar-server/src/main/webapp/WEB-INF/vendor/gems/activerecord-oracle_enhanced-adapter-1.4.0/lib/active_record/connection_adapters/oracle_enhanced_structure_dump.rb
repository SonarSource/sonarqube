module ActiveRecord #:nodoc:
  module ConnectionAdapters #:nodoc:
    module OracleEnhancedStructureDump #:nodoc:

      # Statements separator used in structure dump to allow loading of structure dump also with SQL*Plus
      STATEMENT_TOKEN = "\n\n/\n\n"

      def structure_dump #:nodoc:
        structure = select_values("SELECT sequence_name FROM user_sequences ORDER BY 1").map do |seq|
          "CREATE SEQUENCE \"#{seq}\""
        end
        select_values("SELECT table_name FROM all_tables t
                    WHERE owner = SYS_CONTEXT('userenv', 'session_user') AND secondary = 'N'
                      AND NOT EXISTS (SELECT mv.mview_name FROM all_mviews mv WHERE mv.owner = t.owner AND mv.mview_name = t.table_name)
                      AND NOT EXISTS (SELECT mvl.log_table FROM all_mview_logs mvl WHERE mvl.log_owner = t.owner AND mvl.log_table = t.table_name)
                    ORDER BY 1").each do |table_name|
          virtual_columns = virtual_columns_for(table_name)
          ddl = "CREATE#{ ' GLOBAL TEMPORARY' if temporary_table?(table_name)} TABLE \"#{table_name}\" (\n"
          cols = select_all(%Q{
            SELECT column_name, data_type, data_length, char_used, char_length, data_precision, data_scale, data_default, nullable
            FROM user_tab_columns
            WHERE table_name = '#{table_name}'
            ORDER BY column_id
          }).map do |row|
            if(v = virtual_columns.find {|col| col['column_name'] == row['column_name']})
              structure_dump_virtual_column(row, v['data_default'])
            else
              structure_dump_column(row)
            end
          end
          ddl << cols.join(",\n ")
          ddl << structure_dump_primary_key(table_name)
          ddl << "\n)"
          structure << ddl
          structure << structure_dump_indexes(table_name)
          structure << structure_dump_unique_keys(table_name)
        end

        join_with_statement_token(structure) << structure_dump_fk_constraints
      end

      def structure_dump_column(column) #:nodoc:
        col = "\"#{column['column_name']}\" #{column['data_type']}"
        if column['data_type'] =='NUMBER' and !column['data_precision'].nil?
          col << "(#{column['data_precision'].to_i}"
          col << ",#{column['data_scale'].to_i}" if !column['data_scale'].nil?
          col << ')'
        elsif column['data_type'].include?('CHAR')
          length = column['char_used'] == 'C' ? column['char_length'].to_i : column['data_length'].to_i
          col <<  "(#{length})"
        end
        col << " DEFAULT #{column['data_default']}" if !column['data_default'].nil?
        col << ' NOT NULL' if column['nullable'] == 'N'
        col
      end

      def structure_dump_virtual_column(column, data_default) #:nodoc:
        data_default = data_default.gsub(/"/, '')
        col = "\"#{column['column_name']}\" #{column['data_type']}"
        if column['data_type'] =='NUMBER' and !column['data_precision'].nil?
          col << "(#{column['data_precision'].to_i}"
          col << ",#{column['data_scale'].to_i}" if !column['data_scale'].nil?
          col << ')'
        elsif column['data_type'].include?('CHAR')
          length = column['char_used'] == 'C' ? column['char_length'].to_i : column['data_length'].to_i
          col <<  "(#{length})"
        end
        col << " GENERATED ALWAYS AS (#{data_default}) VIRTUAL"
      end

      def structure_dump_primary_key(table) #:nodoc:
        opts = {:name => '', :cols => []}
        pks = select_all(<<-SQL, "Primary Keys") 
          SELECT a.constraint_name, a.column_name, a.position
            FROM user_cons_columns a
            JOIN user_constraints c
              ON a.constraint_name = c.constraint_name
           WHERE c.table_name = '#{table.upcase}'
             AND c.constraint_type = 'P'
             AND c.owner = SYS_CONTEXT('userenv', 'session_user')
        SQL
        pks.each do |row|
          opts[:name] = row['constraint_name']
          opts[:cols][row['position']-1] = row['column_name']
        end
        opts[:cols].length > 0 ? ",\n CONSTRAINT #{opts[:name]} PRIMARY KEY (#{opts[:cols].join(',')})" : ''
      end

      def structure_dump_unique_keys(table) #:nodoc:
        keys = {}
        uks = select_all(<<-SQL, "Primary Keys") 
          SELECT a.constraint_name, a.column_name, a.position
            FROM user_cons_columns a
            JOIN user_constraints c
              ON a.constraint_name = c.constraint_name
           WHERE c.table_name = '#{table.upcase}'
             AND c.constraint_type = 'U'
             AND c.owner = SYS_CONTEXT('userenv', 'session_user')
        SQL
        uks.each do |uk|
          keys[uk['constraint_name']] ||= []
          keys[uk['constraint_name']][uk['position']-1] = uk['column_name']
        end
        keys.map do |k,v|
          "ALTER TABLE #{table.upcase} ADD CONSTRAINT #{k} UNIQUE (#{v.join(',')})"
        end
      end

      def structure_dump_indexes(table_name) #:nodoc:
        indexes(table_name).map do |options|
          column_names = options[:columns]
          options = {:name => options[:name], :unique => options[:unique]}
          index_name   = index_name(table_name, :column => column_names)
          if Hash === options # legacy support, since this param was a string
            index_type = options[:unique] ? "UNIQUE" : ""
            index_name = options[:name] || index_name
          else
            index_type = options
          end
          quoted_column_names = column_names.map { |e| quote_column_name(e) }.join(", ")
          "CREATE #{index_type} INDEX #{quote_column_name(index_name)} ON #{quote_table_name(table_name)} (#{quoted_column_names})"
        end
      end

      def structure_dump_fk_constraints #:nodoc:
        fks = select_all("SELECT table_name FROM all_tables WHERE owner = SYS_CONTEXT('userenv', 'session_user') ORDER BY 1").map do |table|
          if respond_to?(:foreign_keys) && (foreign_keys = foreign_keys(table["table_name"])).any?
            foreign_keys.map do |fk|
              sql = "ALTER TABLE #{quote_table_name(fk.from_table)} ADD CONSTRAINT #{quote_column_name(fk.options[:name])} "
              sql << "#{foreign_key_definition(fk.to_table, fk.options)}"
            end
          end
        end.flatten.compact
        join_with_statement_token(fks)
      end

      def dump_schema_information #:nodoc:
        sm_table = ActiveRecord::Migrator.schema_migrations_table_name
        migrated = select_values("SELECT version FROM #{sm_table}")
        join_with_statement_token(migrated.map{|v| "INSERT INTO #{sm_table} (version) VALUES ('#{v}')" })
      end

      # Extract all stored procedures, packages, synonyms and views.
      def structure_dump_db_stored_code #:nodoc:
        structure = []
        select_all("SELECT DISTINCT name, type
                     FROM all_source
                    WHERE type IN ('PROCEDURE', 'PACKAGE', 'PACKAGE BODY', 'FUNCTION', 'TRIGGER', 'TYPE')
                      AND name NOT LIKE 'BIN$%'
                      AND owner = SYS_CONTEXT('userenv', 'session_user') ORDER BY type").each do |source|
          ddl = "CREATE OR REPLACE   \n"
          lines = select_all(%Q{
                  SELECT text
                    FROM all_source
                   WHERE name = '#{source['name']}'
                     AND type = '#{source['type']}'
                     AND owner = SYS_CONTEXT('userenv', 'session_user')
                   ORDER BY line
                }).map do |row|
            ddl << row['text']
          end
          ddl << ";" unless ddl.strip[-1,1] == ";"
          structure << ddl
        end

        # export views 
        select_all("SELECT view_name, text FROM user_views").each do |view|
          structure << "CREATE OR REPLACE VIEW #{view['view_name']} AS\n #{view['text']}"
        end

        # export synonyms
        select_all("SELECT owner, synonym_name, table_name, table_owner
                      FROM all_synonyms
                     WHERE owner = SYS_CONTEXT('userenv', 'session_user') ").each do |synonym|
          structure << "CREATE OR REPLACE #{synonym['owner'] == 'PUBLIC' ? 'PUBLIC' : '' } SYNONYM #{synonym['synonym_name']}"
          structure << " FOR #{synonym['table_owner']}.#{synonym['table_name']}"
        end

        join_with_statement_token(structure)
      end

      def structure_drop #:nodoc:
        statements = select_values("SELECT sequence_name FROM user_sequences ORDER BY 1").map do |seq|
          "DROP SEQUENCE \"#{seq}\""
        end
        select_values("SELECT table_name from all_tables t
                    WHERE owner = SYS_CONTEXT('userenv', 'session_user') AND secondary = 'N'
                      AND NOT EXISTS (SELECT mv.mview_name FROM all_mviews mv WHERE mv.owner = t.owner AND mv.mview_name = t.table_name)
                      AND NOT EXISTS (SELECT mvl.log_table FROM all_mview_logs mvl WHERE mvl.log_owner = t.owner AND mvl.log_table = t.table_name)
                    ORDER BY 1").each do |table|
          statements << "DROP TABLE \"#{table}\" CASCADE CONSTRAINTS"
        end
        join_with_statement_token(statements)
      end

      def temp_table_drop #:nodoc:
        join_with_statement_token(select_values(
                  "SELECT table_name FROM all_tables
                    WHERE owner = SYS_CONTEXT('userenv', 'session_user') AND secondary = 'N' AND temporary = 'Y' ORDER BY 1").map do |table|
          "DROP TABLE \"#{table}\" CASCADE CONSTRAINTS"
        end)
      end

      def full_drop(preserve_tables=false) #:nodoc:
        s = preserve_tables ? [] : [structure_drop]
        s << temp_table_drop if preserve_tables
        s << drop_sql_for_feature("view")
        s << drop_sql_for_feature("materialized view")
        s << drop_sql_for_feature("synonym")
        s << drop_sql_for_feature("type")
        s << drop_sql_for_object("package")
        s << drop_sql_for_object("function")
        s << drop_sql_for_object("procedure")
        s.join
      end

      def add_column_options!(sql, options) #:nodoc:
        type = options[:type] || ((column = options[:column]) && column.type)
        type = type && type.to_sym
        # handle case of defaults for CLOB columns, which would otherwise get "quoted" incorrectly
        if options_include_default?(options)
          if type == :text
            sql << " DEFAULT #{quote(options[:default])}"
          else
            # from abstract adapter
            sql << " DEFAULT #{quote(options[:default], options[:column])}"
          end
        end
        # must explicitly add NULL or NOT NULL to allow change_column to work on migrations
        if options[:null] == false
          sql << " NOT NULL"
        elsif options[:null] == true
          sql << " NULL" unless type == :primary_key
        end
      end

      def execute_structure_dump(string)
        string.split(STATEMENT_TOKEN).each do |ddl|
          ddl.chop! if ddl.last == ";"
          execute(ddl) unless ddl.blank?
        end
      end

      private

      # virtual columns are an 11g feature.  This returns [] if feature is not 
      # present or none are found.
      # return [{'column_name' => 'FOOS', 'data_default' => '...'}, ...]
      def virtual_columns_for(table)
        begin
          select_all <<-SQL
            SELECT column_name, data_default
              FROM user_tab_cols
             WHERE virtual_column = 'YES'
               AND table_name = '#{table.upcase}'
          SQL
        # feature not supported previous to 11g
        rescue ActiveRecord::StatementInvalid => e
          []
        end
      end

      def drop_sql_for_feature(type)
        short_type = type == 'materialized view' ? 'mview' : type
        join_with_statement_token(
        select_values("SELECT #{short_type}_name FROM user_#{short_type.tableize}").map do |name|
          "DROP #{type.upcase} \"#{name}\""
        end)
      end

      def drop_sql_for_object(type)
        join_with_statement_token(
        select_values("SELECT object_name FROM user_objects WHERE object_type = '#{type.upcase}'").map do |name|
          "DROP #{type.upcase} \"#{name}\""
        end)
      end

      def join_with_statement_token(array)
        string = array.join(STATEMENT_TOKEN)
        string << STATEMENT_TOKEN unless string.blank?
        string
      end

    end
  end
end

ActiveRecord::ConnectionAdapters::OracleEnhancedAdapter.class_eval do
  include ActiveRecord::ConnectionAdapters::OracleEnhancedStructureDump
end
