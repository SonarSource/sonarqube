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
class CreateRulesProfiles < ActiveRecord::Migration

  def self.up
    create_table 'rules_profiles'do |t|
      t.column :name,   :string, :limit => 100, :null => false
      t.column 'language', :string, :null => true, :limit => 20
      t.column 'parent_name', :string, :limit => 100, :null => true
      t.column 'version',      :integer, :default => 1
      t.column 'used_profile', :boolean, :default => false
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
