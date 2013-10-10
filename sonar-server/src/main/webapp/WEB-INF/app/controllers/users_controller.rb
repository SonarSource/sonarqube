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
### Users Controller from restful_authentication (http://agilewebdevelopment.com/plugins/restful_authentication)
class UsersController < ApplicationController

  SECTION=Navigation::SECTION_CONFIGURATION
  before_filter :admin_required, :except => ['new', 'signup', 'autocomplete']
  skip_before_filter :check_authentication, :only => ['new', 'signup', 'autocomplete']

  def create
    return unless request.post?
    cookies.delete :auth_token

    user = User.find_by_login(params[:user][:login])
    if user && !user.active
      # users is deativated, this is a special case:
      # 1- first, we save the given information, in case the user is reactivated (to not ask for it twice)
       if user.update_attributes(params[:user])
        # 2- if info correctly saved, then we display a message to ask wether the user should be reactivated or not
        @user = user
        init_users_list
        render :index
      else
        to_index(user.errors, nil)
      end
    else
      user=prepare_user
      if user.save
        user.notify_creation_handlers
        flash[:notice] = 'User is created.'
      end
      to_index(user.errors, nil)
    end

  end

  def create_modal
    return unless request.post?
    cookies.delete :auth_token
    @errors = []
    user = User.find_by_login(params[:user][:login])
    if user && !user.active
      if user.update_attributes(params[:user])
        # case user: exist,inactive,no errors when update BUT TO REACTIVATE
        @user = user
        user.errors.full_messages.each{|msg| @errors<<msg}
        render :partial => 'users/reactivate_modal_form', :status => 400
      else
        # case user: exist,inactive, WITH ERRORS when update
        @user = user
        @user.id = nil
        user.errors.full_messages.each{|msg| @errors<<msg}
        render :partial => 'users/create_modal_form', :status => 400
      end
    else
        user=prepare_user
        if user.save
          # case user: don't exist, no errors when create
          user.notify_creation_handlers
          flash[:notice] = 'User is created.'
          render :text => 'ok', :status => 200
        else
          # case user: don't exist, WITH ERRORS when create
          # case user: exist and ACTIVE, whith or without errors when create
          @user = user
          user.errors.full_messages.each{|msg| @errors<<msg}
          render :partial => 'users/create_modal_form', :status => 400
        end
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

  def create_modal_form
    init_users_list
    if params[:id]
      @user = User.find(params[:id])
    else
      @user = User.new
    end
    render :partial => 'users/create_modal_form'
  end

  def reactivate_modal_form
    init_users_list
    if params[:id]
      @user = User.find(params[:id])
    else
      @user = User.new
    end
    render :partial => 'users/reactivate_modal_form'
  end

  def new
    render :action => 'new', :layout => 'nonav'
  end

  def edit
    redirect_to(:action => 'index', :id => params[:id])
  end

  def change_password
    init_users_list
    @user = User.find(params[:id])
    render :action => 'index', :id => params[:id]
  end

  def update_password
    user = User.find(params[:id])

    if params[:user][:password].blank?
      flash[:error] = 'Password required.'
      redirect_to(:action => 'change_password', :id => params[:id])
    elsif user.update_attributes(:password => params[:user][:password], :password_confirmation => params[:user][:password_confirmation])
      flash[:notice] = 'Password was successfully updated.'
      to_index(user.errors, nil)
    else
      flash[:error] = user.errors.full_messages.join("<br/>\n")
      redirect_to(:action => 'change_password', :id => params[:id])
    end

  end

  def update
    user = User.find(params[:id])

    if user.login!=params[:user][:login]
      flash[:error] = 'Login can not be changed.'

    elsif user.update_attributes(params[:user])
      flash[:notice] = 'User was successfully updated.'
    end

    to_index(user.errors, nil)
  end

  def reactivate
    user = User.find_by_login(params[:user][:login])
    if user
      user.reactivate!(java_facade.getSettings().getString('sonar.defaultGroup'))
      user.notify_creation_handlers
      flash[:notice] = 'User was successfully reactivated.'
    else
      flash[:error] = "A user with login #{params[:user][:login]} does not exist."
    end
    to_index(user.errors, nil)
  end

  def reactivate_modal
    user = User.find_by_login(params[:user][:login])
    if user
      user.reactivate!(java_facade.getSettings().getString('sonar.defaultGroup'))
      user.notify_creation_handlers
      flash[:notice] = 'User was successfully reactivated.'
      render :text => 'ok', :status => 200
    else
      flash[:error] = "A user with login #{params[:user][:login]} does not exist."
      render :text => 'login unknown', :status => 200
    end
  end

  def destroy
    begin
      user = User.find(params[:id])
      Api.users.deactivate(user.login)
      flash[:notice] = 'User is deleted.'
    rescue NativeException => exception
      if exception.cause.java_kind_of? Java::OrgSonarServerExceptions::HttpException
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
