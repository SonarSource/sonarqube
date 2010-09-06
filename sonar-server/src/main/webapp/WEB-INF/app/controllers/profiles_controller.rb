#
# Sonar, open source software quality management tool.
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
class ProfilesController < ApplicationController
  SECTION=Navigation::SECTION_CONFIGURATION
  
  before_filter :admin_required, :except => [ 'index', 'show', 'projects' ]

  # GET /profiles
  # GET /profiles.xml
  def index
    @profiles = Profile.find(:all, :order => 'name')
    
    @rules_count_by_language = {}
    java_facade.getLanguages().collect { |language| language.getKey() }.each do |language|
      @rules_count_by_language[language] = 0        
      java_facade.getRulesCountByCategory(language).each do |category, nb_rules|
        @rules_count_by_language[language] += nb_rules
      end
    end

    respond_to do |format|
      format.html # index.html.erb
      format.xml  { render :xml => @profiles }
    end
  end

  # GET /profiles/1
  # GET /profiles/1.xml
  def show
    @profile = Profile.find(params[:id])

    respond_to do |format|
      format.html # show.html.erb
      format.xml  { render :xml => @profile }
    end
  end

  # GET /profiles/new
  # GET /profiles/new.xml
  def new
    @profile = Profile.new
    @plugins_by_language = {}
    java_facade.getLanguages().each do |language|
      @plugins_by_language[language.getKey()] = java_facade.getPluginsWithConfigurationImportable(language)
    end

    respond_to do |format|
      format.js {
        render :update do |page|
          page.replace_html('create_profile_row', :partial => 'new')
        end
      }
      format.html # new.html.erb
      format.xml  { render :xml => @profile }
    end
  end

  # GET /profiles/1/edit
  def edit
    @profile = Profile.find(params[:id])
  end

  def create
    profile = RulesProfile.new(:name => params[:name], :language => params[:language], :default_profile => false)
    profile.save
    if profile.errors.empty?
      java_facade.getLanguages().select{|l| l.getKey()==profile.language}.each do |language|
        java_facade.getPluginsWithConfigurationImportable(language).each do |plugin|
          file = params[plugin.getKey()]
          configuration = read_configuration(file)
          if not configuration.nil?
            import_configuration(profile.id, configuration, plugin.getKey())
          end
        end
      end
      flash[:notice]= "Profile '#{profile.name}' created. Set it as default or link it to a project to use it for next measures."
    else
      flash[:error] = profile.errors.full_messages.first
    end
    redirect_to :action => 'index'

  rescue NativeException
    profile.destroy
    flash[:error] = "No profile created. Please check your configuration files."
    redirect_to :action => 'index'
  end


  # PUT /profiles/1
  # PUT /profiles/1.xml
  def update
    @profile = Profile.find(params[:id])

    respond_to do |format|
      if @profile.update_attributes(params[:profile])
        flash[:notice] = 'Profile was successfully updated.'
        format.html { redirect_to(@profile) }
        format.xml  { head :ok }
      else
        format.html { render :action => "edit" }
        format.xml  { render :xml => @profile.errors, :status => :unprocessable_entity }
      end
    end
  end

  # DELETE /profiles/1
  # DELETE /profiles/1.xml
  def destroy
    @profile = Profile.find(params[:id])
    if @profile && !@profile.provided? && !@profile.default_profile?
      java_facade.deleteProfile(@profile.id)
      flash[:notice]="Profile '#{@profile.name}' is deleted."
    end

    respond_to do |format|
      format.html { redirect_to(:controller => 'profiles', :action => 'index') }
      format.xml  { head :ok }
    end
  end

  def set_as_default
    profile = Profile.find(params[:id])
    profile.set_as_default
    flash[:notice]="Default profile is '#{profile.name}'."
    redirect_to :action => 'index'
  end

  def copy
    profile = Profile.find(params[:id])
    name = params['copy_' + profile.id.to_s]

    validation_errors = profile.validate_copy(name)
    if validation_errors.empty?
      java_facade.copyProfile(profile.id, name)
      flash[:notice]= "Profile '#{name}' is created but not activated."
    else
      flash[:error] = validation_errors.full_messages.first
    end

    redirect_to :action => 'index'
  end

  def projects
    @profile = Profile.find(params[:id])
    @available_projects=Project.find(:all, 
      :include => ['profile','snapshots'], 
      :conditions => ['projects.qualifier=? AND projects.scope=? AND snapshots.islast=?', Project::QUALIFIER_PROJECT, Project::SCOPE_SET, true],
      :order => 'projects.name asc')
    @available_projects-=@profile.projects
  end

  
  def set_projects
    @profile = Profile.find(params[:id])
    @profile.projects.clear
    
    projects=Project.find(params[:projects] || [])
    @profile.projects=projects
    flash[:notice]="Profile '#{@profile.name}' associated to #{projects.size} projects."
    redirect_to :action => 'projects', :id => @profile.id
  end

  private

  def language_names_by_key
    languages_by_key = {}
    java_facade.getLanguages().each do |language|
      languages_by_key[language.getKey()] = language.getName()
    end
    languages_by_key
  end
  
  def read_configuration(configuration_file)
    # configuration file is a StringIO
    if configuration_file.respond_to?(:read)
      return configuration_file.read
    end
    # configuration file is not a readable object
    nil
  end

  def import_configuration(profile_id, configuration, plugin_key)
    java_facade.importConfiguration(plugin_key, profile_id, configuration)
  end
end
