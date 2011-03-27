module JdbcSpec
  module MissingFunctionalityHelper
    #Taken from SQLite adapter

    def alter_table(table_name, options = {}) #:nodoc:
      table_name = table_name.to_s.downcase
      altered_table_name = "altered_#{table_name}"
      caller = lambda {|definition| yield definition if block_given?}

      transaction do
        move_table(table_name, altered_table_name, options)
        move_table(altered_table_name, table_name, &caller)
      end
    end
    
    def move_table(from, to, options = {}, &block) #:nodoc:
      copy_table(from, to, options, &block)
      drop_table(from)
    end

    def copy_table(from, to, options = {}) #:nodoc:
      create_table(to, options) do |@definition|
        columns(from).each do |column|
          column_name = options[:rename] ?
          (options[:rename][column.name] ||
           options[:rename][column.name.to_sym] ||
           column.name) : column.name
          column_name = column_name.to_s
          @definition.column(column_name, column.type, 
                             :limit => column.limit, :default => column.default,
                             :null => column.null)
        end
        @definition.primary_key(primary_key(from))
        yield @definition if block_given?
      end
      
      copy_table_indexes(from, to)
      copy_table_contents(from, to, 
                          @definition.columns, 
                          options[:rename] || {})
    end
    
    def copy_table_indexes(from, to) #:nodoc:
      indexes(from).each do |index|
        name = index.name.downcase
        if to == "altered_#{from}"
          name = "temp_#{name}"
        elsif from == "altered_#{to}"
          name = name[5..-1]
        end
        
        # index name can't be the same
        opts = { :name => name.gsub(/_(#{from})_/, "_#{to}_") }
        opts[:unique] = true if index.unique
        add_index(to, index.columns, opts)
      end
    end
    
    def copy_table_contents(from, to, columns, rename = {}) #:nodoc:
      column_mappings = Hash[*columns.map {|col| [col.name, col.name]}.flatten]
      rename.inject(column_mappings) {|map, a| map[a.last] = a.first; map}
      from_columns = columns(from).collect {|col| col.name}
      columns = columns.find_all{|col| from_columns.include?(column_mappings[col.name])}
      execute("SELECT * FROM #{from}").each do |row|
        sql = "INSERT INTO #{to} ("+columns.map(&:name)*','+") VALUES ("            
        sql << columns.map {|col| quote(row[column_mappings[col.name]],col)} * ', '
        sql << ')'
        execute sql
      end
    end
  end
end
