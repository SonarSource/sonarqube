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

#
# Use Sonar database (table USERS) to authenticate users.
#
class DefaultRealm
  def authenticate?(username, password, servlet_request)
    result=nil
    if !username.blank? && !password.blank?
      user=User.find_active_by_login(username)
      result=user if user && user.authenticated?(password)
    end
    result
  end

  def editable_password?
    true
  end
end

#
# Use an external security system with fallback to Sonar database.
# See the Java extension point org.sonar.api.security.SecurityRealm
#
class PluginRealm
  def initialize(java_realm)
    @java_authenticator = java_realm.doGetAuthenticator()
    @java_users_provider = java_realm.getUsersProvider()
    @java_groups_provider = java_realm.getGroupsProvider()
    @save_password = Api::Utils.java_facade.getSettings().getBoolean('sonar.security.savePassword')
  end

  def authenticate?(username, password, servlet_request)
    countUserLocal = User.count('id', :conditions => ['login=? and user_local=?', username, true])
    if countUserLocal > 0
      local_auth(username, password)
    else
      auth(username, password, servlet_request)
    end
  end

  #
  # Authenticate using password from Sonar Database
  #
  def local_auth(username, password)
    result = nil
    if !username.blank? && !password.blank?
      user = User.find_active_by_login(username)
      result = user if user && user.authenticated?(password)
    end
    result
  end

  #
  # Authenticate using external system
  #
  def external_auth(username, password, servlet_request, details)
    if @java_authenticator
      context = org.sonar.api.security.Authenticator::Context.new(username, password, servlet_request)
      status = @java_authenticator.doAuthenticate(context)
      status ? synchronize(username, password, details, servlet_request) : nil
    else
      # No authenticator
      nil
    end
  end

  #
  # Authenticate user using external system. Can fallback on local auth if external system is not available. Return the user.
  #
  def auth(username, password, servlet_request)
    if @java_users_provider
      begin
        provider_context = org.sonar.api.security.ExternalUsersProvider::Context.new(username, servlet_request)
        details = @java_users_provider.doGetUserDetails(provider_context)
      rescue => e
        # Maybe LDAP server is not available
        Rails.logger.error("Error from external users provider: exception #{e.class.name}: #{e.message}")
        @save_password ? local_auth(username, password) : false
      else
        if details
          # User exist in external system
          external_auth(username, password, servlet_request, details)
        else
          # No such user in external system
          nil
        end
      end
    else
      # Legacy authenticator
      external_auth(username, password, servlet_request, nil)
    end
  end

  #
  # Authentication in external system was successful - replicate password, details and groups into Sonar
  # Return the user.
  #
  def synchronize(username, password, details, servlet_request)
    user_id = nil
    user_id = details.getUserId() if details

    username = user_id if !user_id.blank?
    username = details.getName() if username.blank? && details

    user = User.find_by_login(username)

    now = java.lang.System.currentTimeMillis

    # SONAR-4950 Use a transaction to prevent multiple insertion of same groups
    User.transaction do
      if !user
        # No such user in Sonar database
        return nil if !Api::Utils.java_facade.getSettings().getBoolean('sonar.authenticator.createUsers')
        # Automatically create a user in the sonar db if authentication has been successfully done
        user = User.new(:login => username, :name => username, :email => '', :created_at => now, :updated_at => now)
        user.external_identity= username
        user.external_identity_provider= 'sonarqube'
        user.user_local= false

        if details
          user.name = details.getName()
          user.email = details.getEmail()
        end
        default_group_name = Api::Utils.java_facade.getSettings().getString('sonar.defaultGroup')
        default_group = Group.find_by_name(default_group_name)
        if default_group
          user.groups << default_group
        else
          Rails.logger.error("The default user group does not exist: #{default_group_name}. Please check the parameter 'Default user group' in general settings.")
        end
      else
        # Existing user
        if details
          user.name = details.getName()
          user.email = details.getEmail()
        end

        # Update external identity and local info for existing users
        user.external_identity= username
        user.external_identity_provider= 'sonarqube'
        user.user_local= false

        # Force the update of updated_at in order to execute an SQL update to block other session
        user.updated_at = now
      end
      if @save_password
        user.password = password
        user.password_confirmation = password
      end

      # A user that is synchronized with an external system is always set to 'active' (see SONAR-3258 for the deactivation concept)
      user.active=true
      # Note that validation disabled
      user.save(false)

      synchronize_groups(user, servlet_request)
      # Note that validation disabled
      user.save(false)
    end
    # Must be outside the transaction in order to not have a lock on the users table : http://jira.sonarsource.com/browse/SONAR-6726
    user.notify_creation_handlers
    
    Internal.users_api.index()

    # Return user
    user
  end

  def synchronize_groups(user, servlet_request)
    if @java_groups_provider
      begin
        provider_context = org.sonar.api.security.ExternalGroupsProvider::Context.new(user.login, servlet_request)
        groups = @java_groups_provider.doGetGroups(provider_context)
      rescue Exception => e
        Rails.logger.error("Error from external groups provider: #{e.message}")
      else
        if groups
          user.groups = []
          for group_name in groups
            group = Group.find_by_name(group_name)
            if group
              user.groups << group
            end
          end
        end
      end
    end
  end

  def editable_password?
    false
  end
end

#
# Load the realm to use. The server must be restarted when configuration is changed.
#
class RealmFactory
  @@realm = nil

  def self.realm
    if @@realm.nil?
      realm_factory = Api::Utils.java_facade.getCoreComponentByClassname('org.sonar.server.user.SecurityRealmFactory')
      if realm_factory
        component = realm_factory.getRealm()
        @@realm = component ? PluginRealm.new(component) : DefaultRealm.new
      else
        # SecurityRealmFactory is not yet available in pico, for example during db upgrade
      end
    end
    @@realm
  end
end


module NeedAuthentication

  # ForUser module is included by the class User. It redirects authentication to the selected Authenticator.
  module ForUser

    # Stuff directives into including module
    def self.included(recipient)
      recipient.extend(ModelClassMethods)
    end

    module ModelClassMethods
      # Authenticates a user by their login name and unencrypted password.  Returns the user or nil.
      #
      # uff.  this is really an authorization, not authentication routine.
      # We really need a Dispatch Chain here or something.
      # This will also let us return a human error message.
      #
      def authenticate(login, password, servlet_request)
        # Downcase login (typically for Active Directory)
        # Note that login in Sonar DB is case-sensitive, however in this case authentication and automatic user creation will always happen with downcase login
        downcase = Api::Utils.java_facade.getSettings().getBoolean('sonar.authenticator.downcase')
        if login && downcase
          login = login.downcase
        end

        RealmFactory.realm.authenticate?(login, password, servlet_request) if RealmFactory.realm
      end

      def editable_password?
        RealmFactory.realm && RealmFactory.realm.editable_password?
      end
    end
  end
end
