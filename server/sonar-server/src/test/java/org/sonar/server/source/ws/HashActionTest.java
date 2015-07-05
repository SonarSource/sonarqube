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
package org.sonar.server.source.ws;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.server.component.db.ComponentDao;
import org.sonar.server.db.DbClient;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.db.source.FileSourceDao;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsTester;
import org.sonar.test.DbTests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

@RunWith(MockitoJUnitRunner.class)
@Category(DbTests.class)
public class HashActionTest {

  final static String COMPONENT_KEY = "Action.java";
  final static String PROJECT_UUID = "ABCD";

  @ClassRule
  public static DbTester db = new DbTester();

  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();

  DbSession session;

  WsTester tester;

  @Before
  public void before() {
    db.truncateTables();
    this.session = db.myBatis().openSession(false);

    DbClient dbClient = new DbClient(db.database(), db.myBatis(), new FileSourceDao(db.myBatis()), new ComponentDao());

    tester = new WsTester(new SourcesWs(new HashAction(dbClient, userSessionRule)));
  }

  @After
  public void after() {
    this.session.close();
  }

  @Test
  public void show_hashes() throws Exception {
    db.prepareDbUnit(getClass(), "shared.xml");
    userSessionRule.login("polop").addProjectUuidPermissions(UserRole.USER, PROJECT_UUID);

    WsTester.TestRequest request = tester.newGetRequest("api/sources", "hash").setParam("key", COMPONENT_KEY);
    assertThat(request.execute().outputAsString()).isEqualTo("987654");
  }

  @Test
  public void show_hashes_on_test_file() throws Exception {
    db.prepareDbUnit(getClass(), "show_hashes_on_test_file.xml");
    userSessionRule.login("polop").addProjectUuidPermissions(UserRole.USER, PROJECT_UUID);

    WsTester.TestRequest request = tester.newGetRequest("api/sources", "hash").setParam("key", "ActionTest.java");
    assertThat(request.execute().outputAsString()).isEqualTo("987654");
  }

  @Test
  public void hashes_empty_if_no_source() throws Exception {
    db.prepareDbUnit(getClass(), "no_source.xml");
    userSessionRule.login("polop").addProjectUuidPermissions(UserRole.USER, PROJECT_UUID);

    WsTester.TestRequest request = tester.newGetRequest("api/sources", "hash").setParam("key", COMPONENT_KEY);
    request.execute().assertNoContent();
  }

  @Test
  public void fail_to_show_hashes_if_file_does_not_exist() {
    userSessionRule.login("polop").addProjectUuidPermissions(UserRole.USER, PROJECT_UUID);
    try {
      WsTester.TestRequest request = tester.newGetRequest("api/sources", "hash").setParam("key", COMPONENT_KEY);
      request.execute();
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(NotFoundException.class);
    }
  }

  @Test(expected = ForbiddenException.class)
  public void fail_on_missing_permission() throws Exception {
    db.prepareDbUnit(getClass(), "shared.xml");

    userSessionRule.login("polop");
    tester.newGetRequest("api/sources", "hash").setParam("key", COMPONENT_KEY).execute();
  }
}
