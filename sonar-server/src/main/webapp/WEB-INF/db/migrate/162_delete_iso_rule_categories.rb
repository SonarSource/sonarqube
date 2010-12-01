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
class DeleteIsoRuleCategories < ActiveRecord::Migration

  def self.up
    remove_rule_categories
    delete_measures_on_iso_category
  end

  private

  def self.remove_rule_categories
    begin
      remove_column('rules', 'rules_category_id')
    rescue
      # already removed
    end
  end

  def self.delete_measures_on_iso_category
    puts "If the following step fails, please execute the SQL request 'DELETE FROM PROJECT_MEASURES WHERE RULE_ID IS NULL AND RULES_CATEGORY_ID IS NOT NULL' and restart Sonar."
    ProjectMeasure.delete_all('rule_id is null and rules_category_id is not null')
  end
end
