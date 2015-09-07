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

package org.sonar.server.permission;

import java.util.List;
import org.sonar.server.exceptions.BadRequestException;

import static com.google.common.base.CharMatcher.WHITESPACE;

public class ApplyPermissionTemplateQuery {

  private final String templateUuid;
  private List<String> componentKeys;

  private ApplyPermissionTemplateQuery(String templateUuid, List<String> componentKeys) {
    this.templateUuid = templateUuid;
    this.componentKeys = componentKeys;
    validate();
  }

  public static ApplyPermissionTemplateQuery create(String templateUuid, List<String> componentKeys) {
    return new ApplyPermissionTemplateQuery(templateUuid, componentKeys);
  }

  public String getTemplateUuid() {
    return templateUuid;
  }

  public List<String> getComponentKeys() {
    return componentKeys;
  }

  private void validate() {
    if (templateUuid == null || WHITESPACE.trimFrom(templateUuid).isEmpty()) {
      throw new BadRequestException("Permission template is mandatory");
    }
    if (componentKeys == null || componentKeys.isEmpty()) {
      throw new BadRequestException("No project provided. Please provide at least one project.");
    }
  }
}
