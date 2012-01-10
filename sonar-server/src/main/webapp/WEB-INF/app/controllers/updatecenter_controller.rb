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
class UpdatecenterController < ApplicationController

  SECTION=Navigation::SECTION_CONFIGURATION
  before_filter :admin_required
  before_filter :updatecenter_activated

  verify :method => :post, :only => [:cancel, :install], :redirect_to => {:action => :index}

  def index
    @uninstalls=java_facade.getPluginUninstalls()
    @downloads=java_facade.getPluginDownloads()

    @user_plugins=user_plugins()
    @core_plugins=core_plugins()
  end

  def updates
    @uninstalls=java_facade.getPluginUninstalls()
    @downloads=java_facade.getPluginDownloads()

    @center=nil
    @matrix=nil
    @updates_by_plugin={}
    @user_plugins={}
    @last_compatible={}

    user_plugins.each do |plugin|
      @user_plugins[plugin.getKey()]=plugin.getVersion()
    end

    load_matrix()
    if @matrix
      @center=@matrix.getCenter()

      @matrix.findPluginUpdates().each do |update|
        plugin=update.getPlugin()
        @updates_by_plugin[plugin]||=[]
        @updates_by_plugin[plugin]<<update
        if update.isCompatible
          @last_compatible[plugin.getKey()]=update.getRelease().getVersion()
        end
      end
    end
  end

  def available
    @uninstalls=java_facade.getPluginUninstalls()
    @downloads=java_facade.getPluginDownloads()

    @center=nil
    @updates_by_category={}

    load_matrix()
    if @matrix
      @center=@matrix.getCenter()
      @matrix.findAvailablePlugins().each do |update|
        category=update.getPlugin().getCategory()||''
        @updates_by_category[category]||=[]
        @updates_by_category[category]<<update
      end
    end
  end

  def cancel_downloads
    java_facade.cancelPluginDownloads()
    flash[:notice]="Pending plugin installations are canceled."
    redirect_to :action => 'index'
  end

  def install
    key=params[:key]
    version=params[:version]
    if key && version
      begin
        java_facade.downloadPlugin(key, version)
      rescue Exception => e
        flash[:error]=e.message
      end
    end
    redirect_to :action => (params[:from] || 'index')
  end

  def uninstall
    key=params[:key]
    if key
      begin
        java_facade.uninstallPlugin(key)
      rescue Exception => e
        flash[:error]=e.message
      end
    end
    redirect_to :action => (params[:from] || 'index')
  end

  def cancel_uninstalls
    java_facade.cancelPluginUninstalls()
    flash[:notice]="Pending plugin uninstalls are canceled."
    redirect_to :action => 'index'
  end

  def system_updates
    @uninstalls=java_facade.getPluginUninstalls()
    @downloads=java_facade.getPluginDownloads()

    @center=nil
    @matrix=nil
    @sonar_updates=[]
    load_matrix()
    if @matrix
      @center=@matrix.getCenter()
      @sonar_updates=@matrix.findSonarUpdates()
    end
  end

  private
  def load_matrix
    @matrix=java_facade.getUpdateCenterMatrix(params[:reload]=='true')
  end

  def updatecenter_activated
    update_center_activated = java_facade.getSettings().getBoolean('sonar.updatecenter.activate')
    unless update_center_activated
      redirect_to home_url
    end
  end

  def user_plugins
    java_facade.getPluginsMetadata().select{|plugin| !plugin.isCore()}.sort
  end

  def core_plugins
    java_facade.getPluginsMetadata().select{|plugin| plugin.isCore()}.sort
  end
end
