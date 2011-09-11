#
# Sonar, entreprise quality control tool.
# Copyright (C) 2008-2011 SonarSource
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
  
  SPECIAL_CATEGORIES=['email', 'server_id']

  verify :method => :post, :only => ['update'], :redirect_to => {:action => :index}

  def index
    return access_denied unless is_admin?
    load_properties(false)
    @category ||= 'general'
  end

  def update
    if params[:resource_id]
      project=Project.by_key(params[:resource_id])
      return access_denied unless (project && is_admin?(project))
      resource_id=project.id
    else
      return access_denied unless is_admin?
      resource_id=nil
    end

    load_properties(true)

    if @category && @properties_per_category[@category]
      @properties_per_category[@category].each do |property|
        value=params[property.key()]
        persisted_property = Property.find(:first, :conditions => {:prop_key=> property.key(), :resource_id => resource_id, :user_id => nil})

        if persisted_property
          if value.empty?
            Property.delete_all('prop_key' => property.key(), 'resource_id' => resource_id, 'user_id' => nil)
          elsif persisted_property.text_value != value.to_s
            persisted_property.text_value = value.to_s
            persisted_property.save!
          end
        elsif !value.blank?
          Property.create(:prop_key => property.key(), :text_value => value.to_s, :resource_id => resource_id)
        end
      end
      java_facade.reloadConfiguration()
      flash[:notice] = 'Parameters updated'
    end

    if resource_id
      redirect_to :controller => 'project', :action => 'settings', :id => resource_id, :category => @category
    else
      redirect_to :controller => 'settings', :action => 'index', :category => @category
    end
  end

  private

  def load_properties(all=true)
    @category=params[:category]
    @properties_per_category={}
    java_facade.getPluginsMetadata().each do |plugin|
      java_facade.getPluginProperties(plugin).select { |property| all || property.global }.each do |property|
        category = (property.category().present? ? property.category() : plugin.name())
        @properties_per_category[category]||=[]
        @properties_per_category[category]<<property
      end
    end
    SPECIAL_CATEGORIES.each do |category|
      @properties_per_category[category]=[]
    end
  end
end
