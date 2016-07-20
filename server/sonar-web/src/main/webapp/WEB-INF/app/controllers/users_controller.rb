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
### Users Controller from restful_authentication (http://agilewebdevelopment.com/plugins/restful_authentication)
class UsersController < ApplicationController

  SECTION=Navigation::SECTION_CONFIGURATION
  before_filter :admin_required, :except => ['new', 'signup', 'autocomplete']
  skip_before_filter :check_authentication, :only => ['new', 'signup', 'autocomplete']

  def signup
    access_denied unless request.post? && Property.value('sonar.allowUsersToSignUp')=='true'

    @user=prepare_user
    if @user.save
      Internal.users_api.index()
      @user.notify_creation_handlers
      flash[:notice] = 'Please log in now.'
      redirect_to home_url
    else
      render :action => 'new', :layout => 'nonav'
    end
  end

  def index

  end

  def new
    render :action => 'new', :layout => 'nonav'
  end


  private

  def prepare_user
    user = User.new(params[:user])
    user.external_identity = user.login
    user.external_identity_provider = 'sonarqube'
    user.user_local = true
    default_group_name=java_facade.getSettings().getString('sonar.defaultGroup')
    default_group=Group.find_by_name(default_group_name)
    user.groups<<default_group if default_group
    user
  end

end
