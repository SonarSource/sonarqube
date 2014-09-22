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
# Sonar 3.6
#
class CreateIssues < ActiveRecord::Migration

  def self.up
    create_table :issues do |t|
      t.column :kee,                  :string,    :null => false,   :limit => 50
      t.column :component_id,         :integer,   :null => false
      t.column :root_component_id,    :integer,   :null => true
      t.column :rule_id,              :integer,   :null => true
      t.column :severity, 					  :string, 	  :null => true,	  :limit => 10
      t.column :manual_severity,      :boolean,   :null => false
      t.column :message,              :string,    :null => true,    :limit => 4000
      t.column :line,                 :integer,   :null => true
      t.column :effort_to_fix,        :decimal,   :null => true,    :precision => 30,   :scale => 20
      t.column :status,               :string ,   :null => true,    :limit => 20
      t.column :resolution,           :string ,   :null => true,    :limit => 20
      t.column :checksum,             :string ,   :null => true,    :limit => 1000
      t.column :reporter,             :string,    :null => true,	  :limit => 40
      t.column :assignee,             :string,    :null => true,	  :limit => 40
      t.column :author_login,         :string,    :null => true,    :limit => 100
      t.column :action_plan_key,      :string,    :null => true,    :limit => 50
      t.column :issue_attributes,           :string,    :null => true,    :limit => 4000

      # functional dates
      t.column :issue_creation_date,  :datetime,  :null => true
      t.column :issue_close_date,     :datetime,  :null => true
      t.column :issue_update_date,    :datetime,  :null => true

      # technical dates
      t.column :created_at,           :datetime,  :null => true
      t.column :updated_at,           :datetime,  :null => true
    end
  end

end

