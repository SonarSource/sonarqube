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
package org.sonar.server.v2.api.email.config.request;

import io.swagger.v3.oas.annotations.media.Schema;
import org.sonar.server.v2.api.email.config.resource.EmailConfigurationAuthMethod;
import org.sonar.server.v2.api.email.config.resource.EmailConfigurationSecurityProtocol;
import org.sonar.server.v2.common.model.NullOrNotEmpty;
import org.sonar.server.v2.common.model.UpdateField;

public class EmailConfigurationUpdateRestRequest {

  private UpdateField<@NullOrNotEmpty String> host = UpdateField.undefined();
  private UpdateField<@NullOrNotEmpty String> port = UpdateField.undefined();
  private UpdateField<EmailConfigurationSecurityProtocol> securityProtocol = UpdateField.undefined();
  private UpdateField<@NullOrNotEmpty String> fromAddress = UpdateField.undefined();
  private UpdateField<@NullOrNotEmpty String> fromName = UpdateField.undefined();
  private UpdateField<@NullOrNotEmpty String> subjectPrefix = UpdateField.undefined();
  private UpdateField<EmailConfigurationAuthMethod> authMethod = UpdateField.undefined();
  private UpdateField<@NullOrNotEmpty String> username = UpdateField.undefined();
  private UpdateField<String> basicPassword = UpdateField.undefined();
  private UpdateField<String> oauthAuthenticationHost = UpdateField.undefined();
  private UpdateField<String> oauthClientId = UpdateField.undefined();
  private UpdateField<String> oauthClientSecret = UpdateField.undefined();
  private UpdateField<String> oauthTenant = UpdateField.undefined();

  @Schema(implementation = String.class, description = "URL of your SMTP server")
  public UpdateField<String> getHost() {
    return host;
  }

  public void setHost(String host) {
    this.host = UpdateField.withValue(host);
  }

  @Schema(implementation = String.class, description = "Port of your SMTP server (usually 25, 587 or 465)")
  public UpdateField<String> getPort() {
    return port;
  }

  public void setPort(String port) {
    this.port = UpdateField.withValue(port);
  }

  @Schema(implementation = EmailConfigurationSecurityProtocol.class, description = "Security protocol used to connect to your SMTP server (SSLTLS is recommended)")
  public UpdateField<EmailConfigurationSecurityProtocol> getSecurityProtocol() {
    return securityProtocol;
  }

  public void setSecurityProtocol(EmailConfigurationSecurityProtocol securityProtocol) {
    this.securityProtocol = UpdateField.withValue(securityProtocol);
  }

  @Schema(implementation = String.class, description = "Address emails will come from")
  public UpdateField<String> getFromAddress() {
    return fromAddress;
  }

  public void setFromAddress(String fromAddress) {
    this.fromAddress = UpdateField.withValue(fromAddress);
  }

  @Schema(implementation = String.class, description = "Name emails will come from (usually \"SonarQube\")")
  public UpdateField<String> getFromName() {
    return fromName;
  }

  public void setFromName(String fromName) {
    this.fromName = UpdateField.withValue(fromName);
  }

  @Schema(implementation = String.class, description = "Prefix added to email so they can be easily recognized (usually \"[SonarQube]\")")
  public UpdateField<String> getSubjectPrefix() {
    return subjectPrefix;
  }

  public void setSubjectPrefix(String subjectPrefix) {
    this.subjectPrefix = UpdateField.withValue(subjectPrefix);
  }

  @Schema(implementation = EmailConfigurationAuthMethod.class,
    description = "Authentication method used to connect to the SMTP server. OAuth is only supported for Microsoft Exchange")
  public UpdateField<EmailConfigurationAuthMethod> getAuthMethod() {
    return authMethod;
  }

  public void setAuthMethod(EmailConfigurationAuthMethod authMethod) {
    this.authMethod = UpdateField.withValue(authMethod);
  }

  @Schema(implementation = String.class, description = "For Basic and OAuth authentication: username used to authenticate to the SMTP server")
  public UpdateField<String> getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = UpdateField.withValue(username);
  }

  @Schema(implementation = String.class, description = "For basic authentication: password used to authenticate to the SMTP server")
  public UpdateField<String> getBasicPassword() {
    return basicPassword;
  }

  public void setBasicPassword(String basicPassword) {
    this.basicPassword = UpdateField.withValue(basicPassword);
  }

  @Schema(implementation = String.class, description = "For OAuth authentication: host of the Identity Provider issuing access tokens")
  public UpdateField<String> getOauthAuthenticationHost() {
    return oauthAuthenticationHost;
  }

  public void setOauthAuthenticationHost(String oauthAuthenticationHost) {
    this.oauthAuthenticationHost = UpdateField.withValue(oauthAuthenticationHost);
  }

  @Schema(implementation = String.class, description = "For OAuth authentication: Client ID provided by Microsoft Exchange when registering the application")
  public UpdateField<String> getOauthClientId() {
    return oauthClientId;
  }

  public void setOauthClientId(String oauthClientId) {
    this.oauthClientId = UpdateField.withValue(oauthClientId);
  }

  @Schema(implementation = String.class, description = "For OAuth authentication: Client password provided by Microsoft Exchange when registering the application")
  public UpdateField<String> getOauthClientSecret() {
    return oauthClientSecret;
  }

  public void setOauthClientSecret(String oauthClientSecret) {
    this.oauthClientSecret = UpdateField.withValue(oauthClientSecret);
  }

  @Schema(implementation = String.class, description = "For OAuth authentication: Microsoft tenant")
  public UpdateField<String> getOauthTenant() {
    return oauthTenant;
  }

  public void setOauthTenant(String oauthTenant) {
    this.oauthTenant = UpdateField.withValue(oauthTenant);
  }

}
