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
class CreateRuleFailureParams < ActiveRecord::Migration

  def self.up
    create_table :rule_failure_params do |t|
      t.column :rule_failure_id ,    :integer,   :null => false
      t.column :snapshot_id,         :integer,   :null => false
      t.column :param_key,           :string,    :null => false, :limit => 100, :null => false
      t.column :value,               :decimal,   :null => false, :precision => 30, :scale => 20
      t.column :value2,              :decimal,   :null => true, :precision => 30, :scale => 20
    end

    drop_table :parameters
  end

  def self.down
    raise IrreversibleMigration
  end
end