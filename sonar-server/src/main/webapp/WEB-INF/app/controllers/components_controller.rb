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
class ComponentsController < ApplicationController

  helper :metrics, :components

  verify :method => :post, :only => :update_default_treemap_metrics  

  before_filter :admin_required, :only => :update_default_treemap_metrics

  TREEMAP_SIZE = 280
  SECTION = Navigation::SECTION_RESOURCE

  def index
    @dashboard_configuration = Sonar::DashboardConfiguration.new

    @project = Project.by_key(params[:id])
    return access_denied unless has_role?(:user, @project)
    @snapshot = @project.last_snapshot
    @snapshots = Snapshot.find(:all, :include => 'project', :conditions => ['snapshots.parent_snapshot_id=? and snapshots.qualifier<>? and projects.qualifier<>?', @snapshot.id, Snapshot::QUALIFIER_UNIT_TEST_CLASS, Snapshot::QUALIFIER_UNIT_TEST_CLASS])
    @snapshots = select_authorized(:user, @snapshots)

    @columns = @dashboard_configuration.selected_columns
    metrics = @dashboard_configuration.homepage_metrics

    measures = component_measures(@snapshots, metrics)
    @measures_by_snapshot = measures_by_snapshot(@snapshots, measures)
    if @dashboard_configuration.treemap_enabled? && @snapshots.size>1
      @treemap = Sonar::TreemapBuilder.build(@snapshots, TREEMAP_SIZE, TREEMAP_SIZE)
    end
  end

  def treemap
    @snapshot=Snapshot.find(params[:sid])
    @snapshots = Snapshot.find(:all, :conditions => ['parent_snapshot_id=? and qualifier<>?', @snapshot.id, Snapshot::QUALIFIER_UNIT_TEST_CLASS])
    @treemap = Sonar::TreemapBuilder.build(@snapshots, TREEMAP_SIZE, TREEMAP_SIZE, params[:size_metric], params[:color_metric])

    render(:update) do |page|
      page.replace_html 'treemap', @treemap.generate_html
      page.replace_html 'treemap_gradient', :partial => 'components/treemap_gradient',
        :locals => { :color_metric => @treemap.color_metric}
      page.replace_html 'treemap_set_default', :partial => 'components/treemap_set_default',
        :locals => { :controller => 'components', :size_metric => params[:size_metric], :color_metric => params[:color_metric], :rid => @snapshot.project_id }
    end
  end

  def update_default_treemap_metrics
    Property.set(Sonar::TreemapBuilder::CONFIGURATION_DEFAULT_COLOR_METRIC, params[:color_metric])
    Property.set(Sonar::TreemapBuilder::CONFIGURATION_DEFAULT_SIZE_METRIC, params[:size_metric])
    redirect_to :action => 'index', :id => params[:rid], :configuring => true
  end

  protected

  def refresh_configure
    render :update do |page|
      page.replace_html("rule_id_#{@rule.id}", :partial => 'rule', :locals => { :rule => @rule })
    end
  end

  def measures_by_snapshot(snapshots, measures)
    snapshot_by_id = {}
    snapshots.each {|s| snapshot_by_id[s.id]=s}
    hash={}
    measures.each do |m|
      if m && m.snapshot_id && snapshot_by_id[m.snapshot_id]
        hash[snapshot_by_id[m.snapshot_id]] ||= []
        hash[snapshot_by_id[m.snapshot_id]] << m
      end
    end
    hash
  end

  def component_measures(snapshots, metrics)
    sids = snapshots.collect{|s| s.id}
    if sids && sids.size>0
      mids = metrics.collect{|metric| metric.id}
      measures=[]

      page_size=950
      page_count=(snapshots.size/page_size)
      page_count+=1 if (snapshots.size % page_size)>0

      page_count.times do |page_index|
        page_sids=sids[page_index*page_size...(page_index+1)*page_size]
        measures.concat(ProjectMeasure.find(:all, :conditions => {
          'snapshot_id' => page_sids,
          'metric_id' => mids,
          'rule_id' => nil,
          'rules_category_id' => nil,
          'rule_priority' => nil}))
        measures.concat(AsyncMeasureSnapshot.search(page_sids, mids))
      end
      measures
    else
      []
    end
  end
end