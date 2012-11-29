#
# Sonar, open source software quality management tool.
# Copyright (C) 2008-2012 SonarSource
# mailto:contact AT sonarsource DOT com
#
# Sonar is free software; you can redistribute it and/or
# modify it under the terms of the GNU Lesser General Public
# License as published by the Free Software Foundation; either
# version 3 of the License, or (at your option) any later version.
#
# Sonar is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
# Lesser General Public License for more details.
#
# You should have received a copy of the GNU Lesser General Public
# License along with Sonar; if not, write to the Free Software
# Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
#

#
# Sonar 3.4
#
class MoveExistingMeasureFilters < ActiveRecord::Migration

  # the new table
  class MeasureFilter < ActiveRecord::Base
  end

  # the old tables
  class FilterColumn < ActiveRecord::Base
    set_table_name 'filter_columns'
  end
  class Criteria < ActiveRecord::Base
    set_table_name 'criteria'
  end
  class Resource < ActiveRecord::Base
    set_table_name 'projects'
  end
  class Filter < ActiveRecord::Base
    set_table_name 'filters'
  end


  def self.up
    old_filters = Filter.find(:all)
    say_with_time "Moving #{old_filters.size} measure filters" do
      old_filters.each do |old_filter|
        move(old_filter)
      end
    end
  end

  private

  def self.move(old_filter)
    new_filter = MeasureFilter.new
    new_filter.name = old_filter.name
    new_filter.user_id = old_filter.user_id
    new_filter.system = old_filter.user_id.nil?
    new_filter.shared = (old_filter.shared || old_filter.user_id.nil?)
    data = []
    data << 'onFavourites=true' if old_filter.favourites
    data << "baseId=#{old_filter.resource_id}" if old_filter.resource_id
    data << "pageSize=#{old_filter.page_size}" if old_filter.page_size
    data << "display=#{old_filter.default_value || 'list'}"

    columns = []
    asc = nil
    sort = nil
    old_columns = FilterColumn.find(:all, :conditions => ['filter_id=?', old_filter.id], :order => 'order_index')
    old_columns.each do |old_column|
      column_key = old_column.family
      column_key += ":#{old_column.kee}" if old_column.kee
      columns << column_key
      # TODO old_column.variation
      if old_column.sort_direction=='ASC'
        asc = true
        sort = column_key
      elsif old_column.sort_direction=='DESC'
        asc = false
        sort = column_key
      end
    end
    data << "columns=#{columns.join(',')}" unless columns.empty?
    if sort
      data << "sort=#{sort}"
      data << "asc=#{asc}"
    end

    # TODO move criteria

    new_filter.data = data.join('|') unless data.empty?
    new_filter.save
    # TODO Filter.delete(old_filter.id)
  end
end
