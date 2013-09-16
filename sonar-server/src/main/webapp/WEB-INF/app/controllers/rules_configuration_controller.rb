#
# Sonar, entreprise quality control tool.
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

class RulesConfigurationController < ApplicationController

  before_filter :hide_sidebar

  STATUS_ACTIVE = "ACTIVE"
  STATUS_INACTIVE = "INACTIVE"

  ANY_SELECTION = []
  RULE_PRIORITIES = Sonar::RulePriority.as_options.reverse

  def index
    require_parameters :id

    @profile = Profile.find(params[:id])
    add_breadcrumbs ProfilesController::root_breadcrumb, Api::Utils.language_name(@profile.language),
                    {:name => @profile.name, :url => {:controller => 'rules_configuration', :action => 'index', :id => @profile.id}}

    init_params()

    @select_repositories = ANY_SELECTION + java_facade.getRuleRepositoriesByLanguage(@profile.language).collect { |repo| [repo.getName(true), repo.getKey()] }.sort
    @select_priority = ANY_SELECTION + RULE_PRIORITIES
    @select_activation = [[message('any'), 'any'], [message('active'), STATUS_ACTIVE], [message('inactive'), STATUS_INACTIVE]]
    @select_inheritance = [[message('any'), 'any'], [message('rules_configuration.not_inherited'), 'NOT'], [message('rules_configuration.inherited'), 'INHERITED'],
                           [message('rules_configuration.overrides'), 'OVERRIDES']]
    @select_status = ANY_SELECTION + [[message('rules.status.beta'), Rule::STATUS_BETA],
                      [message('rules.status.deprecated'), Rule::STATUS_DEPRECATED],
                      [message('rules.status.ready'), Rule::STATUS_READY]]
    @select_sort_by = [[message('rules_configuration.rule_name'), Rule::SORT_BY_RULE_NAME], [message('rules_configuration.creation_date'), Rule::SORT_BY_CREATION_DATE]]

    @rules = Rule.search(java_facade, {
        :profile => @profile, :activation => @activation, :priorities => @priorities, :inheritance => @inheritance, :status => @status,
        :repositories => @repositories, :searchtext => @searchtext, :include_parameters_and_notes => true, :language => @profile.language, :sort_by => @sort_by})

    unless @searchtext.blank?
      if @activation==STATUS_ACTIVE
        @hidden_inactives = Rule.search(java_facade, {
            :profile => @profile, :activation => STATUS_INACTIVE, :priorities => @priorities, :status => @status,
            :repositories => @repositories, :language => @profile.language, :searchtext => @searchtext, :include_parameters_and_notes => false}).size

      elsif @activation==STATUS_INACTIVE
        @hidden_actives = Rule.search(java_facade, {
            :profile => @profile, :activation => STATUS_ACTIVE, :priorities => @priorities, :status => @status,
            :repositories => @repositories, :language => @profile.language, :searchtext => @searchtext, :include_parameters_and_notes => false}).size
      end
    end

    @pagination = Api::Pagination.new(params)
    @pagination.count = @rules.size
    @current_rules = @rules[@pagination.offset, @pagination.limit]
  end


  #
  #
  # POST /rules_configuration/revert_rule?id=<profile id>&active_rule_id=<active rule id>
  #
  #
  def revert_rule
    verify_post_request
    access_denied unless has_role?(:profileadmin)
    require_parameters :id, :active_rule_id
    id = params[:id].to_i
    rule_id = params[:active_rule_id].to_i
    java_facade.revertRule(id, rule_id, current_user.name)
    redirect_to request.query_parameters.merge({:action => 'index', :id => params[:id], :commit => nil})
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
    access_denied unless has_role?(:profileadmin)
    require_parameters :id, :rule_id
    profile = Profile.find(params[:id].to_i)
    if profile
      rule=Rule.first(:conditions => ["id = ? and status <> ?", params[:rule_id].to_i, Rule::STATUS_REMOVED])
      priority=params[:level]

      active_rule=profile.active_by_rule_id(rule.id)
      if priority.blank?
        # deactivate the rule
        if active_rule
          java_facade.ruleDeactivated(profile.id, active_rule.id, current_user.name)
          active_rule.destroy
          active_rule=nil
        end
      else
        # activate the rule
        activated = false
        if active_rule.nil?
          active_rule = ActiveRule.new(:profile_id => profile.id, :rule => rule)
          rule.parameters.select { |p| p.default_value.present? }.each do |p|
            active_rule.active_rule_parameters.build(:rules_parameter => p, :value => p.default_value)
          end
          activated = true
        end
        old_severity = active_rule.failure_level
        active_rule.failure_level=Sonar::RulePriority.id(priority)
        active_rule.save!
        if activated
          java_facade.ruleActivated(profile.id, active_rule.id, current_user.name)
        else
          java_facade.ruleSeverityChanged(profile.id, active_rule.id, old_severity, active_rule.failure_level, current_user.name)
        end
      end
      if active_rule
        active_rule.reload
      end

      render :update do |page|
        page.replace_html("rule_#{rule.id}", :partial => 'rule', :object => rule, :locals => {:profile => profile, :rule => rule, :active_rule => active_rule})
        page.assign('localModifications', true)
      end
    end
  end


  #
  #
  # GET /rules_configuration/new/<profile id>?rule_id=<rule id>
  #
  #
  def new
    # form to duplicate a rule
    access_denied unless has_role?(:profileadmin)
    require_parameters :id, :rule_id
    @profile = Profile.find(params[:id].to_i)
    @rule = Rule.find(params[:rule_id])
  end

  #
  #
  # POST /rules_configuration/create/<profile id>?rule_id=<rule id>&rule[name]=<new name>&...
  #
  #
  def create
    verify_post_request
    access_denied unless has_role?(:profileadmin)
    require_parameters :id, :rule_id
    profile = Profile.find(params[:id].to_i)
    template=Rule.find(params[:rule_id])
    rule=Rule.create(params[:rule].merge(
                         {
                             :priority => Sonar::RulePriority.id(params[:rule][:priority]),
                             :parent_id => template.id,
                             :plugin_name => template.plugin_name,
                             :cardinality => 'SINGLE',
                             :plugin_rule_key => "#{template.plugin_rule_key}_#{Time.now.to_i}",
                             :plugin_config_key => template.plugin_config_key,
                             :status => Rule::STATUS_READY,
                             :language => profile.language
                         }
                     ))

    template.parameters.each do |template_parameter|
      rule.rules_parameters.build(:name => template_parameter.name, :param_type => template_parameter.param_type, :description => template_parameter.description,
                                  :default_value => params[:rule_param][template_parameter.name])
    end

    if rule.save
      redirect_to :action => 'index', :id => profile.id, :searchtext => rule.name, :rule_activation => 'INACTIVE', "plugins[]" => rule.plugin_name

    else
      flash[:error]=message('rules_configuration.rule_not_valid_message_x', :params => rule.errors.full_messages.join('<br/>'))
      redirect_to :action => 'new', :id => profile.id, :rule_id => params[:rule_id]
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
    access_denied unless has_role?(:profileadmin)
    require_parameters :id, :rule_id
    @profile = Profile.find(params[:id])
    @rule = Rule.find(params[:rule_id])
    if !@rule.editable?
      redirect_to :action => 'index', :id => params[:id]
    end
  end

  #
  #
  # POST /rules_configuration/update/<profile id>?rule_id=<rule id>&rule[name]=<new name>&...
  #
  #
  def update
    verify_post_request
    access_denied unless has_role?(:profileadmin)
    require_parameters :id, :rule_id
    rule=Rule.find(params[:rule_id])
    if rule.editable?
      rule.name=params[:rule][:name]
      rule.description=params[:rule][:description]
      rule.priority=Sonar::RulePriority.id(params[:rule][:priority])
      rule.parameters.each do |parameter|
        parameter.default_value=params[:rule_param][parameter.name]
        parameter.save
      end
      if rule.save
        redirect_to :action => 'index', :id => params[:id], :searchtext => rule.name, :rule_activation => '', "plugins[]" => rule.plugin_name
      else
        flash[:error]=message('rules_configuration.rule_not_valid_message_x', :params => rule.errors.full_messages.join('<br/>'))
        redirect_to :action => 'new', :id => params[:id], :rule_id => params[:rule_id]
      end
    else
      flash[:error]='Unknown rule'
      redirect_to :action => 'index', :id => params[:id]
    end
  end


  #
  #
  # POST /rules_configuration/delete/<profile id>?rule_id=<rule id>
  #
  #
  def delete
    verify_post_request
    access_denied unless has_role?(:profileadmin)
    require_parameters :id, :rule_id
    rule=Rule.find(params[:rule_id])
    if rule.editable?
      rule.status=Rule::STATUS_REMOVED
      rule.save

      # it's mandatory to execute 'destroy_all' but not 'delete_all' because active_rule_parameters must
      # also be destroyed in cascade.
      ActiveRule.destroy_all("rule_id=#{rule.id}")
      flash[:notice]=message('rules_configuration.rule_deleted')
    else
      flash[:error]=message('rules_configuration.unknown_rule')
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
    require_parameters :profile_id, :param_id, :active_rule_id
    profile = Profile.find(params[:profile_id].to_i)
    rule_param = RulesParameter.find(params[:param_id].to_i)
    active_rule = ActiveRule.find(params[:active_rule_id].to_i)
    # As the active param can be null, we should not raise a RecordNotFound exception when it's not found (as it would be done when using find(:id) function)
    active_param = ActiveRuleParameter.find_by_id(params[:id].to_i) if params[:id].to_i > 0
    value = params[:value]
    if value != ""
      active_param = ActiveRuleParameter.new(:rules_parameter => rule_param, :active_rule => active_rule) if active_param.nil?
      old_value = active_param.value
      active_param.value = value
      if active_param.save! && active_param.valid?
        active_param.reload
        java_facade.ruleParamChanged(profile.id, active_rule.id, rule_param.name, old_value, value, current_user.name)
      end
    elsif !active_param.nil?
      old_value = active_param.value
      active_param.destroy
      java_facade.ruleParamChanged(profile.id, active_rule.id, rule_param.name, old_value, nil, current_user.name)
    end
    # let's reload the active rule
    active_rule = ActiveRule.find(active_rule.id)
    render :partial => 'rule', :locals => {:profile => profile, :rule => active_rule.rule, :active_rule => active_rule}
  end


  def update_rule_note
    verify_post_request
    access_denied unless has_role?(:profileadmin)
    require_parameters :rule_id
    rule = Rule.find(params[:rule_id])
    note = rule.note
    unless note
      note = RuleNote.new({:rule => rule})
      # set the note on the rule to avoid reloading the rule
      rule.note = note
    end
    note.text = params[:text]
    note.user_login = current_user.login
    note.save!
    render :partial => 'rule_note', :locals => {:rule => rule}
  end


  def update_active_rule_note
    verify_post_request
    access_denied unless has_role?(:profileadmin)
    require_parameters :active_rule_id, :note
    active_rule = ActiveRule.find(params[:active_rule_id])
    note = active_rule.note
    unless note
      note = ActiveRuleNote.new({:active_rule => active_rule})
      # set the note on the rule to avoid reloading the rule
      active_rule.note = note
    end
    note.text = params[:note]
    note.user_login = current_user.login
    note.save!
    render :partial => 'active_rule_note', :locals => {:active_rule => active_rule, :profile => active_rule.rules_profile}
  end


  def delete_active_rule_note
    verify_post_request
    access_denied unless has_role?(:profileadmin)
    require_parameters :active_rule_id
    active_rule = ActiveRule.find(params[:active_rule_id])
    active_rule.note.destroy if active_rule.note
    active_rule.note = nil
    render :partial => 'active_rule_note', :locals => {:active_rule => active_rule, :profile => active_rule.rules_profile}
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
