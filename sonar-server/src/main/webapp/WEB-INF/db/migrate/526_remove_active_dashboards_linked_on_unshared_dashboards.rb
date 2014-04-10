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
class RemoveActiveDashboardsLinkedOnUnsharedDashboards < ActiveRecord::Migration

  class ActiveDashboard < ActiveRecord::Base
  end

  def self.up
    ActiveDashboard.reset_column_information

    # Delete every active_dashboards linked on unshared dashboard not owned by the user
    ActiveDashboard.delete_all(['dashboard_id in (SELECT d.id FROM dashboards d INNER JOIN active_dashboards ad on ad.dashboard_id=d.id WHERE ad.user_id<>d.user_id AND d.shared=?)', false])
  end

end
