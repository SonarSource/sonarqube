#
# SonarQube, open source software quality management tool.
# Copyright (C) 2008-2014 SonarSource
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

class ComparisonController < ApplicationController

  SECTION=Navigation::SECTION_HOME

  def index
    snapshots = []
    resource_key = params[:resource]
    if resource_key && !resource_key.blank?
      # the request comes from a project: let's select its 5 latest versions
      project = Project.by_key(resource_key)
      return render_not_found('Project not found') unless project

      snapshots = project.events.select { |event| !event.snapshot.nil? && event.category==EventCategory::KEY_VERSION }[0..5].reverse.map {|e| e.snapshot}
      # if last snapshot is not in the list, add it at the end (=> might be the case for views or developers which do not have events)
      last_snapshot = project.last_snapshot
      unless snapshots.last == last_snapshot
        snapshots.shift
        snapshots.push(last_snapshot)
      end
    else
      # the request comes from the comparison page: let's compare the given snapshots
      sids = get_params_as_array(:sids)
      unless sids.empty?
        selected_snapshots = Snapshot.all(:conditions => ['id in (?)', sids])
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

    @metric_to_choose = Metric.all.select {|m| m.display? && !m.on_new_code? && !@metrics.include?(m)}.sort_by(&:short_name)
  end

  def versions
    key = params[:resource]
    sids = get_params_as_array(:sids)

    unless key.blank?
      resource = Project.by_key(params[:resource])

      # we look for the events that are versions and that are not linked to snapshots already displayed on the page
      @versions = resource.events.select { |event| !event.snapshot_id.nil? && event.category==EventCategory::KEY_VERSION && !sids.include?(event.snapshot_id.to_s) }

      # check if the latest snapshot if suggested or not (and if not, suggest it as "LATEST" => this is used for views or developers which do not have events)
      latest_snapshot_id = resource.last_snapshot.id
      current_and_suggested_sids = sids + @versions.map {|e| e.snapshot_id.to_s}
      unless current_and_suggested_sids.include?(latest_snapshot_id.to_s)
        @versions.unshift Event.new(:name => Api::Utils.message('comparison.version.latest'), :snapshot_id => latest_snapshot_id)
      end
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
