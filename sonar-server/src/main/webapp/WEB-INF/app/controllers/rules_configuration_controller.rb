#
# Sonar, entreprise quality control tool.
# Copyright (C) 2009 SonarSource SA
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
  verify :method => :post, :only => ['activate_rule', 'update_param', 'bulk_edit', 'create', 'update', 'delete'], :redirect_to => { :action => 'index' }

  before_filter :admin_required, :except => [ 'index', 'export' ]

  def index
    unless params[:id].blank?
      if params[:id].to_i<=0
        redirect_to :controller => 'profiles'
        return
      end
      begin
        @profile = RulesProfile.find(params[:id].to_i)
      rescue
        redirect_to :controller => 'profiles'
        return
      end
    else
      @profile = RulesProfile.default_profile
    end
    
    init_params()
    
    @select_plugins = ANY_SELECTION + java_facade.getRuleRepositoriesByLanguage(@profile.language).collect { |repo| [repo.getName(true), repo.getKey()]}.sort
    @select_categories = ANY_SELECTION + RulesCategory.all.collect {|rc| [ rc.name, rc.name ] }.sort
    @select_priority = ANY_SELECTION + RULE_PRIORITIES
    @select_status = [['Any',''], ["Active", STATUS_ACTIVE], ["Inactive", STATUS_INACTIVE]]

    @rules = Rule.search(java_facade, {
        :profile => @profile, :categories => @categories, :status => @status, :priorities => @priorities,
        :plugins =>  @plugins, :searchtext => @searchtext, :include_parameters => true, :language => @profile.language})

    unless @searchtext.blank?
      if @status==STATUS_ACTIVE
        @hidden_inactives=Rule.search(java_facade, {
            :profile => @profile, :categories => @categories, :status => STATUS_INACTIVE, :priorities => @priorities,
            :plugins =>  @plugins, :language => @profile.language, :searchtext => @searchtext, :include_parameters => false}).size

      elsif @status==STATUS_INACTIVE
        @hidden_actives=Rule.search(java_facade, {
            :profile => @profile, :categories => @categories, :status => STATUS_ACTIVE, :priorities => @priorities,
            :plugins =>  @plugins, :language => @profile.language, :searchtext => @searchtext, :include_parameters => false}).size
      end
    end

  end


  #
  #
  # POST /rules_configuration/activate_rule?id=<profile id>&rule_id=<rule id>&level=<priority>
  #
  # If the parameter "level" is blank or null, then the rule is removed from the profile.
  #
  #
  def activate_rule
    profile = RulesProfile.find(params[:id].to_i)
    if profile && !profile.provided?
      rule=Rule.find(:first, :conditions => {:id => params[:rule_id].to_i, :enabled => true})
      priority=params[:level]

      active_rule=profile.active_by_rule_id(rule.id)
      if priority.blank?
        # deactivate the rule
        active_rule.destroy if active_rule
        active_rule=nil
      else
        # activate the rule
        if active_rule.nil?
          active_rule = ActiveRule.new(:profile_id => profile.id, :rule => rule)
          rule.parameters.select{|p| p.default_value.present?}.each do |p|
            active_rule.active_rule_parameters.build(:rules_parameter => p, :value => p.default_value)
          end
        end
        active_rule.failure_level=Sonar::RulePriority.id(priority)
        active_rule.save!
      end

      is_admin=true # security has already been checked by controller filters
      render :update do |page|
        page.replace_html("rule_#{rule.id}", :partial => 'rule', :object => rule, :locals => {:profile => profile, :active_rule => active_rule, :is_admin => is_admin})
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
    @profile = RulesProfile.find(params[:id].to_i)
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
      :rules_category_id => template.rules_category_id,
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
      flash[:error]="Rule is not valid: <br/>#{rule.errors.full_messages.join('<br/>')}"
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
    @profile = RulesProfile.find(params[:id])
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
        flash[:error]="Rule is not valid: <br/>#{rule.errors.full_messages.join('<br/>')}"
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
      flash[:notice]='Rule deleted'
    else
      flash[:error]='Unknown rule'
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
      flash[:notice]="#{count} rules have been activated."
      status=STATUS_ACTIVE if status==STATUS_INACTIVE

    when 'deactivate'
      count=deactivate_rules(profile, rule_ids)
      flash[:notice]="#{count} rules have been deactivated."
      status=STATUS_INACTIVE if status==STATUS_ACTIVE
    end

    url_parameters=request.query_parameters.merge({:action => 'index', :bulk_action => nil, :bulk_rule_ids => nil, :id => profile.id, :rule_status => status})
    redirect_to url_parameters
  end



  def update_param
    is_admin=true # security has already been checked by controller filters
    profile = RulesProfile.find(params[:profile_id].to_i)
    rule_param = RulesParameter.find(params[:param_id].to_i)
    active_rule = ActiveRule.find(params[:active_rule_id].to_i)
    active_param = ActiveRuleParameter.find(params[:id].to_i) if params[:id].to_i > 0
    value = params[:value]
    if !profile.provided?
      if value != ""
        active_param = ActiveRuleParameter.new(:rules_parameter => rule_param, :active_rule => active_rule ) if active_param.nil?
        active_param.value = value
        active_param.save
        active_param.valid?
        active_param.reload
      elsif !active_param.nil?
        active_param.destroy
        active_param = nil
      end
    end
    render :partial => 'rule_param', :object => nil,
      :locals => {:parameter => rule_param, :active_parameter => active_param, :profile => profile, :active_rule => active_rule, :is_admin => is_admin }
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
        profile.active_rules.create(:rule => rule, :failure_level => rule.priority)
      end
    end
    count
  end

  def deactivate_rules(profile, rule_ids)
    count=0
    profile.active_rules.each do |ar|
      if rule_ids.include?(ar.rule_id)
        ar.destroy
        count+=1
      end
    end
    count
  end

  def select_category(categories, categ_id_string)
    return categories.first if categ_id_string.nil? or categ_id_string.blank?
    categories.each do |categ|
      if categ_id_string==categ.id.to_s
        return categ
      end
    end
    nil
  end

  def init_params
    @id = params[:id]
    @priorities = filter_any(params[:priorities]) || ['']
    @plugins=filter_any(params[:plugins]) || ['']
    @categories=filter_any(params[:categories]) || ['']
    @status=params[:rule_status] || STATUS_ACTIVE
    @searchtext=params[:searchtext]
  end

  def filter_any(array)
    if array && array.size>1 && array.include?('')
      array=['']  #keep only 'any'
    end
    array
  end

end
