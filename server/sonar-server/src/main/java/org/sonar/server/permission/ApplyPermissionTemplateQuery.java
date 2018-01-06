/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.permission;

import java.util.List;

import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.sonar.server.ws.WsUtils.checkRequest;

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
    checkRequest(isNotBlank(templateUuid), "Permission template is mandatory");
    checkRequest(componentKeys != null && !componentKeys.isEmpty(), "No project provided. Please provide at least one project.");
  }
}
