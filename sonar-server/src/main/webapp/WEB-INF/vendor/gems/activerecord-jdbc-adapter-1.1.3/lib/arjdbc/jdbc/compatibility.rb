# AR's 2.2 version of this method is sufficient, but we need it for
# older versions
if ActiveRecord::VERSION::MAJOR <= 2 && ActiveRecord::VERSION::MINOR < 2
  module ActiveRecord
    module ConnectionAdapters # :nodoc:
      module SchemaStatements
        # Convert the speficied column type to a SQL string.
        def type_to_sql(type, limit = nil, precision = nil, scale = nil)
          if native = native_database_types[type]
            column_type_sql = (native.is_a?(Hash) ? native[:name] : native).dup

            if type == :decimal # ignore limit, use precision and scale
              scale ||= native[:scale]

              if precision ||= native[:precision]
                if scale
                  column_type_sql << "(#{precision},#{scale})"
                else
                  column_type_sql << "(#{precision})"
                end
              elsif scale
                raise ArgumentError, "Error adding decimal column: precision cannot be empty if scale if specified"
              end

            elsif limit ||= native.is_a?(Hash) && native[:limit]
              column_type_sql << "(#{limit})"
            end

            column_type_sql
          else
            type
          end
        end
      end
    end
  end
end

module ActiveRecord
  module ConnectionAdapters
    module CompatibilityMethods
      def self.needed?(base)
        !base.instance_methods.include?("quote_table_name")
      end

      def quote_table_name(name)
        quote_column_name(name)
      end
    end
  end
end
