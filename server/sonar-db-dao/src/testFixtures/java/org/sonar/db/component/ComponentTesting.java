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
package org.sonar.db.component;

import java.util.Date;
import javax.annotation.Nullable;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.Scopes;
import org.sonar.core.util.Uuids;
import org.sonar.db.project.ProjectDto;

import static com.google.common.base.Preconditions.checkArgument;
import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.sonar.db.component.BranchDto.DEFAULT_MAIN_BRANCH_NAME;
import static org.sonar.db.component.ComponentDto.UUID_PATH_OF_ROOT;
import static org.sonar.db.component.ComponentDto.UUID_PATH_SEPARATOR;
import static org.sonar.db.component.ComponentDto.formatUuidPathFromParent;
import static org.sonar.db.component.ComponentDto.generateBranchKey;

public class ComponentTesting {

  public static ComponentDto newFileDto(ComponentDto subProjectOrProject) {
    return newFileDto(subProjectOrProject, null);
  }

  public static ComponentDto newFileDto(ComponentDto subProjectOrProject, @Nullable ComponentDto directory) {
    return newFileDto(subProjectOrProject, directory, Uuids.createFast());
  }

  public static ComponentDto newFileDto(ComponentDto module, @Nullable ComponentDto directory, String fileUuid) {
    String filename = "NAME_" + fileUuid;
    String path = directory != null ? directory.path() + "/" + filename : module.path() + "/" + filename;
    return newChildComponent(fileUuid, module, directory == null ? module : directory)
      .setKey("FILE_KEY_" + fileUuid)
      .setName(filename)
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
    String key = !path.equals("/") ? module.getKey() + ":" + path : module.getKey() + ":/";
    return newChildComponent(uuid, module, module)
      .setKey(key)
      .setName(path)
      .setLongName(path)
      .setPath(path)
      .setScope(Scopes.DIRECTORY)
      .setQualifier(Qualifiers.DIRECTORY);
  }

  public static ComponentDto newSubPortfolio(ComponentDto portfolioOrSubPortfolio, String uuid, String key) {
    return newModuleDto(uuid, portfolioOrSubPortfolio)
      .setKey(key)
      .setName(key)
      .setLongName(key)
      .setScope(Scopes.PROJECT)
      .setQualifier(Qualifiers.SUBVIEW)
      .setPath(null);
  }

  public static ComponentDto newSubPortfolio(ComponentDto viewOrSubView) {
    String uuid = Uuids.createFast();
    return newSubPortfolio(viewOrSubView, uuid, "KEY_" + uuid);
  }

