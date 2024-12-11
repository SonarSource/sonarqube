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
package org.sonar.server.v2.api.github.config.request;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import javax.annotation.Nullable;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.sonar.server.v2.api.model.ProvisioningType;

public record GithubConfigurationCreateRestRequest(

  @NotNull
  @Schema(description = "Enable GitHub authentication")
  boolean enabled,

  @NotEmpty
  @Schema(accessMode = Schema.AccessMode.WRITE_ONLY,  description = "Client ID provided by GitHub when registering the application.")
  String clientId,

  @NotEmpty
  @Schema(accessMode = Schema.AccessMode.WRITE_ONLY,  description = "Client password provided by GitHub when registering the application.")
  String clientSecret,

  @NotEmpty
  @Schema(description = "The App ID is found on your GitHub App's page on GitHub at Settings > Developer Settings > GitHub Apps.")
  String applicationId,

  @NotEmpty
  @Schema(accessMode = Schema.AccessMode.WRITE_ONLY,  description = """
    Your GitHub App's private key. You can generate a .pem file from your GitHub App's page under Private keys.
    Copy and paste the whole contents of the file here.
    """)
  String privateKey,

  @NotNull
  @Schema(description = """
    Synchronize GitHub team with SonarQube group memberships when users log in to SonarQube.
    For each GitHub team they belong to, users will be associated to a group of the same name if it exists in SonarQube.
    """)
  Boolean synchronizeGroups,

  @NotEmpty
  @Schema(description = "The API url for a GitHub instance. https://api.github.com/ for Github.com, https://github.company.com/api/v3/ when using Github Enterprise")
  String apiUrl,

  @NotEmpty
  @Schema(description = "The WEB url for a GitHub instance. https://github.com/ for Github.com, https://github.company.com/ when using GitHub Enterprise.\n")
  String webUrl,

  @NotNull
  @ArraySchema(arraySchema = @Schema(description = """
    Only members of these organizations will be able to authenticate to the server.
    ⚠ if not set, users from any organization where the GitHub App is installed will be able to login to this SonarQube instance.
    """))
  List<String> allowedOrganizations,

  @NotNull
  @Schema(description = "Type of synchronization")
  ProvisioningType provisioningType,

  @Nullable
  @Schema(description = "Allow user to sign up")
  Boolean allowUsersToSignUp,

  @Nullable
  @Schema(description = """
    Change project visibility based on GitHub repository visibility.
    If disabled, every provisioned project will be private in SonarQube and visible only to users with explicit GitHub permissions for the corresponding repository.
    Changes take effect at the next synchronization.
    """)
  Boolean projectVisibility,

  @Nullable
  @Schema(description = "Admin consent to synchronize permissions from GitHub")
  Boolean userConsentRequiredAfterUpgrade
) {
}
