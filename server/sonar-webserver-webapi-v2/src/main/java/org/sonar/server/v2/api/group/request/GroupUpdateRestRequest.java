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
import javax.validation.constraints.Size;
import org.sonar.server.v2.common.model.UpdateField;

import static org.sonar.api.user.UserGroupValidation.GROUP_NAME_MAX_LENGTH;

public class GroupUpdateRestRequest {

  private UpdateField<String> name = UpdateField.undefined();
  private UpdateField<String> description = UpdateField.undefined();
  private String organization;  // Added organization field
  @Size(min=1, max = GROUP_NAME_MAX_LENGTH)
  @Schema(implementation = String.class, description = "Group name")
  public UpdateField<String> getName() {
    return name;
  }

  public void setName(String name) {
    this.name = UpdateField.withValue(name);
  }

  @Size(max = 200)
  @Schema(implementation = String.class, description = "Description of the group")
  public UpdateField<String> getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = UpdateField.withValue(description);
  }
  public String getOrganization() {
    return organization;
  }

}
