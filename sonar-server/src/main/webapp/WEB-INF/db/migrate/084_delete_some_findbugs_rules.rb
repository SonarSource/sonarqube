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
class DeleteSomeFindbugsRules < ActiveRecord::Migration
  
  def self.up
    delete_rule('EQ_DOESNT_OVERRIDE_EQUALS')
    delete_rule('OBL_UNSATISFIED_OBLIGATION')
  end

  def self.down
    
  end
  
  private
  def self.delete_rule(rule_key)
    rule=Rule.find(:first, :conditions => {:plugin_name => 'findbugs', :plugin_rule_key => rule_key})
    if rule
      say_with_time "Deleting Findbugs rule #{rule_key}..." do
        rule_id=rule.id
        ActiveRule.delete_all(["rule_id=?", rule_id])
        rule.delete
      end
    end
  end
end