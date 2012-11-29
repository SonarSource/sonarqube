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

class ComparisonController < ApplicationController

  def index
    snapshots = []
    resource_key = params[:resource]
    if resource_key && !resource_key.blank?
      # the request comes from a project: let's select its 5 latest versions
      project = Project.by_key(resource_key)
      snapshots = project.events.select { |event| !event.snapshot_id.nil? && event.category==EventCategory::KEY_VERSION }[0..5].reverse.map {|e| e.snapshot}
    else
      # the request comes from the comparison page: let's compare the given snapshots
      sids = get_params_as_array(:sids)
      unless sids.empty?
        selected_snapshots = Snapshot.find(:all, :conditions => ['id in (?)', sids])
        # next loop is required to keep the order that was decided by the user and which comes from the "sids" parameter
        sids.each do |id|
          selected_snapshots.each do |s|
            snapshots << s if id==s.id.to_s
          end 
        end
      end
    end    
    @snapshots = select_authorized(:user, snapshots)
    
    metrics = get_params_as_array(:metrics)
    if metrics.empty?
      metrics = [        
        'ncloc',
        'complexity',
        'comment_lines_density',
        'duplicated_lines_density',
        'violations',
        'coverage'
      ]
    end
    @metrics = Metric.by_keys(metrics)
    
    @metric_to_choose = Metric.all.select {|m| m.display? && !@metrics.include?(m)}.sort_by(&:short_name)
    
  end
  
  def versions
    key = params[:resource]
    sids = get_params_as_array(:sids)
    
    unless key.blank?
      resource = Project.by_key(params[:resource])
      # we look for the events that are versions and that are not linked to snapshots already displayed on the page
      @versions = resource.events.select { |event| !event.snapshot_id.nil? && event.category==EventCategory::KEY_VERSION && !sids.include?(event.snapshot_id.to_s) }
    end
    
    render :partial => 'versions'
  end

  
  private
  
  def get_params_as_array(name)
    list = params[name]
    if list.blank?
      []
    else
      list.split(',')
    end
  end

end