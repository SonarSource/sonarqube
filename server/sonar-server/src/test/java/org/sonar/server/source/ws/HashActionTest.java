/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.source.ws;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.server.component.TestComponentFinder;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsTester;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

@RunWith(MockitoJUnitRunner.class)

public class HashActionTest {

  final static String COMPONENT_KEY = "Action.java";
  final static String PROJECT_UUID = "ABCD";

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();

  WsTester tester;

  @Before
  public void before() {
    DbClient dbClient = db.getDbClient();

    tester = new WsTester(new SourcesWs(new HashAction(dbClient, userSessionRule, TestComponentFinder.from(db))));
  }

  @Test
  public void show_hashes() throws Exception {
    db.prepareDbUnit(getClass(), "shared.xml");
    loginAndRegisterComponent(PROJECT_UUID);

    WsTester.TestRequest request = tester.newGetRequest("api/sources", "hash").setParam("key", COMPONENT_KEY);
    assertThat(request.execute().outputAsString()).isEqualTo("987654");
  }

  @Test
  public void show_hashes_on_test_file() throws Exception {
    db.prepareDbUnit(getClass(), "show_hashes_on_test_file.xml");
    loginAndRegisterComponent(PROJECT_UUID);

    WsTester.TestRequest request = tester.newGetRequest("api/sources", "hash").setParam("key", "ActionTest.java");
    assertThat(request.execute().outputAsString()).isEqualTo("987654");
  }

  @Test
  public void hashes_empty_if_no_source() throws Exception {
    db.prepareDbUnit(getClass(), "no_source.xml");
    loginAndRegisterComponent(PROJECT_UUID);

    WsTester.TestRequest request = tester.newGetRequest("api/sources", "hash").setParam("key", COMPONENT_KEY);
    request.execute().assertNoContent();
  }

  @Test
  public void fail_to_show_hashes_if_file_does_not_exist() {
    try {
      WsTester.TestRequest request = tester.newGetRequest("api/sources", "hash").setParam("key", COMPONENT_KEY);
      request.execute();
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(NotFoundException.class);
    }
  }

  @Test
  public void fail_when_using_branch_db_key() throws Exception {
    ComponentDto project = db.components().insertMainBranch();
    ComponentDto branch = db.components().insertProjectBranch(project);
    loginAndRegisterComponent(project.uuid());

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage(format("Component key '%s' not found", branch.getDbKey()));

    tester.newGetRequest("api/sources", "hash")
      .setParam("key", branch.getDbKey())
      .execute();
  }

  @Test(expected = ForbiddenException.class)
  public void fail_on_missing_permission() throws Exception {
    db.prepareDbUnit(getClass(), "shared.xml");

    userSessionRule.logIn("polop");
    tester.newGetRequest("api/sources", "hash").setParam("key", COMPONENT_KEY).execute();
  }

  private void loginAndRegisterComponent(String componentUuid) {
    userSessionRule.logIn("polop").registerComponents(db.getDbClient().componentDao().selectByUuid(db.getSession(), componentUuid).get());
  }
}
