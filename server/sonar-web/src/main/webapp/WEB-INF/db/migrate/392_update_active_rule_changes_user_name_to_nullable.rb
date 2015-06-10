#
# SonarQube, open source software quality management tool.
# Copyright (C) 2008-2014 SonarSource
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
# Sonar 3.6
#
class UpdateActiveRuleChangesUserNameToNullable < ActiveRecord::Migration

  class ActiveRuleChange < ActiveRecord::Base
  end

  def self.up
    # Due to an issue on Oracle, it's not possible to use change_column to set a column to nullable, we had to create another column

    add_column 'active_rule_changes', 'username', :string, :limit => 200, :null => true

    remove_column 'active_rule_changes', 'user_name'
  end

end
