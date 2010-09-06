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
class CreateRulesProfiles < ActiveRecord::Migration

  def self.up
    create_tables
  end

  def self.down
    drop_table 'active_rule_parameters'
    drop_table 'active_rules'
    drop_table 'rules_profiles'
  end

  protected
  	
  def self.create_tables
    create_table 'rules_profiles'do |t|
      t.column :name,   :string, :limit => 40, :null => false
      t.column :active, :boolean, :default => false
    end

    create_table 'active_rules' do |t|
      t.column :profile_id, :integer, :null => false
      t.column :rule_id,             :integer,   :null => false
      t.column :failure_level,       :integer,   :null => false
    end

    create_table :active_rule_parameters do |t|
      t.column :active_rule_id,     :integer,   :null => false
      t.column :rules_parameter_id,  :integer,   :null => false
      t.column :value,               :string,    :limit => 4000, :null => true
    end
  end

end
