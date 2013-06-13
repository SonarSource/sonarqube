#
# Sonar, entreprise quality control tool.
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
# Sonar 3.3
#
class RemoveProjectsProfileId < ActiveRecord::Migration

  class Profile < ActiveRecord::Base
    set_table_name 'rules_profiles'
  end

  class Project < ActiveRecord::Base

  end

  class Property < ActiveRecord::Base
  end

  def self.up
    projects=Project.find(:all, :conditions => ['profile_id is not null and copy_resource_id is null'])
    say_with_time "Process #{projects.size} projects..." do
      projects.each do |project|
        profile = Profile.find(:first, :conditions => ['id=?', project.profile_id])
        if profile
          Property.create(:prop_key => "sonar.profile.#{profile.language}", :text_value => profile.name, :resource_id => project.id)
        end
      end
    end
    remove_column('projects', 'profile_id')
  end

end
