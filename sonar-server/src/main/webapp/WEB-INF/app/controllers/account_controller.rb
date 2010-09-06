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
class AccountController < ApplicationController

  SECTION=Navigation::SECTION_CONFIGURATION

  before_filter :login_required

  def index

  end

  def change_password
    return unless request.post?
    if User.authenticate(current_user.login, params[:old_password])
      if ((params[:password] == params[:password_confirmation]))
        current_user.password = params[:password]
        current_user.password_confirmation = params[:password]
        @result = current_user.save
        if @result
          flash[:notice] = 'Password changed.'
        else
          flash[:error] = 'Password cannot be empty'
        end
      else
        flash[:error] = 'Password mismatch'
      end
    else
      flash[:error] = 'Wrong old password'
    end
    redirect_to :controller => 'account', :action => 'index'
  end
end
