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
package org.sonar.server.v2.api.group.request;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.annotation.Nullable;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;

import static org.sonar.api.user.UserGroupValidation.GROUP_NAME_MAX_LENGTH;

public record GroupCreateRestRequest(

  @NotEmpty
  @Size(min = 1, max = GROUP_NAME_MAX_LENGTH)
  @Schema(description = "Name for the new group. Must be unique. The value 'anyone' is reserved and cannot be used.")
  String name,

  @Nullable
  @Size(max = 200)
  @Schema(description = "Description for the new group.")
  String description

) {}
