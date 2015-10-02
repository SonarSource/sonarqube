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
require 'json'

class Api::ProfilesController < Api::ApiController

  # GET /api/profiles?language=<language>[&name=<name>]
  def index
    require_parameters :language

    language=params[:language]
    name=params[:name]
    if name.blank?
      @profile=Profile.by_default(language)
    else
      @profile=Profile.find_by_name_and_language(name, language)
    end
    not_found('Profile not found') unless @profile

    @active_rules=filter_rules()

    respond_to do |format|
      format.json { render :json => jsonp(to_json) }
      format.xml { render :xml => to_xml }
      format.text { render :text => text_not_supported }
    end
  end

  # Backup a profile. If output format is xml, then backup is directly returned.
  # GET /api/profiles/backup?language=<language>[&name=my_profile] -v
  def backup
    require_parameters :language

    language = params[:language]
    if (params[:name].blank?)
      profile = Internal.qprofile_service.getDefault(language)
    else
      profile = Internal.quality_profiles.profile(params[:name], params[:language])
    end

    not_found('Profile not found') unless profile
    backup = Internal.component(Java::OrgSonarServerQualityprofile::QProfileService.java_class).backup(profile.key)

    respond_to do |format|
      format.xml { render :xml => backup }
      format.json { render :json => jsonp({:backup => backup}) }
    end
  end

  # Restore a profile backup.
  # curl -X POST -u admin:admin -F 'backup=<my>backup</my>' -v http://localhost:9000/api/profiles/restore
  # curl -X POST -u admin:admin -F 'backup=@backup.xml' -v http://localhost:9000/api/profiles/restore
  def restore
    verify_post_request
    require_parameters :backup

    backup = Api::Utils.read_post_request_param(params[:backup])
    Internal.component(Java::OrgSonarServerQualityprofile::QProfileService.java_class).restore(backup)

    respond_to do |format|
      #TODO format.json { render :json => jsonp(validation_result_to_json(result)), :status => 200 }
      format.json { render :json => jsonp({}), :status => 200 }
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

  def validation_result_to_json(result)
    hash={}
    hash[:warnings]=result.warnings().to_a.map { |message| message }
    hash[:infos]=result.infos().to_a.map { |message| message }
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
    result[:parent]=@profile.parent_kee if @profile.parent_kee.present?
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

    [result]
  end

  def to_xml
    xml = Builder::XmlMarkup.new(:indent => 0)
    xml.instruct!

    xml.profile do
      xml.name(@profile.name)
      xml.language(@profile.language)
      xml.parent(@profile.parent_kee) if @profile.parent_kee.present?
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
    end
  end

end
