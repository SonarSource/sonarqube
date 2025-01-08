/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
import org.sonar.core.util.Uuids;
import org.sonar.db.portfolio.PortfolioDto;
import org.sonar.db.project.CreationMethod;
import org.sonar.db.project.ProjectDto;

import static com.google.common.base.Preconditions.checkArgument;
import static org.apache.commons.lang3.RandomStringUtils.secure;
import static org.sonar.db.component.BranchDto.DEFAULT_MAIN_BRANCH_NAME;
import static org.sonar.db.component.ComponentDto.UUID_PATH_OF_ROOT;
import static org.sonar.db.component.ComponentDto.formatUuidPathFromParent;
import static org.sonar.db.component.ComponentDto.generateBranchKey;
import static org.sonar.db.portfolio.PortfolioDto.SelectionMode.NONE;

public class ComponentTesting {

  public static ComponentDto newFileDto(ComponentDto branch) {
    return newFileDto(branch, (ComponentDto) null);
  }

  public static ComponentDto newFileDto(ComponentDto branch, @Nullable ComponentDto directory) {
    return newFileDto(branch, directory, Uuids.createFast());
  }

  public static ComponentDto newFileDto(ComponentDto branch, String mainBranchUuid) {
    return newFileDto(mainBranchUuid, branch, null);
  }

  public static ComponentDto newFileDto(String mainBranchUuid, ComponentDto projectOrBranch, @Nullable ComponentDto directory) {
    return newFileDto(projectOrBranch, directory, Uuids.createFast(), mainBranchUuid);
  }

  public static ComponentDto newFileDto(ComponentDto branch, @Nullable ComponentDto directory, String fileUuid) {
    return newFileDto(branch, directory, fileUuid, null);
  }

  public static ComponentDto newFileDto(ComponentDto branch, @Nullable ComponentDto directory, String fileUuid, @Nullable String mainBranchUuid) {
    String filename = "NAME_" + fileUuid;
    String path = directory != null ? directory.path() + "/" + filename : branch.path() + "/" + filename;
    return newChildComponent(fileUuid, branch, directory == null ? branch : directory)
      .setKey("FILE_KEY_" + fileUuid)
      .setName(filename)
      .setLongName(path)
      .setScope(ComponentScopes.FILE)
      .setBranchUuid(branch.branchUuid())
      .setQualifier(ComponentQualifiers.FILE)
      .setPath(path)
      .setCreatedAt(new Date())
      .setLanguage("xoo");
  }

  public static ComponentDto newDependencyDto(ComponentDto branch, String dependencyUuid) {
    String name = "NAME_" + dependencyUuid;
    return newChildComponent(dependencyUuid, branch, branch)
      .setKey("DEP_KEY_" + dependencyUuid)
      .setName("NAME_" + dependencyUuid)
      .setLongName("LONGNAME_" + dependencyUuid)
      .setScope("DEP")
      .setBranchUuid(branch.branchUuid())
      .setQualifier("DEP")
      .setCreatedAt(new Date());
  }

  public static ComponentDto newDirectory(ComponentDto branch, String path) {
    return newDirectory(branch, Uuids.createFast(), path);
  }

  public static ComponentDto newDirectoryOnBranch(ComponentDto branch, String path, String mainBranchUuid) {
    return newDirectory(branch, Uuids.createFast(), path, mainBranchUuid);
  }

  private static ComponentDto newDirectory(ComponentDto branch, String uuid, String path, String mainBranchUuid) {
    String key = !path.equals("/") ? branch.getKey() + ":" + path : branch.getKey() + ":/";
    return newChildComponent(uuid, branch, branch)
      .setKey(key)
      .setName(path)
      .setLongName(path)
      .setBranchUuid(branch.branchUuid())
      .setPath(path)
      .setScope(ComponentScopes.DIRECTORY)
      .setQualifier(ComponentQualifiers.DIRECTORY);
  }

  public static ComponentDto newDirectory(ComponentDto branch, String uuid, String path) {
    return newDirectory(branch, uuid, path, null);
  }

  public static ComponentDto newSubPortfolio(ComponentDto portfolioOrSubPortfolio, String uuid, String key) {
    return newChildComponent(uuid, portfolioOrSubPortfolio, portfolioOrSubPortfolio)
      .setKey(key)
      .setName(key)
      .setLongName(key)
      .setScope(ComponentScopes.PROJECT)
      .setQualifier(ComponentQualifiers.SUBVIEW)
      .setPath(null);
  }

  public static ComponentDto newSubPortfolio(ComponentDto viewOrSubView) {
    String uuid = Uuids.createFast();
    return newSubPortfolio(viewOrSubView, uuid, "KEY_" + uuid);
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
      .setKey("KEY_" + uuid)
      .setName("NAME_" + uuid)
      .setLongName("LONG_NAME_" + uuid)
      .setDescription("DESCRIPTION_" + uuid)
      .setScope(ComponentScopes.PROJECT)
      .setQualifier(ComponentQualifiers.PROJECT)
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
      .setScope(ComponentScopes.PROJECT)
      .setQualifier(ComponentQualifiers.VIEW)
      .setPrivate(false);
  }

  public static PortfolioDto newPortfolioDto(String uuid, String key, String name, @Nullable PortfolioDto parent) {
    return new PortfolioDto()
      .setUuid(uuid)
      .setKey(key)
      .setParentUuid(parent == null ? null : parent.getUuid())
      .setRootUuid(parent == null ? uuid : parent.getRootUuid())
      .setSelectionMode(NONE.name())
      .setCreatedAt(1L)
      .setUpdatedAt(1L)
      .setPrivate(false)
      .setName(name);
  }

