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

  SECTION=Navigation::SECTION_CONFIGURATION

  before_filter :login_required

  def index
    notification_service = java_facade.getCoreComponentByClassname('org.sonar.server.notifications.NotificationService')
    @channels = notification_service.getChannels()
    @dispatchers = notification_service.getDispatchers()
    @notifications = {}
    for property in Property.find(:all, :conditions => ['prop_key like ? AND user_id = ?', 'notification.%', current_user.id])
      @notifications[property.key.sub('notification.', '')] = true
    end
  end

  def change_password
    return unless request.post?
    if User.authenticate(current_user.login, params[:old_password])
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
    Property.delete_all(['prop_key like ? AND user_id = ?', 'notification.%', current_user.id])
    notifications.each_key { |key| current_user.set_property(:prop_key => 'notification.' + key, :text_value => 'true') } unless notifications.nil?
    redirect_to :action => 'index'
  end

end
