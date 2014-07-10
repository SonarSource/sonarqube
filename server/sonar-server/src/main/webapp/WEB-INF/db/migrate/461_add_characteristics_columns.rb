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
# Sonar 4.1
# SONAR-4831
# SONAR-4895
#
class AddCharacteristicsColumns < ActiveRecord::Migration

  def self.up
    add_column 'characteristics', :parent_id,         :integer,   :null => true
    add_column 'characteristics', :root_id,           :integer,   :null => true
    add_column 'characteristics', :function_key,      :string,    :null => true,   :limit => 100
    add_column 'characteristics', :factor_value,      :decimal,   :null => true,   :precision => 30,   :scale => 20
    add_column 'characteristics', :factor_unit,       :string,    :null => true,   :limit => 100
    add_column 'characteristics', :offset_value,      :decimal,   :null => true,   :precision => 30,   :scale => 20
    add_column 'characteristics', :offset_unit,       :string,    :null => true,   :limit => 100
    add_column 'characteristics', :created_at,        :datetime,  :null => true
    add_column 'characteristics', :updated_at,        :datetime,  :null => true
  end

end

