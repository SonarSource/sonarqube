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

class RulesConfigurationController < ApplicationController

  SECTION=Navigation::SECTION_QUALITY_PROFILES

  STATUS_ACTIVE = "ACTIVE"
  STATUS_INACTIVE = "INACTIVE"

  ANY_SELECTION = []
  RULE_PRIORITIES = Sonar::RulePriority.as_options.reverse

  def index
    require_parameters :id

    call_backend do
      @profile = Internal.quality_profiles.profile(params[:id].to_i)
      not_found('Profile not found') unless @profile
      @parent_profile = Internal.quality_profiles.parent(@profile) if @profile.parent()

      add_breadcrumbs ProfilesController::root_breadcrumb, Api::Utils.language_name(@profile.language),
                      {:name => @profile.name, :url => {:controller => 'rules_configuration', :action => 'index', :id => @profile.id}}

      init_params
      @criteria_params = criteria_params
      stop_watch = Internal.profiling.start("rules", "BASIC")

      @pagination = Api::Pagination.new(params)
      paging = Java::OrgSonarServerQualityprofile::Paging.create(@pagination.per_page.to_i, @pagination.page.to_i)

      criteria = init_criteria
      query = Java::OrgSonarServerQualityprofile::ProfileRuleQuery::parse(criteria.to_java)
      if @activation==STATUS_ACTIVE
        result = Internal.quality_profiles.searchProfileRules(query, paging)
      else
        result = Internal.quality_profiles.searchInactiveProfileRules(query, paging)
      end

      @pagination.count = result.paging.total
      unless @searchtext.blank? && @tags.empty?
        if @activation == STATUS_ACTIVE
          @hidden_inactives = Internal.quality_profiles.countInactiveProfileRules(query)
        else
          @hidden_actives = Internal.quality_profiles.countProfileRules(query)
        end
      end
      stop_watch.stop("found #{@pagination.count} rules with criteria #{criteria.to_json}, displaying #{@pagination.per_page} items")

      @current_rules = result.rules

      @select_repositories = ANY_SELECTION + java_facade.getRuleRepositoriesByLanguage(@profile.language).collect { |repo| [repo.name(), repo.key()] }.sort
      @select_priority = ANY_SELECTION + RULE_PRIORITIES
      @select_activation = [[message('active'), STATUS_ACTIVE], [message('inactive'), STATUS_INACTIVE]]
      @select_inheritance = [[message('any'), 'any'], [message('rules_configuration.not_inherited'), 'NOT'], [message('rules_configuration.inherited'), 'INHERITED'],
                             [message('rules_configuration.overrides'), 'OVERRIDES']]
      @select_status = ANY_SELECTION + [[message('rules.status.beta'), Rule::STATUS_BETA],
                                        [message('rules.status.deprecated'), Rule::STATUS_DEPRECATED],
                                        [message('rules.status.ready'), Rule::STATUS_READY]]
      @select_sort_by = [[message('rules_configuration.rule_name'), Rule::SORT_BY_RULE_NAME], [message('rules_configuration.creation_date'), Rule::SORT_BY_CREATION_DATE]]
      @select_tags = ANY_SELECTION + Internal.rule_tags.listAllTags().sort
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

    active_rule_id = params[:active_rule_id].to_i
    call_backend do
      Internal.quality_profiles.revertActiveRule(active_rule_id)
    end

    redirect_to request.query_parameters.merge({:action => 'index', :id => params[:id].to_i, :commit => nil})
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

    rule = nil
    profile_id = params[:id].to_i
    rule_id = params[:rule_id].to_i
    call_backend do
      severity = params[:level]
      if severity.blank?
        # deactivate the rule
        Internal.quality_profiles.deactivateRule(profile_id, rule_id)
        rule = Internal.quality_profiles.findByRule(rule_id)
      else
        # activate the rule
        Internal.quality_profiles.activateRule(profile_id, rule_id, severity)
        rule = Internal.quality_profiles.findByProfileAndRule(profile_id, rule_id)
      end
    end

    profile = Internal.quality_profiles.profile(profile_id)
    parent_profile = Internal.quality_profiles.parent(profile)

    render :update do |page|
      page.replace_html("rule_#{rule.id}", :partial => 'rule', :object => rule, :locals => {:rule => rule, :profile => profile, :parent_profile => parent_profile})
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
    not_found('Profile not found') unless @profile
    add_breadcrumbs ProfilesController::root_breadcrumb, Api::Utils.language_name(@profile.language),
                    {:name => @profile.name, :url => {:controller => 'rules_configuration', :action => 'index', :id => @profile.id}}

    @rule = Internal.quality_profiles.findByRule(params[:rule_id].to_i)
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
      new_rule_id = Internal.rules.createRule(rule_id, params[:rule][:name], params[:rule][:priority], params[:rule][:description], params[:rule_param])
      new_rule = Internal.quality_profiles.findByRule(new_rule_id)
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
      not_found('Profile not found') unless @profile
      @rule = Internal.quality_profiles.findByRule(params[:rule_id].to_i)
      if @rule.templateId().nil?
        redirect_to :action => 'index', :id => params[:id]
      else
        @parent_rule = Internal.quality_profiles.findByRule(@rule.templateId())
        @active_rules = Internal.quality_profiles.countActiveRules(@rule.id()).to_i
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
      Internal.rules.updateRule(rule_id, params[:rule][:name], params[:rule][:priority], params[:rule][:description], params[:rule_param])
      rule = Internal.quality_profiles.findByRule(rule_id)
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
      Internal.rules.deleteRule(params[:rule_id].to_i)
      flash[:notice]=message('rules_configuration.rule_deleted')
    end
    redirect_to :action => 'index', :id => params[:id]
  end

  #
  #
  # POST /rules_configuration/bulk_edit?id=<profile id>&&bulk_action=<action>
  #
  # Values of the parameter 'bulk_action' :
  #   - 'activate' : activate all the selected rules with their default priority
  #   - 'deactivate' : deactivate all the selected rules
  #
  #
  def bulk_edit
    verify_post_request
    access_denied unless has_role?(:profileadmin)
    require_parameters :id, :bulk_action

    stop_watch = Internal.profiling.start("rules", "BASIC")
    @profile = Internal.quality_profiles.profile(params[:id].to_i)
    not_found('Profile not found') unless @profile
    init_params
    criteria = init_criteria
    query = Java::OrgSonarServerQualityprofile::ProfileRuleQuery::parse(criteria.to_java)
    activation = params[:rule_activation] || STATUS_ACTIVE
    case params[:bulk_action]
      when 'activate'
        count = Internal.quality_profiles.bulkActivateRule(query)
        stop_watch.stop("Activate #{count} rules with criteria #{criteria.to_json}")

        flash[:notice]=message('rules_configuration.x_rules_have_been_activated', :params => count)
        activation=STATUS_ACTIVE if activation==STATUS_INACTIVE

      when 'deactivate'
        count = Internal.quality_profiles.bulkDeactivateRule(query)
        stop_watch.stop("Deactivate #{count} rules with criteria #{criteria.to_json}")

        flash[:notice]=message('rules_configuration.x_rules_have_been_deactivated', :params => count)
        activation=STATUS_INACTIVE if activation==STATUS_ACTIVE
    end

    url_parameters=request.query_parameters.merge({:action => 'index', :bulk_action => nil, :id => @profile.id, :rule_activation => activation})
    redirect_to url_parameters
  end


  def update_param
    verify_post_request

    access_denied unless has_role?(:profileadmin)
    require_parameters :param_id, :active_rule_id, :profile_id

    rule = nil
    profile_id = params[:profile_id].to_i
    active_rule_id = params[:active_rule_id].to_i
    call_backend do
      Internal.quality_profiles.updateActiveRuleParam(active_rule_id, params[:param_id], params[:value])
      rule = Internal.quality_profiles.findByActiveRuleId(active_rule_id)
    end

    profile = Internal.quality_profiles.profile(profile_id)
    parent_profile = Internal.quality_profiles.parent(profile)
    render :partial => 'rule', :locals => {:rule => rule, :profile => profile, :parent_profile => parent_profile}
  end


  def update_rule_note
    verify_post_request
    require_parameters :rule_id, :active_rule_id

    rule = nil
    call_backend do
      Internal.rules.updateRuleNote(params[:rule_id].to_i, params[:text])
      rule = Internal.quality_profiles.findByActiveRuleId(params[:active_rule_id].to_i)
    end
    render :partial => 'rule_note', :locals => {:rule => rule}
  end


  def update_active_rule_note
    verify_post_request
    require_parameters :active_rule_id, :note

    rule = nil
    call_backend do
      Internal.quality_profiles.updateActiveRuleNote(params[:active_rule_id].to_i, params[:note])
      rule = Internal.quality_profiles.findByActiveRuleId(params[:active_rule_id].to_i)
    end
    render :partial => 'active_rule_note', :locals => {:rule => rule}
  end


  def delete_active_rule_note
    verify_post_request
    require_parameters :active_rule_id

    rule = nil
    call_backend do
      Internal.quality_profiles.deleteActiveRuleNote(params[:active_rule_id].to_i)
      rule = Internal.quality_profiles.findByActiveRuleId(params[:active_rule_id].to_i)
    end
    render :partial => 'active_rule_note', :locals => {:rule => rule}
  end

  def show_select_tags
    rule = Internal.quality_profiles.findByRule(params[:rule_id].to_i)
    tags = tag_selection_for_rule(rule)
    render :partial => 'select_tags', :locals => { :rule => rule, :tags => tags, :profile_id => params[:profile_id] }
  end

  def select_tags
    Internal.rules.updateRuleTags(params[:rule_id].to_i, params[:tags])
    rule = Internal.quality_profiles.findByRule(params[:rule_id].to_i)
    render :partial => 'rule_tags', :locals => {:rule => rule}
  end

  def create_tag
    Internal.rule_tags.create(params[:new_tag])
    rule = Internal.quality_profiles.findByRule(params[:rule_id].to_i)
    tags = tag_selection_for_rule(rule)

    render :partial => 'select_tags_list', :locals => {:tags => tags}
  end

  private

  def init_params
    @id = params[:id]
    @priorities = filter_any(params[:priorities]) || ['']
    @repositories = filter_any(params[:repositories]) || ['']
    @activation = params[:rule_activation] || STATUS_ACTIVE
    @inheritance = params[:inheritance] || 'any'
    @status = params[:status]
    @tags = filter_any(params[:tags]) || ['']
    @sort_by = !params[:sort_by].blank? ? params[:sort_by] : Rule::SORT_BY_RULE_NAME
    @searchtext = params[:searchtext]
  end

  def filter_any(array)
    if array && array.size>1 && array.include?('')
      array=[''] #keep only 'any'
    end
    array
  end

  def init_criteria()
    if @sort_by == Rule::SORT_BY_RULE_NAME
      asc = true
    elsif @sort_by == Rule::SORT_BY_CREATION_DATE
      asc = false
    else
      asc = true
    end
    {"profileId" => @profile.id.to_i, "activation" => @activation, "severities" => @priorities, "inheritance" => @inheritance, "statuses" => @status,
     "repositoryKeys" => @repositories, "nameOrKey" => @searchtext, "include_parameters_and_notes" => true, "language" => @profile.language, "tags" => @tags,
     "sort_by" => @sort_by, "asc" => asc}
  end

  def criteria_params
    new_params = params.clone
    new_params.delete('controller')
    new_params.delete('action')
    new_params
  end

  def tag_selection_for_rule(rule)
    Internal.rule_tags.listAllTags().sort.collect do |tag|
      {
        :value => tag,
        :selected => (rule.systemTags.contains?(tag) || rule.adminTags.contains?(tag)),
        :read_only => rule.systemTags.contains?(tag)
      }
    end
  end

end
