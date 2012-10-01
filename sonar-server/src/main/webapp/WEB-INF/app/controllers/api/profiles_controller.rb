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
  verify :method => :post, :only => [:restore]

  def index
    begin
      language=params[:language]
      raise ApiException.new(400, "Missing parameter: language") if language.blank?

      name=params[:name]
      if name.blank?
        @profile=Profile.by_default(language)
      else
        @profile=Profile.find_by_name_and_language(name, language)
      end
      raise ApiException.new(404, "Profile not found") if @profile.nil?

      @active_rules=filter_rules()

      respond_to do |format|
        format.json { render :json => jsonp(to_json) }
        format.xml { render :xml => to_xml }
        format.text { render :text => text_not_supported }
      end

    rescue ApiException => e
      render_error(e.msg, e.code)
    end
  end


  #
  # Backup a profile. If output format is xml, then backup is directly returned.
  #
  # curl -u admin:admin  http://localhost:9000/api/profiles/backup?language=java[&name=my_profile] -v
  #
  def backup
    access_denied unless has_role?(:admin)
    bad_request('Missing parameter: language') if params[:language].blank?

    if params[:name].blank?
      profile=Profile.by_default(params[:language])
    else
      profile=Profile.find_by_name_and_language(params[:name], params[:language])
    end
    not_found('Profile not found') unless profile

    backup = java_facade.backupProfile(profile.id)
    respond_to do |format|
      format.xml { render :xml => backup }
      format.json { render :json => jsonp({:backup => backup}) }
    end
  end

  #
  # Restore a profile backup.
  #
  # curl -X POST -u admin:admin -F 'backup=<my>backup</my>' -v http://localhost:9000/api/profiles/restore
  # curl -X POST -u admin:admin -F 'backup=@backup.xml' -v http://localhost:9000/api/profiles/restore
  #
  def restore
    access_denied unless has_role?(:admin)
    bad_request('Missing parameter: backup') if params[:backup].blank?

    backup = Api::Utils.read_post_request_param(params[:backup])

    messages=java_facade.restoreProfile(backup, true)
    status=(messages.hasErrors() ? 400 : 200)
    respond_to do |format|
      format.json { render :json => jsonp(validation_messages_to_json(messages)), :status => status }
    end
  end

  private

  def validation_messages_to_json(messages)
    hash={}
    hash[:errors]=messages.getErrors().to_a.map { |message| message }
    hash[:warnings]=messages.getWarnings().to_a.map { |message| message }
    hash[:infos]=messages.getInfos().to_a.map { |message| message }
    hash
  end

  def filter_rules
    conditions=['active_rules.profile_id=?']
    condition_values=[@profile.id]

    if params[:rule_repositories].present?
      conditions<<'rules.plugin_name in (?)'
      condition_values<<params[:rule_repositories].split(',')
    end

    if params[:rule_severities].present?
      conditions<<'failure_level in (?)'
      condition_values<<params[:rule_severities].split(',').map { |severity| Sonar::RulePriority.id(severity) }
    end

    ActiveRule.find(:all, :include => [:rule, {:active_rule_parameters => :rules_parameter}], :conditions => [conditions.join(' AND ')].concat(condition_values))
  end

  def to_json
    result={}
    result[:name]=@profile.name
    result[:language]=@profile.language
    result[:parent]=@profile.parent_name if @profile.parent_name.present?
    result[:default]=@profile.default_profile?

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
      xml.default(@profile.default_profile?)

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