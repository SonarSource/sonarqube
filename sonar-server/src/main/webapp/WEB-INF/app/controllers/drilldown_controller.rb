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
class DrilldownController < ApplicationController
  before_filter :init_resource_for_user_role

  helper ProjectHelper, DashboardHelper, IssuesHelper

  SECTION=Navigation::SECTION_RESOURCE

  def measures
    @metric = select_metric(params[:metric], 'ncloc')
    @highlighted_metric = Metric.by_key(params[:highlight]) || @metric

    # selected resources
    if params[:rids]
      selected_rids= params[:rids]
    elsif params[:resource]
      highlighted_resource=Project.by_key(params[:resource])
      selected_rids=(highlighted_resource ? [highlighted_resource.id] : [])
    else
      selected_rids=[]
    end
    selected_rids=selected_rids.map { |r| r.to_i }


    # options
    options={}
    if params[:characteristic_id]
      @characteristic=Characteristic.find(params[:characteristic_id])
    elsif params[:model] && params[:characteristic]
      @characteristic=Characteristic.find(:first, :select => 'id', :include => 'quality_model', :conditions => ['quality_models.name=? AND characteristics.kee=? AND characteristics.enabled=?', params[:model], params[:characteristic], true])
    end
    options[:characteristic]=@characteristic
    if params[:period] && Api::Utils.valid_period_index?(params[:period])
      @period=params[:period].to_i
      options[:period]=@period
    end

    # load data
    @drilldown = Drilldown.new(@resource, @metric, selected_rids, options)

    @highlighted_resource=@drilldown.highlighted_resource
    if @highlighted_resource.nil? && @drilldown.columns.empty?
      @highlighted_resource=@resource
    end

    @display_viewers=display_metric_viewers?(@highlighted_resource||@resource, @highlighted_metric.key)
  end

  def issues
    @rule=Rule.by_key_or_id(params[:rule])

    # variation measures
    if params[:period].present? && params[:period].to_i>0
      @period=params[:period].to_i
      metric_prefix = 'new_'
    else
      @period=nil
      metric_prefix = ''
    end

    @severity = params[:severity]
    @rule_severity = params[:rule_sev] || @severity

    if @rule && @rule_severity.blank?
      # workaround for SONAR-3255 : guess the severity
      @rule_severity=guess_rule_severity_for_issues_metric(@snapshot, @rule, metric_prefix)
    end

    if @rule_severity.present?
      # Filter resources by severity
      @metric = Metric::by_key("#{metric_prefix}#{@rule_severity.downcase}_violations")
    else
      @metric = Metric::by_key("#{metric_prefix}violations")
    end

    # selected resources
    if params[:rids]
      @selected_rids= params[:rids]
    elsif params[:resource]
      highlighted_resource=Project.by_key(params[:resource])
      @selected_rids=(highlighted_resource ? [highlighted_resource.id] : [])
    else
      @selected_rids=[]
    end
    @selected_rids=@selected_rids.map { |r| r.to_i }

    # options for Drilldown
    options={:exclude_zero_value => true, :period => @period}
    if @rule
      params[:rule]=@rule.key # workaround for SONAR-1767 : the javascript hash named "rp" in the HTML source must contain the rule key, but not the rule id
      options[:rule_id]=@rule.id
    end

    # load data
    @drilldown = Drilldown.new(@resource, @metric, @selected_rids, options)

    @highlighted_resource=@drilldown.highlighted_resource
    if @highlighted_resource.nil? && @drilldown.columns.empty?
      @highlighted_resource=@resource
    end

    #
    # Initialize filter by rule
    #
    if @severity.present?
      # Filter on severity -> filter rule measures by the selected metric
      @rule_measures = @snapshot.rule_measures(@metric)
    else
      # No filter -> loads all the rules
      metrics=[
          Metric.by_key("#{metric_prefix}blocker_violations"),
          Metric.by_key("#{metric_prefix}critical_violations"),
          Metric.by_key("#{metric_prefix}major_violations"),
          Metric.by_key("#{metric_prefix}minor_violations"),
          Metric.by_key("#{metric_prefix}info_violations")
      ]
      @rule_measures = @snapshot.rule_measures(metrics)
    end
  end

  # Deprecated in 3.6. Kept for backward-compatibility, for example with SQALE (http://jira.sonarsource.com/browse/SQALE-185)
  def violations
    redirect_to(params.merge({:action => 'issues'}))
  end

  private

  def select_metric(metric_key, default_key)
    metric=nil
    if metric_key
      metric=Metric::by_key(metric_key)
    end
    if metric.nil?
      metric=Metric::by_key(default_key)
    end
    metric
  end

  def select_subsnapshot(snapshot, sid)
    if sid
      snapshot.children.each do |subsnapshot|
        return subsnapshot if subsnapshot.id==sid.to_i
      end
    end
    nil
  end

  def array_to_hash_by_id(array)
    hash={}
    array.each do |s|
      hash[s.id]=s
    end
    hash
  end

  def display_metric_viewers?(resource, metric_key)
    return true if resource.file?
    java_facade.getResourceTabsForMetric(resource.scope, resource.qualifier, resource.language, resource.last_snapshot.metric_keys.to_java(:string), metric_key).each do |tab|
      tab.getUserRoles().each do |role|
        if has_role?(role, resource)
          return true
        end
      end
    end
    false
  end

  def display_violation_viewers?(snapshot)
    return true if snapshot.file?
    snapshot.violations.size>0
  end

  def guess_rule_severity(snapshot, rule, metric_prefix)
    Severity::KEYS.each do |severity|
      if snapshot.rule_measure(Metric.by_key("#{metric_prefix}#{severity.downcase}_violations"), rule)
        return severity
      end
    end
    Severity::MAJOR
  end

  def guess_rule_severity_for_issues_metric(snapshot, rule, metric_prefix)
    Severity::KEYS.each do |severity|
      if snapshot.rule_measure(Metric.by_key("#{metric_prefix}#{severity.downcase}_violations"), rule)
        return severity
      end
    end
    Severity::MAJOR
  end

end
