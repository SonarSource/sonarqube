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
package org.sonar.server.ce.ws;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.server.ws.WebService;
import org.sonar.ce.configuration.WorkerCountProvider;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.Ce.WorkerCountResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.test.JsonAssert.assertJson;

public class WorkerCountActionTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  private WorkerCountProvider workerCountProvider = mock(WorkerCountProvider.class);

  @Test
  public void return_value_and_can_set_worker_count_to_true_when_provider_exists() {
    userSession.logIn().setSystemAdministrator();
    when(workerCountProvider.get()).thenReturn(5);
    WsActionTester ws = new WsActionTester(new WorkerCountAction(userSession, workerCountProvider));

    WorkerCountResponse response = ws.newRequest().executeProtobuf(WorkerCountResponse.class);

    assertThat(response.getValue()).isEqualTo(5);
    assertThat(response.getCanSetWorkerCount()).isTrue();
  }

  @Test
  public void return_1_and_can_set_worker_count_to_false_when_provider_does_not_exist() {
    userSession.logIn().setSystemAdministrator();
    WsActionTester ws = new WsActionTester(new WorkerCountAction(userSession));

    WorkerCountResponse response = ws.newRequest().executeProtobuf(WorkerCountResponse.class);

    assertThat(response.getValue()).isEqualTo(1);
    assertThat(response.getCanSetWorkerCount()).isFalse();
  }

  @Test
  public void fail_when_not_system_administrator() {
    userSession.logIn().setNonSystemAdministrator();
    WsActionTester ws = new WsActionTester(new WorkerCountAction(userSession));

    expectedException.expect(ForbiddenException.class);

    ws.newRequest().execute();
  }

  @Test
  public void test_definition() {
    WsActionTester ws = new WsActionTester(new WorkerCountAction(userSession, workerCountProvider));

    WebService.Action action = ws.getDef();
    assertThat(action.key()).isEqualTo("worker_count");
    assertThat(action.since()).isEqualTo("6.5");
    assertThat(action.responseExampleAsString()).isNotEmpty();
    assertThat(action.params()).isEmpty();
  }

  @Test
  public void test_example() {
    userSession.logIn().setSystemAdministrator();
    when(workerCountProvider.get()).thenReturn(5);
    WsActionTester ws = new WsActionTester(new WorkerCountAction(userSession, workerCountProvider));

    String response = ws.newRequest().execute().getInput();

    assertJson(response).isSimilarTo(ws.getDef().responseExample());
  }
}
