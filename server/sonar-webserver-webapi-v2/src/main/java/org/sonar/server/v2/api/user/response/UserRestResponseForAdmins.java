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
package org.sonar.server.v2.api.user.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import javax.annotation.Nullable;

public record UserRestResponseForAdmins(
  @Schema(accessMode = Schema.AccessMode.READ_ONLY)
  String id,
  String login,
  String name,
  @Nullable
  String email,
  @Nullable
  @Schema(accessMode = Schema.AccessMode.READ_ONLY)
  Boolean active,
  @Nullable
  @Schema(accessMode = Schema.AccessMode.READ_ONLY)
  Boolean local,
  @Nullable
  @Schema(accessMode = Schema.AccessMode.READ_ONLY)
  Boolean managed,
  @Nullable
  String externalLogin,
  @Nullable
  String externalProvider,
  @Nullable
  String avatar,
  @Nullable
  @Schema(accessMode = Schema.AccessMode.READ_ONLY)
  String sonarQubeLastConnectionDate,
  @Nullable
  @Schema(accessMode = Schema.AccessMode.READ_ONLY)
  String sonarLintLastConnectionDate,
  @Nullable
  List<String> scmAccounts
) implements UserRestResponse {
}
