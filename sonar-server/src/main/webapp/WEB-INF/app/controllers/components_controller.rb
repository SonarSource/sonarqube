#
# Sonar, entreprise quality control tool.
# Copyright (C) 2008-2013 SonarSource
# mailto:contact AT sonarsource DOT com
#
# SonarQube is free software; you can redistribute it and/or
# modify it under the terms of the GNU Lesser General Public
# License as published by the Free Software Foundation; either
# version 3 of the License, or (at your option) any later version.
#
# SonarQube is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
# Lesser General Public License for more details.
#
# You should have received a copy of the GNU Lesser General Public License
# along with this program; if not, write to the Free Software Foundation,
# Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
#
class ComponentsController < ApplicationController

  helper :metrics, :components

  SECTION = Navigation::SECTION_RESOURCE

  def index
    init_resource_for_user_role
    @components_configuration = Sonar::ComponentsConfiguration.new

    @snapshots = Snapshot.all(:include => 'project', :conditions => ['snapshots.parent_snapshot_id=?', @snapshot.id])

    @columns = @components_configuration.selected_columns
    metrics = @components_configuration.homepage_metrics

    measures = component_measures(@snapshots, metrics)
    @measures_by_snapshot = measures_by_snapshot(@snapshots, measures)
  end

  protected

  def refresh_configure
    render :update do |page|
      page.replace_html("rule_id_#{@rule.id}", :partial => 'rule', :locals => {:rule => @rule})
    end
  end

  def measures_by_snapshot(snapshots, measures)
    snapshot_by_id = {}
    snapshots.each { |s| snapshot_by_id[s.id]=s }
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
    sids = snapshots.collect { |s| s.id }
    if sids && sids.size>0
      mids = metrics.collect { |metric| metric.id }
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
          'rule_priority' => nil,
          'characteristic_id' => nil,
          'person_id' => nil}))
      end
      measures
    else
      []
    end
  end



end