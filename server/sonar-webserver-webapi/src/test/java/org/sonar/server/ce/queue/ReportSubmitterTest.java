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
package org.sonar.server.ce.queue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Random;
import java.util.stream.IntStream;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.ce.queue.CeQueue;
import org.sonar.ce.queue.CeQueueImpl;
import org.sonar.ce.queue.CeTaskSubmit;
import org.sonar.core.i18n.I18n;
import org.sonar.core.util.SequenceUuidFactory;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.ce.CeTaskTypes;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.permission.GlobalPermission;
import org.sonar.db.user.UserDto;
import org.sonar.server.component.ComponentUpdater;
import org.sonar.server.es.TestProjectIndexers;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.favorite.FavoriteUpdater;
import org.sonar.server.permission.PermissionTemplateService;
import org.sonar.server.project.DefaultBranchNameResolver;
import org.sonar.server.project.ProjectDefaultVisibility;
import org.sonar.server.project.Visibility;
import org.sonar.server.tester.UserSessionRule;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptyMap;
import static java.util.stream.IntStream.rangeClosed;
import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.core.permission.GlobalPermissions.SCAN_EXECUTION;
import static org.sonar.core.util.stream.MoreCollectors.uniqueIndex;
import static org.sonar.db.component.BranchDto.DEFAULT_MAIN_BRANCH_NAME;
import static org.sonar.db.component.ComponentTesting.newModuleDto;
import static org.sonar.db.permission.GlobalPermission.PROVISION_PROJECTS;
import static org.sonar.db.permission.GlobalPermission.SCAN;

public class ReportSubmitterTest {

  private static final String PROJECT_KEY = "MY_PROJECT";
  private static final String PROJECT_UUID = "P1";
  private static final String PROJECT_NAME = "My Project";
  private static final String TASK_UUID = "TASK_1";

  @Rule
  public final UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public final DbTester db = DbTester.create();

  private final ProjectDefaultVisibility projectDefaultVisibility = mock(ProjectDefaultVisibility.class);
  private final DefaultBranchNameResolver defaultBranchNameResolver = mock(DefaultBranchNameResolver.class);

  private final CeQueue queue = mock(CeQueueImpl.class);
  private final TestProjectIndexers projectIndexers = new TestProjectIndexers();
  private final PermissionTemplateService permissionTemplateService = mock(PermissionTemplateService.class);
  private final ComponentUpdater componentUpdater = new ComponentUpdater(db.getDbClient(), mock(I18n.class), mock(System2.class), permissionTemplateService,
    new FavoriteUpdater(db.getDbClient()), projectIndexers, new SequenceUuidFactory(), defaultBranchNameResolver
    );
  private final BranchSupport ossEditionBranchSupport = new BranchSupport(null);

  private final ReportSubmitter underTest = new ReportSubmitter(queue, userSession, componentUpdater, permissionTemplateService, db.getDbClient(), ossEditionBranchSupport,
    projectDefaultVisibility);

  @Before
  public void before() {
    when(projectDefaultVisibility.get(any())).thenReturn(Visibility.PUBLIC);
    when(defaultBranchNameResolver.getEffectiveMainBranchName()).thenReturn(DEFAULT_MAIN_BRANCH_NAME);
  }

  @Test
  public void submit_with_characteristics_fails_with_ISE_when_no_branch_support_delegate() {
    userSession
      .addPermission(GlobalPermission.SCAN)
      .addPermission(PROVISION_PROJECTS);
    mockSuccessfulPrepareSubmitCall();
    when(permissionTemplateService.wouldUserHaveScanPermissionWithDefaultTemplate(any(), any(), eq(PROJECT_KEY)))
      .thenReturn(true);
    Map<String, String> nonEmptyCharacteristics = IntStream.range(0, 1 + new Random().nextInt(5))
      .boxed()
      .collect(uniqueIndex(i -> randomAlphabetic(i + 10), i -> randomAlphabetic(i + 20)));
    InputStream reportInput = IOUtils.toInputStream("{binary}", UTF_8);

    assertThatThrownBy(() -> underTest.submit(PROJECT_KEY, PROJECT_NAME, nonEmptyCharacteristics, reportInput))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Current edition does not support branch feature");
  }

  @Test
  public void submit_stores_report() {
    userSession
      .addPermission(GlobalPermission.SCAN)
      .addPermission(PROVISION_PROJECTS);
    mockSuccessfulPrepareSubmitCall();
    when(permissionTemplateService.wouldUserHaveScanPermissionWithDefaultTemplate(any(), any(), eq(PROJECT_KEY)))
      .thenReturn(true);

    underTest.submit(PROJECT_KEY, PROJECT_NAME, emptyMap(), IOUtils.toInputStream("{binary}", UTF_8));

    verifyReportIsPersisted(TASK_UUID);
  }

