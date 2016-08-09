/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.db.component;

import java.util.Date;
import javax.annotation.Nullable;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.Scopes;
import org.sonar.core.util.Uuids;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonar.db.component.ComponentDto.UUID_PATH_SEPARATOR;

public class ComponentTesting {

  public static ComponentDto newFileDto(ComponentDto subProjectOrProject, @Nullable ComponentDto directory) {
    return newFileDto(subProjectOrProject, directory, Uuids.create());
  }

  public static ComponentDto newFileDto(ComponentDto module, @Nullable ComponentDto directory, String fileUuid) {
    String path = "src/main/xoo/org/sonar/samples/File.xoo";
    return newChildComponent(fileUuid, module, directory == null ? module : directory)
      .setKey("KEY_" + fileUuid)
      .setName("NAME_" + fileUuid)
      .setLongName(path)
      .setScope(Scopes.FILE)
      .setQualifier(Qualifiers.FILE)
      .setPath(path)
      .setCreatedAt(new Date())
      .setLanguage("xoo");
  }

  public static ComponentDto newDirectory(ComponentDto module, String path) {
    return newDirectory(module, Uuids.create(), path);
  }

  public static ComponentDto newDirectory(ComponentDto module, String uuid, String path) {
    return newChildComponent(uuid, module, module)
        .setKey(!path.equals("/") ? module.getKey() + ":" + path : module.getKey() + ":/")
        .setName(path)
        .setLongName(path)
        .setPath(path)
        .setScope(Scopes.DIRECTORY)
        .setQualifier(Qualifiers.DIRECTORY);
  }

  public static ComponentDto newSubView(ComponentDto viewOrSubView, String uuid, String key) {
    return newChildComponent(uuid, viewOrSubView, viewOrSubView)
        .setKey(key)
        .setName(key)
        .setLongName(key)
        .setScope(Scopes.PROJECT)
        .setQualifier(Qualifiers.SUBVIEW);
  }

  public static ComponentDto newModuleDto(String uuid, ComponentDto parentModuleOrProject) {
    return newChildComponent(uuid, parentModuleOrProject, parentModuleOrProject)
      .setModuleUuidPath(parentModuleOrProject.moduleUuidPath() + uuid + UUID_PATH_SEPARATOR)
      .setKey("KEY_" + uuid)
      .setName("NAME_" + uuid)
      .setLongName("LONG_NAME_" + uuid)
      .setPath("module")
      .setScope(Scopes.PROJECT)
      .setQualifier(Qualifiers.MODULE)
      .setLanguage(null);
  }

  public static ComponentDto newModuleDto(ComponentDto subProjectOrProject) {
    return newModuleDto(Uuids.create(), subProjectOrProject);
  }

  public static ComponentDto newProjectDto() {
    return newProjectDto(Uuids.create());
  }

  public static ComponentDto newProjectDto(String uuid) {
    return new ComponentDto()
      .setUuid(uuid)
      .setUuidPath(ComponentDto.UUID_PATH_OF_ROOT)
      .setProjectUuid(uuid)
      .setModuleUuidPath(UUID_PATH_SEPARATOR + uuid + UUID_PATH_SEPARATOR)
      .setRootUuid(uuid)
      .setKey("KEY_" + uuid)
      .setName("NAME_" + uuid)
      .setLongName("LONG_NAME_" + uuid)
      .setScope(Scopes.PROJECT)
      .setQualifier(Qualifiers.PROJECT)
      .setPath(null)
      .setLanguage(null)
      .setEnabled(true);
  }

  public static ComponentDto newDeveloper(String name) {
    String uuid = Uuids.create();
    return new ComponentDto()
      .setUuid(uuid)
      .setUuidPath(ComponentDto.UUID_PATH_OF_ROOT)
      .setProjectUuid(uuid)
      .setModuleUuidPath(UUID_PATH_SEPARATOR + uuid + UUID_PATH_SEPARATOR)
      .setRootUuid(uuid)
      .setKey(uuid)
      .setName(name)
      .setLongName(name)
      .setScope(Scopes.PROJECT)
        // XXX No constant !
      .setQualifier("DEV")
      .setPath(null)
      .setLanguage(null)
      .setEnabled(true);
  }

  public static ComponentDto newDeveloper(String name, String uuid) {
    return new ComponentDto()
      .setUuid(uuid)
      .setUuidPath(ComponentDto.UUID_PATH_OF_ROOT)
      .setProjectUuid(uuid)
      .setModuleUuidPath(UUID_PATH_SEPARATOR + uuid + UUID_PATH_SEPARATOR)
      .setRootUuid(uuid)
      .setKey(uuid)
      .setName(name)
      .setLongName(name)
      .setScope(Scopes.PROJECT)
      // XXX No constant !
      .setQualifier("DEV")
      .setPath(null)
      .setLanguage(null)
      .setEnabled(true);
  }

  public static ComponentDto newView(String uuid) {
    return newProjectDto(uuid)
      .setUuid(uuid)
      .setScope(Scopes.PROJECT)
      .setQualifier(Qualifiers.VIEW);
  }

  public static ComponentDto newView() {
    return newView(Uuids.create());
  }

  public static ComponentDto newProjectCopy(String uuid, ComponentDto project, ComponentDto view) {
    checkNotNull(project.getId(), "The project need to be persisted before creating this technical project.");
    return newChildComponent(uuid, view, view)
      .setUuid(uuid)
      .setKey(view.key() + project.key())
      .setName(project.name())
      .setLongName(project.longName())
      .setCopyComponentUuid(project.uuid())
      .setScope(Scopes.FILE)
      .setQualifier(Qualifiers.PROJECT)
      .setPath(null)
      .setLanguage(null);
  }

  public static ComponentDto newDevProjectCopy(String uuid, ComponentDto project, ComponentDto developer) {
    checkNotNull(project.getId(), "The project need to be persisted before creating this technical project.");
    return newChildComponent(uuid, developer, developer)
      .setUuid(uuid)
      .setKey(developer.key() + ":" + project.key())
      .setName(project.name())
      .setLongName(project.longName())
      .setCopyComponentUuid(project.uuid())
      .setScope(Scopes.PROJECT)
      .setQualifier("DEV_PRJ")
      .setPath(null)
      .setLanguage(null);
  }

  public static ComponentDto newChildComponent(String uuid, ComponentDto moduleOrProject, ComponentDto parent) {
    return new ComponentDto()
      .setUuid(uuid)
      .setUuidPath(ComponentDto.formatUuidPathFromParent(parent))
      .setProjectUuid(moduleOrProject.projectUuid())
      .setRootUuid(moduleOrProject.uuid())
      .setModuleUuid(moduleOrProject.uuid())
      .setModuleUuidPath(moduleOrProject.moduleUuidPath())
      .setCreatedAt(new Date())
      .setEnabled(true);
  }
}
