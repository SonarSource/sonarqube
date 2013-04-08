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

#
# Sonar 3.6
#
class CreateIssueChangelog < ActiveRecord::Migration

  def self.up
    create_table :issue_changelog do |t|
      t.column :issue_uuid,         :string,    :null => false,   :limit => 36
      t.column :user_id,            :integer,   :null => true
      t.column :change_type, 				:string, 	  :null => true,	  :limit => 50
      t.column :change_data,        :string,    :null => true,    :limit => 4000
      t.column :message,            :text,      :null => true
      t.column :created_at,         :datetime,  :null => true
      t.column :updated_at,         :datetime,  :null => true
    end
  end

end

