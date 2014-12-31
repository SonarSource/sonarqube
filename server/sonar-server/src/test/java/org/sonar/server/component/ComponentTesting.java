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

package org.sonar.server.component;

import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.Scopes;
import org.sonar.api.utils.internal.Uuids;
import org.sonar.core.component.ComponentDto;

public class ComponentTesting {

  public static ComponentDto newFileDto(ComponentDto subProjectOrProject) {
    return newFileDto(subProjectOrProject, Uuids.create());
  }

  public static ComponentDto newFileDto(ComponentDto module, String fileUuid) {
    return new ComponentDto()
      .setUuid(fileUuid)
      .setProjectUuid(module.projectUuid())
      .setModuleUuid(module.uuid())
      .setModuleUuidPath(module.moduleUuidPath() == null ? module.uuid() : module.moduleUuidPath() + "." + module.uuid())
      .setKey("KEY_" + fileUuid)
      .setName("NAME_" + fileUuid)
      .setLongName("LONG_NAME_" + fileUuid)
      .setParentProjectId(module.getId())
      .setScope(Scopes.FILE)
      .setQualifier(Qualifiers.FILE)
      .setPath("src/main/xoo/org/sonar/samples/File.xoo")
      .setLanguage("xoo")
      .setEnabled(true);
  }

  public static ComponentDto newModuleDto(ComponentDto subProjectOrProject) {
    String uuid = Uuids.create();
    return new ComponentDto()
      .setUuid(Uuids.create())
      .setProjectUuid(subProjectOrProject.projectUuid())
      .setModuleUuid(subProjectOrProject.uuid())
      .setModuleUuidPath(subProjectOrProject.moduleUuidPath() == null ? subProjectOrProject.uuid() + "." : subProjectOrProject.moduleUuidPath() + subProjectOrProject.uuid() + ".")
      .setKey("KEY_" + uuid)
      .setName("NAME_" + uuid)
      .setLongName("LONG_NAME_" + uuid)
      .setParentProjectId(subProjectOrProject.getId())
      .setScope(Scopes.PROJECT)
      .setQualifier(Qualifiers.MODULE)
      .setPath("module")
      .setLanguage(null)
      .setEnabled(true);
  }

  public static ComponentDto newProjectDto() {
    return newProjectDto(Uuids.create());
  }

  public static ComponentDto newProjectDto(String uuid) {
    return new ComponentDto()
      .setUuid(uuid)
      .setProjectUuid(uuid)
      .setKey("KEY_" + uuid)
      .setName("NAME_" + uuid)
      .setLongName("LONG_NAME_" + uuid)
      .setParentProjectId(null)
      .setScope(Scopes.PROJECT)
      .setQualifier(Qualifiers.PROJECT)
      .setPath(null)
      .setLanguage(null)
      .setEnabled(true);
  }
}
