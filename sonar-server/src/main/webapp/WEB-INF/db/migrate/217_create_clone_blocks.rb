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
# Sonar 2.11
#
class CreateCloneBlocks < ActiveRecord::Migration

  def self.up
    create_table :clone_blocks do |t|
      t.column :project_snapshot_id, :integer, :null => false
      t.column :snapshot_id, :integer, :null => false
      t.column :hash, :string, :null => false, :limit => 50
      t.column :index_in_file, :integer, :null => false
      t.column :start_line, :integer, :null => false
      t.column :end_line, :integer, :null => false
    end

    add_index :clone_blocks, :project_snapshot_id, :name => 'clone_blocks_project_snapshot'
    add_index :clone_blocks, :snapshot_id, :name => 'clone_blocks_snapshot'
    add_index :clone_blocks, :hash, :name => 'clone_blocks_hash'
  end

end
