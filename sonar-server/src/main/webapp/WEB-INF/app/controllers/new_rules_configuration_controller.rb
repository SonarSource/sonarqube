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
require 'cgi'
require 'java'

class NewRulesConfigurationController < ApplicationController

  SECTION=Navigation::SECTION_QUALITY_PROFILES

  STATUS_ACTIVE = "ACTIVE"
  STATUS_INACTIVE = "INACTIVE"

  ANY_SELECTION = []
  RULE_PRIORITIES = Sonar::RulePriority.as_options.reverse

  def index
    require_parameters :id

    call_backend do
      @profile = Internal.quality_profiles.profile(params[:id].to_i)

      add_breadcrumbs ProfilesController::root_breadcrumb, Api::Utils.language_name(@profile.language),
                      {:name => @profile.name, :url => {:controller => 'new_rules_configuration', :action => 'index', :id => @profile.id}}

      init_params()

      @select_repositories = ANY_SELECTION + java_facade.getRuleRepositoriesByLanguage(@profile.language).collect { |repo| [repo.getName(true), repo.getKey()] }.sort
      @select_priority = ANY_SELECTION + RULE_PRIORITIES
      @select_activation = [[message('active'), STATUS_ACTIVE], [message('inactive'), STATUS_INACTIVE]]
      @select_inheritance = [[message('any'), 'any'], [message('rules_configuration.not_inherited'), 'NOT'], [message('rules_configuration.inherited'), 'INHERITED'],
                             [message('rules_configuration.overrides'), 'OVERRIDES']]
      @select_status = ANY_SELECTION + [[message('rules.status.beta'), Rule::STATUS_BETA],
                                        [message('rules.status.deprecated'), Rule::STATUS_DEPRECATED],
                                        [message('rules.status.ready'), Rule::STATUS_READY]]
      @select_sort_by = [[message('rules_configuration.rule_name'), Rule::SORT_BY_RULE_NAME], [message('rules_configuration.creation_date'), Rule::SORT_BY_CREATION_DATE]]

      stop_watch = Internal.profiling.start("rules", "BASIC")

      criteria = {
          "profileId" => @profile.id.to_i, "activation" => @activation, "severities" => @priorities, "inheritance" => @inheritance, "statuses" => @status,
          "repositoryKeys" => @repositories, "nameOrKey" => @searchtext, "include_parameters_and_notes" => true, "language" => @profile.language, "sort_by" => @sort_by}

      @rules = []
      @pagination = Api::Pagination.new(params)

      query = Java::OrgSonarServerRule::ProfileRuleQuery::parse(criteria.to_java)
      paging = Java::OrgSonarServerQualityprofile::Paging.create(@pagination.per_page.to_i, @pagination.page.to_i)

      if @activation==STATUS_ACTIVE
        result = Internal.quality_profiles.searchActiveRules(query, paging)
      else
        result = Internal.quality_profiles.searchInactiveRules(query, paging)
      end

      @rules = result.rules
      @pagination.count = result.paging.total

      unless @searchtext.blank?
        if @activation==STATUS_ACTIVE
          @hidden_inactives = Internal.quality_profiles.countInactiveRules(query)
        else
          @hidden_actives = Internal.quality_profiles.countActiveRules(query)
        end
      end

      stop_watch.stop("found #{@pagination.count} rules with criteria #{criteria.to_json}, displaying #{@pagination.per_page} items")

      @current_rules = @rules
    end
  end


  #
  #
  # POST /rules_configuration/revert_rule?id=<profile id>&active_rule_id=<active rule id>
  #
  #
  def revert_rule
    verify_post_request
    require_parameters :id, :active_rule_id

    profile_id = params[:id].to_i
    rule_id = params[:active_rule_id].to_i

    call_backend do
      Internal.quality_profiles.revertActiveRule(profile_id, rule_id)
    end

    redirect_to request.query_parameters.merge({:action => 'index', :id => profile_id, :commit => nil})
  end


  #
  #
  # POST /rules_configuration/activate_rule?id=<profile id>&rule_id=<rule id>&level=<priority>
  #
  # If the parameter "level" is blank or null, then the rule is removed from the profile.
  #
  #
  def activate_rule
    verify_post_request
    require_parameters :id, :rule_id

    result = nil
    call_backend do
      severity = params[:level]
      if severity.blank?
        # deactivate the rule
        result = Internal.quality_profiles.deactivateRule(params[:id].to_i, params[:rule_id].to_i)
      else
        # activate the rule
        result = Internal.quality_profiles.activateRule(params[:id].to_i, params[:rule_id].to_i, severity)
      end
    end

    profile = result.profile
    rule = result.rule

    render :update do |page|
      page.replace_html("rule_#{rule.id}", :partial => 'rule', :object => rule, :locals => {:profile => profile, :rule => rule})
      page.assign('localModifications', true)
    end
  end


  #
  #
  # GET /rules_configuration/new/<profile id>?rule_id=<rule id>
  #
  #
  def new
    # form to duplicate a rule
    require_parameters :id, :rule_id
    @profile = Internal.quality_profiles.profile(params[:id].to_i)
    add_breadcrumbs ProfilesController::root_breadcrumb, Api::Utils.language_name(@profile.language),
                    {:name => @profile.name, :url => {:controller => 'new_rules_configuration', :action => 'index', :id => @profile.id}}

    @rule = Internal.quality_profiles.rule(params[:rule_id].to_i)
  end

  #
  #
  # POST /rules_configuration/create/<profile id>?rule_id=<rule id>&rule[name]=<new name>&...
  #
  #
  def create
    verify_post_request
    require_parameters :id, :rule_id

    profile_id = params[:id].to_i
    rule_id = params[:rule_id].to_i
    new_rule = nil
    call_backend do
      new_rule = Internal.quality_profiles.createRule(rule_id, params[:rule][:name], params[:rule][:priority], params[:rule][:description], params[:rule_param])
    end

    if new_rule
      redirect_to :action => 'index', :id => profile_id, :searchtext => "\"#{new_rule.name()}\"", :rule_activation => 'INACTIVE', "plugins[]" => new_rule.repositoryKey()
    else
      redirect_to :action => 'new', :id => profile_id, :rule_id => rule_id
    end
  end


  # deprecated since 2.3
  def export
    redirect_to request.query_parameters.merge({:controller => 'profiles', :action => 'export'})
  end

  #
  #
  # GET /rules_configuration/new/<profile id>?rule_id=<rule id>
  #
  #
  def edit
    # form to edit a rule
    require_parameters :id, :rule_id

    call_backend do
      @profile = Internal.quality_profiles.profile(params[:id].to_i)
      @rule = Internal.quality_profiles.rule(params[:rule_id].to_i)
      if @rule.parentId().nil?
        redirect_to :action => 'index', :id => params[:id]
      else
        @parent_rule = Internal.quality_profiles.rule(@rule.parentId())
        @active_rules = Internal.quality_profiles.countActiveRules(@rule)
      end
    end
  end

  #
  #
  # POST /rules_configuration/update/<profile id>?rule_id=<rule id>&rule[name]=<new name>&...
  #
  #
  def update
    verify_post_request
    require_parameters :id, :rule_id

    profile_id = params[:id].to_i
    rule_id = params[:rule_id].to_i
    rule = nil
    call_backend do
      rule = Internal.quality_profiles.updateRule(rule_id, params[:rule][:name], params[:rule][:priority], params[:rule][:description], params[:rule_param])
    end

    if rule
      redirect_to :action => 'index', :id => profile_id, :searchtext => "\"#{rule.name()}\"", :rule_activation => '', "plugins[]" => rule.repositoryKey()
    else
      redirect_to :action => 'new', :id => profile_id, :rule_id => rule_id
    end
  end


  #
  #
  # POST /rules_configuration/delete/<profile id>?rule_id=<rule id>
  #
  #
  def delete
    verify_post_request
    require_parameters :id, :rule_id

    call_backend do
      Internal.quality_profiles.deleteRule(params[:rule_id].to_i)
      flash[:notice]=message('rules_configuration.rule_deleted')
    end
    redirect_to :action => 'index', :id => params[:id]
  end

  #
  #
  # POST /rules_configuration/bulk_edit?id=<profile id>&bulk_rule_ids=<list of rule ids>&bulk_action=<action>
  #
  # Values of the parameter 'bulk_action' :
  #   - 'activate' : activate all the selected rules with their default priority
  #   - 'deactivate' : deactivate all the selected rules
  #
  #
  def bulk_edit
    verify_post_request
    access_denied unless has_role?(:profileadmin)
    require_parameters :id, :bulk_rule_ids, :bulk_action
    profile = Profile.find(params[:id].to_i)
    rule_ids = params[:bulk_rule_ids].split(',').map { |id| id.to_i }
    activation=params[:rule_activation] || STATUS_ACTIVE

    case params[:bulk_action]
      when 'activate'
        count=activate_rules(profile, rule_ids)
        flash[:notice]=message('rules_configuration.x_rules_have_been_activated', :params => count)
        activation=STATUS_ACTIVE if activation==STATUS_INACTIVE

      when 'deactivate'
        count=deactivate_rules(profile, rule_ids)
        flash[:notice]=message('rules_configuration.x_rules_have_been_deactivated', :params => count)
        activation=STATUS_INACTIVE if activation==STATUS_ACTIVE
    end

    url_parameters=request.query_parameters.merge({:action => 'index', :bulk_action => nil, :bulk_rule_ids => nil, :id => profile.id, :rule_activation => activation})
    redirect_to url_parameters
  end


  def update_param
    verify_post_request

    access_denied unless has_role?(:profileadmin)
    require_parameters :param_id, :active_rule_id, :profile_id

    result = nil
    call_backend do
      result = Internal.quality_profiles.updateActiveRuleParam(params[:profile_id].to_i, params[:active_rule_id].to_i, params[:param_id], params[:value])
    end

    profile = result.profile
    rule = result.rule
    render :partial => 'rule', :locals => {:profile => profile, :rule => rule}
  end


  def update_rule_note
    verify_post_request
    require_parameters :rule_id, :active_rule_id

    rule = nil
    call_backend do
      rule = Internal.quality_profiles.updateRuleNote(params[:active_rule_id].to_i, params[:rule_id].to_i, params[:text])
    end
    render :partial => 'rule_note', :locals => {:rule => rule}
  end


  def update_active_rule_note
    verify_post_request
    require_parameters :active_rule_id, :note

    rule = nil
    call_backend do
      rule = Internal.quality_profiles.updateActiveRuleNote(params[:active_rule_id].to_i, params[:note])
    end
    render :partial => 'active_rule_note', :locals => {:rule => rule}
  end


  def delete_active_rule_note
    verify_post_request
    require_parameters :active_rule_id

    rule = nil
    call_backend do
      rule = Internal.quality_profiles.deleteActiveRuleNote(params[:active_rule_id].to_i)
    end
    render :partial => 'active_rule_note', :locals => {:rule => rule}
  end


  private

  # return the number of newly activated rules
  def activate_rules(profile, rule_ids)
    count=0
    rule_ids_to_activate=(rule_ids - profile.active_rules.map { |ar| ar.rule_id })
    unless rule_ids_to_activate.empty?
      rules_to_activate=Rule.all(:conditions => ["status <> ? AND id IN (?)", Rule::STATUS_REMOVED, rule_ids_to_activate])
      count = rules_to_activate.size
      rules_to_activate.each do |rule|
        active_rule = profile.active_rules.create(:rule => rule, :failure_level => rule.priority)
        java_facade.ruleActivated(profile.id, active_rule.id, current_user.name)
      end
    end
    count
  end

  def deactivate_rules(profile, rule_ids)
    count=0
    profile.active_rules.each do |ar|
      if rule_ids.include?(ar.rule_id) && !ar.inheritance.present?
        java_facade.ruleDeactivated(profile.id, ar.id, current_user.name)
        ar.destroy
        count+=1
      end
    end
    count
  end

  def init_params
    @id = params[:id]
    @priorities = filter_any(params[:priorities]) || ['']
    @repositories = filter_any(params[:repositories]) || ['']
    @activation = params[:rule_activation] || STATUS_ACTIVE
    @inheritance = params[:inheritance] || 'any'
    @status = params[:status]
    @sort_by = !params[:sort_by].blank? ? params[:sort_by] : Rule::SORT_BY_RULE_NAME
    @searchtext = params[:searchtext]
  end

  def filter_any(array)
    if array && array.size>1 && array.include?('')
      array=[''] #keep only 'any'
    end
    array
  end

end
