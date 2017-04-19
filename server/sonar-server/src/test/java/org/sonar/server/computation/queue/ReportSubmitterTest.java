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
package org.sonar.server.computation.queue;

import org.apache.commons.io.IOUtils;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.resources.Qualifiers;
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
import org.sonar.server.component.ComponentUpdater;
import org.sonar.server.component.NewComponent;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.favorite.FavoriteUpdater;
import org.sonar.server.permission.PermissionTemplateService;
import org.sonar.server.tester.UserSessionRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.core.permission.GlobalPermissions.SCAN_EXECUTION;
import static org.sonar.db.component.ComponentTesting.newPrivateProjectDto;
import static org.sonar.db.permission.OrganizationPermission.PROVISION_PROJECTS;
import static org.sonar.db.permission.OrganizationPermission.SCAN;

public class ReportSubmitterTest {

  private static final String PROJECT_KEY = "MY_PROJECT";
  private static final String PROJECT_UUID = "P1";
  private static final String PROJECT_NAME = "My Project";
  private static final String TASK_UUID = "TASK_1";

  @Rule
  public ExpectedException thrown = ExpectedException.none();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private String defaultOrganizationKey;
  private String defaultOrganizationUuid;
  private CeQueue queue = mock(CeQueueImpl.class);
  private ComponentUpdater componentUpdater = mock(ComponentUpdater.class);
  private PermissionTemplateService permissionTemplateService = mock(PermissionTemplateService.class);
  private FavoriteUpdater favoriteUpdater = mock(FavoriteUpdater.class);

  private ReportSubmitter underTest = new ReportSubmitter(queue, userSession, componentUpdater, permissionTemplateService, db.getDbClient());

  @Before
  public void setUp() throws Exception {
    defaultOrganizationKey = db.getDefaultOrganization().getKey();
    defaultOrganizationUuid = db.getDefaultOrganization().getUuid();
  }

  @Test
  public void submit_fails_with_NotFoundException_if_organization_with_specified_key_does_not_exist() {
    thrown.expect(NotFoundException.class);
    thrown.expectMessage("Organization with key 'fop' does not exist");

    underTest.submit("fop", PROJECT_KEY, null, null, null /* method will fail before parameter is used */);
  }

  @Test
  public void submit_fails_with_organizationKey_does_not_match_organization_of_specified_component() {
    userSession.logIn().setRoot();
    OrganizationDto organization = db.organizations().insert();
    ComponentDto project = db.components().insertPrivateProject(organization);
    mockSuccessfulPrepareSubmitCall();

    underTest.submit(organization.getKey(), project.getKey(), null, project.name(), IOUtils.toInputStream("{binary}"));
  }

  @Test
  public void submit_a_report_on_existing_project() {
    ComponentDto project = db.components().insertPrivateProject(db.getDefaultOrganization());
    userSession.logIn().addProjectPermission(SCAN_EXECUTION, project);

    mockSuccessfulPrepareSubmitCall();

    underTest.submit(defaultOrganizationKey, project.getKey(), null, project.name(), IOUtils.toInputStream("{binary}"));

    verifyReportIsPersisted(TASK_UUID);
    verifyZeroInteractions(permissionTemplateService);
    verifyZeroInteractions(favoriteUpdater);
    verify(queue).submit(argThat(new TypeSafeMatcher<CeTaskSubmit>() {
      @Override
      protected boolean matchesSafely(CeTaskSubmit submit) {
        return submit.getType().equals(CeTaskTypes.REPORT) && submit.getComponentUuid().equals(project.uuid()) &&
          submit.getUuid().equals(TASK_UUID);
      }

      @Override
      public void describeTo(Description description) {

      }
    }));
  }

  @Test
  public void provision_project_if_does_not_exist() throws Exception {
    OrganizationDto organization = db.organizations().insert();
    userSession
      .addPermission(OrganizationPermission.SCAN, organization.getUuid())
      .addPermission(PROVISION_PROJECTS, organization);

    mockSuccessfulPrepareSubmitCall();
    ComponentDto createdProject = newPrivateProjectDto(organization, PROJECT_UUID).setKey(PROJECT_KEY);
    when(componentUpdater.create(any(DbSession.class), any(NewComponent.class), eq(null))).thenReturn(createdProject);
    when(permissionTemplateService.wouldUserHaveScanPermissionWithDefaultTemplate(any(DbSession.class), eq(organization.getUuid()), anyInt(), anyString(),
      eq(PROJECT_KEY), eq(Qualifiers.PROJECT)))
        .thenReturn(true);
    when(permissionTemplateService.hasDefaultTemplateWithPermissionOnProjectCreator(any(DbSession.class), eq(organization.getUuid()), any(ComponentDto.class))).thenReturn(true);

    underTest.submit(organization.getKey(), PROJECT_KEY, null, PROJECT_NAME, IOUtils.toInputStream("{binary}"));

    verifyReportIsPersisted(TASK_UUID);
    verify(queue).submit(argThat(new TypeSafeMatcher<CeTaskSubmit>() {
      @Override
      protected boolean matchesSafely(CeTaskSubmit submit) {
        return submit.getType().equals(CeTaskTypes.REPORT) && submit.getComponentUuid().equals(PROJECT_UUID) &&
          submit.getUuid().equals(TASK_UUID);
      }

      @Override
      public void describeTo(Description description) {

      }
    }));
  }

