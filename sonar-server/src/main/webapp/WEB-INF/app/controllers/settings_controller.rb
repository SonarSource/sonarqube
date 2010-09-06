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
class SettingsController < ApplicationController

  SECTION=Navigation::SECTION_CONFIGURATION

  verify :method => :post, :only => ['update'], :redirect_to => { :action => :index }

  def index
    return access_denied unless is_admin?  
  end

  def update
    if params[:resource_id]
      project=Project.by_key(params[:resource_id])
      return access_denied unless is_admin?(project)
      resource_id=project.id
    end

    plugins = java_facade.getPlugins()
    plugins.each do |plugin|
      properties=java_facade.getPluginProperties(plugin)
      properties.each do |property|
        value=params[property.key()]
        old_value=params['old_' + property.key()]
        if (value != old_value)
          if value.blank?
            Property.clear(property.key(), resource_id)
          else
            Property.set(property.key(), value, resource_id)
          end
        end
      end
    end
    flash[:notice] = 'Parameters updated.'
    if resource_id
      redirect_to :controller => 'project', :action => 'settings', :id => resource_id
    else
      redirect_to :action => 'index'
    end
  end
end
