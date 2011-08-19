module Arel
  module SqlCompiler
    class HsqldbCompiler < GenericCompiler
      def select_sql
        # HSQLDB needs to add LIMIT in right after SELECT
        query = super
        offset = relation.skipped
        limit = relation.taken
        @engine.connection.add_limit_offset!(query, :limit => limit,
                                             :offset => offset) if offset || limit
        query
      end
    end
  end
end
