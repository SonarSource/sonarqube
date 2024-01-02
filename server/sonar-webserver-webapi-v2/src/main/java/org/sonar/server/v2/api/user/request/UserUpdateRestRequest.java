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

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import javax.validation.constraints.Email;
import javax.validation.constraints.Size;
import org.sonar.server.v2.common.model.UpdateField;

public class UserUpdateRestRequest {

  private UpdateField<String> name = UpdateField.undefined();
  private UpdateField<String> email = UpdateField.undefined();
  private UpdateField<List<String>> scmAccounts = UpdateField.undefined();

  @Size(max = 200)
  @Schema(description = "User first name and last name", implementation = String.class)
  public UpdateField<String> getName() {
    return name;
  }

  public void setName(String name) {
    this.name = UpdateField.withValue(name);
  }

  @Email
  @Size(min = 1, max = 100)
  @Schema(implementation = String.class, description = "Email")
  public UpdateField<String> getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = UpdateField.withValue(email);
  }

  @ArraySchema(arraySchema = @Schema(description = "List of SCM accounts."), schema = @Schema(implementation = String.class))
  public UpdateField<List<String>> getScmAccounts() {
    return scmAccounts;
  }

  public void setScmAccounts(List<String> scmAccounts) {
    this.scmAccounts = UpdateField.withValue(scmAccounts);
  }
}
