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
class CreateAsyncMeasureSnapshotsTable < ActiveRecord::Migration
  
  def self.up
    create_table :async_measure_snapshots do |t|
      t.column :project_measure_id, :integer, :null => true
      t.column :measure_date, :datetime, :null => true
      t.column :snapshot_id, :integer, :null => true
      t.column :snapshot_date, :datetime, :null => true
      t.column :metric_id, :integer, :null => true
      t.column :project_id, :integer, :null => true
    end
    add_index :async_measure_snapshots, :snapshot_id, :name => 'async_m_s_snapshot_id'
    add_index :async_measure_snapshots, :project_measure_id, :name => 'async_m_s_measure_id'
    add_index :async_measure_snapshots, [:project_id, :metric_id], :name => 'async_m_s_project_metric'
  end

end