  @Test
  public void submit_a_report_on_existing_project() {
    ComponentDto project = db.components().insertPrivateProject();
    UserDto user = db.users().insertUser();
    userSession.logIn(user).addProjectPermission(SCAN_EXECUTION, project);
    mockSuccessfulPrepareSubmitCall();

    underTest.submit(project.getKey(), project.name(), emptyMap(), IOUtils.toInputStream("{binary}", StandardCharsets.UTF_8));

    verifyReportIsPersisted(TASK_UUID);
    verifyNoInteractions(permissionTemplateService);
    verify(queue).submit(argThat(submit -> submit.getType().equals(CeTaskTypes.REPORT)
      && submit.getComponent().filter(cpt -> cpt.getUuid().equals(project.uuid()) && cpt.getMainComponentUuid().equals(project.uuid())).isPresent()
      && submit.getSubmitterUuid().equals(user.getUuid())
      && submit.getUuid().equals(TASK_UUID)));
  }

  @Test
  public void provision_project_if_does_not_exist() {
    userSession
      .addPermission(GlobalPermission.SCAN)
      .addPermission(PROVISION_PROJECTS);
    mockSuccessfulPrepareSubmitCall();
    when(permissionTemplateService.wouldUserHaveScanPermissionWithDefaultTemplate(any(DbSession.class), any(), eq(PROJECT_KEY))).thenReturn(true);
    when(permissionTemplateService.hasDefaultTemplateWithPermissionOnProjectCreator(any(DbSession.class), any(ComponentDto.class))).thenReturn(true);

    underTest.submit(PROJECT_KEY, PROJECT_NAME, emptyMap(), IOUtils.toInputStream("{binary}"));

    ComponentDto createdProject = db.getDbClient().componentDao().selectByKey(db.getSession(), PROJECT_KEY).get();
    verifyReportIsPersisted(TASK_UUID);
    verify(queue).submit(argThat(submit -> submit.getType().equals(CeTaskTypes.REPORT)
      && submit.getComponent().filter(cpt -> cpt.getUuid().equals(createdProject.uuid()) && cpt.getMainComponentUuid().equals(createdProject.uuid())).isPresent()
      && submit.getUuid().equals(TASK_UUID)));
  }

  @Test
  public void add_project_as_favorite_when_project_creator_permission_on_permission_template() {
    UserDto user = db.users().insertUser();
    userSession
      .logIn(user)
      .addPermission(GlobalPermission.SCAN)
      .addPermission(PROVISION_PROJECTS);
    mockSuccessfulPrepareSubmitCall();
    when(permissionTemplateService.wouldUserHaveScanPermissionWithDefaultTemplate(any(DbSession.class), any(), eq(PROJECT_KEY))).thenReturn(true);
    when(permissionTemplateService.hasDefaultTemplateWithPermissionOnProjectCreator(any(DbSession.class), any(ComponentDto.class))).thenReturn(true);

    underTest.submit(PROJECT_KEY, PROJECT_NAME, emptyMap(), IOUtils.toInputStream("{binary}"));

    ComponentDto createdProject = db.getDbClient().componentDao().selectByKey(db.getSession(), PROJECT_KEY).get();
    assertThat(db.favorites().hasFavorite(createdProject, user.getUuid())).isTrue();
  }

  @Test
  public void do_no_add_favorite_when_no_project_creator_permission_on_permission_template() {
    userSession
      .addPermission(GlobalPermission.SCAN)
      .addPermission(PROVISION_PROJECTS);
    when(permissionTemplateService.wouldUserHaveScanPermissionWithDefaultTemplate(any(DbSession.class), any(), eq(PROJECT_KEY)))
      .thenReturn(true);
    when(permissionTemplateService.hasDefaultTemplateWithPermissionOnProjectCreator(any(DbSession.class), any(ComponentDto.class))).thenReturn(false);
    mockSuccessfulPrepareSubmitCall();

    underTest.submit(PROJECT_KEY, PROJECT_NAME, emptyMap(), IOUtils.toInputStream("{binary}"));

    ComponentDto createdProject = db.getDbClient().componentDao().selectByKey(db.getSession(), PROJECT_KEY).get();
    assertThat(db.favorites().hasNoFavorite(createdProject)).isTrue();
  }

  @Test
  public void do_no_add_favorite_when_already_100_favorite_projects_and_no_project_creator_permission_on_permission_template() {
    UserDto user = db.users().insertUser();
    rangeClosed(1, 100).forEach(i -> db.favorites().add(db.components().insertPrivateProject(), user.getUuid(), user.getLogin()));
    userSession
      .logIn(user)
      .addPermission(GlobalPermission.SCAN)
      .addPermission(PROVISION_PROJECTS);
    mockSuccessfulPrepareSubmitCall();
    when(permissionTemplateService.wouldUserHaveScanPermissionWithDefaultTemplate(any(DbSession.class), any(), eq(PROJECT_KEY))).thenReturn(true);
    when(permissionTemplateService.hasDefaultTemplateWithPermissionOnProjectCreator(any(DbSession.class), any(ComponentDto.class))).thenReturn(true);

    underTest.submit(PROJECT_KEY, PROJECT_NAME, emptyMap(), IOUtils.toInputStream("{binary}"));

    ComponentDto createdProject = db.getDbClient().componentDao().selectByKey(db.getSession(), PROJECT_KEY).get();
    assertThat(db.favorites().hasNoFavorite(createdProject)).isTrue();
  }

