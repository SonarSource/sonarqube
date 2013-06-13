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
# Sonar 3.6
#
class PurgeViolations < ActiveRecord::Migration

  class RuleFailure < ActiveRecord::Base
  end

  def self.up
    violation_ids = ActiveRecord::Base.connection.select_rows('select rf.id from rule_failures rf inner join snapshots s on rf.snapshot_id=s.id where s.islast=' + ActiveRecord::Base.connection.quoted_false)
    say_with_time "Purge #{violation_ids.size} violations" do
      violation_ids.each_slice(999) do |ids|
        RuleFailure.delete(ids.flatten) if ids.size>0
      end
    end
  end

end

