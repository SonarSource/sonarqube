module ActiveRecord
  module ConnectionAdapters
    module OracleEnhancedContextIndex

      # Define full text index with Oracle specific CONTEXT index type
      #
      # Oracle CONTEXT index by default supports full text indexing of one column.
      # This method allows full text index creation also on several columns
      # as well as indexing related table columns by generating stored procedure
      # that concatenates all columns for indexing as well as generating trigger
      # that will update main index column to trigger reindexing of record.
      #
      # Use +contains+ ActiveRecord model instance method to add CONTAINS where condition
      # and order by score of matched results.
      #
      # Options:
      #
      # * <tt>:name</tt>
      # * <tt>:index_column</tt>
      # * <tt>:index_column_trigger_on</tt>
      # * <tt>:tablespace</tt>
      # * <tt>:sync</tt> - 'MANUAL', 'EVERY "interval-string"' or 'ON COMMIT' (defaults to 'MANUAL').
      # * <tt>:lexer</tt> - Lexer options (e.g. <tt>:type => 'BASIC_LEXER', :base_letter => true</tt>).
      # * <tt>:transactional</tt> - When +true+, the CONTAINS operator will process inserted and updated rows.
      #
      # ===== Examples
      #
      # ====== Creating single column index
      #  add_context_index :posts, :title
      # search with
      #  Post.contains(:title, 'word')
      #
      # ====== Creating index on several columns
      #  add_context_index :posts, [:title, :body]
      # search with (use first column as argument for contains method but it will search in all index columns)
      #  Post.contains(:title, 'word')
      #
      # ====== Creating index on several columns with dummy index column and commit option
      #  add_context_index :posts, [:title, :body], :index_column => :all_text, :sync => 'ON COMMIT'
      # search with
      #  Post.contains(:all_text, 'word')
      #
      # ====== Creating index with trigger option (will reindex when specified columns are updated)
      #  add_context_index :posts, [:title, :body], :index_column => :all_text, :sync => 'ON COMMIT',
      #                     :index_column_trigger_on => [:created_at, :updated_at]
      # search with
      #  Post.contains(:all_text, 'word')
      #
      # ====== Creating index on multiple tables
      #  add_context_index :posts,
      #   [:title, :body,
      #   # specify aliases always with AS keyword
      #   "SELECT comments.author AS comment_author, comments.body AS comment_body FROM comments WHERE comments.post_id = :id"
      #   ],
      #   :name => 'post_and_comments_index',
      #   :index_column => :all_text, :index_column_trigger_on => [:updated_at, :comments_count],
      #   :sync => 'ON COMMIT'
      # search in any table columns
      #  Post.contains(:all_text, 'word')
      # search in specified column
      #  Post.contains(:all_text, "aaa within title")
      #  Post.contains(:all_text, "bbb within comment_author")
      #
      # ====== Creating index using lexer
      #  add_context_index :posts, :title, :lexer => { :type => 'BASIC_LEXER', :base_letter => true, ... }
      #
      # ====== Creating transactional index (will reindex changed rows when querying)
      #  add_context_index :posts, :title, :transactional => true
      #
      def add_context_index(table_name, column_name, options = {})
        self.all_schema_indexes = nil
        column_names = Array(column_name)
        index_name = options[:name] || index_name(table_name, :column => options[:index_column] || column_names,
          # CONEXT index name max length is 25
          :identifier_max_length => 25)

        quoted_column_name = quote_column_name(options[:index_column] || column_names.first)
        if options[:index_column_trigger_on]
          raise ArgumentError, "Option :index_column should be specified together with :index_column_trigger_on option" \
            unless options[:index_column]
          create_index_column_trigger(table_name, index_name, options[:index_column], options[:index_column_trigger_on])
        end

        sql = "CREATE INDEX #{quote_column_name(index_name)} ON #{quote_table_name(table_name)}"
        sql << " (#{quoted_column_name})"
        sql << " INDEXTYPE IS CTXSYS.CONTEXT"
        parameters = []
        if column_names.size > 1
          procedure_name = default_datastore_procedure(index_name)
          datastore_name = default_datastore_name(index_name)
          create_datastore_procedure(table_name, procedure_name, column_names, options)
          create_datastore_preference(datastore_name, procedure_name)
          parameters << "DATASTORE #{datastore_name} SECTION GROUP CTXSYS.AUTO_SECTION_GROUP"
        end
        if options[:tablespace]
          storage_name = default_storage_name(index_name)
          create_storage_preference(storage_name, options[:tablespace])
          parameters << "STORAGE #{storage_name}"
        end
        if options[:sync]
          parameters << "SYNC(#{options[:sync]})"
        end
        if options[:lexer] && (lexer_type = options[:lexer][:type])
          lexer_name = default_lexer_name(index_name)
          (lexer_options = options[:lexer].dup).delete(:type)
          create_lexer_preference(lexer_name, lexer_type, lexer_options)
          parameters << "LEXER #{lexer_name}"
        end
        if options[:transactional]
          parameters << "TRANSACTIONAL"
        end
        unless parameters.empty?
          sql << " PARAMETERS ('#{parameters.join(' ')}')"
        end
        execute sql
      end

      # Drop full text index with Oracle specific CONTEXT index type
      def remove_context_index(table_name, options = {})
        self.all_schema_indexes = nil
        unless Hash === options # if column names passed as argument
          options = {:column => Array(options)}
        end
        index_name = options[:name] || index_name(table_name,
          :column => options[:index_column] || options[:column], :identifier_max_length => 25)
        execute "DROP INDEX #{index_name}"
        drop_ctx_preference(default_datastore_name(index_name))
        drop_ctx_preference(default_storage_name(index_name))
        procedure_name = default_datastore_procedure(index_name)
        execute "DROP PROCEDURE #{quote_table_name(procedure_name)}" rescue nil
        drop_index_column_trigger(index_name)
      end

      private

      def create_datastore_procedure(table_name, procedure_name, column_names, options)
        quoted_table_name = quote_table_name(table_name)
        select_queries, column_names = column_names.partition { |c| c.to_s =~ /^\s*SELECT\s+/i }
        select_queries = select_queries.map { |s| s.strip.gsub(/\s+/, ' ') }
        keys, selected_columns = parse_select_queries(select_queries)
        quoted_column_names = (column_names+keys).map{|col| quote_column_name(col)}
        execute compress_lines(<<-SQL)
          CREATE OR REPLACE PROCEDURE #{quote_table_name(procedure_name)}
            (p_rowid IN	      ROWID,
            p_clob	IN OUT NOCOPY CLOB) IS
            -- add_context_index_parameters #{(column_names+select_queries).inspect}#{!options.empty? ? ', ' << options.inspect[1..-2] : ''}
            #{
            selected_columns.map do |cols|
              cols.map do |col|
                raise ArgumentError, "Alias #{col} too large, should be 28 or less characters long" unless col.length <= 28
                "l_#{col} VARCHAR2(32767);\n"
              end.join
            end.join
            } BEGIN
            FOR r1 IN (
              SELECT #{quoted_column_names.join(', ')}
              FROM	 #{quoted_table_name}
              WHERE  #{quoted_table_name}.ROWID = p_rowid
            ) LOOP
              #{
              (column_names.map do |col|
                col = col.to_s
                "DBMS_LOB.WRITEAPPEND(p_clob, #{col.length+2}, '<#{col}>');\n" <<
                "IF LENGTH(r1.#{col}) > 0 THEN\n" <<
                "DBMS_LOB.WRITEAPPEND(p_clob, LENGTH(r1.#{col}), r1.#{col});\n" <<
                "END IF;\n" <<
                "DBMS_LOB.WRITEAPPEND(p_clob, #{col.length+3}, '</#{col}>');\n"
              end.join) <<
              (selected_columns.zip(select_queries).map do |cols, query|
                (cols.map do |col|
                  "l_#{col} := '';\n"
                end.join) <<
                "FOR r2 IN (\n" <<
                query.gsub(/:(\w+)/,"r1.\\1") << "\n) LOOP\n" <<
                (cols.map do |col|
                  "l_#{col} := l_#{col} || r2.#{col} || CHR(10);\n"
                end.join) <<
                "END LOOP;\n" <<
                (cols.map do |col|
                  col = col.to_s
                  "DBMS_LOB.WRITEAPPEND(p_clob, #{col.length+2}, '<#{col}>');\n" <<
                  "IF LENGTH(l_#{col}) > 0 THEN\n" <<
                  "DBMS_LOB.WRITEAPPEND(p_clob, LENGTH(l_#{col}), l_#{col});\n" <<
                  "END IF;\n" <<
                  "DBMS_LOB.WRITEAPPEND(p_clob, #{col.length+3}, '</#{col}>');\n"
                end.join)
              end.join)
              }
            END LOOP;
          END;
        SQL
      end

      def parse_select_queries(select_queries)
        keys = []
        selected_columns = []
        select_queries.each do |query|
          # get primary or foreign keys like :id or :something_id
          keys << (query.scan(/:\w+/).map{|k| k[1..-1].downcase.to_sym})
          select_part = query.scan(/^select\s.*\sfrom/i).first
          selected_columns << select_part.scan(/\sas\s+(\w+)/i).map{|c| c.first}
        end
        [keys.flatten.uniq, selected_columns]
      end

      def create_datastore_preference(datastore_name, procedure_name)
        drop_ctx_preference(datastore_name)
        execute <<-SQL
          BEGIN
            CTX_DDL.CREATE_PREFERENCE('#{datastore_name}', 'USER_DATASTORE');
            CTX_DDL.SET_ATTRIBUTE('#{datastore_name}', 'PROCEDURE', '#{procedure_name}');
          END;
        SQL
      end

      def create_storage_preference(storage_name, tablespace)
        drop_ctx_preference(storage_name)
        sql = "BEGIN\nCTX_DDL.CREATE_PREFERENCE('#{storage_name}', 'BASIC_STORAGE');\n"
        ['I_TABLE_CLAUSE', 'K_TABLE_CLAUSE', 'R_TABLE_CLAUSE',
        'N_TABLE_CLAUSE', 'I_INDEX_CLAUSE', 'P_TABLE_CLAUSE'].each do |clause|
          default_clause = case clause
          when 'R_TABLE_CLAUSE'; 'LOB(DATA) STORE AS (CACHE) '
          when 'I_INDEX_CLAUSE'; 'COMPRESS 2 '
          else ''
          end
          sql << "CTX_DDL.SET_ATTRIBUTE('#{storage_name}', '#{clause}', '#{default_clause}TABLESPACE #{tablespace}');\n"
        end
        sql << "END;\n"
        execute sql
      end

      def create_lexer_preference(lexer_name, lexer_type, options)
        drop_ctx_preference(lexer_name)
        sql = "BEGIN\nCTX_DDL.CREATE_PREFERENCE('#{lexer_name}', '#{lexer_type}');\n"
        options.each do |key, value|
          plsql_value = case value
          when String; "'#{value}'"
          when true; "'YES'"
          when false; "'NO'"
          when nil; 'NULL'
          else value
          end
          sql << "CTX_DDL.SET_ATTRIBUTE('#{lexer_name}', '#{key}', #{plsql_value});\n"
        end
        sql << "END;\n"
        execute sql
      end

      def drop_ctx_preference(preference_name)
        execute "BEGIN CTX_DDL.DROP_PREFERENCE('#{preference_name}'); END;" rescue nil
      end

      def create_index_column_trigger(table_name, index_name, index_column, index_column_source)
        trigger_name = default_index_column_trigger_name(index_name)
        columns = Array(index_column_source)
        quoted_column_names = columns.map{|col| quote_column_name(col)}.join(', ')
        execute compress_lines(<<-SQL)
          CREATE OR REPLACE TRIGGER #{quote_table_name(trigger_name)}
          BEFORE UPDATE OF #{quoted_column_names} ON #{quote_table_name(table_name)} FOR EACH ROW
          BEGIN
            :new.#{quote_column_name(index_column)} := '1';
          END;
        SQL
      end

      def drop_index_column_trigger(index_name)
        trigger_name = default_index_column_trigger_name(index_name)
        execute "DROP TRIGGER #{quote_table_name(trigger_name)}" rescue nil
      end

      def default_datastore_procedure(index_name)
        "#{index_name}_prc"
      end

      def default_datastore_name(index_name)
        "#{index_name}_dst"
      end

      def default_storage_name(index_name)
        "#{index_name}_sto"
      end

      def default_index_column_trigger_name(index_name)
        "#{index_name}_trg"
      end

      def default_lexer_name(index_name)
        "#{index_name}_lex"
      end

      module BaseClassMethods
        # Declare that model table has context index defined.
        # As a result <tt>contains</tt> class scope method is defined.
        def has_context_index
          extend ContextIndexClassMethods
        end
      end

      module ContextIndexClassMethods
        # Add context index condition.
        case ::ActiveRecord::VERSION::MAJOR
        when 3
          def contains(column, query, options ={})
            score_label = options[:label].to_i || 1
            where("CONTAINS(#{connection.quote_column_name(column)}, ?, #{score_label}) > 0", query).
              order("SCORE(#{score_label}) DESC")
          end
        when 2
          def contains(column, query, options ={})
            score_label = options[:label].to_i || 1
            scoped(:conditions => ["CONTAINS(#{connection.quote_column_name(column)}, ?, #{score_label}) > 0", query],
              :order => "SCORE(#{score_label}) DESC")
          end
        end
      end

    end
    
  end
end

ActiveRecord::ConnectionAdapters::OracleEnhancedAdapter.class_eval do
  include ActiveRecord::ConnectionAdapters::OracleEnhancedContextIndex
end

ActiveRecord::Base.class_eval do
  extend ActiveRecord::ConnectionAdapters::OracleEnhancedContextIndex::BaseClassMethods
end
