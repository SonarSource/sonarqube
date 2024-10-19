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
package org.sonar.server.v2.api.email.config.request;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.annotation.Nullable;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import org.sonar.server.v2.api.email.config.resource.EmailConfigurationAuthMethod;
import org.sonar.server.v2.api.email.config.resource.EmailConfigurationSecurityProtocol;

public record EmailConfigurationCreateRestRequest(

  @NotEmpty
  @Schema(description = "URL of your SMTP server")
  String host,

  @NotEmpty
  @Schema(description = "Port of your SMTP server (usually 25, 587 or 465)")
  String port,

  @NotNull
  @Schema(description = "Security protocol used to connect to your SMTP server (SSLTLS is recommended)")
  EmailConfigurationSecurityProtocol securityProtocol,

  @NotEmpty
  @Schema(description = "Address emails will come from")
  String fromAddress,

  @NotEmpty
  @Schema(description = "Name emails will come from (usually \"SonarQube\")")
  String fromName,

  @NotEmpty
  @Schema(description = "Prefix added to email so they can be easily recognized (usually \"[SonarQube]\")")
  String subjectPrefix,

  @NotNull
  @Schema(description = "Authentication method used to connect to the SMTP server. OAuth is only supported for Microsoft Exchange")
  EmailConfigurationAuthMethod authMethod,

  @NotEmpty
  @Schema(description = "For Basic and OAuth authentication: username used to authenticate to the SMTP server")
  String username,

  @Nullable
  @Schema(accessMode = Schema.AccessMode.WRITE_ONLY, description = "For basic authentication: password used to authenticate to the SMTP server")
  String basicPassword,

  @Nullable
  @Schema(description = "For OAuth authentication: host of the Identity Provider issuing access tokens")
  String oauthAuthenticationHost,

  @Nullable
  @Schema(accessMode = Schema.AccessMode.WRITE_ONLY, description = "For OAuth authentication: Client ID provided by Microsoft Exchange when registering the application")
  String oauthClientId,

  @Nullable
  @Schema(accessMode = Schema.AccessMode.WRITE_ONLY, description = "For OAuth authentication: Client secret provided by Microsoft Exchange when registering the application")
  String oauthClientSecret,

  @Nullable
  @Schema(description = "For OAuth authentication: Microsoft tenant")
  String oauthTenant

) {
}
