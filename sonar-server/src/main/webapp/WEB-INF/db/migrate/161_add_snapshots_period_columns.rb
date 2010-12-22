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

#
# Sonar 2.5
#
class AddSnapshotsPeriodColumns < ActiveRecord::Migration

  def self.up
     Snapshot.reset_column_information()
     
     add_period_column('period1_mode', :string, :null => true, :limit => 100)
     add_period_column('period1_param', :string, :null => true, :limit => 100)
     add_period_column('period1_date', :datetime, :null => true)

     add_period_column('period2_mode', :string, :null => true, :limit => 100)
     add_period_column('period2_param', :string, :null => true, :limit => 100)
     add_period_column('period2_date', :datetime, :null => true)

     add_period_column('period3_mode', :string, :null => true, :limit => 100)
     add_period_column('period3_param', :string, :null => true, :limit => 100)
     add_period_column('period3_date', :datetime, :null => true)

     add_period_column('period4_mode', :string, :null => true, :limit => 100)
     add_period_column('period4_param', :string, :null => true, :limit => 100)
     add_period_column('period4_date', :datetime, :null => true)

     add_period_column('period5_mode', :string, :null => true, :limit => 100)
     add_period_column('period5_param', :string, :null => true, :limit => 100)
     add_period_column('period5_date', :datetime, :null => true)
  end

  private
  def self.add_period_column(name, type, options={})
    unless Snapshot.column_names.include?(name)
      add_column(:snapshots, name, type, options)
    end
  end
end
