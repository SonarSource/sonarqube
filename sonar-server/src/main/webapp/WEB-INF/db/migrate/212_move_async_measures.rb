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
# Sonar 2.10
#
class MoveAsyncMeasures < ActiveRecord::Migration

  class ManualMeasure < ActiveRecord::Base
  end

  class ProjectMeasure < ActiveRecord::Base
  end

  def self.up
    ProjectMeasure.reset_column_information
    ManualMeasure.reset_column_information

    deprecated_measures=ProjectMeasure.find_by_sql("select p1.* from project_measures p1 where p1.snapshot_id is null and p1.measure_date is not null and not exists(select id from project_measures p2 where p2.project_id=p1.project_id and p2.metric_id=p1.metric_id and p2.measure_date is not null and p2.measure_date>p1.measure_date)")

    say_with_time "Move #{deprecated_measures.size} measures" do
      deprecated_measures.each do |dm|
        if dm.project_id
          ManualMeasure.create(
              :resource_id => dm.project_id,
              :metric_id => dm.metric_id,
              :value => dm.value,
              :text_value => dm.text_value,
              :created_at => dm.measure_date,
              :updated_at => dm.measure_date,
              :description => dm.description)
        end
      end
    end

    ProjectMeasure.delete_all("snapshot_id is null and measure_date is not null")
  end

end
