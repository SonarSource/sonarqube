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
# Sonar 3.4
#
class CreateMeasureFilterFavourites < ActiveRecord::Migration
  def self.up
    create_table 'measure_filter_favourites' do |t|
      t.column 'user_id', :integer, :null => false
      t.column 'measure_filter_id', :integer, :null => false
      t.column 'created_at', :datetime
    end
    add_index 'measure_filter_favourites', 'user_id', :name => 'measure_filter_favs_userid'
  end
end

