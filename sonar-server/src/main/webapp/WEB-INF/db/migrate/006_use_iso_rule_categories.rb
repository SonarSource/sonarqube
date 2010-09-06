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
class UseIsoRuleCategories < ActiveRecord::Migration
  def self.up    
    usability = RulesCategory.create(:name => 'Usability', :description => 'The extent to which the project can be understood, learned, operated, attractive and compliant with usability regulations and guidelines. It commonly relies on naming conventions and formatting rules.')
    understandability = RulesCategory.find_by_name 'Understandability'
    code_convention = RulesCategory.find_by_name 'Code Convention'
    documentation = RulesCategory.find_by_name 'Documentation'
    naming_convention = RulesCategory.find_by_name 'Naming Convention'
    RulesCategory.delete_all "id in (#{understandability.id},#{code_convention.id},#{documentation.id},#{naming_convention.id})"

    maintanability = RulesCategory.create(:name => 'Maintanability', :description => 'The extent to which the project facilitates updating to satisfy new requirements. Thus the the project which is maintainable should be not complex.')
    completeness = RulesCategory.find_by_name 'Completeness'
    conciseness = RulesCategory.find_by_name 'Conciseness'
    RulesCategory.delete_all "id in (#{completeness.id},#{conciseness.id})"

    update_description('Efficiency', 'The extent to which the project fulfills its purpose without waste of resources. This means resources in the sense of memory utilisation and processor speed.')
    update_description('Portability', 'The extent to which the project can be operated easily and well on multiple computer configurations. Portability can mean both between different hardware setups and between different operating systems -- such as running on both Mac OS X and GNU/Linux.')
    update_description('Reliability', 'The extent to which the project can be expected to perform its intended function with rescission. Some examples : are loop indexes range tested? Is input data checked for range errors ? Is divide-by-zero avoided ? Is exception handling provided ?')

    RulesCategory.clear_cache
  end

  def self.down
    # not much we can do to restore deleted data
    raise IrreversibleMigration
  end

  protected
  def self.update_description(categ_name, desc)
    rule = RulesCategory.find_by_name(categ_name)
    if (rule) 
      rule.description = desc
      rule.save
    end
  end
end