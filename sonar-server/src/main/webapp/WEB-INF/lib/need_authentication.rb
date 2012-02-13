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

#
# Use Sonar database (table USERS) to authenticate users.
#
class DefaultRealm
  def authenticate?(username, password)
    user = User.find_active_by_login(username)
    if user && user.authenticated?(password)
      return user
    else
      return nil
    end
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
    @java_authenticator = java_realm.getLoginPasswordAuthenticator()
    @java_users_provider = java_realm.getUsersProvider()
    @java_groups_provider = java_realm.getGroupsProvider()

    @save_password = Java::OrgSonarServerUi::JRubyFacade.new.getSettings().getBoolean('sonar.security.savePassword')
  end

  def authenticate?(username, password)
    if @java_users_provider
      begin
        details = @java_users_provider.doGetUserDetails(username)
      rescue Exception => e
        Rails.logger.error("Error from external users provider: #{e.message}")
        return false if !@save_password
        return fallback(username, password)
      else
        # User exist in external system
        return auth(username, password, details) if details
        # No such user in external system
        return fallback(username, password)
      end
    else
      # Legacy authenticator
      return auth(username, password, nil)
    end
  end

  #
  # Fallback to password from Sonar Database
  #
  def fallback(username, password)
    user = User.find_active_by_login(username)
    if user && user.authenticated?(password)
      return user
    else
      return nil
    end
  end

  #
  # Authenticate user using external system
  #
  def auth(username, password, details)
    if @java_authenticator
      begin
        status = @java_authenticator.authenticate(username, password)
      rescue Exception => e
        Rails.logger.error("Error from external authenticator: #{e.message}")
        return fallback(username, password)
      else
        return nil if !status
        # Authenticated
        return synchronize(username, password, details)
      end
    else
      # No authenticator
      return nil
    end
  end

  #
  # Authentication in external system was successful - replicate password, details and groups into Sonar
  #
  def synchronize(username, password, details)
    user = User.find_by_login(username)
    if !user
      # No such user in Sonar database
      java_facade = Java::OrgSonarServerUi::JRubyFacade.new
      return nil if !java_facade.getSettings().getBoolean('sonar.authenticator.createUsers')
      # Automatically create a user in the sonar db if authentication has been successfully done
      user = User.new(:login => username, :name => username, :email => '')
      default_group_name = java_facade.getSettings().getString('sonar.defaultGroup')
      default_group = Group.find_by_name(default_group_name)
      if default_group
        user.groups << default_group
      else
        Rails.logger.error("The default user group does not exist: #{default_group_name}. Please check the parameter 'Default user group' in general settings.")
      end
    end
    if details
      user.name = details.getName()
      user.email = details.getEmail()
    end
    if @save_password
      user.password = password
      user.password_confirmation = password
    end
    synchronize_groups(user)
    # A user that is synchronized with an external system is always set to 'active' (see SONAR-3258 for the deactivation concept)
    user.active=true
    # Note that validation disabled
    user.save(false)
    return user
  end

  def synchronize_groups(user)
    if @java_groups_provider
      begin
        groups = @java_groups_provider.doGetGroups(user.login)
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
      realm_factory = Java::OrgSonarServerUi::JRubyFacade.new.getCoreComponentByClassname('org.sonar.server.ui.SecurityRealmFactory')
      component = realm_factory.getRealm()
      @@realm = component ? PluginRealm.new(component) : DefaultRealm.new
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
      def authenticate(login, password)
        return nil if login.blank? || password.blank?

        # Downcase login (typically for Active Directory)
        # Note that login in Sonar DB is case-sensitive, however in this case authentication and automatic user creation will always happen with downcase login
        downcase = Java::OrgSonarServerUi::JRubyFacade.new.getSettings().getBoolean('sonar.authenticator.downcase')
        if downcase
          login = login.downcase
        end

        return RealmFactory.realm.authenticate?(login, password)
      end

      def editable_password?
        RealmFactory.realm.editable_password?
      end
    end
  end
end
