#
# SonarQube, open source software quality management tool.
# Copyright (C) 2008-2013 SonarSource
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

#
# Sonar 3.1
#
class AddKeyToFilters < ActiveRecord::Migration
  class WidgetProperty < ActiveRecord::Base
  end

  class Filter < ActiveRecord::Base
  end

  def self.up
    keys = add_key_column_to_filters()
    use_key_in_widget_properties(keys)
  end

  def self.add_key_column_to_filters
    keys = {}

	begin
      add_column 'filters', 'kee', :string, :null => true, :limit => 100
    rescue
      # Assume the column was already added by a previous migration
    end

    Filter.reset_column_information
    Filter.find(:all).each do |filter|
      keys[filter.id]=filter.user_id ? filter.id : filter.name
      filter.kee=keys[filter.id]
      filter.save
    end

    keys
  end

  def self.use_key_in_widget_properties(keys)
    WidgetProperty.find(:all, :conditions => {:kee => 'filter'}).each do |property|
      property.text_value=keys[property.text_value.to_i]
      property.save
    end
  end

end
