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
class AddMetricsOriginColumn < ActiveRecord::Migration

  def self.up
    add_column(:metrics, :origin, :string, :null => true, :limit => 3)
    Metric.reset_column_information

    Metric051.find(:all).each do |metric|
      if metric.user_managed?
        metric.origin = 'GUI'
      else
        metric.origin = 'JAV'
      end
      metric.save(false)
    end
  end

  def self.down
    remove_column(:metrics, :origin)
  end

  private

  class Metric051 < ActiveRecord::Base
    set_table_name 'metrics'
  end
end
