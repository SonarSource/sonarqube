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

package org.sonar.server.measure.custom.ws;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.web.UserRole;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.custommeasure.db.CustomMeasureDto;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.DbTester;
import org.sonar.server.component.ComponentTesting;
import org.sonar.server.component.db.ComponentDao;
import org.sonar.server.measure.custom.persistence.CustomMeasureDao;
import org.sonar.server.db.DbClient;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.server.measure.custom.persistence.CustomMeasureTesting.newCustomMeasureDto;
import static org.sonar.server.measure.custom.ws.DeleteAction.PARAM_ID;

public class DeleteActionTest {

  public static final String ACTION = "delete";

  @ClassRule
  public static DbTester db = new DbTester();
  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  WsTester ws;
  DbClient dbClient;
  DbSession dbSession;

  @Before
  public void setUp() {
    dbClient = new DbClient(db.database(), db.myBatis(), new CustomMeasureDao(), new ComponentDao());
    dbSession = dbClient.openSession(false);
    ws = new WsTester(new CustomMeasuresWs(new DeleteAction(dbClient, userSessionRule)));
    userSessionRule.setGlobalPermissions(GlobalPermissions.SYSTEM_ADMIN);
    db.truncateTables();
  }

  @After
  public void tearDown() {
    dbSession.close();
  }

  @Test
  public void delete_in_db() throws Exception {
    long id = insertCustomMeasure(newCustomMeasureDto());
    long anotherId = insertCustomMeasure(newCustomMeasureDto());
    assertThat(dbClient.customMeasureDao().selectNullableById(dbSession, id)).isNotNull();

    WsTester.Result response = newRequest().setParam(PARAM_ID, String.valueOf(id)).execute();
    dbSession.commit();

    assertThat(dbClient.customMeasureDao().selectNullableById(dbSession, id)).isNull();
    assertThat(dbClient.customMeasureDao().selectNullableById(dbSession, anotherId)).isNotNull();
    response.assertNoContent();
  }

  @Test
  public void delete_in_db_when_admin_on_project() throws Exception {
    ComponentDto project = ComponentTesting.newProjectDto("project-uuid");
    dbClient.componentDao().insert(dbSession, project);
    userSessionRule.login("login").addProjectUuidPermissions(UserRole.ADMIN, "project-uuid");
    long id = insertCustomMeasure(newCustomMeasureDto().setComponentId(project.getId()));

    newRequest().setParam(PARAM_ID, String.valueOf(id)).execute();

    assertThat(dbClient.customMeasureDao().selectNullableById(dbSession, id)).isNull();
  }

  @Test
  public void fail_when_not_found_in_db() throws Exception {
    expectedException.expect(NotFoundException.class);

    newRequest().setParam(PARAM_ID, "42").execute();
  }

  @Test
  public void fail_when_insufficient_permissions() throws Exception {
    expectedException.expect(ForbiddenException.class);
    userSessionRule.login("login");
    ComponentDto project = ComponentTesting.newProjectDto("any-uuid");
    dbClient.componentDao().insert(dbSession, project);
    long id = insertCustomMeasure(newCustomMeasureDto().setComponentId(project.getId()));

    newRequest().setParam(PARAM_ID, String.valueOf(id)).execute();
  }

  private long insertCustomMeasure(CustomMeasureDto customMeasure) {
    dbClient.customMeasureDao().insert(dbSession, customMeasure);
    dbSession.commit();
    return customMeasure.getId();
  }

  private WsTester.TestRequest newRequest() {
    return ws.newPostRequest(CustomMeasuresWs.ENDPOINT, ACTION);
  }
}
