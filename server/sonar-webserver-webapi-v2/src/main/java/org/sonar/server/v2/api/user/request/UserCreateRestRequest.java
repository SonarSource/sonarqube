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
package org.sonar.server.v2.api.user.request;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import javax.annotation.Nullable;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public record UserCreateRestRequest(
  @Nullable
  @Email
  @Size(min = 1, max = 100)
  @Schema(description = "User email")
  String email,

  @Nullable
  @Schema(description = "Specify if the user should be authenticated from SonarQube server or from an external authentication system. " +
    "Password should not be set when local is set to false.",
    defaultValue = "true")
  Boolean local,

  @NotNull
  @Size(min = 2, max = 100)
  @Schema(description = "User login")
  String login,

  @NotNull
  @Size(max = 200)
  @Schema(description = "User name")
  String name,

  @Nullable
  @Schema(description = "User password. Only mandatory when creating local user, otherwise it should not be set", accessMode = Schema.AccessMode.WRITE_ONLY)
  String password,

  @Nullable
  @Schema(description = "List of SCM accounts.")
  List<String> scmAccounts) {

  public UserCreateRestRequest(@Nullable String email, @Nullable Boolean local, String login, String name, @Nullable String password, @Nullable List<String> scmAccounts) {
    this.email = email;
    this.local = local == null ? Boolean.TRUE : local;
    this.login = login;
    this.name = name;
    this.password = password;
    this.scmAccounts = scmAccounts;
  }

}
