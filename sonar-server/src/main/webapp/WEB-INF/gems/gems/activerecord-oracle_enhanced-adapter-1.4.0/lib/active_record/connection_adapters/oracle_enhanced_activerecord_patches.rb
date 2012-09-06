# ActiveRecord 2.3 patches
if ActiveRecord::VERSION::MAJOR == 2 && ActiveRecord::VERSION::MINOR == 3
  require "active_record/associations"

  ActiveRecord::Associations::ClassMethods.module_eval do
    private
    def tables_in_string(string)
      return [] if string.blank?
      if self.connection.adapter_name == "OracleEnhanced"
        # always convert table names to downcase as in Oracle quoted table names are in uppercase
        # ignore raw_sql_ that is used by Oracle adapter as alias for limit/offset subqueries
        string.scan(/([a-zA-Z_][\.\w]+).?\./).flatten.map(&:downcase).uniq - ['raw_sql_']
      else
        string.scan(/([\.a-zA-Z_]+).?\./).flatten
      end
    end
  end

  ActiveRecord::Associations::ClassMethods::JoinDependency::JoinAssociation.class_eval do
    protected
    def aliased_table_name_for(name, suffix = nil)
      # always downcase quoted table name as Oracle quoted table names are in uppercase
      if !parent.table_joins.blank? && parent.table_joins.to_s.downcase =~ %r{join(\s+\w+)?\s+#{active_record.connection.quote_table_name(name).downcase}\son}
        @join_dependency.table_aliases[name] += 1
      end

      unless @join_dependency.table_aliases[name].zero?
        # if the table name has been used, then use an alias
        name = active_record.connection.table_alias_for "#{pluralize(reflection.name)}_#{parent_table_name}#{suffix}"
        table_index = @join_dependency.table_aliases[name]
        @join_dependency.table_aliases[name] += 1
        name = name[0..active_record.connection.table_alias_length-3] + "_#{table_index+1}" if table_index > 0
      else
        @join_dependency.table_aliases[name] += 1
      end

      name
    end
  end

end
