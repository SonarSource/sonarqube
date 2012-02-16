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
# sonar 2.0
class SetDefaultProjectRoles < ActiveRecord::Migration

  # Do not use faux models

  def self.up
    administrators=Group.find_by_name('sonar-administrators')
    users=Group.find_by_name('sonar-users')

    # default project roles
    GroupRole.create(:resource_id => nil, :role => 'default-admin', :group_id => administrators.id)
    GroupRole.create(:resource_id => nil, :role => 'default-user', :group_id => users.id)
    GroupRole.create(:resource_id => nil, :role => 'default-user', :group_id => nil)
    GroupRole.create(:resource_id => nil, :role => 'default-codeviewer', :group_id => users.id)
    GroupRole.create(:resource_id => nil, :role => 'default-codeviewer', :group_id => nil)
  end

end
