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
package org.sonar.server.ce.ws;

import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.impl.utils.TestSystem2;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.ce.queue.CeQueue;
import org.sonar.ce.queue.CeQueueImpl;
import org.sonar.ce.queue.CeTaskSubmit;
import org.sonar.ce.task.CeTask;
import org.sonar.core.util.UuidFactoryFast;
import org.sonar.db.DbTester;
import org.sonar.db.ce.CeActivityDto;
import org.sonar.db.ce.CeQueueDto;
import org.sonar.db.ce.CeTaskTypes;
import org.sonar.db.component.ComponentDto;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.platform.NodeInformation;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

public class CancelActionTest {

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create();

  private System2 system2 = new TestSystem2();
  private CeQueue queue = new CeQueueImpl(system2, db.getDbClient(), UuidFactoryFast.getInstance(), mock(NodeInformation.class));

  private CancelAction underTest = new CancelAction(userSession, db.getDbClient(), queue);
  private WsActionTester tester = new WsActionTester(underTest);

  @Test
  public void cancel_pending_task_on_project() {
    logInAsSystemAdministrator();
    ComponentDto project = db.components().insertPrivateProject();
    CeQueueDto queue = createTaskSubmit(project);

    tester.newRequest()
      .setParam("id", queue.getUuid())
      .execute();

    assertThat(db.getDbClient().ceActivityDao().selectByUuid(db.getSession(), queue.getUuid()).get().getStatus()).isEqualTo(CeActivityDto.Status.CANCELED);
  }

  @Test
  public void cancel_pending_task_having_no_component() {
    logInAsSystemAdministrator();
    CeQueueDto queue = createTaskSubmit(null);

    tester.newRequest()
      .setParam("id", queue.getUuid())
      .execute();

    assertThat(db.getDbClient().ceActivityDao().selectByUuid(db.getSession(), queue.getUuid()).get().getStatus()).isEqualTo(CeActivityDto.Status.CANCELED);
  }

  @Test
  public void cancel_pending_task_when_system_administer() {
    logInAsSystemAdministrator();
    ComponentDto project = db.components().insertPrivateProject();
    CeQueueDto queue = createTaskSubmit(project);

    tester.newRequest()
      .setParam("id", queue.getUuid())
      .execute();

    assertThat(db.getDbClient().ceActivityDao().selectByUuid(db.getSession(), queue.getUuid()).get().getStatus()).isEqualTo(CeActivityDto.Status.CANCELED);
  }

  @Test
  public void cancel_pending_task_when_project_administer() {
    ComponentDto project = db.components().insertPrivateProject();
    userSession.addProjectPermission(UserRole.ADMIN, project);
    CeQueueDto queue = createTaskSubmit(project);

    tester.newRequest()
      .setParam("id", queue.getUuid())
      .execute();

    assertThat(db.getDbClient().ceActivityDao().selectByUuid(db.getSession(), queue.getUuid()).get().getStatus()).isEqualTo(CeActivityDto.Status.CANCELED);
  }

  @Test
  public void does_not_fail_on_unknown_task() {
    logInAsSystemAdministrator();

    tester.newRequest()
      .setParam("id", "UNKNOWN")
      .execute();
  }

  @Test
  public void throw_IllegalArgumentException_if_missing_id() {
    logInAsSystemAdministrator();

    assertThatThrownBy(() -> tester.newRequest().execute())
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("The 'id' parameter is missing");
  }

  @Test
  public void throw_ForbiddenException_if_not_enough_permission_when_canceling_task_on_project() {
    userSession.logIn().setNonSystemAdministrator();
    ComponentDto project = db.components().insertPrivateProject();
    CeQueueDto queue = createTaskSubmit(project);

    assertThatThrownBy(() -> {
      tester.newRequest()
        .setParam("id", queue.getUuid())
        .execute();
    })
      .isInstanceOf(ForbiddenException.class)
      .hasMessage("Insufficient privileges");
  }

  @Test
  public void throw_ForbiddenException_if_not_enough_permission_when_canceling_task_without_project() {
    userSession.logIn().setNonSystemAdministrator();
    CeQueueDto queue = createTaskSubmit(null);

    assertThatThrownBy(() -> {
      tester.newRequest()
        .setParam("id", queue.getUuid())
        .execute();
    })
      .isInstanceOf(ForbiddenException.class)
      .hasMessage("Insufficient privileges");
  }

  @Test
  public void throw_ForbiddenException_if_not_enough_permission_when_canceling_task_when_project_does_not_exist() {
    userSession.logIn().setNonSystemAdministrator();
    CeQueueDto queue = createTaskSubmit(nonExistentComponentDot());

    assertThatThrownBy(() -> {
      tester.newRequest()
        .setParam("id", queue.getUuid())
        .execute();
    })
      .isInstanceOf(ForbiddenException.class)
      .hasMessage("Insufficient privileges");
  }

  private static ComponentDto nonExistentComponentDot() {
    return new ComponentDto().setUuid("does_not_exist").setBranchUuid("unknown");
  }

  private void logInAsSystemAdministrator() {
    userSession.logIn().setSystemAdministrator();
  }

  private CeQueueDto createTaskSubmit(@Nullable ComponentDto componentDto) {
    CeTaskSubmit.Builder submission = queue.prepareSubmit()
      .setType(CeTaskTypes.REPORT)
      .setSubmitterUuid(null)
      .setCharacteristics(emptyMap());
    if (componentDto != null) {
      submission.setComponent(CeTaskSubmit.Component.fromDto(componentDto));
    }
    CeTask task = queue.submit(submission.build());
    return db.getDbClient().ceQueueDao().selectByUuid(db.getSession(), task.getUuid()).get();
  }
}
