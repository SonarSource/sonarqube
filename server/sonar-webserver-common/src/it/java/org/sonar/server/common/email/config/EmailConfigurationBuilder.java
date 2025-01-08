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

public final class EmailConfigurationBuilder {
  private String id;
  private String host;
  private String port;
  private EmailConfigurationSecurityProtocol securityProtocol;
  private String fromAddress;
  private String fromName;
  private String subjectPrefix;
  private EmailConfigurationAuthMethod authMethod;
  private String username;
  private String basicPassword;
  private String oauthAuthenticationHost;
  private String oauthClientId;
  private String oauthClientSecret;
  private String oauthTenant;

  public static EmailConfigurationBuilder builder() {
    return new EmailConfigurationBuilder();
  }

  private EmailConfigurationBuilder() {
  }

  public EmailConfigurationBuilder id(String id) {
    this.id = id;
    return this;
  }

  public EmailConfigurationBuilder host(String host) {
    this.host = host;
    return this;
  }

  public EmailConfigurationBuilder port(String port) {
    this.port = port;
    return this;
  }

  public EmailConfigurationBuilder securityProtocol(EmailConfigurationSecurityProtocol securityProtocol) {
    this.securityProtocol = securityProtocol;
    return this;
  }

  public EmailConfigurationBuilder fromAddress(String fromAddress) {
    this.fromAddress = fromAddress;
    return this;
  }

  public EmailConfigurationBuilder fromName(String fromName) {
    this.fromName = fromName;
    return this;
  }

  public EmailConfigurationBuilder subjectPrefix(String subjectPrefix) {
    this.subjectPrefix = subjectPrefix;
    return this;
  }

  public EmailConfigurationBuilder authMethod(EmailConfigurationAuthMethod authMethod) {
    this.authMethod = authMethod;
    return this;
  }

  public EmailConfigurationBuilder username(String username) {
    this.username = username;
    return this;
  }

  public EmailConfigurationBuilder basicPassword(String basicPassword) {
    this.basicPassword = basicPassword;
    return this;
  }

  public EmailConfigurationBuilder oauthAuthenticationHost(String oauthAuthenticationHost) {
    this.oauthAuthenticationHost = oauthAuthenticationHost;
    return this;
  }

  public EmailConfigurationBuilder oauthClientId(String oauthClientId) {
    this.oauthClientId = oauthClientId;
    return this;
  }

  public EmailConfigurationBuilder oauthClientSecret(String oauthClientSecret) {
    this.oauthClientSecret = oauthClientSecret;
    return this;
  }

  public EmailConfigurationBuilder oauthTenant(String oauthTenant) {
    this.oauthTenant = oauthTenant;
    return this;
  }

  public EmailConfiguration build() {
    return new EmailConfiguration(id, host, port, securityProtocol, fromAddress, fromName, subjectPrefix, authMethod, username, basicPassword, oauthAuthenticationHost,
      oauthClientId, oauthClientSecret, oauthTenant);
  }
}
