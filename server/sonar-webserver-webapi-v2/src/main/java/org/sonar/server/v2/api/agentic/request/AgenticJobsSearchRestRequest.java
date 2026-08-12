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
package org.sonar.server.v2.api.agentic.request;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.annotation.Nullable;

public record AgenticJobsSearchRestRequest(

  @Nullable
  @Schema(description = "Comma-separated list of job ids to filter on.")
  String id,

  @Nullable
  @Schema(description = "Comma-separated list of statuses to filter on.", allowableValues = {"PENDING", "IN_PROGRESS", "COMPLETED", "FAILED"})
  String status,

  @Nullable
  @Schema(description = "Comma-separated list of agent types to filter on.", allowableValues = {"HUNTER", "REMEDIATION"})
  String type,

  @Nullable
  @Schema(description = "Only return jobs created on or after this date (or date-time), e.g. 2026-01-01 or 2026-01-01T12:00:00+0000.")
  String createdAfter,

  @Nullable
  @Schema(description = "Only return jobs created on or before this date (or date-time), e.g. 2026-01-01 or 2026-01-01T12:00:00+0000.")
  String createdBefore

) {
}
