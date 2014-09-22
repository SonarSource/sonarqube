#
# SonarQube, open source software quality management tool.
# Copyright (C) 2008-2014 SonarSource
# mailto:contact AT sonarsource DOT com
#
# SonarQube is free software; you can redistribute it and/or
# modify it under the terms of the GNU Lesser General Public
# License as published by the Free Software Foundation; either
# version 3 of the License, or (at your option) any later version.
#
# SonarQube is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
# Lesser General Public License for more details.
#
# You should have received a copy of the GNU Lesser General Public License
# along with this program; if not, write to the Free Software Foundation,
# Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
#
require 'set'
class MeasureFilterDisplayList < MeasureFilterDisplay
  KEY = :list
  MAX_PAGE_SIZE = 250

  class Column
    attr_reader :key, :metric, :period

    def initialize(key)
      @key = key
      fields = @key.split(':')
      if fields.size>=2 && fields[0]=='metric'
        @metric = Metric.by_key(fields[1])
        @period = fields[2].to_i if fields.size>=3
      end
    end

    def title_label
      if @metric
        label = @metric.abbreviation
      else
        label = Api::Utils.message("measure_filter.abbr.#{@key}", :default => @key)
      end
      label
    end

    def tooltip
      if @metric
        tooltip = @metric.description
      else
        tooltip = Api::Utils.message("measure_filter.col.#{@key}", :default => @key)
      end
      tooltip
    end

    def align
      @align ||=
        begin
          # by default is table cells are left-aligned
          (@key=='name' || @key=='short_name' || @key=='description') ? '' : 'right'
        end
    end

    def title_css
      'thin' if @metric && @metric.val_type==Metric::VALUE_TYPE_LEVEL
    end

    def row_css
      'nowrap' unless (@metric && !@metric.numeric?) || @key=='description'
    end

    def sort?
      !links?
    end

    def links?
      @key == 'links'
    end

  end

  attr_reader :columns

  def initialize(filter, options)
    super(filter, options)

    # default values
    filter.set_criteria_default_value(:cols, ['metric:alert_status', 'name', 'date', 'metric:ncloc', 'metric:violations', 'links'])
    filter.set_criteria_default_value(:sort, 'name')
    filter.set_criteria_default_value(:asc, true)
    filter.set_criteria_default_value(:pageSize, 100)
    filter.set_criteria_value(:pageSize, MAX_PAGE_SIZE) if filter.criteria(:pageSize).to_i>MAX_PAGE_SIZE

    @columns = []
    metrics = []
    filter.criteria(:cols).each do |column_key|
      column = Column.new(column_key)
      @columns << column
      metrics << column.metric if column.metric
      filter.require_links=true if column.links?
    end
    filter.metrics=(metrics)
  end

  PROPERTY_KEYS = Set.new([:cols, :sort, :asc, :pageSize])

  def url_params
    @filter.criteria.select { |k, v| PROPERTY_KEYS.include?(k.to_sym) }
  end
end
