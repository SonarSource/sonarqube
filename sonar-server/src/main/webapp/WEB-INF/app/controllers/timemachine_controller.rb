 #
 # Sonar, entreprise quality control tool.
 # Copyright (C) 2008-2012 SonarSource
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
class TimemachineController < ApplicationController

  before_filter :admin_required, :only => [ :set_default_chart_metrics ]
  verify :method => :post, :only => [ :set_default_chart_metrics ], :redirect_to => { :action => :index }

  SECTION=Navigation::SECTION_RESOURCE
  CONFIGURATION_DEFAULT_CHART_METRICS='timemachine.chartMetrics'

  MAX_SNAPSHOTS = 5

  def index
    init_resource_for_user_role

    if params[:sid]
      @sids = params[:sid].split(',').collect {|s| s.to_i}

      #
      # see the explanation of the conditions on scope/qualifier in the method Snapshot.for_timemachine_matrix()
      #
      @snapshots=Snapshot.find(:all,
         :include => 'events',
         :conditions => {:id => @sids, :project_id => @resource.id, :scope => @resource.scope, :qualifier => @resource.qualifier}, :order => 'snapshots.created_at ASC')
    else
      @snapshots=Snapshot.for_timemachine_matrix(@resource)
      @sids = @snapshots.collect{|s| s.id}.uniq
    end

    snapshot_by_id={}
    @snapshots.each do |s|
      snapshot_by_id[s.id]=s
    end

    measures=ProjectMeasure.find(:all, :conditions => {:rule_id => nil, :rule_priority => nil, :snapshot_id => @sids, :characteristic_id => nil, :person_id => nil})

    rows_by_metric_id={}
    @rows=[]

    measures.each do |measure|
      next unless measure.metric
      
      if measure.metric.timemachine? && (measure.value || measure.text_value)
        row=rows_by_metric_id[measure.metric_id]
        unless row
          row=Sonar::TimemachineRow.new(measure.metric)
          @rows<<row
          rows_by_metric_id[measure.metric_id]=row
        end
        
        #optimization : avoid eager loading of snapshots
        measure.snapshot=snapshot_by_id[measure.snapshot_id]
        row.add_measure(measure)
      end
    end

    @rows.sort!

    if params[:metrics]
      @metric_keys=params[:metrics].split(',')
    else
      @metric_keys=Property.value(CONFIGURATION_DEFAULT_CHART_METRICS)
      if @metric_keys.blank?
        @metric_keys=Metric.default_time_machine_metrics
      else
        @metric_keys=@metric_keys.split(',')
      end
    end
  end


  def set_default_chart_metrics
    metric_keys=params[:metrics]
    Property.set(CONFIGURATION_DEFAULT_CHART_METRICS, metric_keys)
    flash['notice']='Default metrics on chart are updated.'
    redirect_to :overwrite_params => { :action => 'index' }
  end

end
