#
# SonarQube, open source software quality management tool.
# Copyright (C) 2008-2013 SonarSource
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
# SonarQube 4.2
# SONAR-926
#
class MigratePackageResources < ActiveRecord::Migration

  class Project < ActiveRecord::Base
  end

  def self.up
    packages = Project.find(:all, :conditions => {:qualifier => 'PAC'})
    packages.each do |package|
      key = package.kee.split(":").last
      prefix = package.kee.chomp(key)
      if key == '[default]'
        package.kee = prefix + '[root]'
      else
        package.kee = prefix + key.gsub('.', '/')
      end
      package.qualifier = 'DIR'
      package.save
    end
  end

end
