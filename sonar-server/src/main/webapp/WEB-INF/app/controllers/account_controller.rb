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
class AccountController < ApplicationController

  before_filter :login_required

  def index
    @channels = notification_service.getChannels()
    @global_dispatchers = dispatchers_for_scope("globalNotification")
    @per_project_dispatchers = dispatchers_for_scope("perProjectNotification")
    
    @global_notifications = {}
    @per_project_notifications = {}
    load_notification_properties
    
    if params[:new_project]
      new_project = Project.by_key params[:new_project]
      unless @per_project_notifications[new_project.id]
        @per_project_notifications[new_project.id] = init_project_notifications
      end
      @selected_project_id = new_project.id
    end
  end

  def change_password
    return unless request.post?
    if User.authenticate(current_user.login, params[:old_password], servlet_request)
      if ((params[:password] == params[:password_confirmation]))
        current_user.password = params[:password]
        current_user.password_confirmation = params[:password]
        @result = current_user.save
        if @result
          flash[:notice] = message('my_profile.password.changed')
        else
          flash[:error] = message('my_profile.password.empty')
        end
      else
        flash[:error] = message('my_profile.password.mismatch')
      end
    else
      flash[:error] = message('my_profile.password.wrong_old')
    end
    redirect_to :controller => 'account', :action => 'index'
  end

  def update_notifications
    notifications = params[:notifications]
    Property.delete_all(['prop_key like ? AND user_id = ? AND resource_id IS NULL', 'notification.%', current_user.id])
    notifications.each_key { |key| current_user.add_property(:prop_key => 'notification.' + key, :text_value => 'true') } if notifications
    redirect_to :action => 'index'
  end

  def update_per_project_notifications
    notifications = params[:notifications]
    Property.delete_all(['prop_key like ? AND user_id = ? AND resource_id IS NOT NULL', 'notification.%', current_user.id])
    if notifications
      notifications.each do |r_id, per_project_notif|
        per_project_notif.each do |dispatch, channels|
          channels.each do |channel|
            current_user.add_property(:prop_key => 'notification.' + dispatch + '.' + channel, :text_value => 'true', :resource_id => r_id)
          end
        end
      end
    end
    
    new_params = {}
    unless params[:new_project].blank?
      new_params[:new_project] = params[:new_project]
    end
    
    redirect_to :action => 'index', :params => new_params
  end
  
  private

  def notification_service
    java_facade.getCoreComponentByClassname('org.sonar.server.notifications.NotificationCenter')
  end
  
  def dispatchers_for_scope(scope)
    notification_service.getDispatcherKeysForProperty(scope, "true").sort {|x,y| dispatcher_name(x) <=> dispatcher_name(y)}
  end
  
  def dispatcher_name(dispatcher_key)
    Api::Utils.message('notification.dispatcher.' + dispatcher_key)
  end
  
  def load_notification_properties
    Property.find(:all, :conditions => ['prop_key like ? AND user_id = ?', 'notification.%', current_user.id]).each do |property|
      r_id = property.resource_id
      if r_id
        # This is a per-project notif
        project_notifs = @per_project_notifications[r_id]
        unless project_notifs
          project_notifs = {}
          @per_project_dispatchers.each do |dispatcher|
            project_notifs[dispatcher] = []
          end
          @per_project_notifications[r_id] = project_notifs
        end
        parts = property.key.split('.')
        dispatcher_key = parts[1]
        channel_key = parts[2]
        project_notifs[dispatcher_key] << channel_key
      else
        # This is a global notif
        @global_notifications[property.key.sub('notification.', '')] = true
      end
    end
  end
  
  def init_project_notifications
    project_notifs = {}
    @per_project_dispatchers.each do |dispatcher|
      project_notifs[dispatcher] = []
    end
    project_notifs
  end

end
