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
require 'fastercsv'
require "json"

class Api::ProfilesController < Api::ApiController

  def index
    begin
      language=params[:language]
      raise ApiException.new(400, "Missing parameter: language") if language.blank?

      name=params[:name]
      if name.blank?
        @profile=Profile.find(:first, :conditions => ['language=? and default_profile=? and enabled=?', language, true, true])
      else
        @profile=Profile.find(:first, :conditions => ['language=? and name=? and enabled=?', language, name, true])
      end
      raise ApiException.new(404, "Profile not found") if @profile.nil?

      @active_rules=filter_rules()
      
      respond_to do |format|
        format.json { render :json => jsonp(to_json) }
        format.xml {render :xml => to_xml}
        format.text { render :text => text_not_supported }
      end
      
    rescue ApiException => e
      render_error(e.msg, e.code)
    end
  end


  private

  def filter_rules
    conditions=['active_rules.profile_id=?']
    condition_values=[@profile.id]

    if params[:rule_repositories].present?
      conditions<<'rules.plugin_name in (?)'
      condition_values<<params[:rule_repositories].split(',')
    end

    if params[:rule_severities].present?
      conditions<<'failure_level in (?)'
      condition_values<<params[:rule_severities].split(',').map{|severity| Sonar::RulePriority.id(severity)}
    end

    ActiveRule.find(:all, :include => [:rule, {:active_rule_parameters => :rules_parameter}], :conditions => [conditions.join(' AND ')].concat(condition_values))
  end

  def to_json
    result={}
    result[:name]=@profile.name
    result[:language]=@profile.language
    result[:parent]=@profile.parent_name if @profile.parent_name.present?
    result[:default]=@profile.default_profile
    result[:provided]=@profile.provided

    rules=[]
    @active_rules.each do |active_rule|
      hash={}
      hash[:key]=active_rule.rule.plugin_rule_key
      hash[:repo]=active_rule.rule.plugin_name
      hash[:severity]=active_rule.priority_text
      hash[:inheritance]=active_rule.inheritance if active_rule.inheritance
      params_hash=[]
      active_rule.active_rule_parameters.each do |param|
        params_hash<<{:key => param.name, :value => param.value}
      end
      hash[:params]=params_hash unless params_hash.empty?
      rules<<hash
    end
    result[:rules]=rules unless rules.empty?

    alerts=[]
    @profile.alerts.each do |alert|
      alert_hash={:metric => alert.metric.key, :operator => alert.operator}
      alert_hash[:error]=alert.value_error if alert.value_error.present?
      alert_hash[:warning]=alert.value_warning if alert.value_warning.present?
      alerts<<alert_hash
    end
    result[:alerts]=alerts unless alerts.empty?
    [result]
  end

  def to_xml
    xml = Builder::XmlMarkup.new(:indent => 0)
    xml.instruct!

    xml.profile do
      xml.name(@profile.name)
      xml.language(@profile.language)
      xml.parent(@profile.parent_name) if @profile.parent_name.present?
      xml.default(@profile.default_profile)
      xml.provided(@profile.provided)

      @active_rules.each do |active_rule|
        xml.rule do
          xml.key(active_rule.rule.plugin_rule_key)
          xml.repo(active_rule.rule.plugin_name)
          xml.severity(active_rule.priority_text)
          xml.inheritance(active_rule.inheritance) if active_rule.inheritance
          active_rule.active_rule_parameters.each do |param|
            xml.param do
              xml.key(param.name)
              xml.value(param.value)
            end
          end
        end
      end

      @profile.alerts.each do |alert|
        xml.alert do
          xml.metric(alert.metric.key)
          xml.operator(alert.operator)
          xml.error(alert.value_error) if alert.value_error.present?
          xml.warning(alert.value_warning) if alert.value_warning.present?
        end
      end
    end
  end
end