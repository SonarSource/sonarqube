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

package org.sonar.server.batch;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.sonar.api.platform.Server;
import org.sonar.api.web.UserRole;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.DbTester;
import org.sonar.core.properties.PropertiesDao;
import org.sonar.server.component.db.ComponentDao;
import org.sonar.server.db.DbClient;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.issue.db.IssueDao;
import org.sonar.server.user.MockUserSession;
import org.sonar.server.ws.WsTester;
import org.sonar.test.DbTests;

import static org.mockito.Mockito.mock;

@Category(DbTests.class)
public class IssuesActionTest {

  private final static String PROJECT_KEY = "struts";
  private final static String MODULE_KEY = "struts-core";

  WsTester tester;

  IssuesAction issuesAction;

  @Rule
  public DbTester db = new DbTester();

  private DbSession session;

  @Before
  public void before() throws Exception {
    this.session = db.myBatis().openSession(false);

    DbClient dbClient = new DbClient(db.database(), db.myBatis(), new IssueDao(db.myBatis()), new ComponentDao());
    issuesAction = new IssuesAction(dbClient);

    tester = new WsTester(new BatchWs(
      new BatchIndex(mock(Server.class)),
      new GlobalRepositoryAction(mock(DbClient.class), mock(PropertiesDao.class)),
      new ProjectRepositoryAction(mock(ProjectRepositoryLoader.class)),
      issuesAction)
      );
  }

  @After
  public void after() {
    this.session.close();
  }

  @Test
  public void return_issues_on_project() throws Exception {
    db.prepareDbUnit(getClass(), "shared.xml");

    MockUserSession.set().setLogin("henry").setGlobalPermissions(GlobalPermissions.PREVIEW_EXECUTION).addComponentPermission(UserRole.USER, PROJECT_KEY, PROJECT_KEY);

    WsTester.TestRequest request = tester.newGetRequest("batch", "issues").setParam("key", PROJECT_KEY);
    request.execute().assertJson(getClass(), "issues_on_project-expected.json", false);
  }

  @Test
  public void return_only_manual_severity() throws Exception {
    db.prepareDbUnit(getClass(), "return_only_manual_severity.xml");

    MockUserSession.set().setLogin("henry").setGlobalPermissions(GlobalPermissions.PREVIEW_EXECUTION).addComponentPermission(UserRole.USER, PROJECT_KEY, PROJECT_KEY);

    WsTester.TestRequest request = tester.newGetRequest("batch", "issues").setParam("key", PROJECT_KEY);
    request.execute().assertJson(getClass(), "return_only_manual_severity-expected.json", false);
  }

  @Test
  public void return_issues_on_module() throws Exception {
    db.prepareDbUnit(getClass(), "shared.xml");

    MockUserSession.set().setLogin("henry").setGlobalPermissions(GlobalPermissions.PREVIEW_EXECUTION).addComponentPermission(UserRole.USER, PROJECT_KEY, MODULE_KEY);

    WsTester.TestRequest request = tester.newGetRequest("batch", "issues").setParam("key", MODULE_KEY);
    request.execute().assertJson(getClass(), "issues_on_module-expected.json", false);
  }

  @Test(expected = ForbiddenException.class)
  public void fail_without_preview_permission() throws Exception {
    db.prepareDbUnit(getClass(), "shared.xml");

    MockUserSession.set().setLogin("henry").setGlobalPermissions(GlobalPermissions.PROVISIONING).addComponentPermission(UserRole.USER, PROJECT_KEY, MODULE_KEY);

    WsTester.TestRequest request = tester.newGetRequest("batch", "issues").setParam("key", MODULE_KEY);
    request.execute();
  }

  @Test(expected = ForbiddenException.class)
  public void fail_without_user_permission_on_project() throws Exception {
    db.prepareDbUnit(getClass(), "shared.xml");

    MockUserSession.set().setLogin("henry").setGlobalPermissions(GlobalPermissions.PREVIEW_EXECUTION).addComponentPermission(UserRole.CODEVIEWER, PROJECT_KEY, PROJECT_KEY);

    WsTester.TestRequest request = tester.newGetRequest("batch", "issues").setParam("key", MODULE_KEY);
    request.execute();
  }

  @Test(expected = ForbiddenException.class)
  public void fail_without_user_permission_on_module() throws Exception {
    db.prepareDbUnit(getClass(), "shared.xml");

    MockUserSession.set().setLogin("henry").setGlobalPermissions(GlobalPermissions.PREVIEW_EXECUTION).addComponentPermission(UserRole.CODEVIEWER, PROJECT_KEY, MODULE_KEY);

    WsTester.TestRequest request = tester.newGetRequest("batch", "issues").setParam("key", MODULE_KEY);
    request.execute();
  }
}
