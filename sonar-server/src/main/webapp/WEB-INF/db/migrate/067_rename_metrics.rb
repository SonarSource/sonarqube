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
class RenameMetrics < ActiveRecord::Migration

  def self.up
    rename_metric('classes_count', 'classes')
    rename_metric('packages_count', 'packages')
    rename_metric('files_count', 'files')
    rename_metric('functions_count', 'functions')
    rename_metric('directories_count', 'directories')
    
    rename_metric('loc', 'lines')
    rename_metric('ccn', 'complexity')
    rename_metric('ccn_class', 'class_complexity')
    rename_metric('ccn_function', 'function_complexity')
    rename_metric('ccn_file', 'file_complexity')
    rename_metric('ccn_classes_count_distribution', 'class_complexity_distribution')
    rename_metric('ccn_functions_count_distribution', 'function_complexity_distribution')
    rename_metric('ccn_vs_cc', 'uncovered_complexity_by_tests')
    
    rename_metric('comment_ratio', 'comment_lines_density')
    
    rename_metric('test_count', 'tests')
    rename_metric('test_errors_count', 'test_errors')
    rename_metric('test_skipped_count', 'skipped_tests')
    rename_metric('test_failures_count', 'test_failures')
    rename_metric('test_success_percentage', 'test_success_density')
    rename_metric('test_details', 'test_data')
    
    rename_metric('duplicated_lines_ratio', 'duplicated_lines_density')
    rename_metric('code_coverage', 'coverage')
    rename_metric('code_coverage_line_hits_data', 'coverage_line_hits_data')
    
    rename_metric('rules_violations_count', 'violations')
    rename_metric('rules_violations', 'mandatory_violations')
    rename_metric('optional_rules_violations', 'optional_violations')
    rename_metric('rules_index', 'violations_density')
    rename_metric('rules_compliance', 'mandatory_violations_density')
    Metric.clear_cache
  end

  def self.down
  end

  def self.rename_metric(metric_name, new_metric_name)
    to_rename = Metric067.find(:first, :conditions => {:name => metric_name})
    if to_rename.nil?
      return
    end
    Metric067.delete_all("name = '#{new_metric_name}'")

    to_rename.name = new_metric_name
    to_rename.save!
    
    projectsdashboard = Property.find(:first, :conditions => {:prop_key => 'sonar.core.projectsdashboard.columns'})
    if not projectsdashboard.nil?
      metric_to_find = "METRIC.#{metric_name};"
      prop_value = projectsdashboard.prop_value
      if not prop_value.index(metric_to_find).nil?
        projectsdashboard.prop_value = prop_value.sub(metric_to_find,"METRIC.#{new_metric_name};")
        projectsdashboard.save!
      end
    end

    timemachine = Property.find(:first, :conditions => {:prop_key => 'timemachine.chartMetrics'})
    if timemachine
      prop_value = timemachine.prop_value
      if not prop_value.index(metric_name).nil?
        timemachine.prop_value = prop_value.sub(metric_name, new_metric_name)
        timemachine.save!
      end
    end    
    

    treemap_color = Property.find(:first, :conditions => {:prop_key => 'sonar.core.treemap.colormetric', :prop_value => metric_name})
    if not treemap_color.nil?
      treemap_color.prop_value = new_metric_name
      treemap_color.save!
    end

    treemap_size = Property.find(:first, :conditions => {:prop_key => 'sonar.core.treemap.sizemetric', :prop_value => metric_name})
    if not treemap_size.nil?
      treemap_size.prop_value = new_metric_name
      treemap_size.save!
    end
    
    Metric.clear_cache
  end

  class Metric067 < ActiveRecord::Base
    set_table_name "metrics"
  end
end