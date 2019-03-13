/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import org.junit.rules.ExpectedException;
import org.sonar.api.i18n.I18n;
import org.sonar.api.utils.System2;
import org.sonar.ce.queue.CeQueue;
import org.sonar.ce.queue.CeQueueImpl;
import org.sonar.ce.queue.CeTaskSubmit;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.ce.CeTaskTypes;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.permission.OrganizationPermission;
import org.sonar.db.user.UserDto;
import org.sonar.server.component.ComponentUpdater;
import org.sonar.server.es.TestProjectIndexers;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.favorite.FavoriteUpdater;
import org.sonar.server.permission.PermissionTemplateService;
import org.sonar.server.tester.UserSessionRule;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptyMap;
import static java.util.stream.IntStream.rangeClosed;
import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.core.permission.GlobalPermissions.SCAN_EXECUTION;
import static org.sonar.core.util.stream.MoreCollectors.uniqueIndex;
import static org.sonar.db.component.ComponentTesting.newModuleDto;
import static org.sonar.db.permission.OrganizationPermission.PROVISION_PROJECTS;
import static org.sonar.db.permission.OrganizationPermission.SCAN;

public class ReportSubmitterTest {

  private static final String PROJECT_KEY = "MY_PROJECT";
  private static final String PROJECT_UUID = "P1";
  private static final String PROJECT_NAME = "My Project";
  private static final String TASK_UUID = "TASK_1";

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create();

  private String defaultOrganizationKey;
  private String defaultOrganizationUuid;

  private CeQueue queue = mock(CeQueueImpl.class);
  private TestProjectIndexers projectIndexers = new TestProjectIndexers();
  private PermissionTemplateService permissionTemplateService = mock(PermissionTemplateService.class);
  private ComponentUpdater componentUpdater = new ComponentUpdater(db.getDbClient(), mock(I18n.class), mock(System2.class), permissionTemplateService,
    new FavoriteUpdater(db.getDbClient()), projectIndexers);
  private BranchSupport ossEditionBranchSupport = new BranchSupport();

  private ReportSubmitter underTest = new ReportSubmitter(queue, userSession, componentUpdater, permissionTemplateService, db.getDbClient(), ossEditionBranchSupport);

  @Before
  public void setUp() throws Exception {
    defaultOrganizationKey = db.getDefaultOrganization().getKey();
    defaultOrganizationUuid = db.getDefaultOrganization().getUuid();
  }

  @Test
  public void submit_with_characteristics_fails_with_ISE_when_no_branch_support_delegate() {
    userSession
      .addPermission(OrganizationPermission.SCAN, db.getDefaultOrganization().getUuid())
      .addPermission(PROVISION_PROJECTS, db.getDefaultOrganization());
    mockSuccessfulPrepareSubmitCall();
    when(permissionTemplateService.wouldUserHaveScanPermissionWithDefaultTemplate(any(), eq(defaultOrganizationUuid), any(), eq(PROJECT_KEY)))
      .thenReturn(true);
    Map<String, String> nonEmptyCharacteristics = IntStream.range(0, 1 + new Random().nextInt(5))
      .boxed()
      .collect(uniqueIndex(i -> randomAlphabetic(i + 10), i -> randomAlphabetic(i + 20)));
    InputStream reportInput = IOUtils.toInputStream("{binary}", UTF_8);

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Current edition does not support branch feature");

    underTest.submit(defaultOrganizationKey, PROJECT_KEY, null, PROJECT_NAME, nonEmptyCharacteristics, reportInput);
  }

  @Test
  public void submit_stores_report() {
    userSession
      .addPermission(OrganizationPermission.SCAN, db.getDefaultOrganization().getUuid())
      .addPermission(PROVISION_PROJECTS, db.getDefaultOrganization());
    mockSuccessfulPrepareSubmitCall();
    when(permissionTemplateService.wouldUserHaveScanPermissionWithDefaultTemplate(any(), eq(defaultOrganizationUuid), any(), eq(PROJECT_KEY)))
      .thenReturn(true);

    underTest.submit(defaultOrganizationKey, PROJECT_KEY, null, PROJECT_NAME, emptyMap(), IOUtils.toInputStream("{binary}", UTF_8));

    verifyReportIsPersisted(TASK_UUID);
  }

