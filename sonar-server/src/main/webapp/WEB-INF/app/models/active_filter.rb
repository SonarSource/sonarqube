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
class ActiveFilter < ActiveRecord::Base

  belongs_to :user
  belongs_to :filter
  
  def name
    filter.name
  end

  def shared
    filter.shared
  end

  def author
    filter.user
  end

  def author_name
    author ? author.name : nil
  end

  def owner?
    filter.user && user && filter.user==user
  end

  def self.default_active_filters
    find(:all, :include => 'filter', :conditions => {:user_id => nil}, :order => 'order_index')
  end

  def self.for_anonymous
    find(:all, :include => 'filter', :conditions => ['active_filters.user_id is null AND (filters.favourites=? OR filters.favourites is null)', false], :order => 'order_index')
  end
end