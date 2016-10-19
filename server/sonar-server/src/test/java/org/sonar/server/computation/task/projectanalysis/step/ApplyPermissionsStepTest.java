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
package org.sonar.server.computation.task.projectanalysis.step;

import java.util.List;
import java.util.Map;
import org.elasticsearch.search.SearchHit;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.MapSettings;
import org.sonar.api.config.Settings;
import org.sonar.api.security.DefaultGroups;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.permission.PermissionRepository;
import org.sonar.db.permission.template.PermissionTemplateDto;
import org.sonar.server.computation.task.projectanalysis.component.Component;
import org.sonar.server.computation.task.projectanalysis.component.MutableDbIdsRepositoryRule;
import org.sonar.server.computation.task.projectanalysis.component.ReportComponent;
import org.sonar.server.computation.task.projectanalysis.component.TreeRootHolderRule;
import org.sonar.server.computation.task.projectanalysis.component.ViewsComponent;
import org.sonar.server.computation.task.step.ComputationStep;
import org.sonar.server.es.EsTester;
import org.sonar.server.issue.index.IssueAuthorizationIndexer;
import org.sonar.server.issue.index.IssueIndexDefinition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.component.ComponentTesting.newView;
import static org.sonar.db.permission.template.PermissionTemplateTesting.newPermissionTemplateDto;
import static org.sonar.server.computation.task.projectanalysis.component.Component.Type.PROJECT;
import static org.sonar.server.computation.task.projectanalysis.component.Component.Type.VIEW;

public class ApplyPermissionsStepTest extends BaseStepTest {

  private static final String ROOT_KEY = "ROOT_KEY";
  private static final String ROOT_UUID = "ROOT_UUID";
  private static final long SOME_DATE = 1000L;

  @Rule
  public EsTester esTester = new EsTester(new IssueIndexDefinition(new MapSettings()));

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule();

  @Rule
  public MutableDbIdsRepositoryRule dbIdsRepository = MutableDbIdsRepositoryRule.create(treeRootHolder);

  private DbSession dbSession;
  private DbClient dbClient = dbTester.getDbClient();
  private Settings settings = new MapSettings();
  private IssueAuthorizationIndexer issueAuthorizationIndexer;
  private ApplyPermissionsStep step;

  @Before
  public void setUp() {
    dbSession = dbClient.openSession(false);

    issueAuthorizationIndexer = new IssueAuthorizationIndexer(dbClient, esTester.client());

    step = new ApplyPermissionsStep(dbClient, dbIdsRepository, issueAuthorizationIndexer, new PermissionRepository(dbClient, settings), treeRootHolder);
  }

  @After
  public void tearDown() {
    dbSession.close();
  }

  @Test
  public void grant_permission_on_new_project() {
    ComponentDto projectDto = ComponentTesting.newProjectDto(ROOT_UUID).setKey(ROOT_KEY);
    dbClient.componentDao().insert(dbSession, projectDto);

    // Create a permission template containing browse permission for anonymous group
    createDefaultPermissionTemplate(UserRole.USER);

    Component project = ReportComponent.builder(PROJECT, 1).setUuid(ROOT_UUID).setKey(ROOT_KEY).build();
    dbIdsRepository.setComponentId(project, projectDto.getId());
    treeRootHolder.setRoot(project);

    step.execute();
    dbSession.commit();

    assertThat(dbClient.componentDao().selectOrFailByKey(dbSession, ROOT_KEY).getAuthorizationUpdatedAt()).isNotNull();
    assertThat(dbClient.groupPermissionDao().selectProjectPermissionsOfGroup(dbSession,
      dbTester.getDefaultOrganization().getUuid(), null, projectDto.getId())).containsOnly(UserRole.USER);
    verifyAuthorisationIndex(ROOT_UUID, DefaultGroups.ANYONE);
  }

