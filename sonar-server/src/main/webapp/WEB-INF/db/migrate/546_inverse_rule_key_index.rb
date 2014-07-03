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
# SQ 4.4
# SONAR-5001
#
require 'set'
class InverseRuleKeyIndex < ActiveRecord::Migration


  class ActiveRule < ActiveRecord::Base
    set_table_name 'active_rules'
  end

  class Characteristic < ActiveRecord::Base
    set_table_name 'characteristics'
  end

  class Issue < ActiveRecord::Base
    set_table_name 'issues'
  end

  class ActiveRuleChange < ActiveRecord::Base
    set_table_name 'active_rule_changes'
  end

  class ProjectMeasure < ActiveRecord::Base
    set_table_name 'project_measures'
  end

  class Rule < ActiveRecord::Base
  end

  class RuleParameter < ActiveRecord::Base
    set_table_name 'rules_parameters'
  end

  def self.up
    begin
      remove_index :rules, :name => 'rules_plugin_key_and_name'
    rescue
      #ignore
    end

    # rows can be duplicated because the unique index was sometimes missing
    delete_duplicated_rules

    add_index :rules, [:plugin_name, :plugin_rule_key], :unique => true, :name => 'rules_repo_key'
  end

  private
  def self.delete_duplicated_rules
    ActiveRule.reset_column_information
    ActiveRuleChange.reset_column_information
    Characteristic.reset_column_information
    Issue.reset_column_information
    ProjectMeasure.reset_column_information
    Rule.reset_column_information
    RuleParameter.reset_column_information

    say_with_time 'Delete duplicated rules' do
      existing_keys = Set.new
      rules=Rule.find(:all, :select => 'id,plugin_name,plugin_rule_key', :order => 'id')
      rules.each do |rule|
        key = "#{rule.plugin_name}:#{rule.plugin_rule_key}"
        if existing_keys.include?(key)
          say "Delete duplicated rule '#{key}' (id=#{rule.id})"
          ActiveRule.delete_all(['rule_id=?', rule.id])
          ActiveRuleChange.delete_all(['rule_id=?', rule.id])
          Characteristic.delete_all(['rule_id=?', rule.id])
          Issue.delete_all(['rule_id=?', rule.id])
          ProjectMeasure.delete_all(['rule_id=?', rule.id])
          RuleParameter.delete_all(['rule_id=?', rule.id])
          rule.destroy
        else
          existing_keys.add(key)
        end
      end
    end
  end
end
