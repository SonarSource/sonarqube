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
class CreateLogTable < ActiveRecord::Migration

  def self.up
    create_table 'logs'do |t|
      t.column 'time_field',   :datetime, :null => false
      t.column 'type_field', :string, :null => false
      t.column 'status_field', :string, :null => false
      t.column 'execution_time_field', :long
      t.column 'author_field', :string
      t.column 'data_field', :blob
    end

  end

end
