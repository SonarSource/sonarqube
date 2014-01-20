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
# Sonar 4.2
# SONAR-926
# TODO Only used to migrate latest milestone. Should be deleted before final release.
#
class RemoveLeadingSlashFromResourceKeys < ActiveRecord::Migration

  class Project < ActiveRecord::Base
  end

  def self.up
    resources = Project.find(:all, :conditions => ['path is not null'])
    resources.each do |resource|
      key = resource.kee.split(":").last
      prefix = resource.kee.chomp(key)
      if key != '/' && key.start_with?('/')
        resource.kee = prefix + key[1, key.length]
      end
      resource.save
    end
  end

end