  @Test
  public void submit_a_report_on_new_project_with_scan_permission() {
    userSession
      .addPermission(GlobalPermission.SCAN)
      .addPermission(PROVISION_PROJECTS);
    mockSuccessfulPrepareSubmitCall();
    when(permissionTemplateService.wouldUserHaveScanPermissionWithDefaultTemplate(any(DbSession.class), any(), eq(PROJECT_KEY)))
      .thenReturn(true);

    underTest.submit(PROJECT_KEY, PROJECT_NAME, emptyMap(), IOUtils.toInputStream("{binary}"));

    verify(queue).submit(any(CeTaskSubmit.class));
  }

  @Test
  public void user_with_scan_permission_is_allowed_to_submit_a_report_on_existing_project() {
    ComponentDto project = db.components().insertPrivateProject();
    userSession.addPermission(SCAN);
    mockSuccessfulPrepareSubmitCall();

    underTest.submit(project.getKey(), project.name(), emptyMap(), IOUtils.toInputStream("{binary}"));

    verify(queue).submit(any(CeTaskSubmit.class));
  }

  @Test
  public void submit_a_report_on_existing_project_with_project_scan_permission() {
    ComponentDto project = db.components().insertPrivateProject();
    userSession.addProjectPermission(SCAN_EXECUTION, project);
    mockSuccessfulPrepareSubmitCall();

    underTest.submit(project.getKey(), project.name(), emptyMap(), IOUtils.toInputStream("{binary}"));

    verify(queue).submit(any(CeTaskSubmit.class));
  }

  @Test
  public void fail_if_component_is_not_a_project() {
    ComponentDto component = db.components().insertPublicPortfolio();
    userSession.logIn().addProjectPermission(SCAN_EXECUTION, component);
    mockSuccessfulPrepareSubmitCall();

    String dbKey = component.getKey();
    String name = component.name();
    Map<String, String> emptyMap = emptyMap();
    InputStream stream = IOUtils.toInputStream("{binary}", UTF_8);
    assertThatThrownBy(() -> underTest.submit(dbKey, name, emptyMap, stream))
      .isInstanceOf(BadRequestException.class)
      .hasMessage(format("Component '%s' is not a project", component.getKey()));
  }

  @Test
  public void fail_if_project_key_already_exists_as_module() {
    ComponentDto project = db.components().insertPrivateProject();
    ComponentDto module = db.components().insertComponent(newModuleDto(project));
    userSession.logIn().addProjectPermission(SCAN_EXECUTION, project);
    mockSuccessfulPrepareSubmitCall();

    String moduleDbKey = module.getKey();
    String name = module.name();
    Map<String, String> emptyMap = emptyMap();
    InputStream inputStream = IOUtils.toInputStream("{binary}", UTF_8);
    assertThatThrownBy(() -> underTest.submit(moduleDbKey, name, emptyMap, inputStream))
      .isInstanceOf(BadRequestException.class)
      .extracting(throwable -> ((BadRequestException) throwable).errors())
      .asList()
      .contains(format("The project '%s' is already defined in SonarQube but as a module of project '%s'. " +
          "If you really want to stop directly analysing project '%s', please first delete it from SonarQube and then relaunch the analysis of project '%s'.",
        module.getKey(), project.getKey(), project.getKey(), module.getKey()));
  }

  @Test
  public void fail_with_forbidden_exception_when_no_scan_permission() {
    Map<String, String> emptyMap = emptyMap();
    InputStream inputStream = IOUtils.toInputStream("{binary}", UTF_8);
    assertThatThrownBy(() -> underTest.submit(PROJECT_KEY, PROJECT_NAME, emptyMap, inputStream))
      .isInstanceOf(ForbiddenException.class);
  }

  @Test
  public void fail_with_forbidden_exception_on_new_project_when_only_project_scan_permission() {
    ComponentDto component = db.components().insertPrivateProject(PROJECT_UUID);
    userSession.addProjectPermission(SCAN_EXECUTION, component);
    mockSuccessfulPrepareSubmitCall();

    Map<String, String> emptyMap = emptyMap();
    InputStream inputStream = IOUtils.toInputStream("{binary}", UTF_8);
    assertThatThrownBy(() -> underTest.submit(PROJECT_KEY, PROJECT_NAME, emptyMap, inputStream))
      .isInstanceOf(ForbiddenException.class);
  }

  private void verifyReportIsPersisted(String taskUuid) {
    assertThat(db.selectFirst("select task_uuid from ce_task_input where task_uuid='" + taskUuid + "'")).isNotNull();
  }

  private void mockSuccessfulPrepareSubmitCall() {
    when(queue.prepareSubmit()).thenReturn(new CeTaskSubmit.Builder(TASK_UUID));
  }

}
