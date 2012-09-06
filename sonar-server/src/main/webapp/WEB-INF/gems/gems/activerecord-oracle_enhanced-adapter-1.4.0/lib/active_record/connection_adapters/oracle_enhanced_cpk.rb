module ActiveRecord #:nodoc:
  module ConnectionAdapters #:nodoc:
    module OracleEnhancedCpk #:nodoc:

      # This mightn't be in Core, but count(distinct x,y) doesn't work for me.
      # Return that not supported if composite_primary_keys gem is required.
      def supports_count_distinct? #:nodoc:
        @supports_count_distinct ||= ! defined?(CompositePrimaryKeys)
      end
      
      def concat(*columns) #:nodoc:
        "(#{columns.join('||')})"
      end
      
    end
  end
end

ActiveRecord::ConnectionAdapters::OracleEnhancedAdapter.class_eval do
  include ActiveRecord::ConnectionAdapters::OracleEnhancedCpk
end
