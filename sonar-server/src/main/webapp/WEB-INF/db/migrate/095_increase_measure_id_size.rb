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
# sonar 2.0
class IncreaseMeasureIdSize < ActiveRecord::Migration

  def self.up
    alter_to_big_primary_key('project_measures')
    alter_to_big_integer('measure_data', 'measure_id', 'measure_data_measure_id')
    alter_to_big_integer('async_measure_snapshots', 'project_measure_id', 'async_m_s_measure_id')
  end

end
