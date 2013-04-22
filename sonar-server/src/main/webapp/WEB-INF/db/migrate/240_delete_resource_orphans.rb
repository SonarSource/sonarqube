#
# Sonar, entreprise quality control tool.
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
# Delete the resource orphans that are automatically deleted as long as
# SONAR-3120 is not fixed.
#
# Sonar 2.13
#
class DeleteResourceOrphans < ActiveRecord::Migration

  class Project < ActiveRecord::Base
  end

  def self.up
    Project.reset_column_information
    ids=Project.find_by_sql(["select id from projects p where qualifier<>'LIB' and not exists (select * from snapshots s where s.project_id = p.id and s.islast=?)", true])
    say_with_time "Delete #{ids.size} resources" do
      # partition ids because of the Oracle limitation on IN statements
      ids.each_slice(999) do |partition_of_ids|
        unless partition_of_ids.empty?
          Project.delete_all(["id in (?)", partition_of_ids])
        end
      end
    end
  end

end