  @Test
  public void nothing_to_do_on_existing_project() {
    ComponentDto projectDto = ComponentTesting.newProjectDto(ROOT_UUID).setKey(ROOT_KEY).setAuthorizationUpdatedAt(SOME_DATE);
    dbTester.components().insertComponent(projectDto);
    // Permissions are already set on the project
    dbTester.users().insertProjectPermissionOnAnyone(UserRole.USER, projectDto);

    dbSession.commit();

    Component project = ReportComponent.builder(PROJECT, 1).setUuid(ROOT_UUID).setKey(ROOT_KEY).build();
    dbIdsRepository.setComponentId(project, projectDto.getId());
    treeRootHolder.setRoot(project);

    step.execute();
    dbSession.commit();

    // Check that authorization updated at has not been changed -> Nothing has been done
    assertThat(projectDto.getAuthorizationUpdatedAt()).isEqualTo(SOME_DATE);
  }

  @Test
  public void grant_permission_on_new_view() {
    ComponentDto viewDto = newView(ROOT_UUID).setKey(ROOT_KEY);
    dbClient.componentDao().insert(dbSession, viewDto);

    String permission = UserRole.USER;
    // Create a permission template containing browse permission for anonymous group
    createDefaultPermissionTemplate(permission);

    Component project = ViewsComponent.builder(VIEW, ROOT_KEY).setUuid(ROOT_UUID).build();
    dbIdsRepository.setComponentId(project, viewDto.getId());
    treeRootHolder.setRoot(project);

    step.execute();
    dbSession.commit();

    assertThat(dbClient.componentDao().selectOrFailByKey(dbSession, ROOT_KEY).getAuthorizationUpdatedAt()).isNotNull();
    assertThat(dbClient.groupPermissionDao().selectProjectPermissionsOfGroup(dbSession,
      dbTester.getDefaultOrganization().getUuid(), null, viewDto.getId())).containsOnly(permission);
  }

  @Test
  public void nothing_to_do_on_existing_view() {
    ComponentDto viewDto = newView(ROOT_UUID).setKey(ROOT_KEY).setAuthorizationUpdatedAt(SOME_DATE);
    dbTester.components().insertComponent(viewDto);
    // Permissions are already set on the view
    dbTester.users().insertProjectPermissionOnAnyone(UserRole.USER, viewDto);

    Component project = ReportComponent.builder(PROJECT, 1).setUuid(ROOT_UUID).setKey(ROOT_KEY).build();
    dbIdsRepository.setComponentId(project, viewDto.getId());
    treeRootHolder.setRoot(project);

    step.execute();
    dbSession.commit();

    // Check that authorization updated at has not been changed -> Nothing has been done
    assertThat(viewDto.getAuthorizationUpdatedAt()).isEqualTo(SOME_DATE);
  }

  private void createDefaultPermissionTemplate(String permission) {
    PermissionTemplateDto defaultTemplate = newPermissionTemplateDto()
      .setOrganizationUuid(dbTester.getDefaultOrganization().getUuid())
      .setName("Default");
    dbClient.permissionTemplateDao().insert(dbSession, defaultTemplate);
    settings.setProperty("sonar.permission.template.default", defaultTemplate.getKee());
    dbClient.permissionTemplateDao().insertGroupPermission(dbSession, defaultTemplate.getId(), null, permission);
    dbSession.commit();
  }

  private void verifyAuthorisationIndex(String rootUuid, String groupPermission) {
    List<SearchHit> issueAuthorizationHits = esTester.getDocuments(IssueIndexDefinition.INDEX, IssueIndexDefinition.TYPE_AUTHORIZATION);
    assertThat(issueAuthorizationHits).hasSize(1);
    Map<String, Object> issueAuthorization = issueAuthorizationHits.get(0).sourceAsMap();
    assertThat(issueAuthorization.get("project")).isEqualTo(rootUuid);
    assertThat((List<String>) issueAuthorization.get("groups")).containsOnly(groupPermission);
    assertThat((List<String>) issueAuthorization.get("users")).isEmpty();
  }

  @Override
  protected ComputationStep step() {
    return step;
  }
}
