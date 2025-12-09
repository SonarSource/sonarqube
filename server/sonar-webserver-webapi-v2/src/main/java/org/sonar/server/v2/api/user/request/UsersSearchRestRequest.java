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
package org.sonar.server.v2.api.user.request;

import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.annotation.Nullable;

public record UsersSearchRestRequest(
  @Schema(defaultValue = "true", description = "Return active/inactive users")
  Boolean active,

  @Nullable
  @Schema(description = "Return managed or non-managed users. Only available for managed instances, throws for non-managed instances")
  Boolean managed,

  @Nullable
  @Schema(description = "Filter on login, name and email.\n"
    + "This parameter performs a partial match (contains), it is case insensitive.")
  String q,

  @Nullable
  @Schema(description = "Filter on externalIdentity.\n"
    + "This parameter perform a case-sensitive exact match")
  String externalIdentity,

  @Nullable
  @Schema(description = "Filter users based on the last connection date field. Only users who interacted with this instance at or after the date will be returned. "
    + "The format must be ISO 8601 datetime format (YYYY-MM-DDThh:mm:ss±hhmm)",
    example = "2020-01-01T00:00:00+0100")
  String sonarQubeLastConnectionDateFrom,

  @Nullable
  @Schema(description = "Filter users based on the last connection date field. Only users that never connected or who interacted with this instance at "
    + "or before the date will be returned. The format must be ISO 8601 datetime format (YYYY-MM-DDThh:mm:ss±hhmm)",
    example = "2020-01-01T00:00:00+0100")
  String sonarQubeLastConnectionDateTo,

  @Nullable
  @Schema(description = "Filter users based on the SonarLint last connection date field Only users who interacted with this instance using SonarLint at or after "
    + "the date will be returned. The format must be ISO 8601 datetime format (YYYY-MM-DDThh:mm:ss±hhmm)",
    example = "2020-01-01T00:00:00+0100")
  String sonarLintLastConnectionDateFrom,

  @Nullable
  @Schema(description = "Filter users based on the SonarLint last connection date field. Only users that never connected or who interacted with this instance "
    + "using SonarLint at or before the date will be returned. The format must be ISO 8601 datetime format (YYYY-MM-DDThh:mm:ss±hhmm)",
    example = "2020-01-01T00:00:00+0100")
  String sonarLintLastConnectionDateTo,

  @Nullable
  @Schema(description = "Filter users belonging to group. Only available for system administrators. Using != operator will exclude users from this group.",
    extensions = @Extension(properties = {@ExtensionProperty(name = "internal", value = "true")}))
  String groupId

) {

}
