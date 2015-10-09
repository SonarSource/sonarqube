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
# SonarQube 5.2
# SONAR-6884
#
class AddCollationRulesKeyMssql < ActiveRecord::Migration
  def self.up
    if dialect()=='sqlserver'
      remove_index :rules, :name => 'rules_plugin_key_and_name'
      remove_index :rules_profiles, :name => 'uniq_qprof_key'
      execute("ALTER TABLE rules ALTER COLUMN plugin_rule_key varchar(200) COLLATE SQL_Latin1_General_CP1_CS_AS NOT NULL")
      execute("ALTER TABLE rules ALTER COLUMN plugin_name varchar(255) COLLATE SQL_Latin1_General_CP1_CS_AS NOT NULL")
      execute("ALTER TABLE rules_profiles ALTER COLUMN kee varchar(255) COLLATE SQL_Latin1_General_CP1_CS_AS NOT NULL")
      execute("ALTER TABLE rules_profiles ALTER COLUMN parent_kee varchar(255) COLLATE SQL_Latin1_General_CP1_CS_AS NULL")
      add_index :rules, [:plugin_rule_key, :plugin_name], :unique => true, :name => 'rules_plugin_key_and_name'
      add_index :rules_profiles, :kee, :name => 'uniq_qprof_key', :unique => true
    end
  end
end
