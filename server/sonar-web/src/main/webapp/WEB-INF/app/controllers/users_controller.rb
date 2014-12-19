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

  def create
    return unless request.post?
    cookies.delete :auth_token

    call_backend do
      isUserReactivated = Internal.users_api.create(params[:user])
      if !isUserReactivated
        flash[:notice] = 'User is created.'
      else
        flash[:notice] = Api::Utils.message('user.reactivated', :params => params[:user][:login])
      end
      render :text => 'ok', :status => 200
    end
  end

  def signup
    access_denied unless request.post? && Property.value('sonar.allowUsersToSignUp')=='true'

    cookies.delete :auth_token
    @user=prepare_user
    if @user.save
      @user.notify_creation_handlers
      flash[:notice] = 'Please log in now.'
      redirect_to home_url
    else
      render :action => 'new', :layout => 'nonav'
    end
  end

  def index
    init_users_list
    if params[:id]
      @user = User.find(params[:id])
    else
      @user = User.new
    end
  end

  def create_form
    @user = User.new
    render :partial => 'users/create_form'
  end

  def new
    render :action => 'new', :layout => 'nonav'
  end

  def edit_form
    call_backend do
      @user = Internal.users_api.getByLogin(params[:id])
      render :partial => 'users/edit_form', :status => 200
    end
  end


  def change_password_form
    @user = User.find(params[:id])
    render :partial => 'users/change_password_form', :status => 200
  end

  def update_password
    user = User.find(params[:id])
    @user = user
    if params[:user][:password].blank?
      @errors = message('my_profile.password.empty')
      render :partial => 'users/change_password_form', :status => 400
    elsif user.update_attributes(:password => params[:user][:password], :password_confirmation => params[:user][:password_confirmation])
      flash[:notice] = 'Password was successfully updated.'
      render :text => 'ok', :status => 200
    else
      @errors = user.errors.full_messages.join("<br/>\n")
      render :partial => 'users/change_password_form', :status => 400
    end
  end

  def update
    call_backend do
      Internal.users_api.update(params[:user])
      flash[:notice] = 'User was successfully updated.'
      render :text => 'ok', :status => 200
    end
  end

  def delete
    begin
      user = User.find(params[:id])
      Api.users.deactivate(user.login)
      flash[:notice] = 'User is deleted.'
    rescue NativeException => exception
      if exception.cause.java_kind_of? Java::OrgSonarServerExceptions::ServerException
        error = exception.cause
        flash[:error] = (error.getMessage ? error.getMessage : Api::Utils.message(error.l10nKey, :params => error.l10nParams.to_a))
      else
        flash[:error] = 'Error when deleting this user.'
      end
    end

    redirect_to(:action => 'index', :id => nil)
  end

  def select_group
    @user = User.find(params[:id])
    render :partial => 'users/select_group'
  end

  def set_groups
    @user = User.find(params[:id])

    if  @user.set_groups(params[:groups])
      flash[:notice] = 'User is updated.'
    end

    redirect_to(:action => 'index')
  end

  def to_index(errors, id)
    if !errors.empty?
      flash[:error] = errors.full_messages.join("<br/>\n")
    end

    redirect_to(:action => 'index', :id => id)
  end

  def prepare_user
    user = User.new(params[:user])
    default_group_name=java_facade.getSettings().getString('sonar.defaultGroup')
    default_group=Group.find_by_name(default_group_name)
    user.groups<<default_group if default_group
    user
  end


  private

  def init_users_list
    @users = User.find(:all, :conditions => ["active=?", true], :include => 'groups')
  end

end