  @Test
  public void submit_a_report_on_existing_project() {
    ComponentDto project = db.components().insertPrivateProject(db.getDefaultOrganization());
    UserDto user = db.users().insertUser();
    userSession.logIn(user).addProjectPermission(SCAN_EXECUTION, project);
    mockSuccessfulPrepareSubmitCall();

    underTest.submit(defaultOrganizationKey, project.getDbKey(), null, project.name(), emptyMap(), IOUtils.toInputStream("{binary}", StandardCharsets.UTF_8));

    verifyReportIsPersisted(TASK_UUID);
    verifyZeroInteractions(permissionTemplateService);
    verify(queue).submit(argThat(submit -> submit.getType().equals(CeTaskTypes.REPORT)
      && submit.getComponent().filter(cpt -> cpt.getUuid().equals(project.uuid()) && cpt.getMainComponentUuid().equals(project.uuid())).isPresent()
      && submit.getSubmitterUuid().equals(user.getUuid())
      && submit.getUuid().equals(TASK_UUID)));
  }

  @Test
  public void provision_project_if_does_not_exist() {
    OrganizationDto organization = db.organizations().insert();
    userSession
      .addPermission(OrganizationPermission.SCAN, organization.getUuid())
      .addPermission(PROVISION_PROJECTS, organization);
    mockSuccessfulPrepareSubmitCall();
    when(permissionTemplateService.wouldUserHaveScanPermissionWithDefaultTemplate(any(DbSession.class), eq(organization.getUuid()), any(), eq(PROJECT_KEY))).thenReturn(true);
    when(permissionTemplateService.hasDefaultTemplateWithPermissionOnProjectCreator(any(DbSession.class), any(ComponentDto.class))).thenReturn(true);

    underTest.submit(organization.getKey(), PROJECT_KEY, null, PROJECT_NAME, emptyMap(), IOUtils.toInputStream("{binary}"));

    ComponentDto createdProject = db.getDbClient().componentDao().selectByKey(db.getSession(), PROJECT_KEY).get();
    verifyReportIsPersisted(TASK_UUID);
    verify(queue).submit(argThat(submit -> submit.getType().equals(CeTaskTypes.REPORT)
      && submit.getComponent().filter(cpt -> cpt.getUuid().equals(createdProject.uuid()) && cpt.getMainComponentUuid().equals(createdProject.uuid())).isPresent()
      && submit.getUuid().equals(TASK_UUID)));
  }

  @Test
  public void add_project_as_favorite_when_project_creator_permission_on_permission_template() {
    UserDto user = db.users().insertUser();
    OrganizationDto organization = db.organizations().insert();
    userSession
      .logIn(user)
      .addPermission(OrganizationPermission.SCAN, organization.getUuid())
      .addPermission(PROVISION_PROJECTS, organization);
    mockSuccessfulPrepareSubmitCall();
    when(permissionTemplateService.wouldUserHaveScanPermissionWithDefaultTemplate(any(DbSession.class), eq(organization.getUuid()), any(), eq(PROJECT_KEY))).thenReturn(true);
    when(permissionTemplateService.hasDefaultTemplateWithPermissionOnProjectCreator(any(DbSession.class), any(ComponentDto.class))).thenReturn(true);

    underTest.submit(organization.getKey(), PROJECT_KEY, null, PROJECT_NAME, emptyMap(), IOUtils.toInputStream("{binary}"));

    ComponentDto createdProject = db.getDbClient().componentDao().selectByKey(db.getSession(), PROJECT_KEY).get();
    assertThat(db.favorites().hasFavorite(createdProject, user.getId())).isTrue();
  }

