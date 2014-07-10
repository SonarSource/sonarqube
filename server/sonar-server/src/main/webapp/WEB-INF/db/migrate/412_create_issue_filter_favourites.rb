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
# Sonar 3.7
# See SONAR-4383
#
class CreateIssueFilterFavourites < ActiveRecord::Migration

  def self.up
    create_table :issue_filter_favourites do |t|
      t.column :user_login,      :string,    :null => false,   :limit => 40
      t.column :issue_filter_id, :integer,   :null => false
      t.column :created_at,      :datetime
    end
    add_index :issue_filter_favourites, :user_login, :name => 'issue_filter_favs_user'
  end

end

