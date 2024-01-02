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
package org.sonar.server.batch;

import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.Scopes;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ResourceTypesRule;
import org.sonar.db.source.FileSourceDto;
import org.sonar.scanner.protocol.input.FileData;
import org.sonar.scanner.protocol.input.MultiModuleProjectRepository;
import org.sonar.scanner.protocol.input.ProjectRepositories;
import org.sonar.scanner.protocol.input.SingleProjectRepository;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.tester.UserSessionRule;

import static com.google.common.collect.ImmutableList.of;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.sonar.core.permission.GlobalPermissions.SCAN_EXECUTION;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.component.ComponentTesting.newModuleDto;

public class ProjectDataLoaderTest {
  @Rule
  public DbTester db = DbTester.create();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  private DbClient dbClient = db.getDbClient();
  private DbSession dbSession = db.getSession();
  private int uuidCounter = 0;
  private ResourceTypesRule resourceTypes = new ResourceTypesRule().setRootQualifiers(Qualifiers.PROJECT);
  private MapSettings settings = new MapSettings();
  private ProjectDataLoader underTest = new ProjectDataLoader(dbClient, userSession, new ComponentFinder(dbClient, resourceTypes));

  @Test
  public void throws_NotFoundException_when_branch_does_not_exist() {
    ComponentDto project = db.components().insertPrivateProject();
    userSession.logIn().addProjectPermission(SCAN_EXECUTION, project);

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
    ComponentDto project = db.components().insertPrivateProject();
    userSession.logIn().addProjectPermission(SCAN_EXECUTION, project);
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    dbClient.fileSourceDao().insert(dbSession, newFileSourceDto(file).setSrcHash("123456"));
    db.commit();

    ProjectRepositories ref = underTest.load(ProjectDataQuery.create().setProjectKey(project.getKey()));

    assertTrue(ref instanceof SingleProjectRepository);
    SingleProjectRepository singleProjectRepository = ((SingleProjectRepository) ref);
    assertThat(singleProjectRepository.fileData()).hasSize(1);
    FileData fileData = singleProjectRepository.fileDataByPath(file.path());
    assertThat(fileData).isNotNull();
    assertThat(fileData.hash()).isEqualTo("123456");
  }

  @Test
  public void return_file_data_from_multi_modules() {
    ComponentDto project = db.components().insertPrivateProject();
    userSession.logIn().addProjectPermission(SCAN_EXECUTION, project);
    ComponentDto module = db.components().insertComponent(newModuleDto(project));
    // File on project
    ComponentDto projectFile = db.components().insertComponent(newFileDto(project));
    dbClient.fileSourceDao().insert(dbSession, newFileSourceDto(projectFile).setSrcHash("123456"));
    // File on module
    ComponentDto moduleFile = db.components().insertComponent(newFileDto(module));
    dbClient.fileSourceDao().insert(dbSession, newFileSourceDto(moduleFile).setSrcHash("789456"));
    dbSession.commit();

    ProjectRepositories ref = underTest.load(ProjectDataQuery.create().setProjectKey(project.getKey()));

    assertTrue(ref instanceof MultiModuleProjectRepository);
    MultiModuleProjectRepository repository = ((MultiModuleProjectRepository) ref);
    assertThat(repository.fileData(project.getKey(), projectFile.path()).hash()).isEqualTo("123456");
    assertThat(repository.fileData(module.getKey(), moduleFile.path()).hash()).isEqualTo("789456");
  }

  @Test
  public void return_file_data_from_branch() {
    ComponentDto project = db.components().insertPrivateProject();
    ComponentDto branch = db.components().insertProjectBranch(project, b -> b.setKey("my_branch"));
    userSession.logIn().addProjectPermission(SCAN_EXECUTION, project);
    ComponentDto moduleBranch = db.components().insertComponent(newModuleDto(branch));
    // File on branch
    ComponentDto projectFile = db.components().insertComponent(newFileDto(branch));
    dbClient.fileSourceDao().insert(dbSession, newFileSourceDto(projectFile).setSrcHash("123456"));
    // File on moduleBranch branch
    ComponentDto moduleFile = db.components().insertComponent(newFileDto(moduleBranch));
    dbClient.fileSourceDao().insert(dbSession, newFileSourceDto(moduleFile).setSrcHash("789456"));
    dbSession.commit();

    ProjectRepositories ref = underTest.load(ProjectDataQuery.create()
      .setProjectKey(project.getKey())
      .setBranch("my_branch"));

    assertTrue(ref instanceof MultiModuleProjectRepository);
    MultiModuleProjectRepository repository = ((MultiModuleProjectRepository) ref);
    assertThat(repository.fileData(branch.getKey(), projectFile.path()).hash()).isEqualTo("123456");
    assertThat(repository.fileData(moduleBranch.getKey(), moduleFile.path()).hash()).isEqualTo("789456");
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
  public void fails_with_BRE_if_component_is_not_a_project() {
    String[][] allScopesAndQualifierButProjectAndModule = {
      {Scopes.PROJECT, "fakeModuleUuid"},
      {Scopes.FILE, null},
      {Scopes.DIRECTORY, null}
    };

    for (String[] scopeAndQualifier : allScopesAndQualifierButProjectAndModule) {
      String scope = scopeAndQualifier[0];
      String moduleUuid = scopeAndQualifier[1];
      String key = "theKey_" + scope + "_" + moduleUuid;
      String uuid = "uuid_" + uuidCounter++;
      dbClient.componentDao().insert(dbSession, new ComponentDto()
        .setUuid(uuid)
        .setUuidPath(uuid + ".")
        .setRootUuid(uuid)
        .setBranchUuid(uuid)
        .setScope(scope)
        .setModuleUuid(moduleUuid)
        .setKey(key));
      dbSession.commit();

      try {
        underTest.load(ProjectDataQuery.create().setProjectKey(key));
        fail("A NotFoundException should have been raised because component is not project");
      } catch (BadRequestException e) {
        assertThat(e).hasMessage("Key '" + key + "' belongs to a component which is not a Project");
      }
    }
  }

  @Test
  public void throw_ForbiddenException_if_no_scan_permission_on_sonarqube() {
    ComponentDto project = db.components().insertPrivateProject();
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
