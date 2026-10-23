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
package org.sonar.server.v2.api.projects.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.Set;
import javax.annotation.Nullable;

public record ProjectsSearchRestRequest(
  @Nullable @Schema(description = "Project IDs to query by.") Set<String> ids,
  @Nullable @Schema(description = "Project keys to query by.") Set<String> keys,
  @Nullable @Schema(description = "Match projects with any of these tags.") Set<String> tags,
  @Min(1) @Schema(description = "The 1-based page index.", defaultValue = "1") Integer pageIndex,
  @Min(0) @Max(5000) @Schema(description = "Page size. Use 0 to return only the total.", defaultValue = "50") Integer pageSize) {

  public ProjectsSearchRestRequest {
    pageIndex = pageIndex == null ? 1 : pageIndex;
    pageSize = pageSize == null ? 50 : pageSize;
  }
}
