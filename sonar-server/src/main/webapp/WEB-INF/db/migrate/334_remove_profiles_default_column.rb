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
class RemoveProfilesDefaultColumn < ActiveRecord::Migration

  class Profile < ActiveRecord::Base
    set_table_name 'rules_profiles'
  end

  class Property < ActiveRecord::Base
  end

  def self.up
    Profile.find(:all, :conditions => ['default_profile=?', true]).each do |profile|
      Property.create :prop_key => "sonar.profile.#{profile.language}", :text_value => profile.name
    end

    remove_column('rules_profiles', 'default_profile')
  end

end
