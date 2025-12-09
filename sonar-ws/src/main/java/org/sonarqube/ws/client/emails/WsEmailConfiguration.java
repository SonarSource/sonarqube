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
package org.sonarqube.ws.client.emails;

public record WsEmailConfiguration(
  String host,
  String port,
  String securityProtocol,
  String fromAddress,
  String fromName,
  String subjectPrefix,
  String authMethod,
  String username,
  String basicPassword,
  String oauthAuthenticationHost,
  String oauthClientId,
  String oauthClientSecret,
  String oauthTenant
  ) {

  public static WsEmailConfigurationBuilder builder() {
    return new WsEmailConfigurationBuilder();
  }

  public static final class WsEmailConfigurationBuilder {
    private String host;
    private String port;
    private String securityProtocol;
    private String fromAddress;
    private String fromName;
    private String subjectPrefix;
    private String authMethod;
    private String username;
    private String basicPassword;
    private String oauthAuthenticationHost;
    private String oauthClientId;
    private String oauthClientSecret;
    private String oauthTenant;

    private WsEmailConfigurationBuilder() {
    }

    public WsEmailConfigurationBuilder host(String host) {
      this.host = host;
      return this;
    }

    public WsEmailConfigurationBuilder port(String port) {
      this.port = port;
      return this;
    }

    public WsEmailConfigurationBuilder securityProtocol(String securityProtocol) {
      this.securityProtocol = securityProtocol;
      return this;
    }

    public WsEmailConfigurationBuilder fromAddress(String fromAddress) {
      this.fromAddress = fromAddress;
      return this;
    }

    public WsEmailConfigurationBuilder fromName(String fromName) {
      this.fromName = fromName;
      return this;
    }

    public WsEmailConfigurationBuilder subjectPrefix(String subjectPrefix) {
      this.subjectPrefix = subjectPrefix;
      return this;
    }

    public WsEmailConfigurationBuilder authMethod(String authMethod) {
      this.authMethod = authMethod;
      return this;
    }

    public WsEmailConfigurationBuilder username(String username) {
      this.username = username;
      return this;
    }

    public WsEmailConfigurationBuilder basicPassword(String basicPassword) {
      this.basicPassword = basicPassword;
      return this;
    }

    public WsEmailConfigurationBuilder oauthAuthenticationHost(String oauthAuthenticationHost) {
      this.oauthAuthenticationHost = oauthAuthenticationHost;
      return this;
    }

    public WsEmailConfigurationBuilder oauthClientId(String oauthClientId) {
      this.oauthClientId = oauthClientId;
      return this;
    }

    public WsEmailConfigurationBuilder oauthClientSecret(String oauthClientSecret) {
      this.oauthClientSecret = oauthClientSecret;
      return this;
    }

    public WsEmailConfigurationBuilder oauthTenant(String oauthTenant) {
      this.oauthTenant = oauthTenant;
      return this;
    }

    public WsEmailConfiguration build() {
      return new WsEmailConfiguration(host, port, securityProtocol, fromAddress, fromName, subjectPrefix, authMethod, username, basicPassword, oauthAuthenticationHost, oauthClientId, oauthClientSecret, oauthTenant);
    }
  }
}
