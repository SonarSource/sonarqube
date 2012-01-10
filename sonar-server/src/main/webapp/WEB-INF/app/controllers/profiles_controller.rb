#
# Sonar, open source software quality management tool.
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
class ProfilesController < ApplicationController
  SECTION=Navigation::SECTION_CONFIGURATION

  # GETs should be safe (see http://www.w3.org/2001/tag/doc/whenToUseGet.html)
  verify :method => :post, :only => ['create', 'delete', 'copy', 'set_as_default', 'restore', 'set_projects', 'rename', 'change_parent'], :redirect_to => { :action => 'index' }

  # the backup action is allow to non-admin users : see http://jira.codehaus.org/browse/SONAR-2039
  before_filter :admin_required, :only => ['create', 'delete', 'set_as_default', 'copy', 'restore', 'change_parent', 'set_projects', 'rename']

  #
  #
  # GET /profiles/index
  #
  #
  def index
    @profiles = Profile.find(:all, :conditions => ['enabled=?', true], :order => 'name')
  end


  #
  #
  # GET /profiles/show/<id>
  #
  #
  def show
    @profile = Profile.find(params[:id])
  end



  #
  #
  # POST /profiles/create?name=<profile name>&language=<language>
  #
  #
  def create
    profile_name=params[:name]
    language=params[:language]
    if profile_name.blank?|| language.blank?
      flash[:warning]=message('quality_profiles.please_type_profile_name')
    else
      profile=Profile.find_by_name_and_language(profile_name, language)
      if profile
        flash[:error]=message('quality_profiles.profile_x_already_exists', :params => profile_name)

      else
        profile = Profile.create(:name => profile_name, :language => language, :default_profile => false, :enabled => true)
        ok=profile.errors.empty?
        if ok && params[:backup]
          params[:backup].each_pair do |importer_key, file|
            if !file.blank? && ok
              messages = java_facade.importProfile(profile_name, language, importer_key, read_file_param(file))
              flash_validation_messages(messages)
              ok &= !messages.hasErrors()
            end
          end
        end
        if ok
          flash[:notice]=message('quality_profiles.profile_x_created', :params => profile.name)
        else
          profile.reload
          profile.destroy
        end
      end
    end
    redirect_to :action => 'index'
  end


  #
  #
  # POST /profiles/delete/<id>
  #
  #
  def delete
    @profile = Profile.find(params[:id])
    if @profile && @profile.deletable?
      java_facade.deleteProfile(@profile.id)
      flash[:notice]=message('quality_profiles.profile_x_deleted', :params => @profile.name)
    end
    redirect_to(:controller => 'profiles', :action => 'index')
  end


  #
  #
  # POST /profiles/set_as_default/<id>
  #
  #
  def set_as_default
    profile = Profile.find(params[:id])
    profile.set_as_default
    flash[:notice]=message('quality_profiles.default_profile_is_x', :params => profile.name)
    redirect_to :action => 'index'
  end



  #
  #
  # POST /profiles/copy/<id>?name=<name of new profile>
  #
  #
  def copy
    profile = Profile.find(params[:id])
    name = params['copy_' + profile.id.to_s]

    validation_errors = profile.validate_copy(name)
    if validation_errors.empty?
      java_facade.copyProfile(profile.id, name)
      flash[:notice]= message('quality_profiles.profile_x_not_activated', :params => name)
    else
      flash[:error] = validation_errors.full_messages.first
    end

    redirect_to :action => 'index'
  end



  #
  #
  # POST /profiles/backup/<id>
  #
  #
  def backup
    profile = Profile.find(params[:id])
    xml = java_facade.backupProfile(profile.id)
    filename=profile.name.gsub(' ', '_')
    send_data(xml, :type => 'text/xml', :disposition => "attachment; filename=#{filename}_#{profile.language}.xml")
  end



  #
  #
  # POST /profiles/restore/<id>
  #
  #
  def restore
    if params[:backup].blank?
      flash[:warning]=message('quality_profiles.please_upload_backup_file')
    else
      messages=java_facade.restoreProfile(read_file_param(params[:backup]))
      flash_validation_messages(messages)
    end
    redirect_to :action => 'index'
  end



  #
  #
  # GET /profiles/export?name=<profile name>&language=<language>&format<exporter key>
  #
  #
  def export
    language = params[:language]
    if (params[:name].blank?)
      profile = Profile.find_active_profile_by_language(language)
    else
      profile = Profile.find_by_name_and_language(CGI::unescape(params[:name]), language)
    end
    not_found('Profile not found') unless profile
    
    if (params[:format].blank?)
      # standard sonar format
      result = java_facade.backupProfile(profile.id)
      send_data(result, :type => 'text/xml', :disposition => 'inline')
    else
      exporter_key = params[:format]
      result = java_facade.exportProfile(profile.id, exporter_key)
      send_data(result, :type => java_facade.getProfileExporterMimeType(exporter_key), :disposition => 'inline')
    end
  end

  #
  #
  # GET /profiles/inheritance?id=<profile id>
  #
  #
  def inheritance
    @profile = Profile.find(params[:id])
    
    profiles=Profile.find(:all, :conditions => ['language=? and id<>? and (parent_name is null or parent_name<>?) and enabled=?', @profile.language, @profile.id, @profile.name, true], :order => 'name')
    @select_parent = [[message('none'), nil]] + profiles.collect{ |profile| [profile.name, profile.name] }
  end

  #
  #
  # GET /profiles/changelog?id=<profile id>
  #
  #
  def changelog
    @profile = Profile.find(params[:id])

    versions = ActiveRuleChange.find(:all, :select => 'profile_version, MAX(change_date) AS change_date', :conditions => ['profile_id=?', @profile.id], :group => 'profile_version')
    versions.sort! { |a,b| b.profile_version <=> a.profile_version }

    if !versions.empty?
      last_version = versions[0].profile_version
      if params[:since].blank?
        @since_version = last_version - 1
      else
        @since_version = params[:since].to_i
      end
      if params[:to].blank?
        @to_version = last_version
      else
        @to_version = params[:to].to_i
      end
      if @since_version > @to_version
        @since_version, @to_version = @to_version, @since_version
      end
      @changes = ActiveRuleChange.find(:all, :conditions => ['profile_id=? and ?<profile_version and profile_version<=?', @profile.id, @since_version, @to_version], :order => 'id desc')

      @select_versions = versions.map {|u| [ message(u.profile_version == last_version ? 'quality_profiles.last_version_x_with_date' : 'quality_profiles.version_x_with_date', :params => [u.profile_version.to_s, l(u.change_date)]), u.profile_version ]} | [[message('quality_profiles.no_version'), 0]];
    end
  end


  #
  #
  # POST /profiles/change_parent?id=<profile id>&parent_name=<parent profile name>
  #
  #
  def change_parent
    id = params[:id].to_i
    parent_name = params[:parent_name]
    if parent_name.blank?
      messages = java_facade.changeParentProfile(id, nil, current_user.name)
    else
      messages = java_facade.changeParentProfile(id, parent_name, current_user.name)
    end
    flash_validation_messages(messages)
    redirect_to :action => 'inheritance', :id => id
  end


  #
  #
  # GET /profiles/permalinks?id=<profile id>
  #
  #
  def permalinks
    @profile = Profile.find(params[:id])
  end


  #
  #
  # GET /profiles/projects/<id>
  #
  #
  def projects
    @profile = Profile.find(params[:id])
    @available_projects=Project.find(:all, 
      :include => ['profile','snapshots'], 
      :conditions => ['projects.qualifier=? AND projects.scope=? AND snapshots.islast=?', Project::QUALIFIER_PROJECT, Project::SCOPE_SET, true],
      :order => 'projects.name asc')
    @available_projects-=@profile.projects
  end



  #
  #
  # POST /profiles/set_projects/<id>?projects=<project ids>
  #
  #
  def set_projects
    @profile = Profile.find(params[:id])
    @profile.projects.clear

    projects=Project.find(params[:projects] || [])
    @profile.projects=projects
    flash[:notice]=message('quality_profiles.profile_x_associated_to_x_projects', :params => [@profile.name, projects.size])
    redirect_to :action => 'projects', :id => @profile.id
  end



  #
  #
  # POST /profiles/rename/<id>?name=<new name>
  #
  #
  def rename
    profile = Profile.find(params[:id])
    name = params['rename_' + profile.id.to_s]

    if name.blank?
      flash[:warning]=message('quality_profiles.profile_name_cant_be_blank')
    else
      existing=Profile.find(:first, :conditions => {:name => name, :language => profile.language, :enabled => true})
      if existing
        flash[:warning]=message('quality_profiles.profile_name_already_exists')
      elsif !profile.provided?
        java_facade.renameProfile(profile.id, name)
      end
    end
    redirect_to :action => 'index'
  end


  #
  #
  # GET /profiles/compare?id1=<profile1 id>&id2=<profile2 id>
  #
  #
  def compare
    @profiles = Profile.find(:all, :conditions => ['enabled=?', true], :order => 'language asc, name')
    if params[:id1].present? && params[:id2].present?
      @profile1 = Profile.find(params[:id1])
      @profile2 = Profile.find(params[:id2])
      
      arules1 = ActiveRule.find(:all, :include => [{:active_rule_parameters => :rules_parameter}, :rule],
        :conditions => ['active_rules.profile_id=?', @profile1.id])
      arules2 = ActiveRule.find(:all, :order => 'rules.plugin_name, rules.plugin_rule_key', :include => [{:active_rule_parameters => :rules_parameter}, :rule],
        :conditions => ['active_rules.profile_id=?', @profile2.id])

      diffs_by_rule={}
      arules1.each do |arule1|
        diffs_by_rule[arule1.rule]||=RuleDiff.new(arule1.rule)
        diffs_by_rule[arule1.rule].arule1=arule1
      end
      arules2.each do |arule2|
        diffs_by_rule[arule2.rule]||=RuleDiff.new(arule2.rule)
        diffs_by_rule[arule2.rule].arule2=arule2
      end
      @in1=[]
      @in2=[]
      @modified=[]
      @sames=[]
      diffs_by_rule.values.sort.each do |diff|
        case diff.status
        when DIFF_IN1: @in1<<diff
        when DIFF_IN2: @in2<<diff
        when DIFF_MODIFIED: @modified<<diff
        when DIFF_SAME: @sames<<diff
        end
      end
    end
  end

  DIFF_IN1=1
  DIFF_IN2=2
  DIFF_MODIFIED=3
  DIFF_SAME=4
  
  private

  class RuleDiff
    attr_reader :rule, :removed_params, :added_params
    attr_accessor :arule1, :arule2

    def initialize(rule)
      @rule=rule
    end

    def status
      @status ||=
          begin
            if @arule1.nil?
              @status=(@arule2 ? DIFF_IN2 : nil)
            else
              if @arule2
                # compare severity and parameters
                @removed_params=[]
                @added_params=[]
                @rule.parameters.each do |param|
                  v1=@arule1.value(param.id)
                  v2=@arule2.value(param.id)
                  if v1
                    if v2
                      if v1!=v2
                        @removed_params<<@arule1.parameter(param.name)
                        @added_params<<@arule2.parameter(param.name)
                      end
                    else
                      @removed_params<<@arule1.parameter(param.name)
                    end          
                  elsif v2
                    @added_params<<@arule2.parameter(param.name)
                  end
                end
                diff=(@arule1.priority!=@arule2.priority) || !@removed_params.empty? || !@added_params.empty?
                @status=(diff ? DIFF_MODIFIED : DIFF_SAME)
              else
                @status=DIFF_IN1
              end
            end
          end
    end

    def <=>(other)
      rule.name()<=>other.rule.name
    end
  end
  
  #
  # Remove active rules that are identical in both collections (same severity and same parameters)
  # and return a map with results {:added => X, :removed => Y, :modified => Z, 
  # :rules => {rule1 => [activeruleleft1, activeruleright1], rule2 => [activeruleleft2, nil], ...]}
  # Assume both collections are ordered by rule key
  #
  def compute_diff(arules1, arules2)
    rules = {}
    removed = 0
    added = 0
    modified = 0
    same = 0
    begin
      diff = false
      #take first item of each collection
      active_rule1 = arules1.first
      active_rule2 = arules2.first
      if active_rule1 != nil and active_rule2 != nil
        order = active_rule1.rule.key <=> active_rule2.rule.key
        if order < 0
          active_rule2 = nil
          rule = active_rule1.rule
          diff = true
          removed = removed +1
        elsif order > 0
          active_rule1 = nil
          rule = active_rule2.rule
          diff = true
          added = added +1
        else
          rule = active_rule1.rule # = active_rule2.rule
          #compare severity
          diff = true if active_rule1.priority != active_rule2.priority
          #compare parameters
          rule.parameters.each do |param|
            diff = true if active_rule1.value(param.id) != active_rule2.value(param.id)
          end
          if diff
            modified = modified + 1
          else
            same = same +1
          end
        end
      elsif active_rule1 != nil
        #no more rule in right collection
        diff = true
        removed = removed +1
        rule = active_rule1.rule
      elsif active_rule2 != nil
        #no more rule in left collection
        diff = true
        added = added +1
        rule = active_rule2.rule
      end
      # remove processed rule(s)
      arules1 = arules1.drop(1) if active_rule1 != nil
      arules2 = arules2.drop(1) if active_rule2 != nil
      if diff
        rules[rule] = [active_rule1, active_rule2]
      end
    end while !arules1.empty? || !arules2.empty?
    return {:same => same, :added => added, :removed => removed, :modified => modified,  :rules => rules}
  end

  def read_file_param(configuration_file)
    # configuration file is a StringIO
    if configuration_file.respond_to?(:read)
      return configuration_file.read
    end
    # configuration file is not a readable object
    nil
  end

  def flash_validation_messages(messages)
    # only 4 messages are kept each time to avoid cookie overflow.
    if messages.hasErrors()
      flash[:error]=messages.getErrors().to_a[0...4].join('<br/>')
    end
    if messages.hasWarnings()
      flash[:warning]=messages.getWarnings().to_a[0...4].join('<br/>')
    end
    if messages.hasInfos()
      flash[:notice]=messages.getInfos().to_a[0...4].join('<br/>')
    end
  end
end
