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
# SonarQube 4.4
# SONAR-5249
#
class MergeMeasureDataIntoProjectMeasures < ActiveRecord::Migration

  class ProjectMeasure < ActiveRecord::Base
  end

  def self.up
    unless ProjectMeasure.column_names.include?('data')
      add_column :project_measures, 'measure_data', :binary, :null => true
    end
    ProjectMeasure.reset_column_information
    execute_ddl('move measure data', 'UPDATE project_measures m SET m.measure_data = (SELECT md.data FROM measure_data md WHERE md.measure_id = m.id)')
    drop_table(:measure_data)
  end
  
  def self.execute_ddl(message, ddl)
    begin
      say_with_time(message) do
        ActiveRecord::Base.connection.execute(ddl)
      end
    rescue
      # already executed
    end
  end
  
end
