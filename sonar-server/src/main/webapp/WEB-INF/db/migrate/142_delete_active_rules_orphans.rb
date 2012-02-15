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
# Sonar 2.3.1
#
class DeleteActiveRulesOrphans < ActiveRecord::Migration
  class ActiveRule < ActiveRecord::Base
  end

  def self.up
    # see http://jira.codehaus.org/browse/SONAR-1881
    ActiveRule.reset_column_information
    orphans=ActiveRule.find_by_sql "SELECT ar.* FROM active_rules ar WHERE NOT EXISTS (SELECT * FROM rules_profiles pr WHERE pr.id=ar.profile_id)"
    orphans.each do |orphan|
      orphan.destroy
    end
  end
end