#
# Sonar, entreprise quality control tool.
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
# Sonar 2.2
#
class CreateFilters < ActiveRecord::Migration

  class Property < ActiveRecord::Base
  end

  def self.up
    create_table 'filters' do |t|
      t.column 'name', :string, :limit => 100
      t.column 'user_id', :integer, :null => true
      t.column 'shared', :boolean, :null => true
      t.column 'favourites', :boolean, :null => true
      t.column 'resource_id', :integer, :null => true
      t.column 'default_view', :string, :limit => 20, :null => true
      t.column 'page_size', :integer, :null => true
    end

    create_table 'filter_columns' do |t|
      t.column 'filter_id', :integer
      t.column 'family', :string, :limit => 100, :null => true
      t.column 'kee', :string, :limit => 100, :null => true
      t.column 'sort_direction', :string, :limit => 5, :null => true
      t.column 'order_index', :integer, :null => true
    end

    create_table 'criteria' do |t|
      t.column 'filter_id', :integer
      t.column 'family', :string, :limit => 100, :null => true
      t.column 'kee', :string, :limit => 100, :null => true
      t.column 'operator', :string, :limit => 20, :null => true
      t.column 'value', :decimal, :null => true, :precision => 30, :scale => 20
      t.column 'text_value', :string, :null => 200, :null => true
    end

    create_table 'active_filters' do |t|
      t.column 'filter_id', :integer
      t.column 'user_id', :integer, :null => true
      t.column 'order_index', :integer, :null => true
    end

    add_default_filters()
  end

  private

  def self.add_default_filters
    ActiveFilter.reset_column_information
    Filter.reset_column_information

    projects_filter=create_projects_filter()
    ActiveFilter.create(:filter => projects_filter, :user_id => nil, :order_index => 1)

    treemap_filter=create_treemap_filter()
    if treemap_filter
      ActiveFilter.create(:filter => treemap_filter, :user_id => nil, :order_index => 2)
    end

    favourites_filter=create_favourites_filter
    ActiveFilter.create(:filter => favourites_filter, :user_id => nil, :order_index => (treemap_filter ? 3 : 2))
  end

  def self.create_projects_filter
    projects_filter=Filter.new(:name => 'Projects', :shared => true, :favourites => false, :default_view => ::Filter::VIEW_LIST)
    projects_filter.criteria<<Criterion.new_for_qualifiers([Project::QUALIFIER_PROJECT])
    projects_filter.columns.build(:family => 'metric', :kee => 'alert_status', :order_index => 1)
    projects_filter.columns.build(:family => 'name', :order_index => 2, :sort_direction => 'ASC')

    prop=property_value('sonar.core.projectsdashboard.columns')
    if prop
      index=3
      prop.split(";").each do |col|
        fields=col.split('.')
        if fields[0]=='METRIC'
          projects_filter.columns.build(:family => 'metric', :kee => fields[1], :order_index=>index)
          index+=1
        elsif fields[0]=='BUILD_TIME'
          projects_filter.columns.build(:family => 'date', :order_index =>index)
          index+=1
        elsif fields[0]=='LINKS'
          projects_filter.columns.build(:family => 'links', :order_index =>index)
          index+=1
        elsif fields[0]=='LANGUAGE'
          projects_filter.columns.build(:family => 'language', :order_index => index)
          index+=1
        elsif fields[0]=='VERSION'
          projects_filter.columns.build(:family => 'version', :order_index =>index)
          index+=1
        end
      end
    else
      projects_filter.columns.build(:family => 'version', :order_index => 3)
      projects_filter.columns.build(:family => 'metric', :kee => 'ncloc', :order_index => 4)
      projects_filter.columns.build(:family => 'metric', :kee => 'violations_density', :order_index => 5)
      projects_filter.columns.build(:family => 'date', :order_index => 6)
      projects_filter.columns.build(:family => 'links', :order_index => 7)
    end
    projects_filter.save
    projects_filter
  end

  def self.create_treemap_filter
    show_treemap=property_value('sonar.core.projectsdashboard.showTreemap', 'true')
    if show_treemap=='true'
      size_metric=property_value('sonar.core.treemap.sizemetric', 'ncloc')
      color_metric=property_value('sonar.core.treemap.colormetric', 'violations_density')

      treemap_filter=Filter.new(:name => 'Treemap', :shared => true, :favourites => false, :default_view => ::Filter::VIEW_TREEMAP)
      treemap_filter.criteria<<Criterion.new_for_qualifiers([Project::QUALIFIER_PROJECT])
      treemap_filter.columns.build(:family => 'name', :order_index => 1)
      treemap_filter.columns.build(:family => 'metric', :kee => size_metric, :order_index => 2)
      treemap_filter.columns.build(:family => 'metric', :kee => color_metric, :order_index => 3)
      treemap_filter.save
      treemap_filter
    else
      nil
    end
  end

  def self.create_favourites_filter
    favourites_filter=Filter.new(:name => 'My favourites', :shared => true, :favourites => true, :default_view => ::Filter::VIEW_LIST)
    favourites_filter.criteria<<Criterion.new_for_qualifiers(Project::QUALIFIERS)
    favourites_filter.columns.build(:family => 'metric', :kee => 'alert_status', :order_index => 1)
    favourites_filter.columns.build(:family => 'name', :order_index => 2, :sort_direction => 'ASC')
    favourites_filter.columns.build(:family => 'metric', :kee => 'ncloc', :order_index => 3)
    favourites_filter.columns.build(:family => 'metric', :kee => 'violations_density', :order_index => 4)
    favourites_filter.columns.build(:family => 'date', :order_index => 5)
    favourites_filter.save
    favourites_filter
  end

  def self.property_value(key, default_value=nil)
    prop = Property.find(:first, :conditions => {'prop_key' => key, 'resource_id' => nil, 'user_id' => nil})
    if prop
      prop.text_value || default_value
    else
      default_value
    end
  end
end
