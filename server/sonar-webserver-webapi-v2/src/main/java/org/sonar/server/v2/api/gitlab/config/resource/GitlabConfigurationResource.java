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
package org.sonar.server.v2.api.gitlab.config.resource;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import javax.annotation.Nullable;

public record GitlabConfigurationResource(

  @Schema(accessMode = Schema.AccessMode.READ_ONLY)
  String id,

  boolean enabled,

  @Schema(implementation = String.class, description = "Gitlab Application id")
  String applicationId,

  @Schema(description = "Url of Gitlab instance for authentication (for instance https://gitlab.com/api/v4)")
  String url,

  boolean synchronizeGroups,

  ProvisioningType provisioningType,

  boolean allowUsersToSignUp,

  @Schema(description = "Root Gitlab groups to provision")
  List<String> provisioningGroups,

  @Schema(accessMode = Schema.AccessMode.READ_ONLY, description = "In case the GitLab configuration is incorrect, error message")
  @Nullable
  String errorMessage
) {
}

