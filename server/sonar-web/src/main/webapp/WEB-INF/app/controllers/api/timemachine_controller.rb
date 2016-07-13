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
require 'fastercsv'

class Api::TimemachineController < Api::ApiController
  MAX_IN_ELEMENTS=990

  class MetadataId < Struct.new(:metric_id)
  end

  class Metadata < Struct.new(:metric)
    def to_id
      @id ||=
        begin
          MetadataId.new(self.metric.id)
        end
    end

    def to_s
      label=self.metric.key
      label
    end
  end


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
  #
  def index
    begin
      component_key = params[:resource]

      @component=Project.by_key(component_key)
      if @component.nil?
        raise ApiException.new 404, "Resource not found: #{component_key}"
      end

      # ---------- PARAMETERS
      load_metadata()

      @analysis_uuids = []
      @dates_by_analysis_uuid = {}
      @measures_by_analysis_uuid = {}

      unless @metrics.empty?
        sql_conditions = ['project_measures.component_uuid=:component_uuid AND snapshots.status=:status AND project_measures.person_id IS NULL']
        sql_values = {:component_uuid => @component.uuid, :status => Snapshot::STATUS_PROCESSED}

        if params[:fromDateTime]
          from = parse_datetime(params[:fromDateTime])
          if from
            sql_conditions << 'snapshots.created_at>=:from'
            sql_values[:from] = from.to_i*1000
          end
        end

        if params[:toDateTime]
          to = parse_datetime(params[:toDateTime])
          if to
            sql_conditions << 'snapshots.created_at<=:to'
            sql_values[:to] = to.to_i*1000
          end
        end

        sql_conditions << 'project_measures.metric_id IN (:metrics)'
        sql_values[:metrics] = @metrics.select{|m| m.id}

        measures = ProjectMeasure.find(:all,
          :joins => :analysis,
          :select => 'project_measures.id,project_measures.value,project_measures.text_value,project_measures.metric_id,project_measures.analysis_uuid,snapshots.created_at',
          :conditions => [sql_conditions.join(' AND '), sql_values],
          :order => 'snapshots.created_at')

        # ---------- PROCESS RESPONSE
        # sorted array of unique snapshot ids

        # workaround to convert snapshot date from string to datetime
        date_column = Snapshot.connection.columns('snapshots')[1]

        measures.each do |m|
          @analysis_uuids << m.analysis_uuid
          @dates_by_analysis_uuid[m.analysis_uuid] = date_column.type_cast(m.attributes['created_at']).to_i
          @measures_by_analysis_uuid[m.analysis_uuid] ||= {}
          @measures_by_analysis_uuid[m.analysis_uuid][MetadataId.new(m.metric_id)] = m
        end
        @analysis_uuids.uniq!
      end

      # ---------- FORMAT RESPONSE
      respond_to do |format|
        format.json { render :json => jsonp(to_json) }
        format.csv  {
          send_data(to_csv,
            :type => 'text/csv; charset=utf-8; header=present',
            :disposition => 'attachment; filename=timemachine.csv')
        }
        format.text { render :text => text_not_supported }
      end
    rescue ApiException => e
      render_error(e.msg, e.code)
    end
  end

  private

  def load_metrics
    if params[:metrics]
      @metrics = Metric.by_keys(params[:metrics].split(','))
    else
      @metrics=[]
    end
  end

  def load_metadata
    load_metrics

    @metadata=[]
    @metrics.each do |metric|
      @metadata << Metadata.new(metric)
    end
    @metadata
  end

  def to_json
    cols=[]
    cells=[]
    result=[{:cols => cols, :cells => cells}]

    @metadata.each do |metadata|
      col={:metric => metadata.metric.key}
      cols<<col
    end

    @analysis_uuids.each do |analysis_uuid|
      cell={:d => Api::Utils.format_datetime(Time.at(@dates_by_analysis_uuid[analysis_uuid]/1000))}
      cell_values=[]
      cell[:v]=cell_values

      @metadata.each do |metadata|
        measure = @measures_by_analysis_uuid[analysis_uuid][metadata.to_id]
        if measure
          cell_values << measure.typed_value
        else
          cell_values << nil
        end
      end
      cells<<cell
    end
    result
  end

  def to_csv
    FasterCSV.generate do |csv|
      header = ['date']
      @metadata.each do |metadata|
        header<<metadata.to_s
      end
      csv << header
      @analysis_uuids.each do |analysis_uuid|
        row=[Api::Utils.format_datetime(Time.at(@dates_by_analysis_uuid[analysis_uuid]/1000))]
        @metadata.each do |metadata|
          measure=@measures_by_analysis_uuid[analysis_uuid][metadata.to_id]
          if measure
            row<<measure.typed_value
          else
            row<<nil
          end
        end
        csv<<row
      end
    end
  end

end
