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
package org.sonar.server.v2.api.projects.request;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.annotation.Nullable;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record BoundProjectCreateRestRequest(

  @NotEmpty
  @Schema(description = "Key of the project to create")
  String projectKey,

  @NotEmpty
  @Schema(description = "Name of the project to create")
  String projectName,

  @NotEmpty
  @Schema(description = "Identifier of DevOps platform configuration to use.")
  String devOpsPlatformSettingId,

  @NotEmpty
  @Schema(
    description = """
      Identifier of the DevOps platform repository to import:
      - repository slug for GitHub and Bitbucket (Cloud and Server)
      - repository id for GitLab
      - repository name for Azure DevOps
      """)
  String repositoryIdentifier,

  @Nullable
  @Schema(
    description = """
      Identifier of the DevOps platform project in which the repository is located.
      This is only needed for Azure and BitBucket Server platforms
      """)
  String projectIdentifier,

  @Nullable
  @Schema(description = """
    Project New Code Definition Type
    New code definitions of the following types are allowed:
      - PREVIOUS_VERSION
      - NUMBER_OF_DAYS
      - REFERENCE_BRANCH - will default to the main branch.
  """)
  String newCodeDefinitionType,

  @Nullable
  @Schema(description = """
    Project New Code Definition Value
    For each new code definition type, a different value is expected:
    - no value, when the new code definition type is PREVIOUS_VERSION and REFERENCE_BRANCH
    - a number between 1 and 90, when the new code definition type is NUMBER_OF_DAYS
  """)
  String newCodeDefinitionValue,

  @NotNull
  @Schema(description = "True if project is part of a mono repo.")
  Boolean monorepo
) {
}
