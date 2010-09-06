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
class PluginsController < ApplicationController

  SECTION=Navigation::SECTION_CONFIGURATION
  before_filter :admin_required

  verify :method => :post, :only => [:cancel, :install], :redirect_to => {:action => :index}

  def index
    @user_plugins=Plugin.user_plugins
    @core_plugins=Plugin.core_plugins
  end

  def updates
    @downloads=java_facade.getPluginDownloads()

    @center=nil
    @sonar_updates=[]
    @plugin_updates=[]

    finder=load_update_finder()
    if finder
      @center=finder.getCenter()
      @sonar_updates=finder.findSonarUpdates()
      @plugin_updates=finder.findPluginUpdates()
    end
  end

  def available
    @downloads=java_facade.getPluginDownloads()
    @center=nil
    @updates_by_category={}

    finder=load_update_finder()
    if finder
      @center=finder.getCenter()
      finder.findAvailablePlugins().each do |update|
        category=update.getPlugin().getCategory()||''
        @updates_by_category[category]||=[]
        @updates_by_category[category]<<update
      end
    end
  end

  def cancel
    java_facade.cancelPluginDownloads()
    flash[:notice]="Plugin downloads are canceled."
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

  private
  def load_update_finder
    @finder=java_facade.getUpdateFinder(params[:reload]=='true')
  end
end