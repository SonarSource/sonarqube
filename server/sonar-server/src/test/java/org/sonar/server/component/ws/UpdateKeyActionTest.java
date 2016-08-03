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

package org.sonar.server.component.ws;

import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.component.ComponentService;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.sonar.db.component.ComponentTesting.newProjectDto;
import static org.sonarqube.ws.client.component.ComponentsWsParameters.PARAM_ID;
import static org.sonarqube.ws.client.component.ComponentsWsParameters.PARAM_KEY;
import static org.sonarqube.ws.client.component.ComponentsWsParameters.PARAM_NEW_KEY;

public class UpdateKeyActionTest {
  private static final String ANOTHER_KEY = "another_key";
  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  ComponentDbTester componentDb = new ComponentDbTester(db);
  DbClient dbClient = db.getDbClient();

  ComponentService componentService = mock(ComponentService.class);

  WsActionTester ws = new WsActionTester(new UpdateKeyAction(dbClient, new ComponentFinder(dbClient), componentService));

  @Test
  public void call_by_key() {
    ComponentDto project = insertProject();

    callByKey(project.key(), ANOTHER_KEY);

    assertCallComponentService(project.key(), ANOTHER_KEY);
  }

  @Test
  public void call_by_uuid() {
    ComponentDto project = insertProject();

    callByUuid(project.uuid(), ANOTHER_KEY);

    assertCallComponentService(project.key(), ANOTHER_KEY);
  }

  @Test
  public void fail_if_new_key_is_not_provided() {
    expectedException.expect(IllegalArgumentException.class);

    ComponentDto project = insertProject();

    callByKey(project.key(), null);
  }

  @Test
  public void fail_if_uuid_nor_key_provided() {
    expectedException.expect(IllegalArgumentException.class);

    call(null, null, ANOTHER_KEY);
  }

  @Test
  public void fail_if_uuid_and_key_provided() {
    expectedException.expect(IllegalArgumentException.class);

    ComponentDto project = insertProject();

    call(project.uuid(), project.key(), ANOTHER_KEY);
  }

  @Test
  public void fail_if_project_does_not_exist() {
    expectedException.expect(NotFoundException.class);

    callByUuid("UNKNOWN_UUID", ANOTHER_KEY);
  }

  @Test
  public void api_definition() {
    WebService.Action definition = ws.getDef();

    assertThat(definition.since()).isEqualTo("6.1");
    assertThat(definition.isPost()).isTrue();
    assertThat(definition.key()).isEqualTo("update_key");
    assertThat(definition.params())
      .hasSize(3)
      .extracting(Param::key)
      .containsOnlyOnce("id", "key", "newKey");
  }

  private void assertCallComponentService(@Nullable String oldKey, @Nullable String newKey) {
    verify(componentService).updateKey(any(DbSession.class), eq(oldKey), eq(newKey));
  }

  private ComponentDto insertProject() {
    return componentDb.insertComponent(newProjectDto());
  }

  private String callByUuid(@Nullable String uuid, @Nullable String newKey) {
    return call(uuid, null, newKey);
  }

  private String callByKey(@Nullable String key, @Nullable String newKey) {
    return call(null, key, newKey);
  }

  private String call(@Nullable String uuid, @Nullable String key, @Nullable String newKey) {
    TestRequest request = ws.newRequest();

    if (uuid != null) {
      request.setParam(PARAM_ID, uuid);
    }
    if (key != null) {
      request.setParam(PARAM_KEY, key);
    }
    if (newKey != null) {
      request.setParam(PARAM_NEW_KEY, newKey);
    }

    return request.execute().getInput();
  }
}
