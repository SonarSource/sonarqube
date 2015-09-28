/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.computation.queue.report;

import org.apache.commons.io.IOUtils;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.ce.CeTaskTypes;
import org.sonar.db.component.ComponentDto;
import org.sonar.server.component.ComponentService;
import org.sonar.server.component.NewComponent;
import org.sonar.server.computation.queue.CeQueue;
import org.sonar.server.computation.queue.CeQueueImpl;
import org.sonar.server.computation.queue.CeTaskSubmit;
import org.sonar.server.permission.PermissionService;
import org.sonar.server.tester.UserSessionRule;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class ReportSubmitterTest {

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  CeQueue queue = mock(CeQueueImpl.class);
  ReportFiles reportFiles = mock(ReportFiles.class);
  ComponentService componentService = mock(ComponentService.class);
  PermissionService permissionService = mock(PermissionService.class);
  ReportSubmitter underTest = new ReportSubmitter(queue, userSession, reportFiles, componentService, permissionService);

  @Test
  public void submit_a_report_on_existing_project() {
    when(queue.prepareSubmit()).thenReturn(new CeTaskSubmit.Builder("TASK_1"));
    userSession.setGlobalPermissions(GlobalPermissions.SCAN_EXECUTION);
    when(componentService.getNullableByKey("MY_PROJECT")).thenReturn(new ComponentDto().setUuid("P1"));

    underTest.submit("MY_PROJECT", null, "My Project", IOUtils.toInputStream("{binary}"));

    verifyZeroInteractions(permissionService);
    verify(queue).submit(argThat(new TypeSafeMatcher<CeTaskSubmit>() {
      @Override
      protected boolean matchesSafely(CeTaskSubmit submit) {
        return submit.getType().equals(CeTaskTypes.REPORT) && submit.getComponentUuid().equals("P1") &&
          submit.getUuid().equals("TASK_1");
      }

      @Override
      public void describeTo(Description description) {

      }
    }));
  }

  @Test
  public void provision_project_if_does_not_exist() throws Exception {
    when(queue.prepareSubmit()).thenReturn(new CeTaskSubmit.Builder("TASK_1"));
    userSession.setGlobalPermissions(GlobalPermissions.SCAN_EXECUTION, GlobalPermissions.PROVISIONING);
    when(componentService.getNullableByKey("MY_PROJECT")).thenReturn(null);
    when(componentService.create(any(NewComponent.class))).thenReturn(new ComponentDto().setUuid("P1").setKey("MY_PROJECT"));

    underTest.submit("MY_PROJECT", null, "My Project", IOUtils.toInputStream("{binary}"));

    verify(permissionService).applyDefaultPermissionTemplate("MY_PROJECT");
    verify(queue).submit(argThat(new TypeSafeMatcher<CeTaskSubmit>() {
      @Override
      protected boolean matchesSafely(CeTaskSubmit submit) {
        return submit.getType().equals(CeTaskTypes.REPORT) && submit.getComponentUuid().equals("P1") &&
          submit.getUuid().equals("TASK_1");
      }

      @Override
      public void describeTo(Description description) {

      }
    }));

  }
}
