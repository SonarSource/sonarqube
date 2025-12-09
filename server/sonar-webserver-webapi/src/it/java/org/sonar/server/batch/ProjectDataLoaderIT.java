/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.server.batch;

import org.junit.Rule;
import org.junit.Test;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentQualifiers;
import org.sonar.db.component.ComponentScopes;
import org.sonar.db.component.ProjectData;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.source.FileSourceDto;
import org.sonar.scanner.protocol.input.FileData;
import org.sonar.scanner.protocol.input.ProjectRepositories;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.component.ComponentTypesRule;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.tester.UserSessionRule;

import static com.google.common.collect.ImmutableList.of;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.permission.ProjectPermission.SCAN;

public class ProjectDataLoaderIT {
  @Rule
  public DbTester db = DbTester.create();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  private final DbClient dbClient = db.getDbClient();
  private final DbSession dbSession = db.getSession();
  private final ComponentTypesRule resourceTypes = new ComponentTypesRule().setRootQualifiers(ComponentQualifiers.PROJECT);
  private final ProjectDataLoader underTest = new ProjectDataLoader(dbClient, userSession, new ComponentFinder(dbClient, resourceTypes));

  @Test
  public void throws_NotFoundException_when_branch_does_not_exist() {
    ProjectData projectData = db.components().insertPrivateProject();
    ProjectDto project = projectData.getProjectDto();
    userSession.logIn().addProjectPermission(SCAN, project)
      .registerBranches(projectData.getMainBranchDto());

    assertThatThrownBy(() -> {
      underTest.load(ProjectDataQuery.create()
        .setProjectKey(project.getKey())
        .setBranch("unknown_branch"));
    })
      .isInstanceOf(NotFoundException.class)
      .hasMessage(format("Component '%s' on branch '%s' not found", project.getKey(), "unknown_branch"));
  }

  @Test
  public void return_file_data_from_single_project() {
    ProjectData projectData = db.components().insertPrivateProject();
    ProjectDto project = projectData.getProjectDto();
    userSession.logIn().addProjectPermission(SCAN, project)
      .registerBranches(projectData.getMainBranchDto());
    ComponentDto file = db.components().insertComponent(newFileDto(projectData.getMainBranchComponent()));
    dbClient.fileSourceDao().insert(dbSession, newFileSourceDto(file).setSrcHash("123456"));
    db.commit();

    ProjectRepositories projectRepository = underTest.load(ProjectDataQuery.create().setProjectKey(project.getKey()));
    assertThat(projectRepository.fileData()).hasSize(1);
    FileData fileData = projectRepository.fileDataByPath(file.path());
    assertThat(fileData).isNotNull();
    assertThat(fileData.hash()).isEqualTo("123456");
  }

  @Test
  public void return_file_data_from_branch() {
    ProjectData projectData = db.components().insertPrivateProject();
    ComponentDto mainBranch = projectData.getMainBranchComponent();
    ComponentDto branch = db.components().insertProjectBranch(mainBranch, b -> b.setKey("my_branch"));
    userSession.logIn().addProjectPermission(SCAN, projectData.getProjectDto())
      .registerBranches(projectData.getMainBranchDto())
      .addProjectBranchMapping(projectData.projectUuid(), branch);
    // File on branch
    ComponentDto projectFile = db.components().insertComponent(newFileDto(branch, mainBranch.uuid()));
    dbClient.fileSourceDao().insert(dbSession, newFileSourceDto(projectFile).setSrcHash("123456"));
    dbSession.commit();

    ProjectRepositories ref = underTest.load(ProjectDataQuery.create()
      .setProjectKey(mainBranch.getKey())
      .setBranch("my_branch"));

    assertThat(ref.fileDataByPath(projectFile.path()).hash()).isEqualTo("123456");
  }

  @Test
  public void fails_with_NPE_if_query_is_null() {
    assertThatThrownBy(() -> underTest.load(null))
      .isInstanceOf(NullPointerException.class);
  }

  @Test
  public void fails_with_NFE_if_query_is_empty() {
    assertThatThrownBy(() -> underTest.load(ProjectDataQuery.create()))
      .isInstanceOf(NotFoundException.class)
      .hasMessage("Component key 'null' not found");
  }

  @Test
  public void throws_NotFoundException_if_component_does_not_exist() {
    String key = "theKey";

    assertThatThrownBy(() -> underTest.load(ProjectDataQuery.create().setProjectKey(key)))
      .isInstanceOf(NotFoundException.class)
      .hasMessage("Component key 'theKey' not found");
  }

  @Test
  public void fails_with_BRE_if_component_is_not_root() {
    String uuid = "uuid";
    String key = "key";
    dbClient.componentDao().insertWithAudit(dbSession, new ComponentDto()
      .setUuid(uuid)
      .setUuidPath(uuid + ".")
      .setBranchUuid("branchUuid")
      .setScope(ComponentScopes.PROJECT)
      .setKey("key"));
    dbSession.commit();

    ProjectDataQuery query = ProjectDataQuery.create().setProjectKey(key);
    assertThatThrownBy(() -> underTest.load(query))
      .isInstanceOf(NotFoundException.class)
      .hasMessage("Component key '" + key + "' not found");
  }

  @Test
  public void throw_ForbiddenException_if_no_scan_permission_on_sonarqube() {
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    userSession.logIn();

    assertThatThrownBy(() -> underTest.load(ProjectDataQuery.create().setProjectKey(project.getKey())))
      .isInstanceOf(ForbiddenException.class)
      .hasMessage("You're not authorized to push analysis results to the SonarQube server. Please contact your SonarQube administrator.");
  }

  private static FileSourceDto newFileSourceDto(ComponentDto file) {
    return new FileSourceDto()
      .setUuid(Uuids.createFast())
      .setFileUuid(file.uuid())
      .setProjectUuid(file.branchUuid())
      .setDataHash("0263047cd758c68c27683625f072f010")
      .setLineHashes(of("8d7b3d6b83c0a517eac07e1aac94b773"))
      .setCreatedAt(System.currentTimeMillis())
      .setUpdatedAt(System.currentTimeMillis())
      .setRevision("123456789")
      .setSrcHash("123456");
  }
}
