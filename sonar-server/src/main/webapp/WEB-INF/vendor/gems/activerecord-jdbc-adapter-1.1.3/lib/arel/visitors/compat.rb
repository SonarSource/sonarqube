module Arel
  module Visitors
    module ArJdbcCompat
      def limit_for(limit_or_node)
        limit_or_node.respond_to?(:expr) ? limit_or_node.expr.to_i : limit_or_node
      end
    end

    class ToSql
      include ArJdbcCompat
    end
  end
end
