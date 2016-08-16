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
package org.sonar.server.computation.queue;

import org.apache.commons.io.IOUtils;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
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
import org.sonar.server.component.ComponentService;
import org.sonar.server.component.NewComponent;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.permission.PermissionService;
import org.sonar.server.tester.UserSessionRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.core.permission.GlobalPermissions.PROVISIONING;
import static org.sonar.core.permission.GlobalPermissions.SCAN_EXECUTION;

public class ReportSubmitterTest {

  static final String PROJECT_KEY = "MY_PROJECT";
  static final String PROJECT_UUID = "P1";
  static final String PROJECT_NAME = "My Project";
  static final String TASK_UUID = "TASK_1";

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  private CeQueue queue = mock(CeQueueImpl.class);
  private ComponentService componentService = mock(ComponentService.class);
  private PermissionService permissionService = mock(PermissionService.class);
  private ReportSubmitter underTest = new ReportSubmitter(queue, userSession, componentService, permissionService, dbTester.getDbClient());

  @Test
  public void submit_a_report_on_existing_project() {
    userSession.setGlobalPermissions(SCAN_EXECUTION);

    when(queue.prepareSubmit()).thenReturn(new CeTaskSubmit.Builder(TASK_UUID));
    when(componentService.getNullableByKey(PROJECT_KEY)).thenReturn(new ComponentDto().setUuid(PROJECT_UUID));

    underTest.submit(PROJECT_KEY, null, PROJECT_NAME, IOUtils.toInputStream("{binary}"));

    verifyReportIsPersisted(TASK_UUID);
    verifyZeroInteractions(permissionService);
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
  public void provision_project_if_does_not_exist() throws Exception {
    userSession.setGlobalPermissions(SCAN_EXECUTION, PROVISIONING);

    when(queue.prepareSubmit()).thenReturn(new CeTaskSubmit.Builder(TASK_UUID));
    when(componentService.getNullableByKey(PROJECT_KEY)).thenReturn(null);
    when(componentService.create(any(DbSession.class), any(NewComponent.class))).thenReturn(new ComponentDto().setUuid(PROJECT_UUID).setKey(PROJECT_KEY));
    when(permissionService.wouldCurrentUserHavePermissionWithDefaultTemplate(any(DbSession.class), eq(SCAN_EXECUTION), anyString(), eq(PROJECT_KEY), eq(Qualifiers.PROJECT)))
      .thenReturn(true);

    underTest.submit(PROJECT_KEY, null, PROJECT_NAME, IOUtils.toInputStream("{binary}"));

    verifyReportIsPersisted(TASK_UUID);
    verify(permissionService).applyDefaultPermissionTemplate(any(DbSession.class), eq(PROJECT_KEY));
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
  public void submit_a_report_on_new_project_with_global_scan_permission() {
    userSession.setGlobalPermissions(SCAN_EXECUTION, PROVISIONING);

    when(queue.prepareSubmit()).thenReturn(new CeTaskSubmit.Builder(TASK_UUID));
    when(componentService.getNullableByKey(PROJECT_KEY)).thenReturn(null);
    when(componentService.create(any(DbSession.class), any(NewComponent.class))).thenReturn(new ComponentDto().setUuid(PROJECT_UUID).setKey(PROJECT_KEY));
    when(permissionService.wouldCurrentUserHavePermissionWithDefaultTemplate(any(DbSession.class), eq(SCAN_EXECUTION), anyString(), eq(PROJECT_KEY), eq(Qualifiers.PROJECT)))
      .thenReturn(true);

    underTest.submit(PROJECT_KEY, null, PROJECT_NAME, IOUtils.toInputStream("{binary}"));

    verify(queue).submit(any(CeTaskSubmit.class));
  }

  @Test
  public void submit_a_report_on_existing_project_with_global_scan_permission() {
    userSession.setGlobalPermissions(SCAN_EXECUTION);

    when(queue.prepareSubmit()).thenReturn(new CeTaskSubmit.Builder(TASK_UUID));
    when(componentService.getNullableByKey(PROJECT_KEY)).thenReturn(new ComponentDto().setUuid(PROJECT_UUID));

    underTest.submit(PROJECT_KEY, null, PROJECT_NAME, IOUtils.toInputStream("{binary}"));

    verify(queue).submit(any(CeTaskSubmit.class));
  }

  @Test
  public void submit_a_report_on_existing_project_with_project_scan_permission() {
    userSession.addProjectPermissions(SCAN_EXECUTION, PROJECT_KEY);

    when(queue.prepareSubmit()).thenReturn(new CeTaskSubmit.Builder(TASK_UUID));
    when(componentService.getNullableByKey(PROJECT_KEY)).thenReturn(new ComponentDto().setUuid(PROJECT_UUID));

    underTest.submit(PROJECT_KEY, null, PROJECT_NAME, IOUtils.toInputStream("{binary}"));

    verify(queue).submit(any(CeTaskSubmit.class));
  }

  @Test
  public void fail_with_forbidden_exception_when_no_scan_permission() {
    userSession.setGlobalPermissions(GlobalPermissions.DASHBOARD_SHARING);

    thrown.expect(ForbiddenException.class);
    underTest.submit(PROJECT_KEY, null, PROJECT_NAME, IOUtils.toInputStream("{binary}"));
  }

  @Test
  public void fail_with_forbidden_exception_on_new_project_when_only_project_scan_permission() {
    userSession.addProjectPermissions(SCAN_EXECUTION, PROJECT_KEY);

    when(queue.prepareSubmit()).thenReturn(new CeTaskSubmit.Builder(TASK_UUID));
    when(componentService.getNullableByKey(PROJECT_KEY)).thenReturn(null);
    when(componentService.create(any(DbSession.class), any(NewComponent.class))).thenReturn(new ComponentDto().setUuid(PROJECT_UUID).setKey(PROJECT_KEY));

    thrown.expect(ForbiddenException.class);
    underTest.submit(PROJECT_KEY, null, PROJECT_NAME, IOUtils.toInputStream("{binary}"));
  }

  private void verifyReportIsPersisted(String taskUuid) {
    assertThat(dbTester.selectFirst("select task_uuid from ce_task_input where task_uuid='" + taskUuid + "'")).isNotNull();
  }

}
