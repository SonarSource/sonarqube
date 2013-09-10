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
class ProfilesController < ApplicationController

  def self.root_breadcrumb
    {:name => Api::Utils.message('quality_profiles.page'), :url => {:controller => 'profiles', :action => 'index'}}
  end

  before_filter :hide_sidebar

  # GET /profiles/index
  def index
    add_breadcrumbs ProfilesController::root_breadcrumb
    @profiles = Profile.all
    Api::Utils.insensitive_sort!(@profiles){|profile| profile.name}
  end


  # GET /profiles/create_form?language=<language>
  def create_form
    access_denied unless has_role?(:profileadmin)
    require_parameters 'language'
    render :partial => 'profiles/create_form', :locals => {:language_key => params[:language]}
  end

  # POST /profiles/create?name=<profile name>&language=<language>&[backup=<file>]
  def create
    verify_post_request
    access_denied unless has_role?(:profileadmin)
    require_parameters 'language'

    profile_name=params[:name]
    language=params[:language]

    profile = Profile.create(:name => profile_name, :language => language)
    ok = profile.errors.empty?
    if ok && params[:backup]
      params[:backup].each_pair do |importer_key, file|
        if !file.blank? && ok
          profile.import_configuration(importer_key, file)
          ok &= profile.errors.empty?
        end
      end
    end

    flash_profile(profile)
    if ok
      flash[:notice]=message('quality_profiles.profile_x_created', :params => profile.name)
    elsif profile.id
      Profile.destroy(profile.id)
    end

    redirect_to :action => 'index'
  end


  # POST /profiles/delete/<id>
  def delete
    verify_post_request
    access_denied unless has_role?(:profileadmin)
    require_parameters 'id'

    @profile = Profile.find(params[:id])
    if @profile && @profile.deletable?
      @profile.destroy
    end
    redirect_to(:controller => 'profiles', :action => 'index')
  end


  # POST /profiles/set_as_default/<id>
  def set_as_default
    verify_post_request
    access_denied unless has_role?(:profileadmin)
    require_parameters 'id'

    profile = Profile.find(params[:id])
    profile.set_as_default
    redirect_to :action => 'index'
  end


  # GET /profiles/copy_form/<profile id>
  def copy_form
    access_denied unless has_role?(:profileadmin)
    require_parameters 'id'
    @profile = Profile.find(params[:id])
    render :partial => 'profiles/copy_form'
  end

  # POST /profiles/copy/<id>?name=<name of new profile>
  def copy
    verify_post_request
    verify_ajax_request
    access_denied unless has_role?(:profileadmin)
    require_parameters 'id'

    @profile = Profile.find(params[:id])
    name = params['name']

    target_profile=Profile.new(:name => name, :language => @profile.language)
    if target_profile.valid?
      java_facade.copyProfile(@profile.id, name)
      flash[:notice]= message('quality_profiles.profile_x_not_activated', :params => name)
      render :text => 'ok', :status => 200
    else
      @errors = []
      target_profile.errors.each{|attr,msg| @errors<<msg}
      render :partial => 'profiles/copy_form', :status => 400
    end
  end

  # the backup action is allow to non-admin users : see http://jira.codehaus.org/browse/SONAR-2039
  # POST /profiles/backup?id=<profile id>
  def backup
    verify_post_request
    require_parameters 'id'

    profile = Profile.find(params[:id])
    xml = java_facade.backupProfile(profile.id)
    filename=profile.name.gsub(' ', '_')
    send_data(xml, :type => 'text/xml', :disposition => "attachment; filename=#{filename}_#{profile.language}.xml")
  end


  # Modal window to restore profile backup
  # GET /profiles/restore_form/<profile id>
  def restore_form
    verify_ajax_request
    render :partial => 'profiles/restore_form'
  end

  # POST /profiles/restore?backup=<file>
  def restore
    verify_post_request
    access_denied unless has_role?(:profileadmin)
    if params[:backup].blank?
      flash[:warning]=message('quality_profiles.please_upload_backup_file')
    else
      messages=java_facade.restoreProfile(Api::Utils.read_post_request_param(params[:backup]), false)
      flash_messages(messages)
    end
    redirect_to :action => 'index'
  end


  # GET /profiles/export?name=<profile name>&language=<language>&format<exporter key>
  def export
    language = params[:language]
    if (params[:name].blank?)
      profile = Profile.by_default(language)
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

  # GET /profiles/inheritance?id=<profile id>
  def inheritance
    require_parameters 'id'
    @profile = Profile.find(params[:id])

    profiles=Profile.all(:conditions => ['language=? and id<>? and (parent_name is null or parent_name<>?)', @profile.language, @profile.id, @profile.name], :order => 'name')
    @select_parent = [[message('none'), nil]] + profiles.collect { |profile| [profile.name, profile.name] }

    set_profile_breadcrumbs
  end

  # GET /profiles/changelog?id=<profile id>
  def changelog
    require_parameters 'id'
    @profile = Profile.find(params[:id])

    versions = ActiveRuleChange.all(:select => 'profile_version, MAX(change_date) AS change_date', :conditions => ['profile_id=?', @profile.id], :group => 'profile_version')
    # Add false change version 1 when no change have been made in profile version 1
    versions << ActiveRuleChange.new(:profile_version => 1, :profile_id => @profile.id) unless versions.find {|version| version.profile_version == 1}
    versions.sort! { |a, b| b.profile_version <=> a.profile_version }

    # SONAR-2986
    # Display changelog only from profile version 2
    if @profile.version > 1
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
      @changes = ActiveRuleChange.all(:conditions => ['profile_id=? and ?<profile_version and profile_version<=?', @profile.id, @since_version, @to_version], :order => 'id desc')

      @select_versions = versions.map do |u|
        if u.change_date
          message = message(u.profile_version == last_version ? 'quality_profiles.last_version_x_with_date' : 'quality_profiles.version_x_with_date',
                            :params => [u.profile_version.to_s, l(u.change_date)])
        else
          # Specific case when no change have been made in profile version 1 -> no date will be displayed
          message = message('quality_profiles.version_x', :params => u.profile_version)
        end
        [message, u.profile_version]
      end
    end

    set_profile_breadcrumbs
  end


  # POST /profiles/change_parent?id=<profile id>&parent_name=<parent profile name>
  def change_parent
    verify_post_request
    access_denied unless has_role?(:profileadmin)
    require_parameters 'id'

    id = params[:id].to_i
    parent_name = params[:parent_name]
    if parent_name.blank?
      messages = java_facade.changeParentProfile(id, nil, current_user.name)
    else
      messages = java_facade.changeParentProfile(id, parent_name, current_user.name)
    end
    flash_messages(messages)
    redirect_to :action => 'inheritance', :id => id
  end


  #
  #
  # GET /profiles/permalinks?id=<profile id>
  #
  #
  def permalinks
    require_parameters 'id'
    @profile = Profile.find(params[:id])
    set_profile_breadcrumbs
  end


  #
  #
  # GET /profiles/projects/<id>
  #
  #
  def projects
    require_parameters 'id'
    @profile = Profile.find(params[:id])
    set_profile_breadcrumbs
  end


  # POST /profiles/add_project?id=<profile id>&project=<project id or key>
  def add_project
    verify_post_request
    access_denied unless has_role?(:profileadmin)
    require_parameters 'id', 'project'

    profile=Profile.find(params[:id])
    bad_request('Unknown profile') unless profile
    project=Project.by_key(params[:project])
    bad_request('Unknown project') unless project

    profile.add_project_id(project.id)
    redirect_to :action => 'projects', :id => profile.id
  end

  # POST /profiles/remove_project?id=<profile id>&project=<project id or key>
  def remove_project
    verify_post_request
    access_denied unless has_role?(:profileadmin)
    require_parameters 'id', 'project'

    profile=Profile.find(params[:id])
    bad_request('Unknown profile') unless profile
    project=Project.by_key(params[:project])
    bad_request('Unknown project') unless project

    Profile.reset_default_profile_for_project_id(profile.language, project.id)
    redirect_to :action => 'projects', :id => profile.id
  end

  # POST /profiles/remove_projects?id=<profile id>
  def remove_projects
    verify_post_request
    access_denied unless has_role?(:profileadmin)
    require_parameters 'id'

    profile=Profile.find(params[:id])
    bad_request('Unknown profile') unless profile

    profile.remove_projects
    redirect_to :action => 'projects', :id => profile.id
  end

  # GET /profiles/rename_form?id=<id>
  def rename_form
    access_denied unless has_role?(:profileadmin)
    require_parameters 'id'
    @profile = Profile.find(params[:id])
    render :partial => 'profiles/rename_form'
  end

  # POST /profiles/rename?id=<id>&name=<new name>
  def rename
    verify_post_request
    verify_ajax_request
    access_denied unless has_role?(:profileadmin)
    require_parameters 'id'

    @profile = Profile.find(params[:id])

    if @profile.rename(params[:name]).errors.empty?
      render :text => 'ok', :status => 200
    else
      render :partial => 'profiles/rename_form', :status => 400
    end
  end

  # GET /profiles/compare?id1=<profile1 id>&id2=<profile2 id>
  def compare
    @profiles = Profile.all(:order => 'language asc, name')
    if params[:id1].present? && params[:id2].present?
      @profile1 = Profile.find(params[:id1])
      @profile2 = Profile.find(params[:id2])

      arules1 = ActiveRule.all(:include => [{:active_rule_parameters => :rules_parameter}, :rule],
                                :conditions => ['active_rules.profile_id=?', @profile1.id])
      arules2 = ActiveRule.all(:order => 'rules.plugin_name, rules.plugin_rule_key', :include => [{:active_rule_parameters => :rules_parameter}, :rule],
                                :conditions => ['active_rules.profile_id=?', @profile2.id])

      arules1.reject! { |arule| arule.rule.removed? }
      arules2.reject! { |arule| arule.rule.removed? }

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
          when DIFF_IN1 then
            @in1<<diff
          when DIFF_IN2 then
            @in2<<diff
          when DIFF_MODIFIED then
            @modified<<diff
          when DIFF_SAME then
            @sames<<diff
        end
      end
    end
    add_breadcrumbs ProfilesController::root_breadcrumb, Api::Utils.message('compare')
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
    return {:same => same, :added => added, :removed => removed, :modified => modified, :rules => rules}
  end

  def flash_profile(profile)
    # only 4 messages are kept each time to avoid cookie overflow.
    if !profile.errors.empty?
      flash[:error]=profile.errors.full_messages.to_a[0...4].join('<br/>')
    end
    if profile.warnings?
      flash[:warning]=profile.warnings.full_messages.to_a[0...4].join('<br/>')
    end
  end

  def flash_messages(messages)
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

  def set_profile_breadcrumbs
    add_breadcrumbs ProfilesController::root_breadcrumb, Api::Utils.language_name(@profile.language), {:name => @profile.name, :url => {:controller => 'rules_configuration', :action => 'index', :id => @profile.id}}
  end
end
