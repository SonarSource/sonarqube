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
package org.sonar.server.v2.api.email.config.resource;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.annotation.Nullable;

public record EmailConfigurationResource(

  @Schema(accessMode = Schema.AccessMode.READ_ONLY)
  String id,

  @Schema(description = "URL of your SMTP server")
  String host,

  @Schema(description = "Port of your SMTP server (usually 25, 587 or 465)")
  String port,

  @Schema(description = "Security protocol used to connect to your SMTP server (SSLTLS is recommended)")
  EmailConfigurationSecurityProtocol securityProtocol,

  @Schema(description = "Address emails will come from")
  String fromAddress,

  @Schema(description = "Name emails will come from (usually \"SonarQube\")")
  String fromName,

  @Schema(description = "Prefix added to email so they can be easily recognized (usually \"[SonarQube]\")")
  String subjectPrefix,

  @Schema(description = "Authentication method used to connect to the SMTP server. OAuth is only supported for Microsoft Exchange")
  EmailConfigurationAuthMethod authMethod,

  @Nullable
  @Schema(description = "For Basic and OAuth authentication: username used to authenticate to the SMTP server")
  String username,

  @Schema(description = "For Basic authentication: has the password field been set?")
  boolean isBasicPasswordSet,

  @Nullable
  @Schema(description = "For OAuth authentication: host of the Identity Provider issuing access tokens")
  String oauthAuthenticationHost,

  @Schema(description = "For OAuth authentication: has the Client ID field been set?")
  boolean isOauthClientIdSet,

  @Schema(description = "For OAuth authentication: has the Client secret field been set?")
  boolean isOauthClientSecretSet,

  @Nullable
  @Schema(description = "For OAuth authentication: Microsoft tenant")
  String oauthTenant

) {
}

