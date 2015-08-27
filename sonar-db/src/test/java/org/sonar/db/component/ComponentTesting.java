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

package org.sonar.db.component;

import com.google.common.base.Preconditions;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.Scopes;
import org.sonar.core.util.Uuids;

import static org.sonar.db.component.ComponentDto.MODULE_UUID_PATH_SEP;

public class ComponentTesting {

  public static ComponentDto newFileDto(ComponentDto subProjectOrProject) {
    return newFileDto(subProjectOrProject, Uuids.create());
  }

  public static ComponentDto newFileDto(ComponentDto module, String fileUuid) {
    String path = "src/main/xoo/org/sonar/samples/File.xoo";
    return newChildComponent(fileUuid, module)
      .setKey("KEY_" + fileUuid)
      .setName("NAME_" + fileUuid)
      .setLongName(path)
      .setScope(Scopes.FILE)
      .setQualifier(Qualifiers.FILE)
      .setPath(path)
      .setLanguage("xoo");
  }

  public static ComponentDto newDirectory(ComponentDto module, String uuid, String path) {
    return newChildComponent(uuid, module)
        .setKey(!path.equals("/") ? module.getKey() + ":" + path : module.getKey() + ":/")
        .setName(path)
        .setLongName(path)
        .setPath(path)
        .setScope(Scopes.DIRECTORY)
        .setQualifier(Qualifiers.DIRECTORY);
  }

  public static ComponentDto newSubView(ComponentDto viewOrSubView, String uuid, String path) {
    return newChildComponent(uuid, viewOrSubView)
        .setKey(!path.equals("/") ? viewOrSubView.getKey() + ":" + path : viewOrSubView.getKey() + ":/")
        .setName(path)
        .setLongName(path)
        .setPath(path)
        .setScope(Scopes.PROJECT)
        .setQualifier(Qualifiers.SUBVIEW);
  }

  public static ComponentDto newDirectory(ComponentDto module, String path) {
    return newDirectory(module, Uuids.create(), path);
  }

  public static ComponentDto newModuleDto(String uuid, ComponentDto subProjectOrProject) {
    return newChildComponent(uuid, subProjectOrProject)
      .setModuleUuidPath(subProjectOrProject.moduleUuidPath() + uuid + MODULE_UUID_PATH_SEP)
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
      .setProjectUuid(uuid)
      .setModuleUuidPath(MODULE_UUID_PATH_SEP + uuid + MODULE_UUID_PATH_SEP)
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

  public static ComponentDto newDeveloper(String name) {
    String uuid = Uuids.create();
    return new ComponentDto()
      .setUuid(uuid)
      .setProjectUuid(uuid)
      .setModuleUuidPath(MODULE_UUID_PATH_SEP + uuid + MODULE_UUID_PATH_SEP)
      .setParentProjectId(null)
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
    Preconditions.checkNotNull(project.getId(), "The project need to be persisted before creating this technical project.");
    return newChildComponent(uuid, view)
      .setUuid(uuid)
      .setKey(view.key() + project.key())
      .setName(project.name())
      .setLongName(project.longName())
      .setCopyResourceId(project.getId())
      .setScope(Scopes.FILE)
      .setQualifier(Qualifiers.PROJECT)
      .setPath(null)
      .setLanguage(null);
  }

  public static ComponentDto newDevProjectCopy(String uuid, ComponentDto project, ComponentDto developer) {
    Preconditions.checkNotNull(project.getId(), "The project need to be persisted before creating this technical project.");
    return newChildComponent(uuid, developer)
      .setUuid(uuid)
      .setKey(developer.key() + ":" + project.key())
      .setName(project.name())
      .setLongName(project.longName())
      .setCopyResourceId(project.getId())
      .setScope(Scopes.PROJECT)
      .setQualifier("DEV_PRJ")
      .setPath(null)
      .setLanguage(null);
  }

  private static ComponentDto newChildComponent(String uuid, ComponentDto module) {
    return new ComponentDto()
      .setUuid(uuid)
      .setProjectUuid(module.projectUuid())
      .setModuleUuid(module.uuid())
      .setModuleUuidPath(module.moduleUuidPath())
      .setParentProjectId(module.getId())
      .setEnabled(true);
  }
}
