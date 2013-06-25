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
class CloudController < ApplicationController

  SECTION=Navigation::SECTION_RESOURCE
  before_filter :init_resource_for_user_role

  def index
    @size_metric=Metric.by_key(params[:size]||'ncloc')
    @color_metric=Metric.by_key(params[:color]||'coverage')

    if @snapshot.measure(@color_metric).nil?
      @color_metric=Metric.by_key('violations_density')
    end

    snapshot_conditions='snapshots.islast=:islast AND snapshots.qualifier in (:qualifiers) AND snapshots.qualifier!=:test_qualifier AND
      (snapshots.id=:sid OR (snapshots.root_snapshot_id=:root_sid AND snapshots.path LIKE :path))'
    snapshot_values={
        :islast => true,
        :qualifiers => @snapshot.leaves_qualifiers,
        :test_qualifier => 'UTS',
        :sid => @snapshot.id,
        :root_sid => (@snapshot.root_snapshot_id || @snapshot.id),
        :path => "#{@snapshot.path}#{@snapshot.id}.%"
    }

    @snapshots=Snapshot.find(:all, :conditions => [snapshot_conditions, snapshot_values], :include => 'project', :order => 'projects.name')

    size_measures=ProjectMeasure.find(:all,
                                      :select => 'project_measures.id,project_measures.value,project_measures.metric_id,project_measures.snapshot_id,project_measures.rule_id,project_measures.text_value,project_measures.characteristic_id,project_measures.alert_status',
                                      :joins => :snapshot,
                                      :conditions => [snapshot_conditions + " AND project_measures.metric_id=#{@size_metric.id} AND project_measures.rule_id IS NULL AND project_measures.characteristic_id IS NULL AND project_measures.person_id IS NULL", snapshot_values],
                                      :order => 'project_measures.value')

    color_measures=ProjectMeasure.find(:all,
                                       :select => 'project_measures.id,project_measures.value,project_measures.metric_id,project_measures.snapshot_id,project_measures.rule_id,project_measures.text_value,project_measures.characteristic_id,project_measures.alert_status',
                                       :joins => :snapshot,
                                       :conditions => [snapshot_conditions + " AND project_measures.metric_id=#{@color_metric.id} AND project_measures.rule_id IS NULL AND project_measures.characteristic_id IS NULL AND project_measures.person_id IS NULL", snapshot_values],
                                       :order => 'project_measures.value')

    @size_measure_by_sid={}
    @color_measure_by_sid={}
    size_measures.each do |m|
      @size_measure_by_sid[m.snapshot_id]=m
    end
    color_measures.each do |m|
      @color_measure_by_sid[m.snapshot_id]=m
    end
    @min_size_value=(size_measures.empty? ? 0.0 : size_measures.first.value)
    @max_size_value=(size_measures.empty? ? 0.0 : size_measures.last.value)
  end

end
