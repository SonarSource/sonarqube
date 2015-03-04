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
class ProfilesController < ApplicationController

  SECTION=Navigation::SECTION_QUALITY_PROFILES

  def self.root_breadcrumb
    {:name => Api::Utils.message('quality_profiles.page'), :url => {:controller => 'profiles', :action => 'index'}}
  end

  # GET /profiles/index
  def index
    add_breadcrumbs ProfilesController::root_breadcrumb
    call_backend do
      @profiles = Internal.quality_profiles.allProfiles().to_a
      @active_rule_counts = Internal.qprofile_loader.countAllActiveRules()
    end
    Api::Utils.insensitive_sort!(@profiles) { |profile| profile.name() }
  end

  # GET /profiles/show?key=<key>
  def show
    require_parameters 'key'
    call_backend do
      @profile = Internal.qprofile_loader.getByKey(params[:key])
      if @profile
        @deprecated_active_rules = Internal.qprofile_loader.countDeprecatedActiveRulesByProfile(@profile.getKey())
        @stats = Internal.qprofile_loader.getStatsByProfile(@profile.getKey())
        set_profile_breadcrumbs
      else
        # SONAR-5630
        flash[:error] = message('quality_profiles.deleted_profile', :params => params[:key])
        redirect_to :controller => 'profiles', :action => 'index'
      end
    end
  end

  # GET /profiles/create_form?language=<language>
  def create_form
    require_parameters 'language'
    render :partial => 'profiles/create_form', :locals => {:language_key => params[:language]}
  end

  # POST /profiles/create?name=<profile name>&language=<language>&[backup=<file>]
  def create
    verify_post_request
    call_backend do
      files_by_key = {}
      if params[:backup]
        params[:backup].each_pair do |importer_key, file|
          unless file.blank?
            files_by_key[importer_key] = Api::Utils.read_post_request_param(file)
          end
        end
      end
      profile_name = Java::OrgSonarServerQualityprofile::QProfileName.new(params[:language], params[:name])
      result = Internal.qprofile_service.create(profile_name, files_by_key)
      flash[:notice] = message('quality_profiles.profile_x_created', :params => result.profile().getName())
      flash_result(result)
    end
    redirect_to :action => 'index'
  end

  # Modal window to restore built-in profiles
  # GET /profiles/restore_built_in_form/<profile id>
  def restore_built_in_form
    verify_ajax_request
    require_parameters 'language'
    @language = java_facade.getLanguages().find { |l| l.getKey()==params[:language].to_s }
    call_backend do
      @builtin_profile_names = Internal.qprofile_service.builtInProfileNamesForLanguage(params[:language].to_s)
    end
    render :partial => 'profiles/restore_built_in_form'
  end

  # POST /profiles/restore_built_in?language=<language>
  def restore_built_in
    verify_post_request
    require_parameters 'language'
    call_backend do
      Internal.qprofile_service.restoreBuiltInProfilesForLanguage(params[:language].to_s)
    end
    redirect_to :action => 'index'
  end

  # POST /profiles/delete/<id>
  def delete
    verify_post_request
    require_parameters 'id'

    profile_key = profile_id_to_key(params[:id].to_i)
    call_backend do
      Internal.qprofile_service.delete(profile_key)
    end

    redirect_to(:controller => 'profiles', :action => 'index')
  end


  # POST /profiles/set_as_default/<id>
  def set_as_default
    verify_post_request
    require_parameters 'id'

    profile_key = profile_id_to_key(params[:id].to_i)
    call_backend do
      Internal.qprofile_service.setDefault(profile_key)
    end
    redirect_to :action => 'index'
  end


  # GET /profiles/copy_form/<profile id>
  def copy_form
    require_parameters 'id'

    profile_id = params[:id].to_i
    call_backend do
      @profile = Internal.quality_profiles.profile(profile_id)
    end
    not_found('Profile not found') unless @profile

    render :partial => 'profiles/copy_form'
  end

  # POST /profiles/copy/<id>?name=<name of new profile>[&overwrite=<name of overwritten profile>]
  def copy
    verify_post_request
    verify_ajax_request
    require_parameters 'id'

    source_id = params[:id].to_i
    source_profile = Internal.quality_profiles.profile(source_id)

    source_key=profile_id_to_key(source_id)
    target_name = params['name']

    overwrite = (params['overwrite'] == target_name)
    target_profile = nil

    unless overwrite
      target_profile = Internal.quality_profiles.profile(target_name, source_profile.language())
    end

    if target_profile.nil? || overwrite
      call_backend do
        Internal.qprofile_service.copyToName(source_key, target_name)
        if overwrite
          flash[:notice] = message('quality_profiles.copy_x_overwritten', :params => target_name)
        else
          flash[:notice] = message('quality_profiles.profile_x_not_activated', :params => target_name)
        end
        render :text => 'ok', :status => 200
      end
    else
      render :text => message('quality_profiles.copy_overwrite_x', :params => target_name), :status => 409
    end
  end

  # the backup action is allow to non-admin users : see http://jira.codehaus.org/browse/SONAR-2039
  def backup
    verify_post_request
    require_parameters 'key'

    profile_key=params[:key]
    call_backend do
      xml = Internal.qprofile_service.backup(profile_key)
      send_data(xml, :type => 'text/xml', :disposition => "attachment; filename=#{profile_key}.xml")
    end
  end


  # Modal window to restore profile backup
  # GET /profiles/restore_form/<profile id>
  def restore_form
    verify_ajax_request
    render :partial => 'profiles/restore_form'
  end

  # POST /profiles/restore?backup=<file>
  def restore
    if params[:backup].blank?
      flash[:warning] = message('quality_profiles.please_upload_backup_file')
    else
      call_backend do
        xml=Api::Utils.read_post_request_param(params[:backup])
        Internal.qprofile_service.restore(xml)
      end
    end
    redirect_to :action => 'index'
  end


  # GET /profiles/export?name=<profile name>&language=<language>&format<exporter key>
  def export
    language = params[:language]
    if params[:name].blank?
      profile = Internal.qprofile_service.getDefault(language)
    else
      profile = Internal.qprofile_loader.getByLangAndName(language, CGI::unescape(params[:name]))
    end
    not_found('Profile not found') unless profile

    if params[:format].blank?
      # standard sonar format
      result = Internal.qprofile_service.backup(profile.getKee())
      send_data(result, :type => 'text/xml', :disposition => 'inline')
    else
      exporter_key = params[:format]
      result = Internal.qprofile_exporters.export(profile.getKee(), exporter_key)
      send_data(result, :type => Internal.qprofile_exporters.mimeType(exporter_key), :disposition => 'inline')
    end
  end

  # GET /profiles/inheritance?id=<profile id>
  def inheritance
    require_parameters 'id'

    call_backend do
      @profile = Internal.quality_profiles.profile(params[:id].to_i)
      not_found('Profile not found') unless @profile
      @parent = Internal.quality_profiles.parent(@profile) if @profile.parent
      @ancestors = Internal.quality_profiles.ancestors(@profile).to_a
      @children = Internal.quality_profiles.children(@profile).to_a
      profiles = Internal.quality_profiles.profilesByLanguage(@profile.language()).to_a.reject { |p| p.id == @profile.id() || p.parent() == @profile.name() }
      profiles = Api::Utils.insensitive_sort(profiles) { |p| p.name() }
      @select_parent = [[message('none'), nil]] + profiles.collect { |profile| [profile.name(), profile.id()] }

      @all_profile_stats = Internal.qprofile_loader.getAllProfileStats()
    end

    set_profile_breadcrumbs
  end

  # POST /profiles/change_parent?id=<profile id>&parent_id=<parent id>
  def change_parent
    verify_post_request
    access_denied unless has_role?(:profileadmin)
    require_parameters 'id'

    profile_key = profile_id_to_key(params[:id].to_i)
    parent_key = profile_id_to_key(params[:parent_id].to_i) unless params[:parent_id].empty?
    call_backend do
      Internal.qprofile_service.setParent(profile_key, parent_key)
    end
    redirect_to :action => 'inheritance', :id => params[:id]
  end

  # GET /profiles/changelog?key=<profile key>
  def changelog
    require_parameters 'key'

    @profile = Internal.qprofile_loader.getByKey(params[:key])
    not_found('Quality profile does not exist') unless @profile
    search = {'profileKey' => @profile.key().to_s, 'since' => params[:since], 'to' => params[:to], 'p' => params[:p]}
    result = Internal.component(Java::OrgSonarServerActivity::RubyQProfileActivityService.java_class).search(search)
    @changes = result.activities
    @paging = result.paging

    set_profile_breadcrumbs
  end

  #
  #
  # GET /profiles/permalinks?id=<profile id>
  #
  #
  def permalinks
    require_parameters 'id'
    @profile = Internal.quality_profiles.profile(params[:id].to_i)
    not_found('Profile not found') unless @profile
    set_profile_breadcrumbs
  end


  #
  #
  # GET /profiles/projects/<id>
  #
  #
  def projects
    require_parameters 'id'

    call_backend do
      @profile = Internal.quality_profiles.profile(params[:id].to_i)
      not_found('Profile not found') unless @profile
      projects = Internal.quality_profiles.projects(params[:id].to_i)
      @projects = Api::Utils.insensitive_sort(projects.to_a) { |p| p.name }
      set_profile_breadcrumbs
    end
  end


  # POST /profiles/add_project?id=<profile id>&project=<project id>
  def add_project
    verify_post_request
    require_parameters 'id', 'project'

    project_id = Api::Utils.project_id(params[:project])
    profile_id = params[:id].to_i

    call_backend do
      Internal.quality_profiles.addProject(profile_id, project_id.to_i)
    end
    redirect_to :action => 'projects', :id => profile_id
  end

  # POST /profiles/remove_project?id=<profile id>&project=<project id>
  def remove_project
    verify_post_request
    require_parameters 'id', 'project'

    profile_id = params[:id].to_i
    call_backend do
      Internal.quality_profiles.removeProject(profile_id, params[:project].to_i)
    end
    redirect_to :action => 'projects', :id => profile_id
  end

  # POST /profiles/remove_projects?id=<profile id>
  def remove_projects
    verify_post_request
    require_parameters 'id'

    profile_id = params[:id].to_i
    call_backend do
      Internal.quality_profiles.removeAllProjects(profile_id)
    end
    redirect_to :action => 'projects', :id => profile_id
  end

  # GET /profiles/rename_form?id=<id>
  def rename_form
    require_parameters 'id'
    call_backend do
      @profile = Internal.quality_profiles.profile(params[:id].to_i)
      not_found('Profile not found') unless @profile
    end
    render :partial => 'profiles/rename_form'
  end

  # POST /profiles/rename?id=<id>&name=<new name>
  def rename
    verify_post_request
    verify_ajax_request
    require_parameters 'id'

    call_backend do
      profile_key = profile_id_to_key(params[:id].to_i)
      Internal.qprofile_service.rename(profile_key, params[:new_name])
    end
    render :text => 'ok', :status => 200
  end

  # GET /profiles/compare?id1=<profile1 id>&id2=<profile2 id>
  def compare
    @profiles = Profile.all(:order => 'language asc, name')
    id1 = params[:id1]
    id2 = params[:id2]
    if id1.present? && id2.present? && id1.respond_to?(:to_i) && id2.respond_to?(:to_i)
      @id1 = params[:id1].to_i
      @id2 = params[:id2].to_i
      @profile1 = Profile.find(id1)
      @profile2 = Profile.find(id2)

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

  def flash_result(result)
    # only 4 messages are kept each time to avoid cookie overflow.
    unless result.infos.empty?
      flash[:notice] += '<br/>' + result.infos.to_a[0...4].join('<br/>')
    end
    unless result.warnings.empty?
      flash[:warning] = result.warnings.to_a[0...4].join('<br/>')
    end

  end

  def set_profile_breadcrumbs
    add_breadcrumbs ProfilesController::root_breadcrumb, {:name => "#{@profile.name} (#{Api::Utils.language_name(@profile.language)})"}
  end

  def profile_id_to_key(profile_id)
    profile = Profile.find(profile_id)
    not_found('Profile not found') unless profile
    profile.kee
  end
end
