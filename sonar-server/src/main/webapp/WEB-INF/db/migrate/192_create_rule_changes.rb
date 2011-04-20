#
# Sonar, entreprise quality control tool.
# Copyright (C) 2008-2011 SonarSource
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

#
# Sonar 2.8
#
class CreateRuleChanges < ActiveRecord::Migration
  def self.up

    create_table :active_rule_changes do |t|
      t.column :user_login,              :string,    :limit => 40,  :null => false
      t.column :profile_id,              :integer,   :null => false
      t.column :rule_id,                 :integer,   :null => false
      t.column :change_date,             :datetime,  :null => false
      t.column :enabled,                 :boolean,   :null => true
      t.column :old_severity,            :integer,   :null => true
      t.column :new_severity,            :integer,   :null => true
    end
    add_index :active_rule_changes, [:profile_id], :name => 'active_rule_changes_profileid'

    create_table :active_rule_param_changes do |t|
      t.column :active_rule_change_id,   :integer,   :null => false
      t.column :rules_parameter_id,      :integer,   :null => false
      t.column :old_value,               :string,    :limit => 4000, :null => true
      t.column :new_value,               :string,    :limit => 4000, :null => true
    end
    add_index :active_rule_param_changes, [:active_rule_change_id], :name => 'active_rule_param_changes_rulechangeid'

  end
end 
