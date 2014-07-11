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
# Sonar 3.5
#
class CreateGraphs < ActiveRecord::Migration
  def self.up
    create_table 'graphs' do |t|
      t.column 'snapshot_id', :integer, :null => false
      t.column 'resource_id', :integer, :null => false
      t.column 'format', :string, :null => true, :limit => 20
      t.column 'perspective', :string, :null => true, :limit => 30
      t.column 'version', :string, :null => true, :limit => 20
      t.column 'root_vertex_id', :string, :null => true, :limit => 30
      t.column 'data', :text, :null => true
      t.timestamps
    end
    add_index 'graphs', ['snapshot_id', 'perspective'], :name => 'graphs_perspectives', :unique => true
  end
end

