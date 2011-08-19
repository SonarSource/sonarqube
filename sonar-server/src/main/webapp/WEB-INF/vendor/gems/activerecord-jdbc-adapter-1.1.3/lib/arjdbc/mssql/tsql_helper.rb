# Common methods for handling TSQL databases.
module TSqlMethods

  def modify_types(tp) #:nodoc:
    tp[:primary_key] = "int NOT NULL IDENTITY(1, 1) PRIMARY KEY"
    tp[:integer][:limit] = nil
    tp[:boolean] = {:name => "bit"}
    tp[:binary] = { :name => "image"}
    tp
  end

  def type_to_sql(type, limit = nil, precision = nil, scale = nil) #:nodoc:
    limit = nil if %w(text binary).include? type.to_s
    return 'uniqueidentifier' if (type.to_s == 'uniqueidentifier')
    return super unless type.to_s == 'integer'

    if limit.nil? || limit == 4
      'int'
    elsif limit == 2
      'smallint'
    elsif limit == 1
      'tinyint'
    else
      'bigint'
    end
  end

  def add_limit_offset!(sql, options)
    if options[:limit] and options[:offset]
      total_rows = select_all("SELECT count(*) as TotalRows from (#{sql.gsub(/\bSELECT(\s+DISTINCT)?\b/i, "SELECT\\1 TOP 1000000000")}) tally")[0]["TotalRows"].to_i
      if (options[:limit] + options[:offset]) >= total_rows
        options[:limit] = (total_rows - options[:offset] >= 0) ? (total_rows - options[:offset]) : 0
      end
      sql.sub!(/^\s*SELECT(\s+DISTINCT)?/i, "SELECT * FROM (SELECT TOP #{options[:limit]} * FROM (SELECT\\1 TOP #{options[:limit] + options[:offset]} ")
      sql << ") AS tmp1"
      if options[:order]
        options[:order] = options[:order].split(',').map do |field|
          parts = field.split(" ")
          tc = parts[0]
          if sql =~ /\.\[/ and tc =~ /\./ # if column quoting used in query
            tc.gsub!(/\./, '\\.\\[')
            tc << '\\]'
          end
          if sql =~ /#{tc} AS (t\d_r\d\d?)/
              parts[0] = $1
          elsif parts[0] =~ /\w+\.(\w+)/
            parts[0] = $1
          end
          parts.join(' ')
        end.join(', ')
        sql << " ORDER BY #{change_order_direction(options[:order])}) AS tmp2 ORDER BY #{options[:order]}"
      else
        sql << " ) AS tmp2"
      end
    elsif sql !~ /^\s*SELECT (@@|COUNT\()/i
      sql.sub!(/^\s*SELECT(\s+DISTINCT)?/i) do
        "SELECT#{$1} TOP #{options[:limit]}"
      end unless options[:limit].nil?
    end
  end
end
