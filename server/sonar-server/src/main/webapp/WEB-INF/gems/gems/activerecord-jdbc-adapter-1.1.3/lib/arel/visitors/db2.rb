require 'arel/visitors/compat'

module Arel
  module Visitors
    class DB2 < Arel::Visitors::ToSql
      def visit_Arel_Nodes_SelectStatement o
        add_limit_offset([o.cores.map { |x| visit_Arel_Nodes_SelectCore x }.join,
         ("ORDER BY #{o.orders.map { |x| visit x }.join(', ')}" unless o.orders.empty?),
        ].compact.join(' '), o)
      end

      def add_limit_offset(sql, o)
        @connection.replace_limit_offset! sql, limit_for(o.limit), o.offset && o.offset.value
      end
    end
  end
end
