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

# since 3.6
class Api::UsersController < Api::ApiController

  #
  # GET /api/users/search?<parameters>
  #
  # -- Example
  # curl -v 'http://localhost:9000/api/users/search?includeDeactivated=true&logins=simon,julien'
  #
  def search
    users = Api.users.find(params)

    select2_format=(params[:f]=='s2')
    if select2_format
      hash = {
        :more => false,
        :results => users.map { |user| {:id => user.login, :text => "#{user.name} (#{user.login})"} }
      }
    else
      hash = {:users => users.map { |user| User.to_hash(user) }}
    end

    respond_to do |format|
      format.json { render :json => jsonp(hash) }
      format.xml { render :xml => hash.to_xml(:skip_types => true, :root => 'users') }
    end
  end

  #
  # POST /api/users/create
  #
  # -- Mandatory parameters
  # 'login' is the user identifier
  # 'name' is the user display name
  # 'password' is the user password
  # 'password_confirmation' is the confirmed user password
  #
  # -- Optional parameters
  # 'email' is the user email
  #
  # -- Example
  # curl -X POST -v -u admin:admin 'http://localhost:9000/api/users/create?login=user&name=user_name&password=user_pw&password_confirmation=user_pw'
  #
  # since SonarQube 3.7
  # SonarQube 3.7.1 update : name is now mandatory
  #
  def create
    verify_post_request
    access_denied unless has_role?(:admin)
    require_parameters :login, :password, :password_confirmation

    user = User.find_by_login(params[:login])

    if user && user.active
      render_bad_request('An active user with this login already exists')
    else
      if user
        user.reactivate!(java_facade.getSettings().getString('sonar.defaultGroup'))
        user.notify_creation_handlers
      else
        user = prepare_user
        user.save!
        user.notify_creation_handlers
      end
      hash = user.to_hash
      respond_to do |format|
        format.json { render :json => jsonp(hash) }
        format.xml { render :xml => hash.to_xml(:skip_types => true, :root => 'users') }
      end
    end
  end

  #
  # POST /api/users/update
  #
  # -- Mandatory parameters
  # 'login' is the user identifier
  #
  # -- Optional parameters
  # 'name' is the user display name
  # 'email' is the user email
  #
  # -- Example
  # curl -X POST -v -u admin:admin 'http://localhost:9000/api/users/update?login=user&email=new_email'
  #
  # since 3.7
  #
  def update
    verify_post_request
    access_denied unless has_role?(:admin)
    require_parameters :login

    user = User.find_active_by_login(params[:login])

    if user.nil?
      render_bad_request("Could not find user with login #{params[:login]}")
    elsif user.update_attributes!(params)
      hash = user.to_hash
      respond_to do |format|
        format.json { render :json => jsonp(hash) }
        format.xml { render :xml => hash.to_xml(:skip_types => true, :root => 'users') }
      end
    end
  end


  #
  # POST /api/users/deactivate
  #
  # -- Mandatory parameters
  # 'login' is the user identifier
  #
  # -- Example
  # curl -X POST -v -u admin:admin 'http://localhost:9000/api/users/deactivate?login=<the user to deactivate>'
  #
  # since 3.7
  #
  def deactivate
    verify_post_request
    require_parameters :login

    Api.users.deactivate(params[:login])

    hash={}
    respond_to do |format|
      format.json { render :json => jsonp(hash) }
      format.xml { render :xml => hash.to_xml(:skip_types => true, :root => 'users') }
    end
  end


  private

  def prepare_user
    user = User.new(params)
    default_group_name=java_facade.getSettings().getString('sonar.defaultGroup')
    default_group=Group.find_by_name(default_group_name)
    user.groups<<default_group if default_group
    user
  end

end
