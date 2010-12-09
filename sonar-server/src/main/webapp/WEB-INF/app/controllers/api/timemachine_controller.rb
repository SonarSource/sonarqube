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
class Api::TimemachineController < Api::ApiController
  MAX_IN_ELEMENTS=990
  #
  # GET /api/timemachine
  #
  def index
    begin
      resource_id = params[:resource]
      metric_keys = params[:metrics].split(',')
      metrics = Metric.by_keys(metric_keys)
      first_date = parse_datetime(params[:first_date])
      last_date = parse_datetime(params[:last_date])

      @resource=Project.by_key(resource_id)
      if @resource.nil?
        raise ApiException.new 404, "Resource not found: #{resource_id}"
      end

      snapshots = Snapshot.find(:all,
      :conditions => ['created_at>=? AND created_at<=? AND project_id=? AND status=?',
        first_date, last_date, @resource.id, Snapshot::STATUS_PROCESSED],
      :order => 'created_at')

      # Oracle limitation : no more than 1000 elements in IN clause
      if snapshots.length > MAX_IN_ELEMENTS
        size=snapshots.size
        snapshots=snapshots[size-MAX_IN_ELEMENTS .. size-1]
      end

      measures = find_measures(metrics, snapshots)

      result = []
      if !measures.empty?
        measures_by_sid = {}
        measures.each do |measure|
          measures_by_sid[measure.snapshot_id]||=[]
          measures_by_sid[measure.snapshot_id]<<measure
        end

        snapshots.each do |snapshot|
          snapshot_measures = measures_by_sid[snapshot.id] || []
          values_by_key = {}
          snapshot_measures.each do |measure|
            values_by_key[measure.metric.name] = measure.value.to_f if measure.value
          end

          values = []
          metric_keys.each do |metric|
            values<<values_by_key[metric]
          end
          result<<{format_datetime(snapshot.created_at) => values}
        end
      end

      # ---------- FORMAT RESPONSE
      respond_to do |format|
        format.json { render :json => jsonp(result) }
        format.xml  { render :xml  => xml_not_supported }
        format.text { render :text => text_not_supported }
      end
    rescue ApiException => e
      render_error(e.msg, e.code)
    end
  end

  private

  def find_measures(metrics, snapshots)
    ProjectMeasure.find(:all,
    :select => 'project_measures.id,project_measures.value,project_measures.metric_id,project_measures.snapshot_id',
    :conditions => ['rules_category_id IS NULL AND rule_id IS NULL AND rule_priority IS NULL AND metric_id IN (?) AND snapshot_id IN (?)',
      metrics.select{|m| m.id}, snapshots.map{|s| s.id}])
  end

end
