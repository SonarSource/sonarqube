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
# Sonar 4.2
# SONAR-4923
# Create columns to copy rule_notes (resp. active_rule_notes) contents to rules (resp. active_rules)
#
class AddRuleNoteColumns < ActiveRecord::Migration

  def self.up
    add_column 'rules',          :note_created_at,   :datetime,   :null => true
    add_column 'rules',          :note_updated_at,   :datetime,   :null => true
    add_column 'rules',          :note_user_login,   :string,     :null => true, :limit => 40
    add_column 'rules',          :note_data,         :text,       :null => true

    add_column 'active_rules',   :note_created_at,   :datetime,   :null => true
    add_column 'active_rules',   :note_updated_at,   :datetime,   :null => true
    add_column 'active_rules',   :note_user_login,   :string,     :null => true, :limit => 40
    add_column 'active_rules',   :note_data,         :text,       :null => true
  end

end

