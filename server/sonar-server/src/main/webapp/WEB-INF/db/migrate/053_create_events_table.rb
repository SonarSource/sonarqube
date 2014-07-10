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
class CreateEventsTable < ActiveRecord::Migration

  def self.up
    create_table :events do |t|
      t.column :name,        :string,  :limit => 50, :null => true
      t.column :resource_id,  :integer, :null => true
      t.column :snapshot_id, :integer, :null => true
      t.column :category, :string,  :limit=> 50, :null => true
      t.column :event_date, :datetime,  :null => true
      t.column :created_at, :datetime,  :null => true
      t.column :description, :string,  :limit => 3072, :null => true
      t.column :data, :string, :null => true, :limit => 4000
    end
    add_index :events, :resource_id, :name => 'events_resource_id'
    add_index :events, :snapshot_id, :name => 'events_snapshot_id'
  end

end