  public static ComponentDto newApplication() {
    return newPortfolio(Uuids.createFast()).setQualifier(ComponentQualifiers.APP);
  }

  public static ComponentDto newProjectCopy(ProjectData project, ProjectData view) {
    return newProjectCopy(Uuids.createFast(), project.getMainBranchComponent(), view.getMainBranchComponent());
  }

  public static ComponentDto newProjectCopy(ProjectData project, PortfolioData view) {
    return newProjectCopy(Uuids.createFast(), project.getMainBranchComponent(), view.getRootComponent());
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
      .setScope(ComponentScopes.FILE)
      .setQualifier(ComponentQualifiers.PROJECT)
      .setPath(null)
      .setLanguage(null);
  }

  public static ComponentDto newProjectBranchCopy(String uuid, ComponentDto project, ComponentDto view, String branch) {
    return newChildComponent(uuid, view, view)
      .setKey(generateBranchKey(view.getKey() + project.getKey(), branch))
      .setName(project.name())
      .setLongName(project.longName())
      .setCopyComponentUuid(project.uuid())
      .setScope(ComponentScopes.FILE)
      .setQualifier(ComponentQualifiers.PROJECT)
      .setPath(null)
      .setLanguage(null);
  }

  public static ComponentDto newChildComponent(String uuid, ComponentDto branch, ComponentDto parent) {
    checkArgument(branch.isPrivate() == parent.isPrivate(),
      "private flag inconsistent between branch (%s) and parent (%s)",
      branch.isPrivate(), parent.isPrivate());
    return new ComponentDto()
      .setUuid(uuid)
      .setUuidPath(formatUuidPathFromParent(parent))
      .setKey(uuid)
      .setBranchUuid(branch.branchUuid())
      .setCreatedAt(new Date())
      .setEnabled(true)
      .setPrivate(branch.isPrivate());
  }


  public static BranchDto newBranchDto(@Nullable String projectUuid, BranchType branchType) {
    String key = "branch_" + secure().nextAlphanumeric(248);
    return new BranchDto()
      .setKey(key)
      .setUuid(Uuids.createFast())
      .setIsMain(false)
      .setProjectUuid(projectUuid)
      .setBranchType(branchType);
  }

  public static BranchDto newBranchDto(ComponentDto project) {
    return newBranchDto(project.branchUuid(), BranchType.BRANCH);
  }

  public static BranchDto newBranchDto(ComponentDto branchComponent, BranchType branchType, String projectUuid) {
    String key = "branch_" + secure().nextAlphanumeric(248);

    return new BranchDto()
      .setKey(key)
      .setIsMain(false)
      .setUuid(branchComponent.uuid())
      .setProjectUuid(projectUuid)
      .setBranchType(branchType);
  }

  public static BranchDto newMainBranchDto(ComponentDto branchComponent, String projectUUid) {
    return new BranchDto()
      .setKey(DEFAULT_MAIN_BRANCH_NAME)
      .setIsMain(true)
      .setUuid(branchComponent.uuid())
      .setProjectUuid(projectUUid)
      .setBranchType(BranchType.BRANCH);
  }

  public static BranchDto newMainBranchDto(String projectUUid) {
    return new BranchDto()
      .setKey(DEFAULT_MAIN_BRANCH_NAME)
      .setIsMain(true)
      .setUuid(Uuids.createFast())
      .setProjectUuid(projectUUid)
      .setBranchType(BranchType.BRANCH);
  }

  public static ProjectDto newProjectDto() {
    return newProjectDto("uuid").setPrivate(true).setCreationMethod(CreationMethod.LOCAL_API);
  }

  public static ProjectDto newProjectDto(String projectUuid) {
    return new ProjectDto()
      .setKey("projectKey")
      .setUuid(projectUuid)
      .setName("projectName")
      .setCreationMethod(CreationMethod.LOCAL_API)
      .setQualifier(ComponentQualifiers.PROJECT);
  }

  public static ProjectDto newApplicationDto() {
    return new ProjectDto()
      .setKey("appKey")
      .setUuid("uuid")
      .setName("appName")
      .setCreationMethod(CreationMethod.LOCAL_API)
      .setQualifier(ComponentQualifiers.APP);
  }

  public static ComponentDto newBranchComponent(ProjectDto project, BranchDto branchDto) {
    String uuid = branchDto.getUuid();
    return new ComponentDto()
      .setUuid(uuid)
      .setUuidPath(UUID_PATH_OF_ROOT)
      .setBranchUuid(uuid)
      .setKey(project.getKey())
      .setName(project.getName())
      .setLongName(project.getName())
      .setDescription(project.getDescription())
      .setScope(ComponentScopes.PROJECT)
      .setQualifier(project.getQualifier())
      .setPath(null)
      .setLanguage(null)
      .setEnabled(true)
      .setPrivate(project.isPrivate());
  }

  public static ComponentDto newBranchComponent(ComponentDto project, BranchDto branchDto) {
    checkArgument(project.qualifier().equals(ComponentQualifiers.PROJECT) || project.qualifier().equals(ComponentQualifiers.APP));
    String uuid = branchDto.getUuid();
    return new ComponentDto()
      .setUuid(uuid)
      .setUuidPath(UUID_PATH_OF_ROOT)
      .setBranchUuid(uuid)
      .setKey(project.getKey())
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
