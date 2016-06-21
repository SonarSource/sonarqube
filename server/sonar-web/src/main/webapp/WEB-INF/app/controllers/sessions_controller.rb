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
  
  def logout
    if logged_in?
      self.current_user.on_logout
    end
    flash[:notice]=message('session.flash_notice.logged_out')
    reset_session
    redirect_to(home_path)
  end

  def new
    if params[:return_to]
      # user clicked on the link "login" : redirect to the original uri after authentication
      session[:return_to] = Api::Utils.absolute_to_relative_url(params[:return_to])
      return_to = Api::Utils.absolute_to_relative_url(params[:return_to])
    # else the original uri can be set by ApplicationController#access_denied
    end
    @return_to = get_redirect_back_or_default(home_url)

    # Needed to bypass session fixation vulnerability (https://jira.sonarsource.com/browse/SONAR-6880)
    reset_session
  end

  private

  # Get redirection to the URI stored by the most recent store_location call or to the passed default.
  def get_redirect_back_or_default(default)
    # Prevent CSRF attack -> do not accept absolute urls
    url = session[:return_to] || default
    begin
      url = URI(url).request_uri
    rescue
      url
    end
    anchor=params[:return_to_anchor]
    url += anchor if anchor && anchor.start_with?('#')
    url
  end

end