  public static ComponentDto newModuleDto(String uuid, ComponentDto parentModuleOrProject) {
    return newChildComponent(uuid, parentModuleOrProject, parentModuleOrProject)
      .setModuleUuidPath(parentModuleOrProject.moduleUuidPath() + uuid + UUID_PATH_SEPARATOR)
      .setKey("MODULE_KEY_" + uuid)
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

  public static ComponentDto newPrivateProjectDto() {
    return newProjectDto(Uuids.createFast(), true);
  }

  public static ComponentDto newPrivateProjectDto(String uuid) {
    return newProjectDto(uuid, true);
  }

  public static ComponentDto newPublicProjectDto() {
    return newProjectDto(Uuids.createFast(), false);
  }

  public static ComponentDto newPublicProjectDto(String uuid) {
    return newProjectDto(uuid, false);
  }

  private static ComponentDto newProjectDto(String uuid, boolean isPrivate) {
    return new ComponentDto()
      .setUuid(uuid)
      .setUuidPath(UUID_PATH_OF_ROOT)
      .setBranchUuid(uuid)
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

  public static ComponentDto newPortfolio() {
    return newPortfolio(Uuids.createFast());
  }

  public static ComponentDto newPortfolio(String uuid) {
    return newPrivateProjectDto(uuid)
      .setUuid(uuid)
      .setScope(Scopes.PROJECT)
      .setQualifier(Qualifiers.VIEW)
      .setPrivate(false);
  }

  public static ComponentDto newApplication() {
    return newPortfolio(Uuids.createFast()).setQualifier(Qualifiers.APP);
  }

  public static ComponentDto newProjectCopy(ComponentDto project, ComponentDto view) {
    return newProjectCopy(Uuids.createFast(), project, view);
  }

  public static ComponentDto newProjectCopy(String uuid, ComponentDto project, ComponentDto view) {
    return newChildComponent(uuid, view, view)
      .setKey(view.getKey() + project.getKey())
      .setName(project.name())
      .setLongName(project.longName())
      .setCopyComponentUuid(project.uuid())
      .setScope(Scopes.FILE)
      .setQualifier(Qualifiers.PROJECT)
      .setPath(null)
      .setLanguage(null);
  }

  public static ComponentDto newProjectBranchCopy(String uuid, ComponentDto project, ComponentDto view, String branch) {
    return newChildComponent(uuid, view, view)
      .setKey(generateBranchKey(view.getKey() + project.getKey(), branch))
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
      .setUuid(uuid)
      .setUuidPath(formatUuidPathFromParent(parent))
      .setKey(uuid)
      .setBranchUuid(moduleOrProject.branchUuid())
      .setRootUuid(moduleOrProject.uuid())
      .setModuleUuid(moduleOrProject.uuid())
      .setModuleUuidPath(moduleOrProject.moduleUuidPath())
      .setMainBranchProjectUuid(moduleOrProject.getMainBranchProjectUuid())
      .setCreatedAt(new Date())
      .setEnabled(true)
      .setPrivate(moduleOrProject.isPrivate());
  }

  public static BranchDto newBranchDto(@Nullable String projectUuid, BranchType branchType) {
    String key = projectUuid == null ? null : "branch_" + randomAlphanumeric(248);
    return new BranchDto()
      .setKey(key)
      .setUuid(Uuids.createFast())
      // MainBranchProjectUuid will be null if it's a main branch
      .setProjectUuid(projectUuid)
      .setBranchType(branchType);
  }

  public static BranchDto newBranchDto(ComponentDto project) {
    return newBranchDto(project.branchUuid(), BranchType.BRANCH);
  }

  public static BranchDto newBranchDto(ComponentDto branchComponent, BranchType branchType) {
    boolean isMain = branchComponent.getMainBranchProjectUuid() == null;
    String projectUuid = isMain ? branchComponent.uuid() : branchComponent.getMainBranchProjectUuid();
    String key = isMain ? DEFAULT_MAIN_BRANCH_NAME : "branch_" + randomAlphanumeric(248);

    return new BranchDto()
      .setKey(key)
      .setUuid(branchComponent.uuid())
      .setProjectUuid(projectUuid)
      .setBranchType(branchType);
  }

  public static ComponentDto newBranchComponent(ProjectDto project, BranchDto branchDto) {
    String uuid = branchDto.getUuid();
    return new ComponentDto()
      .setUuid(uuid)
      .setUuidPath(UUID_PATH_OF_ROOT)
      .setBranchUuid(uuid)
      .setModuleUuidPath(UUID_PATH_SEPARATOR + uuid + UUID_PATH_SEPARATOR)
      .setRootUuid(uuid)
      .setKey(project.getKey())
      .setMainBranchProjectUuid(project.getUuid())
      .setName(project.getName())
      .setLongName(project.getName())
      .setDescription(project.getDescription())
      .setScope(Scopes.PROJECT)
      .setQualifier(project.getQualifier())
      .setPath(null)
      .setLanguage(null)
      .setEnabled(true)
      .setPrivate(project.isPrivate());
  }

  public static ComponentDto newBranchComponent(ComponentDto project, BranchDto branchDto) {
    checkArgument(project.qualifier().equals(Qualifiers.PROJECT) || project.qualifier().equals(Qualifiers.APP));
    checkArgument(project.getMainBranchProjectUuid() == null);
    String uuid = branchDto.getUuid();
    return new ComponentDto()
      .setUuid(uuid)
      .setUuidPath(UUID_PATH_OF_ROOT)
      .setBranchUuid(uuid)
      .setModuleUuidPath(UUID_PATH_SEPARATOR + uuid + UUID_PATH_SEPARATOR)
      .setRootUuid(uuid)
      .setKey(project.getKey())
      .setMainBranchProjectUuid(project.uuid())
      .setName(project.name())
      .setLongName(project.longName())
      .setDescription(project.description())
      .setScope(project.scope())
      .setQualifier(project.qualifier())
      .setPath(null)
      .setLanguage(null)
      .setEnabled(true)
      .setPrivate(project.isPrivate());
  }
}
