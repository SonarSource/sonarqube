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
  # Required parameters :
  # - resource (id or key)
  # - metrics
  #
  # Optional parameters :
  # - fromDateTime
  # - toDateTime
  # - model
  # - characteristics
  #
  def index
    begin
      resource_id = params[:resource]

      @resource=Project.by_key(resource_id)
      if @resource.nil?
        raise ApiException.new 404, "Resource not found: #{resource_id}"
      end

      # ---------- PARAMETERS
      measures_conditions = []
      measures_values = {}
      snapshots_conditions = []
      snapshots_values = {}

      if params[:fromDateTime]
        from = parse_datetime(params[:fromDateTime])
      end
      if from
        snapshots_conditions << 'created_at>=:from'
        snapshots_values[:from] = from
      end

      if params[:toDateTime]
        to = parse_datetime(params[:toDateTime])
      end
      if to
        snapshots_conditions << 'created_at<=:to'
        snapshots_values[:to] = to
      end

      snapshots_conditions << 'project_id=:rid AND status=:status'
      snapshots_values[:rid] = @resource.id
      snapshots_values[:status] = Snapshot::STATUS_PROCESSED

      snapshots = Snapshot.find(:all,
        :conditions => [ snapshots_conditions.join(' AND '), snapshots_values],
        :order => 'created_at')
      # Oracle limitation : no more than 1000 elements in IN clause
      if snapshots.length > MAX_IN_ELEMENTS
        size=snapshots.size
        snapshots=snapshots[size-MAX_IN_ELEMENTS .. size-1]
      end

      measures_conditions << 'project_measures.rules_category_id IS NULL AND project_measures.rule_id IS NULL AND project_measures.rule_priority IS NULL'

      measures_conditions << 'project_measures.snapshot_id IN (:snapshots)'
      measures_values[:snapshots] = snapshots.map{|s| s.id}

      metric_keys = params[:metrics].split(',')
      metrics = Metric.by_keys(metric_keys)
      measures_conditions << 'project_measures.metric_id IN (:metrics)'
      measures_values[:metrics] = metrics.select{|m| m.id}

      add_characteristic_filters(measures_conditions, measures_values)

      measures = ProjectMeasure.find(:all,
        :select => 'project_measures.id,project_measures.value,project_measures.metric_id,project_measures.snapshot_id',
        :conditions => [ measures_conditions.join(' AND '), measures_values])

      # ---------- PREPARE RESPONSE
      measures_by_sid = {}
      measures.each do |measure|
        measures_by_sid[measure.snapshot_id]||=[]
        measures_by_sid[measure.snapshot_id]<<measure
      end

      # ---------- FORMAT RESPONSE
      objects = { :snapshots => snapshots, :measures_by_sid => measures_by_sid, :metric_keys => metric_keys }
      respond_to do |format|
        format.json { render :json => jsonp(to_json(objects)) }
        format.xml  { render :xml  => to_xml(objects) }
        format.text { render :text => text_not_supported }
      end
    rescue ApiException => e
      render_error(e.msg, e.code)
    end
  end

  private

  def to_json(objects)
    snapshots = objects[:snapshots]
    measures_by_sid = objects[:measures_by_sid]
    metric_keys = objects[:metric_keys]

    result = []
    snapshots.each do |snapshot|
      result << snapshot_to_json(snapshot, measures_by_sid[snapshot.id] || [], metric_keys)
    end
    result
  end

  def snapshot_to_json(snapshot, measures, metric_keys)
    values_by_key = {}
    measures.each do |measure|
      values_by_key[measure.metric.name] = measure.value.to_f if measure.value
    end
    values = []
    metric_keys.each do |metric|
      values << values_by_key[metric]
    end
    json = { format_datetime(snapshot.created_at) => values }
    json
  end

  def to_xml(objects)
    snapshots = objects[:snapshots]
    measures_by_sid = objects[:measures_by_sid]
    metric_keys = objects[:metric_keys]

    xml = Builder::XmlMarkup.new(:indent => 0)
    xml.instruct!

    xml.snapshots do
      snapshots.each do |snapshot|
        snapshot_to_xml(xml, snapshot, measures_by_sid[snapshot.id])
      end
    end
  end

  def snapshot_to_xml(xml, snapshot, measures)
    xml.snapshot do
      xml.date(format_datetime(snapshot.created_at))
      # TODO measures
    end
  end

  def add_characteristic_filters(measures_conditions, measures_values)
    @characteristics=[]
    @characteristic_by_id={}
    if params[:model].present? && params[:characteristics].present?
      @characteristics=Characteristic.find(:all,
        :select => 'characteristics.id,characteristics.kee,characteristics.name',
        :joins => :quality_model,
        :conditions => ['quality_models.name=? AND characteristics.kee IN (?)', params[:model], params[:characteristics].split(',')])
      if @characteristics.empty?
        measures_conditions<<'project_measures.characteristic_id=-1'
      else
        @characteristics.each { |c| @characteristic_by_id[c.id]=c }
        measures_conditions<<'project_measures.characteristic_id IN (:characteristics)'
        measures_values[:characteristics]=@characteristic_by_id.keys
      end
    else
      measures_conditions<<'project_measures.characteristic_id IS NULL'
    end
  end

end
