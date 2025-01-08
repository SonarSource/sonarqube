/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.server.common.email.config;

import javax.annotation.Nullable;

import static org.sonar.server.email.EmailSmtpConfiguration.EMAIL_CONFIG_SMTP_OAUTH_GRANT_DEFAULT;
import static org.sonar.server.email.EmailSmtpConfiguration.EMAIL_CONFIG_SMTP_OAUTH_SCOPE_DEFAULT;

public record EmailConfiguration(
  String id,
  String host,
  String port,
  EmailConfigurationSecurityProtocol securityProtocol,
  String fromAddress,
  String fromName,
  String subjectPrefix,
  EmailConfigurationAuthMethod authMethod,
  String username,
  @Nullable String basicPassword,
  @Nullable String oauthAuthenticationHost,
  @Nullable String oauthClientId,
  @Nullable String oauthClientSecret,
  @Nullable String oauthTenant,
  @Nullable String oauthScope,
  @Nullable String oauthGrant
) {

  public EmailConfiguration(String id, String host, String port, EmailConfigurationSecurityProtocol securityProtocol, String fromAddress, String fromName, String subjectPrefix,
    EmailConfigurationAuthMethod authMethod, String username, @Nullable String basicPassword, @Nullable String oauthAuthenticationHost,
    @Nullable String oauthClientId, @Nullable String oauthClientSecret, @Nullable String oauthTenant) {
    this(id, host, port, securityProtocol, fromAddress, fromName, subjectPrefix, authMethod, username, basicPassword, oauthAuthenticationHost, oauthClientId,
      oauthClientSecret, oauthTenant, EMAIL_CONFIG_SMTP_OAUTH_SCOPE_DEFAULT, EMAIL_CONFIG_SMTP_OAUTH_GRANT_DEFAULT);
  }
}
