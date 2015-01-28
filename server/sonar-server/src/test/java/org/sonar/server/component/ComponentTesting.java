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

import static org.sonar.core.component.ComponentDto.MODULE_UUID_PATH_SEP;

public class ComponentTesting {

  public static ComponentDto newFileDto(ComponentDto subProjectOrProject) {
    return newFileDto(subProjectOrProject, Uuids.create());
  }

  public static ComponentDto newFileDto(ComponentDto module, String fileUuid) {
    return newComponent(module, fileUuid)
      .setKey("KEY_" + fileUuid)
      .setName("NAME_" + fileUuid)
      .setLongName("LONG_NAME_" + fileUuid)
      .setScope(Scopes.FILE)
      .setQualifier(Qualifiers.FILE)
      .setPath("src/main/xoo/org/sonar/samples/File.xoo")
      .setLanguage("xoo");
  }

  public static ComponentDto newDirectory(ComponentDto module, String path) {
    String uuid = Uuids.create();
    return newComponent(module, uuid)
      .setKey(!path.equals("/") ? module.getKey() + ":" + path : module.getKey() + ":/")
      .setName(path)
      .setLongName(path)
      .setPath(path)
      .setScope(Scopes.DIRECTORY)
      .setQualifier(Qualifiers.DIRECTORY);
  }

  public static ComponentDto newModuleDto(ComponentDto subProjectOrProject) {
    return newModuleDto(subProjectOrProject, Uuids.create());
  }

  public static ComponentDto newModuleDto(ComponentDto subProjectOrProject, String uuid) {
    return newComponent(subProjectOrProject, uuid)
      .setKey("KEY_" + uuid)
      .setName("NAME_" + uuid)
      .setLongName("LONG_NAME_" + uuid)
      .setPath("module")
      .setScope(Scopes.PROJECT)
      .setQualifier(Qualifiers.MODULE)
      .setLanguage(null);
  }

  public static ComponentDto newProjectDto() {
    return newProjectDto(Uuids.create());
  }

  public static ComponentDto newProjectDto(String uuid) {
    return new ComponentDto()
      .setUuid(uuid)
      .setProjectUuid(uuid)
      .setModuleUuidPath(MODULE_UUID_PATH_SEP)
      .setParentProjectId(null)
      .setKey("KEY_" + uuid)
      .setName("NAME_" + uuid)
      .setLongName("LONG_NAME_" + uuid)
      .setScope(Scopes.PROJECT)
      .setQualifier(Qualifiers.PROJECT)
      .setPath(null)
      .setLanguage(null)
      .setEnabled(true);
  }

  private static ComponentDto newComponent(ComponentDto module, String uuid) {
    return new ComponentDto()
      .setUuid(uuid)
      .setProjectUuid(module.projectUuid())
      .setModuleUuid(module.uuid())
      .setModuleUuidPath(module.moduleUuidPath() + module.uuid() + MODULE_UUID_PATH_SEP)
      .setParentProjectId(module.getId())
      .setEnabled(true);
  }
}
