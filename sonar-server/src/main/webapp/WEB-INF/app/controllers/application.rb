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
class ApplicationController < ActionController::Base
  
  include AuthenticatedSystem
  include NeedAuthorization::Helper
  
  before_filter :check_database_version, :set_locale, :check_authentication

  def self.root_context
    ActionController::Base.relative_url_root || ''
  end
    
  def java_facade
    @java_facade ||= Java::OrgSonarServerUi::JRubyFacade.new
  end
  
  def available_locales
    # see config/initializers/available_locales.rb
    AVAILABLE_LOCALES
  end


  # Filter method to enforce a resource.
  #
  # To require resource for all actions, use this in your controllers:
  #
  #   before_filter :resource_required
  #
  # To require resourcess for specific actions, use this in your controllers:
  #
  #   before_filter :resource_required, :only => [ :edit, :update ]
  #
  # To skip this in a subclassed controller:
  #
  #   skip_before_filter :resource_required
  #
  #
  # Mandatory parameter : 'resource' or 'id'
  #
  # After filter is executed :
  #   @resource is the current resource.
  #   @resource.project is the current project
  #
  #
  def resource_required
    key=params[:resource] || params[:id]
    @resource=Project.by_key(key) if key
    redirect_to_default unless @resource
  end

  protected

  def check_database_version
    unless DatabaseVersion.uptodate?
      redirect_to :controller => 'maintenance', :action => 'index'
    end
  end
  
  # Do not log common errors like 404.
  # See http://maintainable.com/articles/rails_logging_tips
  EXCEPTIONS_NOT_LOGGED = ['ActionController::UnknownAction','ActionController::RoutingError']
  def log_error(exc)
    super unless EXCEPTIONS_NOT_LOGGED.include?(exc.class.name)
  end
  
  def set_locale
    if params[:locale]
      I18n.locale = request.compatible_language_from(available_locales, [params[:locale]])
    else
      I18n.locale = request.compatible_language_from(available_locales)
    end
  end

  def check_authentication
    if current_user.nil? && Property.value('sonar.forceAuthentication')=='true'
      return access_denied
    end
  end
end
