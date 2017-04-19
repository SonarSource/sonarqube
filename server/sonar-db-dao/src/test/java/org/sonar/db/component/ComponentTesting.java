/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.db.component;

import java.util.Date;
import javax.annotation.Nullable;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.Scopes;
import org.sonar.core.util.Uuids;
import org.sonar.db.organization.OrganizationDto;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonar.db.component.ComponentDto.UUID_PATH_SEPARATOR;

public class ComponentTesting {

  public static ComponentDto newFileDto(ComponentDto subProjectOrProject) {
    return newFileDto(subProjectOrProject, null);
  }

  public static ComponentDto newFileDto(ComponentDto subProjectOrProject, @Nullable ComponentDto directory) {
    return newFileDto(subProjectOrProject, directory, Uuids.createFast());
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
    return newDirectory(module, Uuids.createFast(), path);
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

  public static ComponentDto newSubView(ComponentDto viewOrSubView) {
    String uuid = Uuids.createFast();
    return newSubView(viewOrSubView, uuid, "KEY_" + uuid);
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
    return newModuleDto(Uuids.createFast(), subProjectOrProject);
  }

  public static ComponentDto newPrivateProjectDto(OrganizationDto organizationDto) {
    return newProjectDto(organizationDto.getUuid(), Uuids.createFast(), true);
  }

  public static ComponentDto newPrivateProjectDto(OrganizationDto organizationDto, String uuid) {
    return newProjectDto(organizationDto.getUuid(), uuid, true);
  }

  public static ComponentDto newPublicProjectDto(OrganizationDto organizationDto) {
    return newProjectDto(organizationDto.getUuid(), Uuids.createFast(), false);
  }

  public static ComponentDto newPublicProjectDto(OrganizationDto organizationDto, String uuid) {
    return newProjectDto(organizationDto.getUuid(), uuid, false);
  }

  private static ComponentDto newProjectDto(String organizationUuid, String uuid, boolean isPrivate) {
    return new ComponentDto()
      .setOrganizationUuid(organizationUuid)
      .setUuid(uuid)
      .setUuidPath(ComponentDto.UUID_PATH_OF_ROOT)
      .setProjectUuid(uuid)
      .setModuleUuidPath(UUID_PATH_SEPARATOR + uuid + UUID_PATH_SEPARATOR)
      .setRootUuid(uuid)
      .setKey("KEY_" + uuid)
      .setName("NAME_" + uuid)
      .setLongName("LONG_NAME_" + uuid)
      .setDescription("DESCRIPTION_" + uuid)
      .setScope(Scopes.PROJECT)
      .setQualifier(Qualifiers.PROJECT)
      .setPath(null)
      .setLanguage(null)
      .setEnabled(true)
        .setPrivate(isPrivate);
  }

  public static ComponentDto newView(OrganizationDto organizationDto) {
    return newView(organizationDto.getUuid(), Uuids.createFast());
  }

  public static ComponentDto newView(OrganizationDto organizationDto, String uuid) {
    return newPrivateProjectDto(organizationDto, uuid)
      .setUuid(uuid)
      .setScope(Scopes.PROJECT)
      .setQualifier(Qualifiers.VIEW)
      .setPrivate(false);
  }

  private static ComponentDto newView(String organizationUuid, String uuid) {
    return newProjectDto(organizationUuid, uuid, false)
      .setUuid(uuid)
      .setScope(Scopes.PROJECT)
      .setQualifier(Qualifiers.VIEW);
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

  public static ComponentDto newChildComponent(String uuid, ComponentDto moduleOrProject, ComponentDto parent) {
    checkArgument(moduleOrProject.isPrivate() == parent.isPrivate(),
      "private flag inconsistent between moduleOrProject (%s) and parent (%s)",
      moduleOrProject.isPrivate(), parent.isPrivate());
    return new ComponentDto()
      .setOrganizationUuid(parent.getOrganizationUuid())
      .setUuid(uuid)
      .setUuidPath(ComponentDto.formatUuidPathFromParent(parent))
      .setProjectUuid(moduleOrProject.projectUuid())
      .setRootUuid(moduleOrProject.uuid())
      .setModuleUuid(moduleOrProject.uuid())
      .setModuleUuidPath(moduleOrProject.moduleUuidPath())
      .setCreatedAt(new Date())
      .setEnabled(true)
      .setPrivate(moduleOrProject.isPrivate());
  }
}
