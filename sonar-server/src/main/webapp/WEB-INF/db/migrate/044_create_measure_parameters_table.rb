#
# Sonar, entreprise quality control tool.
# Copyright (C) 2009 SonarSource SA
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
class CreateMeasureParametersTable < ActiveRecord::Migration

  def self.up    
    create_table :measure_parameters do |t|
      t.column :measure_id,     :integer,   :null => true
      t.column :snapshot_id,    :integer,   :null => true
      t.column :param_key,      :string,    :null => true, :limit => 100
      t.column :value,          :decimal,   :null => true, :precision => 30, :scale => 20
      t.column :lob_value,      :binary,    :null => true
    end
    add_index :measure_parameters, :measure_id, :name => 'measure_params_meas_id'
    add_index :measure_parameters, :snapshot_id, :name => 'measure_params_snap_id'    
  end

  def self.down
    remove_index :measure_parameters, :name => 'measure_params_meas_id'
    remove_index :measure_parameters, :name => 'measure_params_snap_id'    
    drop_table :measure_parameters
  end
  
end
