require 'arel/visitors/compat'

module Arel
  module Visitors
    class Firebird < Arel::Visitors::ToSql
      def visit_Arel_Nodes_SelectStatement o
        [
         o.cores.map { |x| visit_Arel_Nodes_SelectCore x }.join,
         ("ORDER BY #{o.orders.map { |x| visit x }.join(', ')}" unless o.orders.empty?),
         ("ROWS #{limit_for(o.limit)} " if o.limit),
         ("TO #{o.offset} " if o.offset),
        ].compact.join ' '
      end

    end
  end
end
