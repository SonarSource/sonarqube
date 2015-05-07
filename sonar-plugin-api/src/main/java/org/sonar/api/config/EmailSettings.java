/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.api.config;

import com.google.common.base.Objects;
import org.sonar.api.BatchSide;
import org.sonar.api.CoreProperties;
import org.sonar.api.ServerSide;

/**
 * If batch extensions use this component, then batch must be executed with administrator rights (see properties sonar.login and sonar.password)
 *
 * @since 3.2
 */
@BatchSide
@ServerSide
public class EmailSettings {
  public static final String SMTP_HOST = "email.smtp_host.secured";
  public static final String SMTP_HOST_DEFAULT = "";
  public static final String SMTP_PORT = "email.smtp_port.secured";
  public static final String SMTP_PORT_DEFAULT = "25";
  public static final String SMTP_SECURE_CONNECTION = "email.smtp_secure_connection.secured";
  public static final String SMTP_SECURE_CONNECTION_DEFAULT = "";
  public static final String SMTP_USERNAME = "email.smtp_username.secured";
  public static final String SMTP_USERNAME_DEFAULT = "";
  public static final String SMTP_PASSWORD = "email.smtp_password.secured";
  public static final String SMTP_PASSWORD_DEFAULT = "";
  public static final String FROM = "email.from";
  public static final String FROM_DEFAULT = "noreply@nowhere";
  public static final String PREFIX = "email.prefix";
  public static final String PREFIX_DEFAULT = "[SONARQUBE]";

  private final Settings settings;

  public EmailSettings(Settings settings) {
    this.settings = settings;
  }

  public String getSmtpHost() {
    return get(SMTP_HOST, SMTP_HOST_DEFAULT);
  }

  public int getSmtpPort() {
    return Integer.parseInt(get(SMTP_PORT, SMTP_PORT_DEFAULT));
  }

  public String getSecureConnection() {
    return get(SMTP_SECURE_CONNECTION, SMTP_SECURE_CONNECTION_DEFAULT);
  }

  public String getSmtpUsername() {
    return get(SMTP_USERNAME, SMTP_USERNAME_DEFAULT);
  }

  public String getSmtpPassword() {
    return get(SMTP_PASSWORD, SMTP_PASSWORD_DEFAULT);
  }

  public String getFrom() {
    return get(FROM, FROM_DEFAULT);
  }

  public String getPrefix() {
    return get(PREFIX, PREFIX_DEFAULT);
  }

  public String getServerBaseURL() {
    return get(CoreProperties.SERVER_BASE_URL, CoreProperties.SERVER_BASE_URL_DEFAULT_VALUE);
  }

  private String get(String key, String defaultValue) {
    return Objects.firstNonNull(settings.getString(key), defaultValue);
  }
}
