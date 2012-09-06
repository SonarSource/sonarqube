module ActiveRecord
  module ConnectionAdapters #:nodoc:
    class OracleEnhancedColumn < Column

      attr_reader :table_name, :forced_column_type, :nchar, :virtual_column_data_default #:nodoc:
      
      def initialize(name, default, sql_type = nil, null = true, table_name = nil, forced_column_type = nil, virtual=false) #:nodoc:
        @table_name = table_name
        @forced_column_type = forced_column_type
        @virtual = virtual
        super(name, default, sql_type, null)
        @virtual_column_data_default = default.inspect if virtual
        # Is column NCHAR or NVARCHAR2 (will need to use N'...' value quoting for these data types)?
        # Define only when needed as adapter "quote" method will check at first if instance variable is defined.
        @nchar = true if @type == :string && sql_type[0,1] == 'N'
      end

      def type_cast(value) #:nodoc:
        return OracleEnhancedColumn::string_to_raw(value) if type == :raw
        return guess_date_or_time(value) if type == :datetime && OracleEnhancedAdapter.emulate_dates
        super
      end

      def virtual?
        @virtual
      end

      # convert something to a boolean
      # added y as boolean value
      def self.value_to_boolean(value) #:nodoc:
        if value == true || value == false
          value
        elsif value.is_a?(String) && value.blank?
          nil
        else
          %w(true t 1 y +).include?(value.to_s.downcase)
        end
      end

      # convert Time or DateTime value to Date for :date columns
      def self.string_to_date(string) #:nodoc:
        return string.to_date if string.is_a?(Time) || string.is_a?(DateTime)
        super
      end

      # convert Date value to Time for :datetime columns
      def self.string_to_time(string) #:nodoc:
        return string.to_time if string.is_a?(Date) && !OracleEnhancedAdapter.emulate_dates
        super
      end

      # convert RAW column values back to byte strings.
      def self.string_to_raw(string) #:nodoc:
        string
      end

      # Get column comment from schema definition.
      # Will work only if using default ActiveRecord connection.
      def comment
        ActiveRecord::Base.connection.column_comment(@table_name, name)
      end
      
      private

      def simplified_type(field_type)
        forced_column_type ||
        case field_type
        when /decimal|numeric|number/i
          if OracleEnhancedAdapter.emulate_booleans && field_type == 'NUMBER(1)'
            :boolean
          elsif extract_scale(field_type) == 0 ||
                # if column name is ID or ends with _ID
                OracleEnhancedAdapter.emulate_integers_by_column_name && OracleEnhancedAdapter.is_integer_column?(name, table_name)
            :integer
          else
            :decimal
          end
        when /raw/i
          :raw
        when /char/i
          if OracleEnhancedAdapter.emulate_booleans_from_strings &&
             OracleEnhancedAdapter.is_boolean_column?(name, field_type, table_name)
            :boolean
          else
            :string
          end
        when /date/i
          if OracleEnhancedAdapter.emulate_dates_by_column_name && OracleEnhancedAdapter.is_date_column?(name, table_name)
            :date
          else
            :datetime
          end
        when /timestamp/i
          :timestamp
        when /time/i
          :datetime
        else
          super
        end
      end

      def guess_date_or_time(value)
        value.respond_to?(:hour) && (value.hour == 0 and value.min == 0 and value.sec == 0) ?
          Date.new(value.year, value.month, value.day) : value
      end
      
      class << self
        protected

        def fallback_string_to_date(string) #:nodoc:
          if OracleEnhancedAdapter.string_to_date_format || OracleEnhancedAdapter.string_to_time_format
            return (string_to_date_or_time_using_format(string).to_date rescue super)
          end
          super
        end

        def fallback_string_to_time(string) #:nodoc:
          if OracleEnhancedAdapter.string_to_time_format || OracleEnhancedAdapter.string_to_date_format
            return (string_to_date_or_time_using_format(string).to_time rescue super)
          end
          super
        end

        def string_to_date_or_time_using_format(string) #:nodoc:
          if OracleEnhancedAdapter.string_to_time_format && dt=Date._strptime(string, OracleEnhancedAdapter.string_to_time_format)
            return Time.parse("#{dt[:year]}-#{dt[:mon]}-#{dt[:mday]} #{dt[:hour]}:#{dt[:min]}:#{dt[:sec]}#{dt[:zone]}")
          end
          DateTime.strptime(string, OracleEnhancedAdapter.string_to_date_format).to_date
        end
        
      end
    end

  end

end
