#
# Sonar, open source software quality management tool.
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
class AddMeasureData < ActiveRecord::Migration

  def self.up
    create_table :measure_data do |t|
      t.column :measure_id, :integer, :null => true
      t.column :snapshot_id, :integer, :null => true
      t.column :data, :binary, :null => true
    end
    add_index :measure_data, :measure_id, :name => 'measure_data_measure_id'   
    MeasureData061.reset_column_information 

    add_column(:project_measures, :alert_text, :string, :null => true, :limit => 4000)
    add_column(:project_measures, :url, :string, :null => true, :limit => 2000)
    add_column(:project_measures, :description, :string, :null => true, :limit => 4000)
    ProjectMeasure.reset_column_information


    copy_alerts
    copy_code_coverage_data
    copy_manual_measures

    remove_index :measure_parameters, :name => 'measure_params_meas_id'
    remove_index :measure_parameters, :name => 'measure_params_snap_id'
    drop_table :measure_parameters
  end

  def self.down

  end

  def self.copy_alerts
    MeasureParameter061.find(:all, :conditions => {:param_key => 'alert_label'}).each do |param|
      measure=ProjectMeasure.find(param.measure_id)
      if measure
        measure.update_attribute 'alert_text', param.text_value
      end
    end
  end

  def self.copy_code_coverage_data
    metric=Metric.find(:first, :conditions => {:name => 'code_coverage_line_hits_data'})
    if metric.nil?
      metric=Metric.create(:name => 'code_coverage_line_hits_data', :description => 'Code coverage line hits data', 
         :direction => 0, :qualitative => false, :domain => 'Tests', :val_type => 'DATA',
         :short_name => 'Code coverage data')
    end
    MeasureParameter061.find(:all, :conditions => {:param_key => 'lines'}).each do |param|
      measure=ProjectMeasure61.create(:metric_id => metric.id, :snapshot_id => param.snapshot_id)
      MeasureData061.create(:measure_id => measure.id, :snapshot_id => measure.snapshot_id, :data => param.lob_value)
    end
  end

  def self.copy_manual_measures
    MeasureParameter061.find(:all, :conditions => {:param_key => 'url'}).each do |param|
      measure=ProjectMeasure.find(param.measure_id)
      measure.url = param.text_value
      measure.save
    end
    MeasureParameter061.find(:all, :conditions => {:param_key => 'summary'}).each do |param|
      measure=ProjectMeasure.find(param.measure_id)
      measure.description = param.text_value
      measure.save
    end
  end

  class MeasureParameter061 < ActiveRecord::Base
    set_table_name :measure_parameters
  end
  
  class MeasureData061 < ActiveRecord::Base
    set_table_name :measure_data
  end
  
  class ProjectMeasure61 < ActiveRecord::Base
    set_table_name :project_measures
  end

end