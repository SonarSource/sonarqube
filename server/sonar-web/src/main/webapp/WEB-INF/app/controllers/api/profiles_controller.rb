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

  # GET /api/profiles/list?[language=<language][&project=<project id or key>]
  #
  # Since v.3.3
  #
  # ==== Examples
  # - get all the profiles : GET /api/profiles/list
  # - get all the Java profiles : GET /api/profiles/list?language=java
  # - get the profiles used by the project 'foo' : GET /api/profiles/list?project=foo
  # - get the Java profile used by the project 'foo' : GET /api/profiles/list?project=foo&language=java
  def list
    language = params[:language]
    project_key = params[:project]

    profiles = []
    default_profile_by_language = {}
    if project_key.present?
      project = Project.by_key(project_key)
      not_found('Unknown project') unless project
      if language.present?
        default_profile_by_language[language] = Internal.qprofile_service.getDefault(language)
        profile = Internal.quality_profiles.findProfileByProjectAndLanguage(project.id, language)
        profiles << profile if profile
        # Return default profile if the project is not associate to a profile
        profiles << default_profile_by_language[language] unless profile
      else
        Api::Utils.languages.each do |language|
          default_profile_by_language[language.getKey()] = Internal.qprofile_service.getDefault(language.getKey())
          profile = Internal.quality_profiles.findProfileByProjectAndLanguage(project.id, language.getKey())
          profiles << profile if profile
          # Return default profile if the project is not associate to a profile
          profiles << default_profile_by_language[language.getKey()] unless profile
        end
      end
    elsif language.present?
      profiles = Internal.quality_profiles.profilesByLanguage(language).to_a
    else
      profiles = Internal.quality_profiles.allProfiles().to_a
    end

    # Populate the map of default profile by language by searching for all profiles languages
    # We have to do that as the profiles list do not contain this information (maybe we should add it?)
    profiles.each do |p|
      lang = p.language
      unless default_profile_by_language[lang]
        default_profile_by_language[lang] = Internal.qprofile_service.getDefault(lang.to_s)
      end
    end

    json = profiles.compact.map { |profile| {
      :key => profile.key,
      :name => profile.name,
      :language => profile.language,
      :default => default_profile_by_language[profile.language].name == profile.name
    } }
    respond_to do |format|
      format.json { render :json => jsonp(json) }
      format.xml { render :xml => xml_not_supported }
      format.text { render :text => text_not_supported }
    end
  end
  
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
