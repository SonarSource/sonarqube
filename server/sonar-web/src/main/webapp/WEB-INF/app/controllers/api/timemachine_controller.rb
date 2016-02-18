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
      resource_id = params[:resource]

      @resource=Project.by_key(resource_id)
      if @resource.nil?
        raise ApiException.new 404, "Resource not found: #{resource_id}"
      end

      # ---------- PARAMETERS
      load_metadata()

      @sids=[]
      @dates_by_sid={}
      @measures_by_sid={}

      unless @metrics.empty?
        sql_conditions = ['snapshots.project_id=:rid AND snapshots.status=:status AND project_measures.rule_id IS NULL AND project_measures.rule_priority IS NULL AND project_measures.person_id IS NULL']
        sql_values = {:rid => @resource.id, :status => Snapshot::STATUS_PROCESSED}

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
          :joins => :snapshot,
          :select => 'project_measures.id,project_measures.value,project_measures.text_value,project_measures.metric_id,project_measures.snapshot_id,snapshots.created_at',
          :conditions => [sql_conditions.join(' AND '), sql_values],
          :order => 'snapshots.created_at')

        # ---------- PROCESS RESPONSE
        # sorted array of unique snapshot ids

        # workaround to convert snapshot date from string to datetime
        date_column=Snapshot.connection.columns('snapshots')[1]

        measures.each do |m|
          @sids<<m.snapshot_id
          @dates_by_sid[m.snapshot_id]=date_column.type_cast(m.attributes['created_at'])
          @measures_by_sid[m.snapshot_id]||={}
          @measures_by_sid[m.snapshot_id][MetadataId.new(m.metric_id)]=m
        end
        @sids.uniq!
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
      @metadata<<Metadata.new(metric)
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

    @sids.each do |snapshot_id|
      cell={:d => Api::Utils.format_datetime(Time.at(@dates_by_sid[snapshot_id]/1000))}
      cell_values=[]
      cell[:v]=cell_values

      @metadata.each do |metadata|
        measure=@measures_by_sid[snapshot_id][metadata.to_id]
        if measure
          cell_values<<measure.typed_value
        else
          cell_values<<nil
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
      @sids.each do |snapshot_id|
        row=[Api::Utils.format_datetime(Time.at(@dates_by_sid[snapshot_id]/1000))]
        @metadata.each do |metadata|
          measure=@measures_by_sid[snapshot_id][metadata.to_id]
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
