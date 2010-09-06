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
class AddRulePriority < ActiveRecord::Migration

  def self.up
    add_column(:rules, :priority, :integer, :null => true)
    Rule.reset_column_information
    
    add_column(:project_measures, :rule_priority, :integer, :null => true)
    ProjectMeasure.reset_column_information

    violations = Metric.find(:first, :conditions => {:name => Metric::VIOLATIONS})
    
    mandatory_violations_density = Metric.find(:first, :conditions => {:name => 'mandatory_violations_density'})        
    ProjectMeasure.delete_all("metric_id=" + mandatory_violations_density.id.to_s) if mandatory_violations_density
        
    mandatory_violations = Metric.find(:first, :conditions => {:name => 'mandatory_violations'})
    if mandatory_violations and violations
      ProjectMeasure.update_all("rule_priority=" + Sonar::RulePriority::PRIORITY_MAJOR.to_s, "metric_id=" + mandatory_violations.id.to_s)
      ProjectMeasure.update_all("metric_id=" + violations.id.to_s, "metric_id=" + mandatory_violations.id.to_s)
    end
    
    optional_violations = Metric.find(:first, :conditions => {:name => 'optional_violations'})
    if optional_violations and violations
      ProjectMeasure.update_all("rule_priority=" +  Sonar::RulePriority::PRIORITY_INFO.to_s, "metric_id=" + optional_violations.id.to_s)
      ProjectMeasure.update_all("metric_id=" + violations.id.to_s, "metric_id=" + optional_violations.id.to_s)
    end

    #SONAR-1062 Active rules with optional level are not converted to priority INFO during 1.10 migration
    ActiveRule.update_all('failure_level=0', 'failure_level=1')
  end

  def self.down
  end

end