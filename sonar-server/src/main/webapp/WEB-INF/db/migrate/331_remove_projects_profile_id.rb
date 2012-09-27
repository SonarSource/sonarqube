#
# Sonar, entreprise quality control tool.
# Copyright (C) 2008-2012 SonarSource
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
# Sonar 3.3
#
class RemoveProjectsProfileId < ActiveRecord::Migration

  class Profile < ActiveRecord::Base
    set_table_name 'rules_profiles'
  end

  class Project < ActiveRecord::Base
    belongs_to :profile
  end

  class Property < ActiveRecord::Base
  end

  def self.up
    projects=Project.find(:all, :conditions => ['profile_id is not null and copy_resource_id is null'], :include => :profile)
    projects.each do |project|
      Property.create(:prop_key => "sonar.profile.#{project.profile.language}", :text_value => project.profile.name, :resource_id => project.id)
    end
    remove_column('projects', 'profile_id')
  end

end
