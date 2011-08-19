require 'arel/visitors/compat'

module Arel
  module Visitors
    class Derby < Arel::Visitors::ToSql
      def visit_Arel_Nodes_SelectStatement o
        [
         o.cores.map { |x| visit_Arel_Nodes_SelectCore x }.join,
         ("ORDER BY #{o.orders.map { |x| visit x }.join(', ')}" unless o.orders.empty?),
         ("FETCH FIRST #{limit_for(o.limit)} ROWS ONLY" if o.limit),
         (visit(o.offset) if o.offset),
         (visit(o.lock) if o.lock),
        ].compact.join ' '
      end

      def visit_Arel_Nodes_Offset o
        "OFFSET #{visit o.value} ROWS"
      end
    end
  end
end
