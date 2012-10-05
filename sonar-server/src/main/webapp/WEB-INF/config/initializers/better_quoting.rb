# Override quoting for database without a specific jdbc adapter
# for example, H2
#
# Default quoting leads to bugs, see SONAR-3765
#
module ActiveRecord
  module ConnectionAdapters # :nodoc:
    module Quoting
      def quote_string(s)
        s.gsub(/'/, "''") # ' (for ruby-mode)
      end
    end
  end
end