  @Test
  public void no_favorite_when_no_project_creator_permission_on_permission_template() {
    userSession
      .addPermission(OrganizationPermission.SCAN, db.getDefaultOrganization().getUuid())
      .addPermission(PROVISION_PROJECTS, db.getDefaultOrganization());

    mockSuccessfulPrepareSubmitCall();
    ComponentDto createdProject = newPrivateProjectDto(db.getDefaultOrganization(), PROJECT_UUID).setKey(PROJECT_KEY);
    when(componentUpdater.create(any(DbSession.class), any(NewComponent.class), eq(null))).thenReturn(createdProject);
    when(permissionTemplateService.wouldUserHaveScanPermissionWithDefaultTemplate(any(DbSession.class), eq(defaultOrganizationUuid), anyInt(), anyString(),
      eq(PROJECT_KEY), eq(Qualifiers.PROJECT)))
        .thenReturn(true);
    when(permissionTemplateService.hasDefaultTemplateWithPermissionOnProjectCreator(any(DbSession.class), eq(defaultOrganizationUuid), any(ComponentDto.class))).thenReturn(false);

    underTest.submit(defaultOrganizationKey, PROJECT_KEY, null, PROJECT_NAME, IOUtils.toInputStream("{binary}"));

    verifyZeroInteractions(favoriteUpdater);
  }

  @Test
  public void submit_a_report_on_new_project_with_scan_permission_on_organization() {
    userSession
      .addPermission(OrganizationPermission.SCAN, db.getDefaultOrganization().getUuid())
      .addPermission(PROVISION_PROJECTS, db.getDefaultOrganization());

    mockSuccessfulPrepareSubmitCall();
    ComponentDto project = newPrivateProjectDto(db.getDefaultOrganization(), PROJECT_UUID).setKey(PROJECT_KEY);
    when(componentUpdater.create(any(DbSession.class), any(NewComponent.class), eq(null))).thenReturn(project);
    when(permissionTemplateService.wouldUserHaveScanPermissionWithDefaultTemplate(any(DbSession.class), eq(defaultOrganizationUuid), anyInt(), anyString(),
      eq(PROJECT_KEY), eq(Qualifiers.PROJECT)))
        .thenReturn(true);

    underTest.submit(defaultOrganizationKey, PROJECT_KEY, null, PROJECT_NAME, IOUtils.toInputStream("{binary}"));

    verify(queue).submit(any(CeTaskSubmit.class));
  }

  @Test
  public void user_with_scan_permission_on_organization_is_allowed_to_submit_a_report_on_existing_project() {
    OrganizationDto org = db.organizations().insert();
    ComponentDto project = db.components().insertPrivateProject(org);
    userSession.addPermission(SCAN, org);

    mockSuccessfulPrepareSubmitCall();

    underTest.submit(org.getKey(), project.getKey(), null, project.name(), IOUtils.toInputStream("{binary}"));

    verify(queue).submit(any(CeTaskSubmit.class));
  }

  @Test
  public void submit_a_report_on_existing_project_with_project_scan_permission() {
    ComponentDto project = db.components().insertPrivateProject(db.getDefaultOrganization());
    userSession.addProjectPermission(SCAN_EXECUTION, project);

    mockSuccessfulPrepareSubmitCall();

    underTest.submit(defaultOrganizationKey, project.getKey(), null, project.name(), IOUtils.toInputStream("{binary}"));

    verify(queue).submit(any(CeTaskSubmit.class));
  }

  @Test
  public void fail_with_forbidden_exception_when_no_scan_permission() {
    thrown.expect(ForbiddenException.class);

    underTest.submit(defaultOrganizationKey, PROJECT_KEY, null, PROJECT_NAME, IOUtils.toInputStream("{binary}"));
  }

  @Test
  public void fail_with_forbidden_exception_on_new_project_when_only_project_scan_permission() {
    userSession.addProjectPermission(SCAN_EXECUTION, ComponentTesting.newPrivateProjectDto(db.getDefaultOrganization(), PROJECT_UUID));

    mockSuccessfulPrepareSubmitCall();
    when(componentUpdater.create(any(DbSession.class), any(NewComponent.class), eq(null))).thenReturn(new ComponentDto().setUuid(PROJECT_UUID).setKey(PROJECT_KEY));

    thrown.expect(ForbiddenException.class);
    underTest.submit(defaultOrganizationKey, PROJECT_KEY, null, PROJECT_NAME, IOUtils.toInputStream("{binary}"));
  }

  /**
   * SONAR-8757
   */
  @Test
  public void project_branch_must_not_benefit_from_the_scan_permission_on_main_project() {
    ComponentDto mainProject = db.components().insertPrivateProject();
    userSession.addProjectPermission(GlobalPermissions.SCAN_EXECUTION, mainProject);

    // user does not have the "scan" permission on the branch, so it can't scan it
    String branchName = "branchFoo";
    ComponentDto branchProject = db.components().insertPrivateProject(p -> p.setKey(mainProject.getKey() + ":" + branchName));

    thrown.expect(ForbiddenException.class);
    underTest.submit(defaultOrganizationKey, mainProject.key(), branchName, PROJECT_NAME, IOUtils.toInputStream("{binary}"));
  }

  private void verifyReportIsPersisted(String taskUuid) {
    assertThat(db.selectFirst("select task_uuid from ce_task_input where task_uuid='" + taskUuid + "'")).isNotNull();
  }

  private void mockSuccessfulPrepareSubmitCall() {
    when(queue.prepareSubmit()).thenReturn(new CeTaskSubmit.Builder(TASK_UUID));
  }

}
