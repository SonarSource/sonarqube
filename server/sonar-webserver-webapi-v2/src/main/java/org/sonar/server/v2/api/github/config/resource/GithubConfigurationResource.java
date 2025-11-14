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
package org.sonar.server.v2.api.github.config.resource;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import javax.annotation.Nullable;
import org.sonar.server.v2.api.model.ProvisioningType;

public record GithubConfigurationResource(

  @Schema(accessMode = Schema.AccessMode.READ_ONLY)
  String id,

  boolean enabled,

  @Schema(implementation = String.class, description = "GitHub Application id")
  String applicationId,

  boolean synchronizeGroups,

  @Schema(description = "Url of GitHub instance for API connectivity (for instance https://api.github.com)")
  String apiUrl,

  @Schema(description = "Url of GitHub instance for authentication (for instance https://github.com)")
  String webUrl,

  @Schema(description = "GitHub organizations allowed to authenticate and provisioned")
  List<String> allowedOrganizations,

  ProvisioningType provisioningType,

  boolean allowUsersToSignUp,

  boolean projectVisibility,

  boolean userConsentRequiredAfterUpgrade,

  @Schema(accessMode = Schema.AccessMode.READ_ONLY, description = "In case the GitHub configuration is incorrect, error message")
  @Nullable
  String errorMessage
) {
}

