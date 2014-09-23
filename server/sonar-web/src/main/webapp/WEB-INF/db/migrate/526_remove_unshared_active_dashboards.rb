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

#
# SonarQube 4.3
# SONAR-4979
#
class RemoveUnsharedActiveDashboards < ActiveRecord::Migration

  class Dashboard < ActiveRecord::Base
    set_table_name 'dashboards'
  end

  class ActiveDashboard < ActiveRecord::Base
    set_table_name 'active_dashboards'
    belongs_to :dashboard, :class_name => 'Dashboard'
  end

  def self.up
    Dashboard.reset_column_information
    ActiveDashboard.reset_column_information

    # Delete every active_dashboards linked on unshared dashboard not owned by the user
    ActiveDashboard.all(:joins => 'inner join dashboards on dashboards.id=active_dashboards.dashboard_id', :conditions => ['dashboards.shared=? AND active_dashboards.user_id<>dashboards.user_id', false]).each {|ad| ad.delete}
  end

end
