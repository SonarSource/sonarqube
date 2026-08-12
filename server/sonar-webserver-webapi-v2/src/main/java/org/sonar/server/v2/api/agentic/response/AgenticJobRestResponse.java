/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.server.v2.api.agentic.response;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.annotation.Nullable;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;

public record AgenticJobRestResponse(

  @Schema(accessMode = READ_ONLY)
  String id,

  @Schema(accessMode = READ_ONLY)
  String projectId,

  @Nullable
  @Schema(accessMode = READ_ONLY, description = "Not set if the project has been deleted.")
  String projectKey,

  @Nullable
  @Schema(accessMode = READ_ONLY, description = "Not set if the project has been deleted.")
  String projectName,

  @Schema(accessMode = READ_ONLY, allowableValues = {"HUNTER", "REMEDIATION"})
  String type,

  @Schema(accessMode = READ_ONLY, allowableValues = {"FULL", "INCREMENTAL"})
  String analysisType,

  @Schema(accessMode = READ_ONLY, allowableValues = {"PENDING", "IN_PROGRESS", "COMPLETED", "FAILED"})
  String status,

  @Nullable
  @Schema(accessMode = READ_ONLY)
  String branch,

  @Schema(accessMode = READ_ONLY)
  String repositoryUrl,

  @Nullable
  @Schema(accessMode = READ_ONLY)
  String revision,

  @Nullable
  @Schema(accessMode = READ_ONLY)
  String workflowType,

  @Nullable
  @Schema(accessMode = READ_ONLY)
  Integer findingsCount,

  @Schema(accessMode = READ_ONLY)
  long createdAt,

  @Schema(accessMode = READ_ONLY)
  long updatedAt,

  @Nullable
  @Schema(accessMode = READ_ONLY)
  Long startedAt,

  @Nullable
  @Schema(accessMode = READ_ONLY)
  Long finishedAt,

  @Nullable
  @Schema(accessMode = READ_ONLY, description = "Only set when status is FAILED.")
  String failureReason

) {
}