  @Test
  public void do_no_add_favorite_when_no_project_creator_permission_on_permission_template() {
    userSession
      .addPermission(OrganizationPermission.SCAN, db.getDefaultOrganization().getUuid())
      .addPermission(PROVISION_PROJECTS, db.getDefaultOrganization());
    when(permissionTemplateService.wouldUserHaveScanPermissionWithDefaultTemplate(any(DbSession.class), eq(defaultOrganizationUuid), any(), eq(PROJECT_KEY)))
      .thenReturn(true);
    when(permissionTemplateService.hasDefaultTemplateWithPermissionOnProjectCreator(any(DbSession.class), any(ComponentDto.class))).thenReturn(false);
    mockSuccessfulPrepareSubmitCall();

    underTest.submit(defaultOrganizationKey, PROJECT_KEY, null, PROJECT_NAME, emptyMap(), IOUtils.toInputStream("{binary}"));

    ComponentDto createdProject = db.getDbClient().componentDao().selectByKey(db.getSession(), PROJECT_KEY).get();
    assertThat(db.favorites().hasNoFavorite(createdProject)).isTrue();
  }

  @Test
  public void do_no_add_favorite_when_already_100_favorite_projects_and_no_project_creator_permission_on_permission_template() {
    UserDto user = db.users().insertUser();
    rangeClosed(1, 100).forEach(i -> db.favorites().add(db.components().insertPrivateProject(), user.getId()));
    OrganizationDto organization = db.organizations().insert();
    userSession
      .logIn(user)
      .addPermission(OrganizationPermission.SCAN, organization.getUuid())
      .addPermission(PROVISION_PROJECTS, organization);
    mockSuccessfulPrepareSubmitCall();
    when(permissionTemplateService.wouldUserHaveScanPermissionWithDefaultTemplate(any(DbSession.class), eq(organization.getUuid()), any(), eq(PROJECT_KEY))).thenReturn(true);
    when(permissionTemplateService.hasDefaultTemplateWithPermissionOnProjectCreator(any(DbSession.class), any(ComponentDto.class))).thenReturn(true);

    underTest.submit(organization.getKey(), PROJECT_KEY, null, PROJECT_NAME, emptyMap(), IOUtils.toInputStream("{binary}"));

    ComponentDto createdProject = db.getDbClient().componentDao().selectByKey(db.getSession(), PROJECT_KEY).get();
    assertThat(db.favorites().hasNoFavorite(createdProject)).isTrue();
  }

  @Test
  public void submit_a_report_on_new_project_with_scan_permission_on_organization() {
    userSession
      .addPermission(OrganizationPermission.SCAN, db.getDefaultOrganization().getUuid())
      .addPermission(PROVISION_PROJECTS, db.getDefaultOrganization());
    mockSuccessfulPrepareSubmitCall();
    when(permissionTemplateService.wouldUserHaveScanPermissionWithDefaultTemplate(any(DbSession.class), eq(defaultOrganizationUuid), any(), eq(PROJECT_KEY)))
      .thenReturn(true);

    underTest.submit(defaultOrganizationKey, PROJECT_KEY, null, PROJECT_NAME, emptyMap(), IOUtils.toInputStream("{binary}"));

    verify(queue).submit(any(CeTaskSubmit.class));
  }

  @Test
  public void user_with_scan_permission_on_organization_is_allowed_to_submit_a_report_on_existing_project() {
    OrganizationDto org = db.organizations().insert();
    ComponentDto project = db.components().insertPrivateProject(org);
    userSession.addPermission(SCAN, org);
    mockSuccessfulPrepareSubmitCall();

    underTest.submit(org.getKey(), project.getDbKey(), null, project.name(), emptyMap(), IOUtils.toInputStream("{binary}"));

    verify(queue).submit(any(CeTaskSubmit.class));
  }

  @Test
  public void submit_a_report_on_existing_project_with_project_scan_permission() {
    ComponentDto project = db.components().insertPrivateProject(db.getDefaultOrganization());
    userSession.addProjectPermission(SCAN_EXECUTION, project);
    mockSuccessfulPrepareSubmitCall();

    underTest.submit(defaultOrganizationKey, project.getDbKey(), null, project.name(), emptyMap(), IOUtils.toInputStream("{binary}"));

    verify(queue).submit(any(CeTaskSubmit.class));
  }

