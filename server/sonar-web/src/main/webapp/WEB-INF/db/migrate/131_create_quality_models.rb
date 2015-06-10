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
# Sonar 2.3
#
class CreateQualityModels < ActiveRecord::Migration

  def self.up
    create_table 'quality_models' do |t|
      t.column 'name', :string, :limit => 100
    end

    create_table 'characteristics' do |t|
      t.column 'quality_model_id', :integer, :null => true
      t.column 'kee', :string, :limit => 100, :null => true
      t.column 'name', :string, :limit => 100, :null => true
      t.column 'rule_id', :integer, :null => true
      t.column 'depth', :integer, :null => true
      t.column 'characteristic_order', :integer, :null => true
      t.column 'description', :string, :null => true, :limit => 4000
      t.column 'enabled', :boolean, :null => true
    end

    create_table 'characteristic_edges', :id => false do |t|
      t.column 'child_id', :integer
      t.column 'parent_id', :integer
    end
  end
  
end
