#
# Sonar, entreprise quality control tool.
# Copyright (C) 2009 SonarSource SA
# mailto:contact AT sonarsource DOT com
#
# Sonar is free software; you can redistribute it and/or
# modify it under the terms of the GNU Lesser General Public
# License as published by the Free Software Foundation; either
# version 3 of the License, or (at your option) any later version.
#
# Sonar is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
# Lesser General Public License for more details.
#
# You should have received a copy of the GNU Lesser General Public
# License along with Sonar; if not, write to the Free Software
# Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
#
class SimplifyMetrics < ActiveRecord::Migration

  TYPES_MAP=['INT','INT','INT','INT','FLOAT','FLOAT','PERCENT','MILLISEC','BOOL']

  def self.up
    add_column(:metrics, :val_type, :string, :null => true, :limit => 8)
    add_column(:metrics, :user_managed, :boolean, :null => true, :default => false)
    add_column(:metrics, :enabled, :boolean, :null => true, :default => true)
    remove_column(:metrics, :long_name)
    Metric046.reset_column_information

    Metric046.find(:all).each do |metric|
      metric.val_type=TYPES_MAP[metric.value_type]
      metric.enabled=true
      metric.user_managed=(metric.name.index('ext_')==0)
      metric.save!
    end

    remove_column(:metrics, :value_type)

    Metric.reset_column_information
  end

  def self.down
    
  end

  class Metric046 < ActiveRecord::Base
    set_table_name "metrics"
  end
end
