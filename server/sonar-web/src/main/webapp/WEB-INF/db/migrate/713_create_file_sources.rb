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
# SonarQube 5.0
# SONAR-5624
#
class CreateFileSources < ActiveRecord::Migration

  def self.up
    create_table :file_sources do |t|
      t.column :project_uuid, :string,   :limit => 50, :null => false
      t.column :file_uuid,    :string,   :limit => 50, :null => false
      t.column :data,         :text,     :null => true
      t.column :data_hash,    :string,   :limit => 50, :null => true
      t.column :created_at,   :datetime, :null => false
      t.column :updated_at,   :datetime, :null => false
    end

    if dialect()=='mysql'
      ActiveRecord::Base.connection.execute("alter table file_sources modify data longtext")
    end

    ["project_uuid", "file_uuid", "updated_at"].each do |column|
      begin
        add_index "file_sources", column, :name => "file_sources_" + column
      rescue
        # ignore
      end
    end

  end

end
