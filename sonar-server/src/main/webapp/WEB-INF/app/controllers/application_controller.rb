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
class ApplicationController < ActionController::Base

  include AuthenticatedSystem
  include NeedAuthorization::Helper

  before_filter :check_database_version, :set_user_session, :check_authentication

  rescue_from Exception, :with => :render_error
  rescue_from NativeException, :with => :render_native_exception
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
    Api::Utils.java_facade
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
    unless DatabaseMigrationManager.instance.is_sonar_access_allowed?
      redirect_to :controller => 'maintenance', :action => 'index'
    end
  end

  def set_user_session
    if params[:locale]
      I18n.locale = request.compatible_language_from(available_locales, [params[:locale]])
    else
      I18n.locale = request.compatible_language_from(available_locales)
    end

    if current_user && current_user.id
      Java::OrgSonarServerUser::UserSession.setSession(current_user.id.to_i, current_user.login, I18n.locale.to_s)
    else
      Java::OrgSonarServerUser::UserSession.setSession(nil, nil, I18n.locale.to_s)
    end
  end

  def check_authentication
    access_denied if !current_user && java_facade.getConfigurationValue('sonar.forceAuthentication')=='true'
  end

  # i18n
  def message(key, options={})
    Api::Utils.message(key, options)
  end

  # escape '%' and '_' in order to use these characters in sql query using like
  def escape_like(field)
    field.gsub(/[_%]/) { |x| "\\#{x}" }
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

  # since 3.3
  def require_parameters(*keys)
    keys.each do |key|
      bad_request("Missing parameter: #{key}") if params[key].blank?
    end
  end

  # since 3.3
  def verify_post_request
    bad_request('Not a POST request') unless request.post?
  end

  # since 3.3
  def verify_ajax_request
    bad_request('Not an AJAX request') unless request.xhr?
  end

  def render_not_found(error)
    render :file => "#{Rails.public_path}/404.html", :status => 404
  end

  def render_bad_request(error)
    message = error.respond_to?('message') ? error.message : error.to_s
    render :text => message, :status => 400
  end

  def render_native_exception(error)
    if error.cause.java_kind_of? Java::JavaLang::IllegalArgumentException
      render_bad_request(error.cause.getMessage)
    else
      render_error(error)
    end
  end

  def render_error(error)
    # Ruby on Rails has a single logger "rails", so it's not possible to distinguish profiling logs
    # from error logs. For this reason a standard SLF4J logger is used instead of logger.error().
    java_facade.logError("Fail to render: #{request.url}\n#{Api::Utils.exception_message(error, :backtrace => true)}")

    if request.xhr?
      message = error.respond_to?('message') ? error.message : error.to_s
      render :text => message, :status => 500
    else
      render :file => "#{Rails.public_path}/500.html", :status => 500
    end
  end


  #
  # RAILS FILTERS
  #
  def init_resource_for_user_role
    init_resource_for_role :user
  end

  def init_resource_for_admin_role
    init_resource_for_role :admin
  end

  def init_resource_for_role(role, resource_param=:id)
    @resource=Project.by_key(params[resource_param])
    not_found("Project not found") unless @resource
    @resource=@resource.permanent_resource

    @snapshot=@resource.last_snapshot
    not_found("Snapshot not found") unless @snapshot

    access_denied unless has_role?(role, @resource)
  end


  # BREADCRUMBS
  def add_breadcrumbs(*breadcrumbs)
    @breadcrumbs ||= []
    @breadcrumbs.concat(breadcrumbs)
  end

  #
  # SETTINGS
  #
  def by_category_name(categories)
    Api::Utils.insensitive_sort(categories) { |category| category_name(category) }
  end

  def by_subcategory_name(category, subcategories)
    Api::Utils.insensitive_sort(subcategories) { |subcategory|
      if (subcategory == category)
        # Hack to have default category in first position
        "aaaaa"
      else
        subcategory_name(category, subcategory)
      end
    }
  end

  def category_name(category)
    message("property.category.#{category}", :default => category)
  end

  def subcategory_name(category, subcategory)
    message("property.category.#{category}.#{subcategory}", :default => subcategory)
  end
end
