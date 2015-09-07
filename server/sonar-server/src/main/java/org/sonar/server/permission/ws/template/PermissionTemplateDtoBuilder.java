/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.server.permission.ws.template;

import java.util.Date;
import org.sonar.api.utils.System2;
import org.sonar.core.util.Uuids;
import org.sonar.db.permission.PermissionTemplateDto;

public class PermissionTemplateDtoBuilder {
  private final System2 system;
  private String name;
  private String description;
  private String projectKeyPattern;

  private PermissionTemplateDtoBuilder(System2 system) {
    this.system = system;
  }

  public static PermissionTemplateDtoBuilder create(System2 system) {
    return new PermissionTemplateDtoBuilder(system);
  }

  public PermissionTemplateDtoBuilder setName(String name) {
    this.name = name;
    return this;
  }

  public PermissionTemplateDtoBuilder setDescription(String description) {
    this.description = description;
    return this;
  }

  public PermissionTemplateDtoBuilder setProjectKeyPattern(String projectKeyPattern) {
    this.projectKeyPattern = projectKeyPattern;
    return this;
  }

  public PermissionTemplateDto toDto() {
    long now = system.now();
    return new PermissionTemplateDto()
      .setName(name)
      .setDescription(description)
      .setKeyPattern(projectKeyPattern)
      .setUuid(Uuids.create())
      .setCreatedAt(new Date(now))
      .setUpdatedAt(new Date(now));
  }
}
