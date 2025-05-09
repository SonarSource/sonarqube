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
package org.sonar.server.v2.api.group.request;

import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.annotation.Nullable;

public record GroupsSearchRestRequest(
  @Nullable
  @Schema(description = "Return managed or non-managed groups. Only available for managed instances, throws for non-managed instances")
  Boolean managed,

  @Nullable
  @Schema(description = "Filter on name.\n"
                        + "This parameter performs a partial match (contains), it is case insensitive.")
  String q,

  @Nullable
  @Schema(description = "Filter groups containing the user. Only available for system administrators. Using != operator will search for groups without the user.",
    extensions = @Extension(properties = {@ExtensionProperty(name = "internal", value = "true")}))
  String userId

) {

}
