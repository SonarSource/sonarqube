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

  def global?
    dashboard.global
  end

  def owner?(user)
    dashboard.owner?(user)
  end

  def editable_by?(user)
    dashboard.editable_by?(user)
  end

  def follower?(user)
    self.user.nil? || self.user_id==user.id
  end

  def default?
    user_id.nil?
  end

  def self.user_dashboards(user, global)
    result=nil
    if user && user.id
      result=find_for_user(user.id).select { |a| a.global? == global}
    end
    if result.nil? || result.empty?
      result=default_dashboards.select { |a| a.global? == global}
    end
    result
  end

  def self.default_dashboards()
    find_for_user(nil)
  end

  private

  def self.find_for_user(user_id)
    find(:all, :include => 'dashboard', :conditions => {:user_id => user_id}, :order => 'order_index')
  end

end
