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
class EmailConfigurationController < ApplicationController

  SECTION=Navigation::SECTION_CONFIGURATION
  before_filter :admin_required

  def index
    @smtp_host = Property.value(configuration::SMTP_HOST, nil, configuration::SMTP_HOST_DEFAULT)
    @smtp_port = Property.value(configuration::SMTP_PORT, nil, configuration::SMTP_PORT_DEFAULT)
    @smtp_use_tls = Property.value(configuration::SMTP_USE_TLS, nil, configuration::SMTP_USE_TLS_DEFAULT) == 'true'
    @smtp_username = Property.value(configuration::SMTP_USERNAME, nil, configuration::SMTP_USERNAME_DEFAULT)
    @smtp_password = Property.value(configuration::SMTP_PASSWORD, nil, configuration::SMTP_PASSWORD_DEFAULT)
    @email_from = Property.value(configuration::FROM, nil, configuration::FROM_DEFAULT)
    @email_prefix = Property.value(configuration::PREFIX, nil, configuration::PREFIX_DEFAULT)
  end

  def save
    Property.set(configuration::SMTP_HOST, params[:smtp_host])
    Property.set(configuration::SMTP_PORT, params[:smtp_port])
    Property.set(configuration::SMTP_USE_TLS, params[:smtp_use_tls] == 'true')
    Property.set(configuration::SMTP_USERNAME, params[:smtp_username])
    Property.set(configuration::SMTP_PASSWORD, params[:smtp_password])
    Property.set(configuration::FROM, params[:email_from])
    Property.set(configuration::PREFIX, params[:email_prefix])
    redirect_to :action => 'index'
  end

  def send_test_email
    to_address = params[:to_address]
    subject = params[:subject]
    message = params[:message]
    if to_address.blank?
      flash[:notice] = message('email_configuration.test.to_address_required')
    else
      begin
        java_facade.getComponentByClassname('emailnotifications', 'org.sonar.plugins.emailnotifications.EmailNotificationChannel').sendTestEmail(to_address, subject, message)
        flash[:notice] = message('email_configuration.test.email_was_sent_to_x', :params => [to_address])
      rescue Exception => e
        flash[:error] = e.message
      end
    end
    redirect_to :action => 'index'
  end

  private

  def configuration
    java_facade.getComponentByClassname('emailnotifications', 'org.sonar.plugins.emailnotifications.EmailConfiguration').class
  end

end
