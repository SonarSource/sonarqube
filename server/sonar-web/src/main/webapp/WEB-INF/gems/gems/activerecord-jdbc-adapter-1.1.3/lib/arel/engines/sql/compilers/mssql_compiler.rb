module Arel
  module SqlCompiler
    class MsSQLCompiler < GenericCompiler
      def select_sql
        projections = @relation.projections
        offset = relation.skipped
        limit = relation.taken
        if Count === projections.first && projections.size == 1 &&
          (relation.taken.present? || relation.wheres.present?) && relation.joins(self).blank?
          subquery = [
            "SELECT * FROM #{relation.from_clauses}", build_clauses
          ].join ' '
          @engine.connection.add_limit_offset!(subquery, :limit => limit, :offset => offset) if offset || limit
          query = "SELECT COUNT(*) AS count_id FROM (#{subquery}) AS subquery"
        else
          query = [
            "SELECT     #{relation.select_clauses.join(', ')}",
            "FROM       #{relation.from_clauses}",
            build_clauses
          ].compact.join ' '
          @engine.connection.add_limit_offset!(query, :limit => limit, :offset => offset) if offset || limit
        end
        query
      end

      def build_clauses
        joins   = relation.joins(self)
        wheres  = relation.where_clauses
        groups  = relation.group_clauses
        havings = relation.having_clauses
        orders  = relation.order_clauses

        clauses = [ "",
          joins,
          ("WHERE     #{wheres.join(' AND ')}" unless wheres.empty?),
          ("GROUP BY  #{groups.join(', ')}" unless groups.empty?),
          ("HAVING    #{havings.join(' AND ')}" unless havings.empty?),
          ("ORDER BY  #{orders.join(', ')}" unless orders.empty?)
        ].compact.join ' '

        clauses << " #{locked}" unless locked.blank?
        clauses unless clauses.blank?
      end
    end
  end
end
