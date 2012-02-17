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
require 'cgi'

class RulesConfigurationController < ApplicationController

  SECTION=Navigation::SECTION_CONFIGURATION
  
  STATUS_ACTIVE = "ACTIVE"
  STATUS_INACTIVE = "INACTIVE"
  ANY_SELECTION = [["Any", '']]
  RULE_PRIORITIES = Sonar::RulePriority.as_options.reverse

  # GETs should be safe (see http://www.w3.org/2001/tag/doc/whenToUseGet.html)
  verify :method => :post, 
         :only => ['activate_rule', 'update_param', 'bulk_edit', 'create', 'update', 'delete', 'revert_rule', 'update_rule_note', 'update_active_rule_note', 'delete_active_rule_note'], 
         :redirect_to => { :action => 'index' }

  before_filter :admin_required, :except => [ 'index', 'export' ]

  def index
    unless params[:id].blank?
      if params[:id].to_i<=0
        redirect_to :controller => 'profiles'
        return
      end
      begin
        @profile = Profile.find(params[:id].to_i)
      rescue
        redirect_to :controller => 'profiles'
        return
      end
    else
      @profile = Profile.default_profile
    end
    
    init_params()

    @select_plugins = ANY_SELECTION + java_facade.getRuleRepositoriesByLanguage(@profile.language).collect { |repo| [repo.getName(true), repo.getKey()]}.sort
    @select_priority = ANY_SELECTION + RULE_PRIORITIES
    @select_status = [[message('any'),''], [message('active'), STATUS_ACTIVE], [message('inactive'), STATUS_INACTIVE]]
    @select_inheritance = [[message('any'),''], [message('rules_configuration.not_inherited'), 'NOT'], [message('rules_configuration.inherited'), 'INHERITED'], [message('rules_configuration.overrides'), 'OVERRIDES']]

    @rules = Rule.search(java_facade, {
        :profile => @profile, :status => @status, :priorities => @priorities, :inheritance => @inheritance,
        :plugins =>  @plugins, :searchtext => @searchtext, :include_parameters_and_notes => true, :language => @profile.language})

    unless @searchtext.blank?
      if @status==STATUS_ACTIVE
        @hidden_inactives=Rule.search(java_facade, {
            :profile => @profile, :status => STATUS_INACTIVE, :priorities => @priorities,
            :plugins =>  @plugins, :language => @profile.language, :searchtext => @searchtext, :include_parameters_and_notes => false}).size

      elsif @status==STATUS_INACTIVE
        @hidden_actives=Rule.search(java_facade, {
            :profile => @profile, :status => STATUS_ACTIVE, :priorities => @priorities,
            :plugins =>  @plugins, :language => @profile.language, :searchtext => @searchtext, :include_parameters_and_notes => false}).size
      end
    end

  end


  #
  #
  # POST /rules_configuration/revert_rule?id=<profile id>&active_rule_id=<active rule id>
  #
  #
  def revert_rule
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
    profile = Profile.find(params[:id].to_i)
    if profile && !profile.provided?
      rule=Rule.find(:first, :conditions => {:id => params[:rule_id].to_i, :enabled => true})
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
          rule.parameters.select{|p| p.default_value.present?}.each do |p|
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

      is_admin=true # security has already been checked by controller filters
      render :update do |page|
        page.replace_html("rule_#{rule.id}", :partial => 'rule', :object => rule, :locals => {:profile => profile, :rule => rule, :active_rule => active_rule, :is_admin => is_admin})
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
    @profile = Profile.find(params[:id].to_i)
    @rule = Rule.find(params[:rule_id])
  end

  #
  #
  # POST /rules_configuration/create/<profile id>?rule_id=<rule id>&rule[name]=<new name>&...
  #
  #
  def create
    template=Rule.find(params[:rule_id])
    rule=Rule.create(params[:rule].merge(
      {
      :priority => Sonar::RulePriority.id(params[:rule][:priority]),
      :parent_id => template.id,
      :plugin_name => template.plugin_name,
      :cardinality => 'SINGLE',
      :plugin_rule_key => "#{template.plugin_rule_key}_#{Time.now.to_i}",
      :plugin_config_key => template.plugin_config_key,
      :enabled => true}))

    template.parameters.each do |template_parameter|
      rule.rules_parameters.build(:name => template_parameter.name, :param_type => template_parameter.param_type, :description => template_parameter.description,
         :default_value => params[:rule_param][template_parameter.name])
    end

    if rule.save
      redirect_to :action => 'index', :id => params[:id], :searchtext => rule.name, :rule_status => 'INACTIVE', "plugins[]" => rule.plugin_name
      
    else
      flash[:error]=message('rules_configuration.rule_not_valid_message_x', :params => rule.errors.full_messages.join('<br/>'))
      redirect_to :action => 'new', :id => params[:id], :rule_id => params[:rule_id]
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
        redirect_to :action => 'index', :id => params[:id], :searchtext => rule.name, :rule_status => '', "plugins[]" => rule.plugin_name
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
    rule=Rule.find(params[:rule_id])
    if rule.editable?
      rule.enabled=false
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
    profile = Profile.find(params[:id].to_i)
    rule_ids = params[:bulk_rule_ids].split(',').map{|id| id.to_i}
    status=params[:rule_status]
    
    case params[:bulk_action]
    when 'activate'
      count=activate_rules(profile, rule_ids)
      flash[:notice]=message('rules_configuration.x_rules_have_been_activated', :params => count)
      status=STATUS_ACTIVE if status==STATUS_INACTIVE

    when 'deactivate'
      count=deactivate_rules(profile, rule_ids)
      flash[:notice]=message('rules_configuration.x_rules_have_been_deactivated', :params => count)
      status=STATUS_INACTIVE if status==STATUS_ACTIVE
    end

    url_parameters=request.query_parameters.merge({:action => 'index', :bulk_action => nil, :bulk_rule_ids => nil, :id => profile.id, :rule_status => status})
    redirect_to url_parameters
  end



  def update_param
    is_admin=true # security has already been checked by controller filters
    profile = Profile.find(params[:profile_id].to_i)
    rule_param = RulesParameter.find(params[:param_id].to_i)
    active_rule = ActiveRule.find(params[:active_rule_id].to_i)
    active_param = ActiveRuleParameter.find(params[:id].to_i) if params[:id].to_i > 0
    value = params[:value]
    if !profile.provided?
      if value != ""
        active_param = ActiveRuleParameter.new(:rules_parameter => rule_param, :active_rule => active_rule ) if active_param.nil?
        old_value = active_param.value
        active_param.value = value
        if active_param.save && active_param.valid?
          active_param.reload
          java_facade.ruleParamChanged(profile.id, active_rule.id, rule_param.name, old_value, value, current_user.name)
        end
      elsif !active_param.nil?
        old_value = active_param.value
        active_param.destroy
        active_param = nil
        java_facade.ruleParamChanged(profile.id, active_rule.id, rule_param.name, old_value, nil, current_user.name)
      end
    end
    # let's reload the active rule
    active_rule = ActiveRule.find(active_rule.id)
    render :partial => 'rule', :locals => {:profile => profile, :rule => active_rule.rule, :active_rule => active_rule, :is_admin => is_admin }
  end


  def update_rule_note
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
    render :partial => 'rule_note', :locals => {:rule => rule, :is_admin => true } 
  end


  def update_active_rule_note
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
    render :partial => 'active_rule_note', :locals => {:active_rule => active_rule, :is_admin => true } 
  end

  
  def delete_active_rule_note
    active_rule = ActiveRule.find(params[:active_rule_id])
    active_rule.note.destroy if active_rule.note
    active_rule.note = nil
    render :partial => 'active_rule_note', :locals => {:active_rule => active_rule, :is_admin => true }
  end
  
  

  private

  # return the number of newly activated rules
  def activate_rules(profile, rule_ids)
    count=0
    rule_ids_to_activate=(rule_ids - profile.active_rules.map{|ar| ar.rule_id})
    unless rule_ids_to_activate.empty?
      rules_to_activate=Rule.find(:all, :conditions => {:enabled=>true, :id => rule_ids_to_activate})
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
    @plugins=filter_any(params[:plugins]) || ['']
    @status=params[:rule_status] || STATUS_ACTIVE
    @inheritance=params[:inheritance] || ''
    @searchtext=params[:searchtext]
  end

  def filter_any(array)
    if array && array.size>1 && array.include?('')
      array=['']  #keep only 'any'
    end
    array
  end

end
