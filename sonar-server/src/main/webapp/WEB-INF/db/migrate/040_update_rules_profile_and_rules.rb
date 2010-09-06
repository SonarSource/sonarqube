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
class UpdateRulesProfileAndRules < ActiveRecord::Migration

  def self.up
    rename_provided_profiles
    remove_rule_is_ext_column
    update_plugin_names
  end

  def self.down    
  end
  
  private
  
  def self.rename_provided_profiles
    RulesProfile.find(:all, :conditions => {'provided' => true, 'active' => true}).each do |profile|
      profile.provided = false
      profile.name = profile.name + ' (backup)'
      profile.save
    end    
  end
  
  def self.remove_rule_is_ext_column
    remove_column 'rules', 'is_ext'
    Rule.reset_column_information    
  end
  
  def self.update_plugin_names
    Rule.find(:all).each do |rule|
      if (rule.plugin_name == 'maven-checkstyle-plugin')
        rule.plugin_name = 'checkstyle'
      elsif (rule.plugin_name == 'maven-pmd-plugin')
        rule.plugin_name = 'pmd'
      end
      rule.save
    end    
  end
  
end
