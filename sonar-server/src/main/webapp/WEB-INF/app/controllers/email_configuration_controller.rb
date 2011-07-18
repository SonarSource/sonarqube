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

  EmailConfiguration = org.sonar.server.notifications.email.EmailConfiguration

  def index
    @smtp_host = Property.value(EmailConfiguration::SMTP_HOST, nil, EmailConfiguration::SMTP_HOST_DEFAULT)
    @smtp_port = Property.value(EmailConfiguration::SMTP_PORT, nil, EmailConfiguration::SMTP_PORT_DEFAULT)
    @smtp_use_tls = Property.value(EmailConfiguration::SMTP_USE_TLS, nil, EmailConfiguration::SMTP_USE_TLS_DEFAULT) == 'true'
    @smtp_username = Property.value(EmailConfiguration::SMTP_USERNAME, nil, EmailConfiguration::SMTP_USERNAME_DEFAULT)
    @smtp_password = Property.value(EmailConfiguration::SMTP_PASSWORD, nil, EmailConfiguration::SMTP_PASSWORD_DEFAULT)
    @email_from = Property.value(EmailConfiguration::FROM, nil, EmailConfiguration::FROM_DEFAULT)
    @email_prefix = Property.value(EmailConfiguration::PREFIX, nil, EmailConfiguration::PREFIX_DEFAULT)
  end

  def save
    Property.set(EmailConfiguration::SMTP_HOST, params[:smtp_host])
    Property.set(EmailConfiguration::SMTP_PORT, params[:smtp_port])
    Property.set(EmailConfiguration::SMTP_USE_TLS, params[:smtp_use_tls] == 'true')
    Property.set(EmailConfiguration::SMTP_USERNAME, params[:smtp_username])
    Property.set(EmailConfiguration::SMTP_PASSWORD, params[:smtp_password])
    Property.set(EmailConfiguration::FROM, params[:email_from])
    Property.set(EmailConfiguration::PREFIX, params[:email_prefix])
    redirect_to :action => 'index'
  end

  def send_test_email
    to_address = params[:to_address]
    subject = params[:subject]
    message = params[:message]
    if to_address.blank?
      flash[:notice] = 'You must provide address where to send test email'
    else
      begin
        java_facade.getCoreComponentByClassname('org.sonar.server.notifications.email.EmailNotificationChannel').sendTestEmail(to_address, subject, message)
      rescue Exception => e
        flash[:error] = e.message
      end
    end
    redirect_to :action => 'index'
  end

end
