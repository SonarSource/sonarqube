#
# Sonar, entreprise quality control tool.
# Copyright (C) 2008-2011 SonarSource
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
# Sonar 2.13
#
class AddKeyToDashboards < ActiveRecord::Migration

  def self.up
    add_column 'dashboards', 'kee', :string, :limit => 256
    Dashboard.reset_column_information
    
    Dashboard.find(:all).each do |d|
      d.kee = d.name.strip.downcase.sub(/\s+/, '_')
      d.save
    end
    
    main_dashboard = Dashboard.find(:first, :conditions => {:name => 'Dashboard'})
    if main_dashboard
      main_dashboard.kee = 'sonar-main-dashboard'
      main_dashboard.save
    end
    
    change_column 'dashboards', 'kee', :string, :limit => 256, :null => false 
    Dashboard.reset_column_information
  end

end
