#
# Sonar, entreprise quality control tool.
# Copyright (C) 2008-2011 SonarSource
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
class DefaultAuthenticator
  def authenticate?(login, password)
    return false if login.blank? || password.blank?
    user=User.find_by_login(login)
    user && user.authenticated?(password)
  end

  def editable_password?
    true
  end
end


#
# Use an external system to authenticate users, for example LDAP. See the Java extension point org.sonar.api.security.LoginPasswordAuthenticator.
#
class PluginAuthenticator
  def initialize(java_authenticator)
    @java_authenticator=java_authenticator
  end

  def authenticate?(login, password)
    login.present? && password.present? && @java_authenticator.authenticate(login, password)
  end

  def editable_password?
    false
  end
end

#
# Since 2.14
# Experimental
#
# Use an external system to authenticate users with fallback to Sonar database.
#
class FallbackAuthenticator
  def initialize(java_authenticator)
    @java_authenticator = java_authenticator
  end

  def authenticate?(login, password)
    return false if login.blank? || password.blank?
    if @java_authenticator.authenticate(login, password)
      return true
    end
    # Fallback to password in Sonar Database
    user = User.find_by_login(login)
    return user && user.authenticated?(password)
  end

  def editable_password?
    true
  end
end

#
# Load the authentication system to use. The server must be restarted when configuration is changed.
#
class AuthenticatorFactory
  @@authenticator = nil
  @@users_provider = nil

  def self.authenticator
    if @@authenticator.nil?
      authenticator_factory=Java::OrgSonarServerUi::JRubyFacade.new.getCoreComponentByClassname('org.sonar.server.ui.AuthenticatorFactory')
      component=authenticator_factory.getAuthenticator()
      @@authenticator=(component ? FallbackAuthenticator.new(component) : DefaultAuthenticator.new)
      @@users_provider = (component ? authenticator_factory.getUsersProvider() : nil)
    end
    @@authenticator
  end

  def self.users_provider
    @@users_provider
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
        return nil if login.blank?
        java_facade = Java::OrgSonarServerUi::JRubyFacade.new

        # Downcase login (typically for Active Directory)
        # Note that login in Sonar DB is case-sensitive, however in this case authentication and automatic user creation will always happen with downcase login
        downcase = java_facade.getSettings().getBoolean('sonar.authenticator.downcase')
        if downcase
          login = login.downcase
        end

        user = User.find_by_login(login)
        if user
          # User exists
          return nil if !AuthenticatorFactory.authenticator.authenticate?(login, password)
          # Password correct

          users_provider = AuthenticatorFactory.users_provider
          if users_provider
            # Sync details
            details = AuthenticatorFactory.users_provider.doGetUserDetails(login)
            user.update_attributes(:name => details.getName(), :email => details.getEmail(), :password => password, :password_confirmation => password)
            # TODO log if unable to save?
            user.save
          end
        else
          # User not found
          return nil if !AuthenticatorFactory.authenticator.authenticate?(login, password)
          # Password correct

          # Automatically create a user in the sonar db if authentication has been successfully done
          create_user = java_facade.getSettings().getBoolean('sonar.authenticator.createUsers')
          users_provider = AuthenticatorFactory.users_provider
          if create_user && users_provider
            details = users_provider.doGetUserDetails(login)
            user = User.new(:login => login, :name => details.getName(), :email => details.getEmail(), :password => password, :password_confirmation => password)
            default_group_name = java_facade.getSettings().getString('sonar.defaultGroup')
            default_group=Group.find_by_name(default_group_name)
            if default_group
              user.groups<<default_group
              user.save
            else
              logger.error("The default user group does not exist: #{default_group_name}. Please check the parameter 'Default user group' in general settings.")
            end
          end
        end

        return user
      end

      def editable_password?
        AuthenticatorFactory.authenticator.editable_password?
      end
    end
  end
end
