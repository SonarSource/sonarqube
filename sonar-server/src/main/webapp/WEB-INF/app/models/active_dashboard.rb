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
class ActiveDashboard < ActiveRecord::Base

  belongs_to :user
  belongs_to :dashboard
  validates_uniqueness_of :dashboard_id, :scope => :user_id

  def name(l10n=false)
    dashboard.name(l10n)
  end

  def order_index
    read_attribute(:order_index) || 1
  end

  def shared?
    dashboard.shared
  end

  def owner?(user)
    dashboard.owner?(user)
  end

  def follower?(user)
    self.user.nil? || self.user_id==user.id
  end

  def default?
    user_id.nil?
  end

  def self.user_dashboards(user)
    result=nil
    if user && user.id
      result=find(:all, :include => 'dashboard', :conditions => ['user_id=?', user.id], :order => 'order_index')
    end
    if result.nil? || result.empty?
      result=default_dashboards
    end
    result
  end

  def self.default_dashboards
    find(:all, :include => 'dashboard', :conditions => ['user_id IS NULL'], :order => 'order_index')
  end
end