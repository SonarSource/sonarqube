require 'digest/sha1'

module ActiveRecord
  module ConnectionAdapters
    module OracleEnhancedSchemaStatements
      # SCHEMA STATEMENTS ========================================
      #
      # see: abstract/schema_statements.rb

      # Additional options for +create_table+ method in migration files.
      #
      # You can specify individual starting value in table creation migration file, e.g.:
      #
      #   create_table :users, :sequence_start_value => 100 do |t|
      #     # ...
      #   end
      #
      # You can also specify other sequence definition additional parameters, e.g.:
      #
      #   create_table :users, :sequence_start_value => “100 NOCACHE INCREMENT BY 10” do |t|
      #     # ...
      #   end
      #
      # Create primary key trigger (so that you can skip primary key value in INSERT statement).
      # By default trigger name will be "table_name_pkt", you can override the name with 
      # :trigger_name option (but it is not recommended to override it as then this trigger will
      # not be detected by ActiveRecord model and it will still do prefetching of sequence value).
      # Example:
      # 
      #   create_table :users, :primary_key_trigger => true do |t|
      #     # ...
      #   end
      #
      # It is possible to add table and column comments in table creation migration files:
      #
      #   create_table :employees, :comment => “Employees and contractors” do |t|
      #     t.string      :first_name, :comment => “Given name”
      #     t.string      :last_name, :comment => “Surname”
      #   end
      
      def create_table(name, options = {}, &block)
        create_sequence = options[:id] != false
        column_comments = {}
        
        table_definition = TableDefinition.new(self)
        table_definition.primary_key(options[:primary_key] || Base.get_primary_key(name.to_s.singularize)) unless options[:id] == false

        # store that primary key was defined in create_table block
        unless create_sequence
          class << table_definition
            attr_accessor :create_sequence
            def primary_key(*args)
              self.create_sequence = true
              super(*args)
            end
          end
        end

        # store column comments
        class << table_definition
          attr_accessor :column_comments
          def column(name, type, options = {})
            if options[:comment]
              self.column_comments ||= {}
              self.column_comments[name] = options[:comment]
            end
            super(name, type, options)
          end
        end

        result = block.call(table_definition) if block
        create_sequence = create_sequence || table_definition.create_sequence
        column_comments = table_definition.column_comments if table_definition.column_comments
        tablespace = tablespace_for(:table, options[:tablespace])

        if options[:force] && table_exists?(name)
          drop_table(name, options)
        end

        create_sql = "CREATE#{' GLOBAL TEMPORARY' if options[:temporary]} TABLE "
        create_sql << quote_table_name(name)
        create_sql << " (#{table_definition.to_sql})"
        unless options[:temporary]
          create_sql << " ORGANIZATION #{options[:organization]}" if options[:organization]
          create_sql << tablespace
          table_definition.lob_columns.each{|cd| create_sql << tablespace_for(cd.sql_type.downcase.to_sym, nil, name, cd.name)}
        end
        create_sql << " #{options[:options]}"
        execute create_sql
        
        create_sequence_and_trigger(name, options) if create_sequence
        
        add_table_comment name, options[:comment]
        column_comments.each do |column_name, comment|
          add_comment name, column_name, comment
        end
        
      end

      def rename_table(name, new_name) #:nodoc:
        execute "RENAME #{quote_table_name(name)} TO #{quote_table_name(new_name)}"
        execute "RENAME #{quote_table_name("#{name}_seq")} TO #{quote_table_name("#{new_name}_seq")}" rescue nil
      end

      def drop_table(name, options = {}) #:nodoc:
        super(name)
        seq_name = options[:sequence_name] || default_sequence_name(name)
        execute "DROP SEQUENCE #{quote_table_name(seq_name)}" rescue nil
      ensure
        clear_table_columns_cache(name)
      end

      # clear cached indexes when adding new index
      def add_index(table_name, column_name, options = {}) #:nodoc:
        column_names = Array(column_name)
		# sonar - see below
        index_name   = nil
		# /sonar

        if Hash === options # legacy support, since this param was a string
          index_type = options[:unique] ? "UNIQUE" : ""
          index_name = options[:name].to_s if options.key?(:name)
          tablespace = tablespace_for(:index, options[:tablespace])
        else
          index_type = options
        end
        
		# sonar - move the call to index_name() in order to remove the log "Oracle enhanced shortened index name" even if the index name 
		# is explicitly set by migrations with the :name option
        index_name   = index_name(table_name, :column => column_names) unless index_name
		# /sonar

        if index_name.to_s.length > index_name_length
          raise ArgumentError, "Index name '#{index_name}' on table '#{table_name}' is too long; the limit is #{index_name_length} characters"
        end
        if index_name_exists?(table_name, index_name, false)
          raise ArgumentError, "Index name '#{index_name}' on table '#{table_name}' already exists"
        end
        quoted_column_names = column_names.map { |e| quote_column_name_or_expression(e) }.join(", ")

        execute "CREATE #{index_type} INDEX #{quote_column_name(index_name)} ON #{quote_table_name(table_name)} (#{quoted_column_names})#{tablespace} #{options[:options]}"
      ensure
        self.all_schema_indexes = nil
      end

      # Remove the given index from the table.
      # Gives warning if index does not exist
      def remove_index(table_name, options = {}) #:nodoc:
        index_name = index_name(table_name, options)
        unless index_name_exists?(table_name, index_name, true)
          raise ArgumentError, "Index name '#{index_name}' on table '#{table_name}' does not exist"
        end
        remove_index!(table_name, index_name)
      end

      # clear cached indexes when removing index
      def remove_index!(table_name, index_name) #:nodoc:
        execute "DROP INDEX #{quote_column_name(index_name)}"
      ensure
        self.all_schema_indexes = nil
      end

      # returned shortened index name if default is too large
      def index_name(table_name, options) #:nodoc:
        default_name = super(table_name, options).to_s
        # sometimes options can be String or Array with column names
        options = {} unless options.is_a?(Hash)
        identifier_max_length = options[:identifier_max_length] || index_name_length
        return default_name if default_name.length <= identifier_max_length
        
        # remove 'index', 'on' and 'and' keywords
        shortened_name = "i_#{table_name}_#{Array(options[:column]) * '_'}"
        
        # leave just first three letters from each word
        if shortened_name.length > identifier_max_length
          shortened_name = shortened_name.split('_').map{|w| w[0,3]}.join('_')
        end
        # generate unique name using hash function
        if shortened_name.length > identifier_max_length
          shortened_name = 'i'+Digest::SHA1.hexdigest(default_name)[0,identifier_max_length-1]
        end
        @logger.warn "#{adapter_name} shortened default index name #{default_name} to #{shortened_name}" if @logger
        shortened_name
      end

      # Verify the existence of an index with a given name.
      #
      # The default argument is returned if the underlying implementation does not define the indexes method,
      # as there's no way to determine the correct answer in that case.
      #
      # Will always query database and not index cache.
      def index_name_exists?(table_name, index_name, default)
        (owner, table_name, db_link) = @connection.describe(table_name)
        result = select_value(<<-SQL)
          SELECT 1 FROM all_indexes#{db_link} i
          WHERE i.owner = '#{owner}'
             AND i.table_owner = '#{owner}'
             AND i.table_name = '#{table_name}'
             AND i.index_name = '#{index_name.to_s.upcase}'
        SQL
        result == 1
      end

      def rename_index(table_name, index_name, new_index_name) #:nodoc:
        unless index_name_exists?(table_name, index_name, true)
          raise ArgumentError, "Index name '#{index_name}' on table '#{table_name}' does not exist"
        end
        execute "ALTER INDEX #{quote_column_name(index_name)} rename to #{quote_column_name(new_index_name)}"
      ensure
        self.all_schema_indexes = nil
      end

      def add_column(table_name, column_name, type, options = {}) #:nodoc:
        add_column_sql = "ALTER TABLE #{quote_table_name(table_name)} ADD #{quote_column_name(column_name)} #{type_to_sql(type, options[:limit], options[:precision], options[:scale])}"
        add_column_options!(add_column_sql, options.merge(:type=>type, :column_name=>column_name, :table_name=>table_name))
        add_column_sql << tablespace_for((type_to_sql(type).downcase.to_sym), nil, table_name, column_name)
        execute(add_column_sql)
      ensure
        clear_table_columns_cache(table_name)
      end

      def change_column_default(table_name, column_name, default) #:nodoc:
        execute "ALTER TABLE #{quote_table_name(table_name)} MODIFY #{quote_column_name(column_name)} DEFAULT #{quote(default)}"
      ensure
        clear_table_columns_cache(table_name)
      end

      def change_column_null(table_name, column_name, null, default = nil) #:nodoc:
        column = column_for(table_name, column_name)

        unless null || default.nil?
          execute("UPDATE #{quote_table_name(table_name)} SET #{quote_column_name(column_name)}=#{quote(default)} WHERE #{quote_column_name(column_name)} IS NULL")
        end

        change_column table_name, column_name, column.sql_type, :null => null
      end

      def change_column(table_name, column_name, type, options = {}) #:nodoc:
        column = column_for(table_name, column_name)

        # remove :null option if its value is the same as current column definition
        # otherwise Oracle will raise error
        if options.has_key?(:null) && options[:null] == column.null
          options[:null] = nil
        end

        change_column_sql = "ALTER TABLE #{quote_table_name(table_name)} MODIFY #{quote_column_name(column_name)} #{type_to_sql(type, options[:limit], options[:precision], options[:scale])}"
        add_column_options!(change_column_sql, options.merge(:type=>type, :column_name=>column_name, :table_name=>table_name))
        change_column_sql << tablespace_for((type_to_sql(type).downcase.to_sym), nil, options[:table_name], options[:column_name])
        execute(change_column_sql)
      ensure
        clear_table_columns_cache(table_name)
      end

      def rename_column(table_name, column_name, new_column_name) #:nodoc:
        execute "ALTER TABLE #{quote_table_name(table_name)} RENAME COLUMN #{quote_column_name(column_name)} to #{quote_column_name(new_column_name)}"
      ensure
        clear_table_columns_cache(table_name)
      end

      def remove_column(table_name, column_name) #:nodoc:
        execute "ALTER TABLE #{quote_table_name(table_name)} DROP COLUMN #{quote_column_name(column_name)}"
      ensure
        clear_table_columns_cache(table_name)
      end

      def add_comment(table_name, column_name, comment) #:nodoc:
        return if comment.blank?
        execute "COMMENT ON COLUMN #{quote_table_name(table_name)}.#{column_name} IS '#{comment}'"
      end

      def add_table_comment(table_name, comment) #:nodoc:
        return if comment.blank?
        execute "COMMENT ON TABLE #{quote_table_name(table_name)} IS '#{comment}'"
      end

      def table_comment(table_name) #:nodoc:
        (owner, table_name, db_link) = @connection.describe(table_name)
        select_value <<-SQL
          SELECT comments FROM all_tab_comments#{db_link}
          WHERE owner = '#{owner}'
            AND table_name = '#{table_name}'
        SQL
      end

      def column_comment(table_name, column_name) #:nodoc:
        (owner, table_name, db_link) = @connection.describe(table_name)
        select_value <<-SQL
          SELECT comments FROM all_col_comments#{db_link}
          WHERE owner = '#{owner}'
            AND table_name = '#{table_name}'
            AND column_name = '#{column_name.upcase}'
        SQL
      end

      # Maps logical Rails types to Oracle-specific data types.
      def type_to_sql(type, limit = nil, precision = nil, scale = nil) #:nodoc:
        # Ignore options for :text and :binary columns
        return super(type, nil, nil, nil) if ['text', 'binary'].include?(type.to_s)

        super
      end

      def tablespace(table_name)
        select_value <<-SQL
          SELECT tablespace_name
          FROM user_tables
          WHERE table_name='#{table_name.to_s.upcase}'
        SQL
      end

      private

      def tablespace_for(obj_type, tablespace_option, table_name=nil, column_name=nil)
        tablespace_sql = ''
        if tablespace = (tablespace_option || default_tablespace_for(obj_type))
          tablespace_sql << if [:blob, :clob].include?(obj_type.to_sym)
           " LOB (#{column_name}) STORE AS #{column_name.to_s[0..10]}_#{table_name.to_s[0..14]}_ls (TABLESPACE #{tablespace})"
          else
           " TABLESPACE #{tablespace}"
          end
        end
        tablespace_sql
      end

      def default_tablespace_for(type)
        (default_tablespaces[type] || default_tablespaces[native_database_types[type][:name]]) rescue nil
      end


      def column_for(table_name, column_name)
        unless column = columns(table_name).find { |c| c.name == column_name.to_s }
          raise "No such column: #{table_name}.#{column_name}"
        end
        column
      end

      def create_sequence_and_trigger(table_name, options)
        seq_name = options[:sequence_name] || default_sequence_name(table_name)
        seq_start_value = options[:sequence_start_value] || default_sequence_start_value
        execute "CREATE SEQUENCE #{quote_table_name(seq_name)} START WITH #{seq_start_value}"

        create_primary_key_trigger(table_name, options) if options[:primary_key_trigger]
      end
      
      def create_primary_key_trigger(table_name, options)
        seq_name = options[:sequence_name] || default_sequence_name(table_name)
        trigger_name = options[:trigger_name] || default_trigger_name(table_name)
        primary_key = options[:primary_key] || Base.get_primary_key(table_name.to_s.singularize)
        execute compress_lines(<<-SQL)
          CREATE OR REPLACE TRIGGER #{quote_table_name(trigger_name)}
          BEFORE INSERT ON #{quote_table_name(table_name)} FOR EACH ROW
          BEGIN
            IF inserting THEN
              IF :new.#{quote_column_name(primary_key)} IS NULL THEN
                SELECT #{quote_table_name(seq_name)}.NEXTVAL INTO :new.#{quote_column_name(primary_key)} FROM dual;
              END IF;
            END IF;
          END;
        SQL
      end

      def default_trigger_name(table_name)
        # truncate table name if necessary to fit in max length of identifier
        "#{table_name.to_s[0,table_name_length-4]}_pkt"
      end

    end
  end
end

ActiveRecord::ConnectionAdapters::OracleEnhancedAdapter.class_eval do
  include ActiveRecord::ConnectionAdapters::OracleEnhancedSchemaStatements
end
