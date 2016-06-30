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
 # License along with {library}; if not, write to the Free Software
 # Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 #
class ComponentSnapshot
  attr_accessor :analysis, :component

  def initialize(analysis, component)
    @analysis = analysis
    @component = component
  end

  def resource
    @component
  end

  def measures
    @measures ||=
      begin
        ProjectMeasure.all(:conditions => ['component_uuid = ? and analysis_uuid=? and person_id is null', @component.uuid, @analysis.uuid])
      end
  end

  def person_measures
    @person_measures ||=
      begin
        ProjectMeasure.all(:conditions => ['component_uuid = ? and analysis_uuid=? and person_id is not null', @component.uuid, @analysis.uuid])
      end
  end

  def id
    @analysis.id
  end

  def uuid
    @analysis.uuid
  end

  def measure(metric)
    unless metric.is_a? Metric
      metric=Metric.by_key(metric)
    end
    metric ? measures_hash[metric.id] : nil
  end

  def person_measure(metric, person_id)
    person_measures.each do |m|
      return m if m.metric_id==metric.id && m.person_id==person_id
    end
    nil
  end

  def created_at
    @analysis.created_at
  end

  def created_at_long
    @analysis.created_at
  end

  def build_date
    @analysis.build_date
  end

  def period1_date
    @analysis.period1_date
  end

  def period2_date
    @analysis.period2_date
  end

  def period3_date
    @analysis.period3_date
  end

  def period4_date
    @analysis.period4_date
  end

  def period5_date
    @analysis.period5_date
  end

  def last?
    @analysis.islast
  end

  def f_measure(metric)
    m=measure(metric)
    m && m.formatted_value
  end

  def periods?
    (period1_mode || period2_mode || period3_mode || period4_mode || period5_mode) != nil
  end

  def period1_mode
    @analysis.period1_mode
  end

  def period2_mode
    @analysis.period2_mode
  end

  def period3_mode
    @analysis.period3_mode
  end

  def period4_mode
    @analysis.period4_mode
  end

  def period5_mode
    @analysis.period5_mode
  end

  def period1_param
    @analysis.period1_param
  end

  def period2_param
    @analysis.period2_param
  end

  def period3_param
    @analysis.period3_param
  end

  def period4_param
    @analysis.period4_param
  end

  def period5_param
    @analysis.period5_param
  end

  def period1_date
    @analysis.period1_date
  end

  def period2_date
    @analysis.period2_date
  end

  def period3_date
    @analysis.period3_date
  end

  def period4_date
    @analysis.period4_date
  end

  def period5_date
    @analysis.period5_date
  end

  def component_uuid_for_authorization
    @component.project_uuid
  end

  def period_mode(period_index)
    @analysis.send "period#{period_index}_mode"
  end

  def period_param(period_index)
    @analysis.send "period#{period_index}_param"
  end

  def period_datetime(period_index)
    @analysis.send "period#{period_index}_date"
  end

  def metrics
    @metrics ||=
      begin
        measures_hash.keys.map { |metric_id| Metric::by_id(metric_id) }.uniq.compact
      end
  end

  # metrics of all the available measures
  def metric_keys
    @metric_keys ||=
      begin
        metrics.map { |m| m.name }
      end
  end

  def rule_measures(metrics=nil, rule=nil)
    # SONAR-7501 kept for backward-compatibility
    []
  end

  def rule_measure(metric, rule)
    # SONAR-7501 kept for backward-compatibility
    nil
  end

  def event(category)
    @analysis.event(category)
  end

  private

  def measures_hash
    @measures_hash ||=
      begin
        hash = {}
        measures.each do |measure|
          hash[measure.metric_id]=measure
        end
        hash
      end
  end
end
