/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.email;

import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.server.ServerSide;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;

@ServerSide
@ComputeEngineSide
public class EmailSmtpConfiguration {
  // Common configuration
  public static final String EMAIL_CONFIG_SMTP_HOST = "email.smtp_host.secured";
  public static final String EMAIL_CONFIG_SMTP_HOST_DEFAULT = "";
  public static final String EMAIL_CONFIG_SMTP_PORT = "email.smtp_port.secured";
  public static final String EMAIL_CONFIG_SMTP_PORT_DEFAULT = "25";
  public static final String EMAIL_CONFIG_SMTP_SECURE_CONNECTION = "email.smtp_secure_connection.secured";
  public static final String EMAIL_CONFIG_SMTP_SECURE_CONNECTION_DEFAULT = "";
  // Email content
  public static final String EMAIL_CONFIG_FROM = "email.from";
  public static final String EMAIL_CONFIG_FROM_DEFAULT = "noreply@nowhere";
  public static final String EMAIL_CONFIG_FROM_NAME = "email.fromName";
  public static final String EMAIL_CONFIG_FROM_NAME_DEFAULT = "SonarQube Server";
  public static final String EMAIL_CONFIG_PREFIX = "email.prefix";
  public static final String EMAIL_CONFIG_PREFIX_DEFAULT = "[SONARQUBE SERVER]";
  // Auth selection
  public static final String EMAIL_CONFIG_SMTP_AUTH_METHOD = "email.smtp.auth.method";
  public static final String EMAIL_CONFIG_SMTP_AUTH_METHOD_DEFAULT = "BASIC";
  // Basic Auth
  public static final String EMAIL_CONFIG_SMTP_USERNAME = "email.smtp_username.secured";
  public static final String EMAIL_CONFIG_SMTP_USERNAME_DEFAULT = "";
  public static final String EMAIL_CONFIG_SMTP_PASSWORD = "email.smtp_password.secured";
  public static final String EMAIL_CONFIG_SMTP_PASSWORD_DEFAULT = "";
  // OAuth
  public static final String EMAIL_CONFIG_SMTP_OAUTH_HOST = "email.smtp.oauth.host";
  public static final String EMAIL_CONFIG_SMTP_OAUTH_HOST_DEFAULT = "https://login.microsoftonline.com";
  public static final String EMAIL_CONFIG_SMTP_OAUTH_CLIENTID = "email.smtp.oauth.clientId";
  public static final String EMAIL_CONFIG_SMTP_OAUTH_CLIENTSECRET = "email.smtp.oauth.clientSecret";
  public static final String EMAIL_CONFIG_SMTP_OAUTH_TENANT = "email.smtp.oauth.tenant";
  public static final String EMAIL_CONFIG_SMTP_OAUTH_SCOPE = "email.smtp.oauth.scope";
  public static final String EMAIL_CONFIG_SMTP_OAUTH_SCOPE_DEFAULT = "https://outlook.office365.com/.default";
  public static final String EMAIL_CONFIG_SMTP_OAUTH_GRANT = "email.smtp.oauth.grant";
  public static final String EMAIL_CONFIG_SMTP_OAUTH_GRANT_DEFAULT = "client_credentials";

  private final DbClient dbClient;

  public EmailSmtpConfiguration(DbClient dbClient) {
    this.dbClient = dbClient;
  }

  public String getSmtpHost() {
    return get(EMAIL_CONFIG_SMTP_HOST, EMAIL_CONFIG_SMTP_HOST_DEFAULT);
  }

  public int getSmtpPort() {
    return Integer.parseInt(get(EMAIL_CONFIG_SMTP_PORT, EMAIL_CONFIG_SMTP_PORT_DEFAULT));
  }

  public String getSecureConnection() {
    return get(EMAIL_CONFIG_SMTP_SECURE_CONNECTION, EMAIL_CONFIG_SMTP_SECURE_CONNECTION_DEFAULT);
  }

  public String getAuthMethod() {
    return get(EMAIL_CONFIG_SMTP_AUTH_METHOD, EMAIL_CONFIG_SMTP_AUTH_METHOD_DEFAULT);
  }

  public String getFrom() {
    return get(EMAIL_CONFIG_FROM, EMAIL_CONFIG_FROM_DEFAULT);
  }

  public String getFromName() {
    return get(EMAIL_CONFIG_FROM_NAME, EMAIL_CONFIG_FROM_NAME_DEFAULT);
  }

  public String getPrefix() {
    return get(EMAIL_CONFIG_PREFIX, EMAIL_CONFIG_PREFIX_DEFAULT);
  }

  public String getSmtpUsername() {
    return get(EMAIL_CONFIG_SMTP_USERNAME, EMAIL_CONFIG_SMTP_USERNAME_DEFAULT);
  }

  public String getSmtpPassword() {
    return get(EMAIL_CONFIG_SMTP_PASSWORD, EMAIL_CONFIG_SMTP_PASSWORD_DEFAULT);
  }

  public String getOAuthHost() {
    return get(EMAIL_CONFIG_SMTP_OAUTH_HOST, EMAIL_CONFIG_SMTP_OAUTH_HOST_DEFAULT);
  }

  public String getOAuthTenant() {
    return get(EMAIL_CONFIG_SMTP_OAUTH_TENANT, "");
  }

  public String getOAuthClientId() {
    return get(EMAIL_CONFIG_SMTP_OAUTH_CLIENTID, "");
  }

  public String getOAuthClientSecret() {
    return get(EMAIL_CONFIG_SMTP_OAUTH_CLIENTSECRET, "");
  }

  public String getOAuthScope() {
    return get(EMAIL_CONFIG_SMTP_OAUTH_SCOPE, EMAIL_CONFIG_SMTP_OAUTH_SCOPE_DEFAULT);
  }

  public String getOAuthGrant() {
    return get(EMAIL_CONFIG_SMTP_OAUTH_GRANT, EMAIL_CONFIG_SMTP_OAUTH_GRANT_DEFAULT);
  }

  private String get(String key, String defaultValue) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      return dbClient.internalPropertiesDao().selectByKey(dbSession, key).orElse(defaultValue);
    }
  }

}
