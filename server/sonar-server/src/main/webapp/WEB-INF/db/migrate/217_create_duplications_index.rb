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
# Sonar 2.11
#
class CreateDuplicationsIndex < ActiveRecord::Migration

  def self.up
    create_table :duplications_index do |t|
      t.column :project_snapshot_id, :integer, :null => false
      t.column :snapshot_id, :integer, :null => false
      t.column :hash, :string, :null => false, :limit => 50
      t.column :index_in_file, :integer, :null => false
      t.column :start_line, :integer, :null => false
      t.column :end_line, :integer, :null => false
    end

    add_index :duplications_index, :project_snapshot_id, :name => 'duplications_index_psid'
    add_index :duplications_index, :snapshot_id, :name => 'duplications_index_sid'
    add_index :duplications_index, :hash, :name => 'duplications_index_hash'
  end

end
