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
require 'java'

class RemoveExternalMeasuresTable < ActiveRecord::Migration
  
  def self.up
    add_columns_to_project_measures
    add_column_to_measure_parameters
    begin
      migrate_external_measures_to_project_measures
      remove_external_measure_tables
    rescue
      # Do nothing, means external measure table doesn't exist anymore
    end
  end

  def self.down
    
  end
  
  
  private
  
  def self.add_columns_to_project_measures
    add_column(:project_measures, :measure_date, :datetime, :null => true)
    add_column(:project_measures, :project_id, :integer, :null => true)
    change_column(:project_measures, :snapshot_id, :integer, :null => true)
    ProjectMeasure.reset_column_information       
  end
  
  def self.add_column_to_measure_parameters
    add_column(:measure_parameters, :text_value, :string, :limit => 4000, :null => true)
  end  
  
  def self.migrate_external_measures_to_project_measures
    java_facade = Java::OrgSonarServerUi::JRubyFacade.new
    
    ExternalMeasure49.find(:all, :include => :external_measure_params, :order => :measure_date).each do |external_measure| 
      measure = ProjectMeasure.new(
        :value => external_measure.value,
        :metric_id => external_measure.metric_id,
        :project_id => external_measure.project_id,
        :measure_date => external_measure.measure_date        
      )      
      external_measure.external_measure_params.each do |external_measure_param|
        measure.measure_parameters <<
          MeasureParameter049.new(
          :param_key => external_measure_param.name,
          :text_value => external_measure_param.value
        )
      end
      measure.save!
      java_facade.registerAsyncMeasure(measure.id.to_i)
    end  

    java_facade.stop()
    java_facade = nil   
  end
  
  def self.remove_external_measure_tables    
    remove_index :external_measure_params, :name => 'ext_meas_params_m_id'
    drop_table :external_measure_params  
    drop_table :external_measures    
  end    
  
  class ExternalMeasure49 < ActiveRecord::Base
    set_table_name "external_measures"
    
    has_many :external_measure_params, :class_name => 'ExternalMeasureParam49', :foreign_key => 'external_measure_id'
  end
  
  class ExternalMeasureParam49 < ActiveRecord::Base
    set_table_name "external_measure_params"
    
    #    belongs_to :external_measure, :class_name => 'ExternalMeasure48', :foreign_key => 'external_measure_id'
  end

  class MeasureParameter049 < ActiveRecord::Base
    set_table_name :measure_parameters
  end
  
end
