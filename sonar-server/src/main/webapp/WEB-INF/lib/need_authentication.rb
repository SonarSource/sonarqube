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
  def authenticate?(login, password)
    return false if login.blank? || password.blank?
    user = User.find_by_login(login)
    user && user.authenticated?(password)
  end

  def synchronize(user, password)
    # nothing to do
  end

  def editable_password?
    true
  end
end

#
# Use an external security system with fallback to Sonar database.
# See the Java extension point org.sonar.api.security.Realm
#
class PluginRealm
  def initialize(java_realm)
    @java_authenticator = java_realm.getAuthenticator()
    @java_users_provider = java_realm.getUsersProvider()
  end

  def authenticate?(login, password)
    return false if login.blank? || password.blank?
    begin
      if @java_authenticator.authenticate(login, password)
        return true
      end
    rescue Exception => e
      Java::OrgSonarServerUi::JRubyFacade.new.logError("Error from external authenticator: #{e.message}")
    end
    # Fallback to password from Sonar Database
    user = User.find_by_login(login)
    return user && user.authenticated?(password)
  end

  def synchronize(user, password)
    if @java_users_provider
      begin
        details = @java_users_provider.doGetUserDetails(user.login)
      rescue Exception => e
        Java::OrgSonarServerUi::JRubyFacade.new.logError("Error from external users provider: #{e.message}")
      else
        if details
          user.update_attributes(:name => details.getName(), :email => details.getEmail(), :password => password, :password_confirmation => password)
          user.save
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
      realm_factory = Java::OrgSonarServerUi::JRubyFacade.new.getCoreComponentByClassname('org.sonar.server.ui.RealmFactory')
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
          return nil if !RealmFactory.realm.authenticate?(login, password)
          # Password correct

          # Synchronize details
          RealmFactory.realm.synchronize(user, password)
        else
          # User not found
          return nil if !RealmFactory.realm.authenticate?(login, password)
          # Password correct

          # Automatically create a user in the sonar db if authentication has been successfully done
          create_user = java_facade.getSettings().getBoolean('sonar.authenticator.createUsers')
          if create_user
            user = User.new(:login => login, :name => login, :email => '', :password => password, :password_confirmation => password)
            default_group_name = java_facade.getSettings().getString('sonar.defaultGroup')
            default_group = Group.find_by_name(default_group_name)
            if default_group
              user.groups<<default_group
              user.save
            else
              logger.error("The default user group does not exist: #{default_group_name}. Please check the parameter 'Default user group' in general settings.")
            end

            # Synchronize details
            RealmFactory.realm.synchronize(user, password)
          end
        end
        return user
      end

      def editable_password?
        RealmFactory.realm.editable_password?
      end
    end
  end
end
