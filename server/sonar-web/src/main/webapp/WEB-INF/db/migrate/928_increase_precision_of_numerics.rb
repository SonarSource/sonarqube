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
# SONAR-6710
#
class IncreasePrecisionOfNumerics < ActiveRecord::Migration

  def self.up
    change_column :metrics, :worst_value, :decimal,  :null => true, :precision => 30, :scale => 5
    change_column :metrics, :best_value, :decimal,  :null => true, :precision => 30, :scale => 5
    change_column :project_measures, :value, :decimal,  :null => true, :precision => 30, :scale => 5
    change_column :project_measures, :variation_value_1, :decimal,  :null => true, :precision => 30, :scale => 5
    change_column :project_measures, :variation_value_2, :decimal,  :null => true, :precision => 30, :scale => 5
    change_column :project_measures, :variation_value_3, :decimal,  :null => true, :precision => 30, :scale => 5
    change_column :project_measures, :variation_value_4, :decimal,  :null => true, :precision => 30, :scale => 5
    change_column :project_measures, :variation_value_5, :decimal,  :null => true, :precision => 30, :scale => 5
    change_column :manual_measures, :value, :decimal,  :null => true, :precision => 30, :scale => 5
  end

end
