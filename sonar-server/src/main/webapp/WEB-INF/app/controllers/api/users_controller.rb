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

end
