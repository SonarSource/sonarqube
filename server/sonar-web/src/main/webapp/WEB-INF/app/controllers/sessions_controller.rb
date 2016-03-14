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

### Sessions Controller from restful_authentication (http://agilewebdevelopment.com/plugins/restful_authentication)
class SessionsController < ApplicationController
  
  layout 'nonav'
  skip_before_filter :check_authentication
  
  def login
    return unless request.post?

    return_to = session[:return_to]

    # Needed to bypass session fixation vulnerability (https://jira.sonarsource.com/browse/SONAR-6880)
    reset_session

    if return_to
      # user clicked on the link "login" : redirect to the original uri after authentication
      session[:return_to] = Api::Utils.absolute_to_relative_url(return_to)
      # else the original uri can be set by ApplicationController#access_denied
    end

    self.current_user = User.authenticate(params[:login], params[:password], servlet_request)
    if logged_in?
      if params[:remember_me] == '1'
        self.current_user.remember_me
        cookies[:auth_token] = { :value => self.current_user.remember_token , :expires => self.current_user.remember_token_expires_at, :http_only => true }
      end
      redirect_back_or_default(home_url)
    else
      @return_to_anchor = params[:return_to_anchor]
      flash.now[:loginerror] = message('session.flash_notice.authentication_failed')
    end
  end

  def logout
    if logged_in?
      self.current_user.on_logout
      self.current_user.forget_me
    end
    cookies.delete :auth_token    
    flash[:notice]=message('session.flash_notice.logged_out')
    redirect_to(home_path)
    reset_session
  end

  def new
    if params[:return_to]
      # user clicked on the link "login" : redirect to the original uri after authentication
      session[:return_to] = Api::Utils.absolute_to_relative_url(params[:return_to])
    # else the original uri can be set by ApplicationController#access_denied
    end
  end

end
