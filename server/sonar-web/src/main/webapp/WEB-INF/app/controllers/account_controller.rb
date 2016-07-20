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
        @new_project_id = new_project.id
      end
      @selected_project_id = new_project.id
    end
  end

  def update_notifications
    verify_post_request
    # Global notifs
    global_notifs = params[:global_notifs]
    Property.delete_all(['prop_key like ? AND user_id = ? AND resource_id IS NULL', 'notification.%', current_user.id])
    global_notifs.each_key { |key| current_user.add_property(:prop_key => 'notification.' + key, :text_value => 'true') } if global_notifs

    # Per project notifs
    project_notifs = params[:project_notifs]
    Property.delete_all(['prop_key like ? AND user_id = ? AND resource_id IS NOT NULL', 'notification.%', current_user.id])
    if project_notifs
      project_notifs.each do |r_id, per_project_notif|
        per_project_notif.each do |dispatch, channels|
          channels.each do |channel, value|
            current_user.add_property(:prop_key => 'notification.' + dispatch + '.' + channel, :text_value => 'true', :resource_id => r_id)
          end
        end
      end
    end

    redirect_to "#{ApplicationController.root_context}/account/notifications"
  end

  private

  def notification_service
    java_facade.getCoreComponentByClassname('org.sonar.server.notification.NotificationCenter')
  end

  def dispatchers_for_scope(scope)
    notification_service.getDispatcherKeysForProperty(scope, "true").to_a.sort {|x,y| dispatcher_name(x) <=> dispatcher_name(y)}
  end

  def dispatcher_name(dispatcher_key)
    Api::Utils.message('notification.dispatcher.' + dispatcher_key)
  end

  def load_notification_properties
    channel_keys = @channels.map {|c| c.getKey()}

    Property.find(:all, :conditions => ['prop_key like ? AND user_id = ?', 'notification.%', current_user.id]).each do |property|
      r_id = property.resource_id
      if r_id
        # This is a per-project notif
        parts = property.key.split('.')
        dispatcher_key = parts[1]
        channel_key = parts[2]
        if @per_project_dispatchers.include?(dispatcher_key) && channel_keys.include?(channel_key)
          project_notifs = get_project_notifications(r_id)
          project_notifs[dispatcher_key] << channel_key
        end
      else
        # This is a global notif
        @global_notifications[property.key.sub('notification.', '')] = true
      end
    end
  end

  def get_project_notifications(resource_id)
    project_notifs = @per_project_notifications[resource_id]
    unless project_notifs
      project_notifs = init_project_notifications
      @per_project_notifications[resource_id] = project_notifs
    end
    project_notifs
  end

  def init_project_notifications
    project_notifs = {}
    @per_project_dispatchers.each do |dispatcher|
      project_notifs[dispatcher] = []
    end
    project_notifs
  end

end
