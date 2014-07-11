require 'arel/visitors/compat'

module Arel
  module Visitors
    class HSQLDB < Arel::Visitors::ToSql
      def visit_Arel_Nodes_SelectStatement o
        [
          limit_offset(o.cores.map { |x| visit_Arel_Nodes_SelectCore x }.join, o),
          ("ORDER BY #{o.orders.map { |x| visit x }.join(', ')}" unless o.orders.empty?),
        ].compact.join ' '
      end

      def limit_offset sql, o
        offset = o.offset || 0
        bef = sql[7..-1]
        if limit = o.limit
          "SELECT LIMIT #{offset} #{limit_for(limit)} #{bef}"
        elsif offset > 0
          "SELECT LIMIT #{offset} 0 #{bef}"
        else
          sql
        end
      end
    end
  end
end
