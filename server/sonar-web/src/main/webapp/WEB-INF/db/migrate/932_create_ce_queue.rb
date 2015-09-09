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
#
class CreateCeQueue < ActiveRecord::Migration

  def self.up
    create_table 'ce_queue' do |t|
      t.column 'uuid', :string, :limit => 40, :null => false
      t.column 'task_type', :string, :limit => 15, :null => false
      t.column 'component_uuid', :string, :limit => 40, :null => true
      t.column 'status', :string, :limit => 15, :null => false
      t.column 'submitter_login', :string, :limit => 255, :null => true
      t.column 'started_at', :big_integer, :null => true
      t.column 'created_at', :big_integer, :null => false
      t.column 'updated_at', :big_integer, :null => false
    end
    add_index 'ce_queue', 'uuid', :name => 'ce_queue_uuid', :unique => true
  end

end

