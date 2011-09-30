module ::ArJdbc
  module MsSQL
    module LimitHelpers
      module_function
      def get_table_name(sql)
        if sql =~ /^\s*insert\s+into\s+([^\(\s,]+)\s*|^\s*update\s+([^\(\s,]+)\s*/i
          $1
        elsif sql =~ /\bfrom\s+([^\(\s,]+)\s*/i
          $1
        else
          nil
        end
      end

      module SqlServer2000ReplaceLimitOffset
        module_function
        def replace_limit_offset!(sql, limit, offset, order)
          if limit
            offset ||= 0
            start_row = offset + 1
            end_row = offset + limit.to_i
            find_select = /\b(SELECT(?:\s+DISTINCT)?)\b(.*)/im
            whole, select, rest_of_query = find_select.match(sql).to_a
            if (start_row == 1) && (end_row ==1)
              new_sql = "#{select} TOP 1 #{rest_of_query}"
              sql.replace(new_sql)
            else
              #UGLY
              #KLUDGY?
              #removing out stuff before the FROM...
              rest = rest_of_query[/FROM/i=~ rest_of_query.. -1]
              #need the table name for avoiding amiguity
              table_name = LimitHelpers.get_table_name(sql)
              primary_key = order[/(\w*id\w*)/i]
              #I am not sure this will cover all bases.  but all the tests pass
              new_order = "ORDER BY #{order}, #{table_name}.#{primary_key}" if order.index("#{table_name}.#{primary_key}").nil?
              new_order ||= order

              if (rest_of_query.match(/WHERE/).nil?)
                new_sql = "#{select} TOP #{limit} #{rest_of_query} WHERE #{table_name}.#{primary_key} NOT IN (#{select} TOP #{offset} #{table_name}.#{primary_key} #{rest} #{new_order}) #{order} "
              else
                new_sql = "#{select} TOP #{limit} #{rest_of_query} AND #{table_name}.#{primary_key} NOT IN (#{select} TOP #{offset} #{table_name}.#{primary_key} #{rest} #{new_order}) #{order} "
              end

              sql.replace(new_sql)
            end
          end
          sql
        end
      end

      module SqlServer2000AddLimitOffset
        def add_limit_offset!(sql, options)
          if options[:limit]
            order = "ORDER BY #{options[:order] || determine_order_clause(sql)}"
            sql.sub!(/ ORDER BY.*$/i, '')
            SqlServerReplaceLimitOffset.replace_limit_offset!(sql, options[:limit], options[:offset], order)
          end
        end
      end

      module SqlServerReplaceLimitOffset
        module_function
        def replace_limit_offset!(sql, limit, offset, order)
          if limit
            offset ||= 0
            start_row = offset + 1
            end_row = offset + limit.to_i
            find_select = /\b(SELECT(?:\s+DISTINCT)?)\b(.*)/im
            whole, select, rest_of_query = find_select.match(sql).to_a
            rest_of_query.strip!
            if rest_of_query[0] == "1"
              rest_of_query[0] = "*"
            end
            if rest_of_query[0] == "*"
              from_table = LimitHelpers.get_table_name(rest_of_query)
              rest_of_query = from_table + '.' + rest_of_query
            end
            new_sql = "#{select} t.* FROM (SELECT ROW_NUMBER() OVER(#{order}) AS _row_num, #{rest_of_query}"
            new_sql << ") AS t WHERE t._row_num BETWEEN #{start_row.to_s} AND #{end_row.to_s}"
            sql.replace(new_sql)
          end
          sql
        end
      end

      module SqlServerAddLimitOffset
        def add_limit_offset!(sql, options)
          if options[:limit]
            order = "ORDER BY #{options[:order] || determine_order_clause(sql)}"
            sql.sub!(/ ORDER BY.*$/i, '')
            SqlServerReplaceLimitOffset.replace_limit_offset!(sql, options[:limit], options[:offset], order)
          end
        end
      end
    end
  end
end
