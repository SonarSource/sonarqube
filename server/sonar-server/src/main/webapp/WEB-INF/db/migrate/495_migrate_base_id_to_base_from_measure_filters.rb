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
# SonarQube 4.2
# SONAR-4921
#
class MigrateBaseIdToBaseFromMeasureFilters < ActiveRecord::Migration

  class MeasureFilter < ActiveRecord::Base
  end

  def self.up
    filters = MeasureFilter.all(:conditions => "data LIKE '%baseId=%'")
    filters.each do |filter|
      matchBaseId = filter.data.match(/baseId=(\d+)/)
      if matchBaseId
        projectId = matchBaseId[1]
        project = Project.find_by_id(projectId)
        # If project exists, we replace the condition using project id by the condition using project key, otherwise we removed the condition
        filter.data = filter.data.sub(/baseId=\d+/, project ? "base=#{project.kee}" : '')
        filter.save
      end
    end
  end
end
