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
class UpdatecenterController < ApplicationController

  SECTION=Navigation::SECTION_CONFIGURATION
  before_filter :admin_required
  before_filter :updatecenter_activated

  verify :method => :post, :only => [:cancel, :install], :redirect_to => {:action => :index}

  def index
    @uninstalls=java_facade.getPluginUninstalls()
    @downloads=java_facade.getPluginDownloads()

    load_plugin_center
    @plugins = installed_plugins
  end

  def available
    @uninstalls=java_facade.getPluginUninstalls()
    @downloads=java_facade.getPluginDownloads()
    @update_plugin_center=nil
    @updates_by_category={}

    load_plugin_center()
    if @update_plugin_center
      @update_plugin_center.findAvailablePlugins().each do |update|
        already_download = update.plugin.releases.find {|release| @downloads.include? release.filename }
        if !already_download
          category = update.plugin.category||''
          @updates_by_category[category]||=[]
          @updates_by_category[category]<<update
        end
      end
    end
  end

  def updates
    @uninstalls=java_facade.getPluginUninstalls()
    @downloads=java_facade.getPluginDownloads()

    @update_plugin_center=nil
    @updates_by_plugin={}
    @installed_plugins={}
    @last_compatible={}

    load_plugin_center()
    if @update_plugin_center
      installed_plugins.each do |plugin|
        @installed_plugins[plugin.getKey()]=plugin.lastRelease.getVersion()
      end

      @update_plugin_center.findPluginUpdates().each do |update|
        plugin = update.plugin
        already_download = update.plugin.releases.find {|release| @downloads.include? release.filename }
        if !already_download
          @updates_by_plugin[plugin]||=[]
          @updates_by_plugin[plugin]<<update
          if update.isCompatible
            @last_compatible[plugin.key]=update.release.version
          end
        end
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

    @update_plugin_center=nil
    @sonar_updates=[]
    load_plugin_center()
    if @update_plugin_center
      @sonar_updates=@update_plugin_center.findSonarUpdates()
    end
  end

  private

  def load_plugin_center
    @update_plugin_center = java_facade.getUpdatePluginCenter(params[:reload]=='true')
    @installed_plugin_referential = java_facade.installedPluginReferential
  end

  def updatecenter_activated
    update_center_activated = java_facade.getSettings().getBoolean('sonar.updatecenter.activate')
    unless update_center_activated
      redirect_to home_url
    end
  end

  def installed_plugins
    @installed_plugin_referential.lastMasterReleasePlugins
  end
end
