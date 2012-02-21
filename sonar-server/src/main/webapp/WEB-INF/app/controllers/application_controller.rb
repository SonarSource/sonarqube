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
class ApplicationController < ActionController::Base

  include AuthenticatedSystem
  include NeedAuthorization::Helper

  before_filter :check_database_version, :set_locale, :check_authentication

  rescue_from Exception, :with => :render_error
  rescue_from Errors::BadRequest, :with => :render_bad_request
  rescue_from ActionController::UnknownAction, :with => :render_not_found
  rescue_from ActionController::RoutingError, :with => :render_not_found
  rescue_from ActionController::UnknownController, :with => :render_not_found
  rescue_from ActiveRecord::RecordInvalid, :with => :render_bad_request
  rescue_from ActiveRecord::RecordNotFound, :with => :render_not_found
  rescue_from Errors::NotFound, :with => :render_not_found
  rescue_from Errors::AccessDenied, :with => :render_access_denied # See lib/authenticated_system.rb#access_denied()

  def self.root_context
    ActionController::Base.relative_url_root || ''
  end

  def java_facade
    Java::OrgSonarServerUi::JRubyFacade.getInstance()
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

  def set_locale
    if params[:locale]
      I18n.locale = request.compatible_language_from(available_locales, [params[:locale]])
    else
      I18n.locale = request.compatible_language_from(available_locales)
    end
  end

  def check_authentication
    if current_user.nil? && Property.value('sonar.forceAuthentication')=='true'
      access_denied
    end
  end

  # i18n
  def message(key, options={})
    Api::Utils.message(key, options)
  end


  #
  #
  # ERROR HANDLING
  #
  #

  # The request is invalid. An accompanying error message explains why : missing mandatory property, bad value, ...
  def bad_request(message)
    raise Errors::BadRequest.new(message)
  end

  # The resource requested, such as a project, a dashboard or a filter, does not exist
  def not_found(message)
    raise Errors::NotFound.new(message)
  end

  # Authentication credentials are missing/incorrect or user has not the required permissions
  def access_denied
    store_location # required to redirect to origin URL after authentication
    raise Errors::AccessDenied
  end

  def render_not_found(error)
    render :file => "#{Rails.public_path}/404.html", :status => 404
  end

  def render_bad_request(error)
    render :text => error.message, :status => 400
  end

  def render_error(error)
    # Ruby on Rails has a single logger "rails", so it's not possible to distinguish profiling logs
    # from error logs. For this reason a standard SLF4J logger is used instead of logger.error().
    java_facade.logError("Fail to render: #{request.url}\n#{Api::Utils.exception_message(error)}")

    if request.xhr?
      message = error.respond_to?('message') ? error.message : error.to_s
      render :text => message, :status => 500
    else
      render :file => "#{Rails.public_path}/500.html", :status => 500
    end
  end


  #
  # FILTERS
  #
  def init_resource_for_user_role
    init_resource_for_role :user
  end

  def init_resource_for_admin_role
    init_resource_for_role :admin
  end

  def init_resource_for_role(role)
    @resource=Project.by_key(params[:id])
    not_found("Project not found") unless @resource
    @resource=@resource.permanent_resource

    @snapshot=@resource.last_snapshot
    not_found("Snapshot not found") unless @snapshot

    access_denied unless has_role?(role, @resource)
  end
end
