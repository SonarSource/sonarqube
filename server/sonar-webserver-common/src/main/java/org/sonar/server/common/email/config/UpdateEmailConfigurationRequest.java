/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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

import org.sonar.server.common.NonNullUpdatedValue;

public record UpdateEmailConfigurationRequest(
  String emailConfigurationId,
  NonNullUpdatedValue<String> host,
  NonNullUpdatedValue<String> port,
  NonNullUpdatedValue<EmailConfigurationSecurityProtocol> securityProtocol,
  NonNullUpdatedValue<String> fromAddress,
  NonNullUpdatedValue<String> fromName,
  NonNullUpdatedValue<String> subjectPrefix,
  NonNullUpdatedValue<EmailConfigurationAuthMethod> authMethod,
  NonNullUpdatedValue<String> username,
  NonNullUpdatedValue<String> basicPassword,
  NonNullUpdatedValue<String> oauthAuthenticationHost,
  NonNullUpdatedValue<String> oauthClientId,
  NonNullUpdatedValue<String> oauthClientSecret,
  NonNullUpdatedValue<String> oauthTenant
) {

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private String emailConfigurationId;
    private NonNullUpdatedValue<String> host;
    private NonNullUpdatedValue<String> port;
    private NonNullUpdatedValue<EmailConfigurationSecurityProtocol> securityProtocol;
    private NonNullUpdatedValue<String> fromAddress;
    private NonNullUpdatedValue<String> fromName;
    private NonNullUpdatedValue<String> subjectPrefix;
    private NonNullUpdatedValue<EmailConfigurationAuthMethod> authMethod;
    private NonNullUpdatedValue<String> username;
    private NonNullUpdatedValue<String> basicPassword;
    private NonNullUpdatedValue<String> oauthAuthenticationHost;
    private NonNullUpdatedValue<String> oauthClientId;
    private NonNullUpdatedValue<String> oauthClientSecret;
    private NonNullUpdatedValue<String> oauthTenant;

    private Builder() {
    }

    public Builder emailConfigurationId(String emailConfigurationId) {
      this.emailConfigurationId = emailConfigurationId;
      return this;
    }

    public Builder host(NonNullUpdatedValue<String> host) {
      this.host = host;
      return this;
    }

    public Builder port(NonNullUpdatedValue<String> port) {
      this.port = port;
      return this;
    }

    public Builder securityProtocol(NonNullUpdatedValue<EmailConfigurationSecurityProtocol> securityProtocol) {
      this.securityProtocol = securityProtocol;
      return this;
    }

    public Builder fromAddress(NonNullUpdatedValue<String> fromAddress) {
      this.fromAddress = fromAddress;
      return this;
    }

    public Builder fromName(NonNullUpdatedValue<String> fromName) {
      this.fromName = fromName;
      return this;
    }

    public Builder subjectPrefix(NonNullUpdatedValue<String> subjectPrefix) {
      this.subjectPrefix = subjectPrefix;
      return this;
    }

    public Builder authMethod(NonNullUpdatedValue<EmailConfigurationAuthMethod> authMethod) {
      this.authMethod = authMethod;
      return this;
    }

    public Builder username(NonNullUpdatedValue<String> username) {
      this.username = username;
      return this;
    }

    public Builder basicPassword(NonNullUpdatedValue<String> basicPassword) {
      this.basicPassword = basicPassword;
      return this;
    }

    public Builder oauthAuthenticationHost(NonNullUpdatedValue<String> oauthAuthenticationHost) {
      this.oauthAuthenticationHost = oauthAuthenticationHost;
      return this;
    }

    public Builder oauthClientId(NonNullUpdatedValue<String> oauthClientId) {
      this.oauthClientId = oauthClientId;
      return this;
    }

    public Builder oauthClientSecret(NonNullUpdatedValue<String> oauthClientSecret) {
      this.oauthClientSecret = oauthClientSecret;
      return this;
    }

    public Builder oauthTenant(NonNullUpdatedValue<String> oauthTenant) {
      this.oauthTenant = oauthTenant;
      return this;
    }

    public UpdateEmailConfigurationRequest build() {
      return new UpdateEmailConfigurationRequest(emailConfigurationId, host, port, securityProtocol, fromAddress, fromName, subjectPrefix, authMethod, username,
        basicPassword, oauthAuthenticationHost, oauthClientId, oauthClientSecret, oauthTenant);
    }
  }
}
