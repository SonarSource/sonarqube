#
# SonarQube, open source software quality management tool.
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
class Drilldown

  attr_reader :resource, :metric, :selected_resource_ids
  attr_reader :snapshot, :columns, :highlighted_resource, :highlighted_snapshot

  def initialize(resource, metric, selected_resource_ids, options={})
    @resource=resource
    @selected_resource_ids=selected_resource_ids||[]
    @metric=metric
    @snapshot=resource.last_snapshot
    @columns=[]

    if @snapshot
      column=DrilldownColumn.new(self, nil)
      while column.valid?
        column.init_measures(options)
        if column.display?
          @columns<<column
          if column.selected_snapshot
            @highlighted_snapshot=column.selected_snapshot
            @highlighted_resource=column.selected_snapshot.project
          end
        end
        column=DrilldownColumn.new(self, column)
      end
    end
  end

  def display_value?
    ProjectMeasure.exists?(["snapshot_id=? and metric_id=? and value is not null", @snapshot.id, @metric.id])
  end

  def display_period?(period_index)
    ProjectMeasure.exists?(["snapshot_id=? and metric_id=? and variation_value_#{period_index.to_i} is not null", @snapshot.id, @metric.id])
  end
end


class DrilldownColumn

  attr_reader :measures, :base_snapshot, :selected_snapshot, :qualifiers, :person_id

  def initialize(drilldown, previous_column)
    @drilldown = drilldown

    if previous_column
      @base_snapshot=(previous_column.selected_snapshot || previous_column.base_snapshot)
      @person_id=(previous_column.person_id || @base_snapshot.resource.person_id)
    else
      @base_snapshot=drilldown.snapshot
      @person_id=@base_snapshot.resource.person_id
    end

    # switch
    if @base_snapshot.resource.copy_resource
      @base_snapshot=@base_snapshot.resource.copy_resource.last_snapshot
      @qualifiers = @base_snapshot.children_qualifiers

    elsif previous_column
      @qualifiers=previous_column.qualifiers.map { |q| Java::OrgSonarServerUi::JRubyFacade.getInstance().getResourceChildrenQualifiers(q).to_a }.flatten

    else
      @qualifiers=drilldown.snapshot.children_qualifiers
    end
    @resource_per_sid={}
  end

  def init_measures(options)
    value_column = (options[:period] ? "variation_value_#{options[:period]}" : 'value')
    order="project_measures.#{value_column}"
    if @drilldown.metric.direction<0
      order += ' DESC'
    end

    conditions="snapshots.root_snapshot_id=:root_sid AND snapshots.islast=:islast AND snapshots.qualifier in (:qualifiers) " +
        " AND snapshots.path LIKE :path AND project_measures.metric_id=:metric_id AND project_measures.#{value_column} IS NOT NULL"
    condition_values={
        :root_sid => (@base_snapshot.root_snapshot_id || @base_snapshot.id),
        :islast => true,
        :qualifiers => @qualifiers,
        :metric_id => @drilldown.metric.id,
        :path => "#{@base_snapshot.path}#{@base_snapshot.id}.%"}

    if value_column=='value' && @drilldown.metric.best_value
      conditions<<' AND project_measures.value<>:best_value'
      condition_values[:best_value]=@drilldown.metric.best_value
    end

    if options[:exclude_zero_value] || (options[:period] && !@drilldown.metric.on_new_code?)
      conditions += " AND project_measures.#{value_column}<>0"
    end

    if options[:rule_id]
      conditions += ' AND project_measures.rule_id=:rule'
      condition_values[:rule]=options[:rule_id]
    else
      conditions += ' AND project_measures.rule_id IS NULL '
    end

    if options[:characteristic]
      conditions += ' AND project_measures.characteristic_id=:characteristic_id'
      condition_values[:characteristic_id]=options[:characteristic].id
    else
      conditions += ' AND project_measures.characteristic_id IS NULL'
    end

    if @person_id
      conditions += ' AND project_measures.person_id=:person_id'
      condition_values[:person_id]=@person_id
    else
      conditions += ' AND project_measures.person_id IS NULL'
    end

    @measures=ProjectMeasure.all(
        :select => "project_measures.id,project_measures.metric_id,project_measures.#{value_column},project_measures.text_value,project_measures.alert_status,project_measures.alert_text,project_measures.snapshot_id",
        :joins => :snapshot,
        :conditions => [conditions, condition_values],
        :order => order,
        :limit => 200)

    @resource_per_sid={}
    sids=@measures.map { |m| m.snapshot_id }.compact.uniq
    unless sids.empty?
      Snapshot.all(:include => :project, :conditions => {'snapshots.id' => sids}).each do |snapshot|
        @resource_per_sid[snapshot.id]=snapshot.project
        if @drilldown.selected_resource_ids.include?(snapshot.project_id)
          @selected_snapshot=snapshot
        end
      end
    end
  end

  def resource(measure)
    @resource_per_sid[measure.snapshot_id]
  end

  def display?
    @measures && !@measures.empty?
  end

  def valid?
    @base_snapshot && @qualifiers && !@qualifiers.empty?
  end

  def switch?
    selected_snapshot && selected_snapshot.resource && selected_snapshot.resource.copy_resource
  end
end