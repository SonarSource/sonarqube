/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2024 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.email;

import org.apache.commons.configuration.Configuration;
import org.sonar.api.CoreProperties;
import org.sonar.api.ServerExtension;

/**
 * Ruby uses constants from this class.
 * 
 * @since 2.10
 */
public class EmailConfiguration implements ServerExtension {

  public static final String SMTP_HOST = "email.smtp_host";
  public static final String SMTP_HOST_DEFAULT = "";
  public static final String SMTP_PORT = "email.smtp_port";
  public static final String SMTP_PORT_DEFAULT = "25";
  public static final String SMTP_USE_TLS = "email.smtp_use_tls";
  public static final boolean SMTP_USE_TLS_DEFAULT = false;
  public static final String SMTP_USERNAME = "email.smtp_username";
  public static final String SMTP_USERNAME_DEFAULT = "";
  public static final String SMTP_PASSWORD = "email.smtp_password";
  public static final String SMTP_PASSWORD_DEFAULT = "";
  public static final String FROM = "email.from";
  public static final String FROM_DEFAULT = "noreply@nowhere";
  public static final String PREFIX = "email.prefix";
  public static final String PREFIX_DEFAULT = "[SONAR]";

  private Configuration configuration;

  public EmailConfiguration(Configuration configuration) {
    this.configuration = configuration;
  }

  public String getSmtpHost() {
    return configuration.getString(SMTP_HOST, SMTP_HOST_DEFAULT);
  }

  public String getSmtpPort() {
    return configuration.getString(SMTP_PORT, SMTP_PORT_DEFAULT);
  }

  public boolean isUseTLS() {
    return configuration.getBoolean(SMTP_USE_TLS, SMTP_USE_TLS_DEFAULT);
  }

  public String getSmtpUsername() {
    return configuration.getString(SMTP_USERNAME, SMTP_USERNAME_DEFAULT);
  }

  public String getSmtpPassword() {
    return configuration.getString(SMTP_PASSWORD, SMTP_PASSWORD_DEFAULT);
  }

  public String getFrom() {
    return configuration.getString(FROM, FROM_DEFAULT);
  }

  public String getPrefix() {
    return configuration.getString(PREFIX, PREFIX_DEFAULT);
  }

  public String getServerBaseURL() {
    return configuration.getString(CoreProperties.SERVER_BASE_URL, CoreProperties.SERVER_BASE_URL_DEFAULT_VALUE);
  }

}
