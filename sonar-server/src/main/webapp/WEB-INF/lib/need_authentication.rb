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
# Load the authentication system to use. The server must be restarted when configuration is changed.
#
class AuthenticatorFactory
  @@authenticator = nil

  def self.authenticator
    if @@authenticator.nil?
      authenticator_factory=Java::OrgSonarServerUi::JRubyFacade.new.getCoreComponentByClassname('org.sonar.server.ui.AuthenticatorFactory')
      component=authenticator_factory.getAuthenticator()
      @@authenticator=(component ? PluginAuthenticator.new(component) : DefaultAuthenticator.new)
    end
    @@authenticator
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
        return nil if !AuthenticatorFactory.authenticator.authenticate?(login, password)
        user = User.find_by_login(login)

        # Automatically create a user in the sonar db if authentication has been successfully done
        java_facade = Java::OrgSonarServerUi::JRubyFacade.new
        create_user = java_facade.getConfigurationValue('sonar.authenticator.createUsers');
        if !user && create_user=='true'
          user=User.new(:login => login, :name => login, :email => '', :password => password, :password_confirmation => password)
          user.save!

          default_group_name = java_facade.getConfigurationValue('sonar.defaultGroup') || 'sonar-users';
          default_group=Group.find_by_name(default_group_name)
          if default_group
            user.groups<<default_group
            user.save
          else
            logger.error("The default user group does not exist: #{default_group_name}. Please check the parameter 'Default user group' in general settings.")
          end
        end

        user
      end

      def editable_password?
        AuthenticatorFactory.authenticator.editable_password?
      end
    end
  end
end