  /**
   * SONAR-8757
   */
  @Test
  public void project_branch_must_not_benefit_from_the_scan_permission_on_main_project() {
    String branchName = "branchFoo";
    ComponentDto mainProject = db.components().insertPrivateProject();
    userSession.addProjectPermission(GlobalPermissions.SCAN_EXECUTION, mainProject);
    // user does not have the "scan" permission on the branch, so it can't scan it
    ComponentDto branchProject = db.components().insertPrivateProject(p -> p.setDbKey(mainProject.getDbKey() + ":" + branchName));

    expectedException.expect(ForbiddenException.class);
    underTest.submit(defaultOrganizationKey, mainProject.getDbKey(), branchName, PROJECT_NAME, emptyMap(), IOUtils.toInputStream("{binary}"));
  }

  @Test
  public void fail_with_NotFoundException_if_organization_with_specified_key_does_not_exist() {
    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("Organization with key 'fop' does not exist");

    underTest.submit("fop", PROJECT_KEY, null, null, emptyMap(), null /* method will fail before parameter is used */);
  }

  @Test
  public void fail_with_organizationKey_does_not_match_organization_of_specified_component() {
    userSession.logIn().setRoot();
    OrganizationDto organization = db.organizations().insert();
    ComponentDto project = db.components().insertPrivateProject(organization);
    mockSuccessfulPrepareSubmitCall();

    underTest.submit(organization.getKey(), project.getDbKey(), null, project.name(), emptyMap(), IOUtils.toInputStream("{binary}"));
  }

  @Test
  public void fail_if_component_is_not_a_project() {
    ComponentDto component = db.components().insertPublicPortfolio(db.getDefaultOrganization());
    userSession.logIn().addProjectPermission(SCAN_EXECUTION, component);
    mockSuccessfulPrepareSubmitCall();

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage(format("Component '%s' is not a project", component.getKey()));

    underTest.submit(defaultOrganizationKey, component.getDbKey(), null, component.name(), emptyMap(), IOUtils.toInputStream("{binary}"));
  }

  @Test
  public void fail_if_project_key_already_exists_as_module() {
    ComponentDto project = db.components().insertPrivateProject(db.getDefaultOrganization());
    ComponentDto module = db.components().insertComponent(newModuleDto(project));
    userSession.logIn().addProjectPermission(SCAN_EXECUTION, project);
    mockSuccessfulPrepareSubmitCall();

    try {
      underTest.submit(defaultOrganizationKey, module.getDbKey(), null, module.name(), emptyMap(), IOUtils.toInputStream("{binary}"));
      fail();
    } catch (BadRequestException e) {
      assertThat(e.errors()).contains(
        format("The project '%s' is already defined in SonarQube but as a module of project '%s'. " +
          "If you really want to stop directly analysing project '%s', please first delete it from SonarQube and then relaunch the analysis of project '%s'.",
          module.getKey(), project.getKey(), project.getKey(), module.getKey()));
    }
  }

  @Test
  public void fail_with_forbidden_exception_when_no_scan_permission() {
    expectedException.expect(ForbiddenException.class);

    underTest.submit(defaultOrganizationKey, PROJECT_KEY, null, PROJECT_NAME, emptyMap(), IOUtils.toInputStream("{binary}"));
  }

  @Test
  public void fail_with_forbidden_exception_on_new_project_when_only_project_scan_permission() {
    ComponentDto component = ComponentTesting.newPrivateProjectDto(db.getDefaultOrganization(), PROJECT_UUID);
    userSession.addProjectPermission(SCAN_EXECUTION, component);
    mockSuccessfulPrepareSubmitCall();

    expectedException.expect(ForbiddenException.class);
    underTest.submit(defaultOrganizationKey, PROJECT_KEY, null, PROJECT_NAME, emptyMap(), IOUtils.toInputStream("{binary}"));
  }

  private void verifyReportIsPersisted(String taskUuid) {
    assertThat(db.selectFirst("select task_uuid from ce_task_input where task_uuid='" + taskUuid + "'")).isNotNull();
  }

  private void mockSuccessfulPrepareSubmitCall() {
    when(queue.prepareSubmit()).thenReturn(new CeTaskSubmit.Builder(TASK_UUID));
  }

}
