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
# http://jira.codehaus.org/browse/SONAR-1974
#
class NullableRuleFailuresSnapshotId < ActiveRecord::Migration

  def self.up
    dialect = ActiveRecord::Base.configurations[ENV['RAILS_ENV']]["dialect"]

    if dialect == 'sqlserver'
      remove_index 'rule_failures', :name => 'rule_failure_snapshot_id'
    end

    change_column 'rule_failures', 'snapshot_id', :integer, :null => true

    if dialect == 'sqlserver'
      add_index 'rule_failures', 'snapshot_id', :name => 'rule_failure_snapshot_id'
    end
  end

end
