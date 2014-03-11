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
# Copy rule_notes (resp. active_rule_notes) contents to rules (resp. active_rules)
#
class CopyRuleNotesToRules < ActiveRecord::Migration

  class Rule < ActiveRecord::Base
  end

  class RuleNote < ActiveRecord::Base
  end

  class ActiveRule < ActiveRecord::Base
  end

  class ActiveRuleNote < ActiveRecord::Base
  end

  def self.up
    Rule.reset_column_information
    RuleNote.reset_column_information
    ActiveRule.reset_column_information
    ActiveRuleNote.reset_column_information

    rules = {}
    Rule.all.each do |rule|
      rules[rule.id] = rule
    end

    RuleNote.all.each do |note|
      rule = rules[note.rule_id]
      if rule
        rule.note_created_at = note.created_at
        rule.note_updated_at = note.updated_at
        rule.note_user_login = note.user_login
        rule.note_data = note.data
        rule.save
      end
    end

    active_rules = {}
    ActiveRule.all.each do |rule|
      active_rules[rule.id] = rule
    end

    ActiveRuleNote.all.each do |note|
      active_rule = active_rules[note.active_rule_id]
      if active_rule
        active_rule.note_created_at = note.created_at
        active_rule.note_updated_at = note.updated_at
        active_rule.note_user_login = note.user_login
        active_rule.note_data = note.data
        active_rule.save
      end
    end
  end

end

