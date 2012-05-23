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
# Sonar 3.1
#
class AddKeyToFilters < ActiveRecord::Migration
  class Filter < ActiveRecord::Base
  end

  class WidgetProperty < ActiveRecord::Base
  end

  class LoadedTemplate < ActiveRecord::Base
  end

  def self.up
    mark_default_filters_as_loaded()
    keys = add_key_column_to_filters()
    use_key_in_widget_properties(keys)
  end

  def self.mark_default_filters_as_loaded
    mark_filter_as_loaded('Projects')
    mark_filter_as_loaded('Treemap')
    mark_filter_as_loaded('My favourites')
  end

  def self.mark_filter_as_loaded(name)
    if Filter.find(:first, :conditions => {:name => name, :user_id => nil})
      unless LoadedTemplate.find(:first, :conditions => {:kee => name, :template_type => 'FILTER'})
        LoadedTemplate.create(:kee => name, :template_type => 'FILTER').save
      end
    end
  end

  def self.add_key_column_to_filters
    keys = {}

    add_column 'filters', 'kee', :string, :null => true, :limit => 100

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
