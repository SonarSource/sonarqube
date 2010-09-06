 #
 # Sonar, entreprise quality control tool.
 # Copyright (C) 2009 SonarSource SA
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
class AddLineToRuleFailures < ActiveRecord::Migration

  def self.up
    add_column :rule_failures, :line, :integer, :null => true
    RuleFailure.reset_column_information
    migrate_data
  end

  def self.down

  end

  def self.migrate_data
    RuleFailureParam073.find(:all, :select => 'DISTINCT snapshot_id').map(&:snapshot_id).each do |sid|
      RuleFailureParam073.find(:all, :conditions => {:snapshot_id => sid, :param_key => 'line'}).each do |param|
        if param.value and param.value.to_i>0
          RuleFailure.update(param.rule_failure_id, :line => param.value.to_i)
        end
      end
    end
  end

  class RuleFailureParam073 < ActiveRecord::Base
    set_table_name "rule_failure_params"
  end
end