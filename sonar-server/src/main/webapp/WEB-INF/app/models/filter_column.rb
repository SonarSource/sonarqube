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
class FilterColumn < ActiveRecord::Base

  FAMILIES=['date','language','name','links','version','key','metric']

  belongs_to :filter
  validates_inclusion_of :sort_direction, :in => %w( ASC DESC ), :allow_nil => true
  
  def self.create_from_string(string)
    if FAMILIES.include?(string)
      FilterColumn.new(:family => string)
    else
      metric=Metric.by_key(string)
      metric ? FilterColumn.new(:family => 'metric', :kee => metric.key) : nil
    end
  end
  
  def key
    kee
  end

  def name
    if on_metric?
      Api::Utils.message("metric.#{kee}.name", :default => (metric ? metric.short_name : kee))
    else
      Api::Utils.message("filters.col.#{family}", :default => kee)
    end
  end

  def display_name
    name
  end

  def metric
    @metric ||=
      begin
        on_metric? ? Metric.by_key(kee) : nil
      end
  end

  def on_language?
    family=='language'
  end

  def on_version?
    family=='version'
  end

  def on_name?
    family=='name'
  end

  def on_date?
    family=='date'
  end

  def on_links?
    family=='links'
  end

  def on_metric?
    family=='metric'
  end

  def on_profile?
    family=='profile'
  end

  def on_key?
    family=='key'
  end

  def deletable?
    !on_name?
  end

  def sorted?
    sort_direction=='ASC' || sort_direction=='DESC'
  end

  def ascending?
    sort_direction=='ASC'
  end

  def ascending2?
    sort_direction=='ASC'
  end

  def descending?
    sort_direction=='DESC'
  end

  def ascending=(asc)
    write_attribute(:sort_direction, (asc ? 'ASC' : 'DESC'))
  end

  def sortable?
    !on_links?
  end

  def small_width?
    on_metric? && metric && (metric.val_type==Metric::VALUE_TYPE_LEVEL || metric.val_type==Metric::VALUE_TYPE_BOOLEAN) 
  end
end
