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
class PopulateDatabase < ActiveRecord::Migration
  def self.up
    insert_rule_categories
  end

  def self.down

  end
  
  protected

  
  def self.insert_rule_categories
    RulesCategory.create(:name => 'Code Convention', :description => 'These rules do not suppose Quality of the development but make it possible to define a standard within the companies.')	
    RulesCategory.create(:name => 'Naming Convention', :description => 'These rules do not suppose Quality of the development but make it possible to define a common language.')
    RulesCategory.create(:name => 'Documentation', :description => 'Documentation level.')
    RulesCategory.create(:name => 'Completeness', :description => 'These rules make it possible to measure the completness of the source code.')
    RulesCategory.create(:name => 'Conciseness', :description => 'These rules make it possible to measure the level of concision of the code and the Quality of object-oriented design.')
    RulesCategory.create(:name => 'Understandability', :description => 'These rules make it possible to measure the understanbility of the code.')
    RulesCategory.create(:name => 'Reliability', :description => 'These rules make it possible to measure the reliability of the code.')
    RulesCategory.create(:name => 'Efficiency', :description => 'These rules make it possible to measure the level of efficiency, with the direction performance of the developed code.')
    RulesCategory.create(:name => 'Portability', :description => 'Portability of the code.')
  end
  

end
