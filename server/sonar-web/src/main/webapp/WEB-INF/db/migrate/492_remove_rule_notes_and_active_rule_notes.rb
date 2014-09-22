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
# SonarQube 4.2
# SONAR-4923
#
class RemoveRuleNotesAndActiveRuleNotes < ActiveRecord::Migration

  class Project < ActiveRecord::Base
  end

  def self.up
    remove_index_quietly(:active_rule_notes, 'arn_active_rule_id')
    drop_table(:active_rule_notes)

    remove_index_quietly(:rule_notes, 'rule_notes_rule_id')
    drop_table(:rule_notes)
  end

  def self.remove_index_quietly(table, name)
    begin
      remove_index(table, :name => name)
    rescue
      # probably already removed
    end
  end

end
