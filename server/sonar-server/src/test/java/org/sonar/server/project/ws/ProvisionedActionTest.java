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

package org.sonar.server.project.ws;

import com.google.common.io.Resources;
import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.System2;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDao;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.server.component.SnapshotTesting;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsTester;
import org.sonar.test.DbTests;
import org.sonar.test.JsonAssert;

import static org.assertj.core.api.Assertions.assertThat;

@Category(DbTests.class)
public class ProvisionedActionTest {

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  WsTester ws;
  DbClient dbClient = db.getDbClient();
  ComponentDao componentDao;

  @Before
  public void setUp() {
    componentDao = dbClient.componentDao();
    db.truncateTables();
    ws = new WsTester(new ProjectsWs(new ProvisionedAction(dbClient, userSessionRule)));
  }

  @Test
  public void all_provisioned_projects_without_analyzed_projects() throws Exception {
    userSessionRule.setGlobalPermissions(GlobalPermissions.PROVISIONING);
    ComponentDto analyzedProject = ComponentTesting.newProjectDto("analyzed-uuid-1");
    componentDao.insert(db.getSession(), newProvisionedProject("1"), newProvisionedProject("2"), analyzedProject);
    SnapshotDto snapshot = SnapshotTesting.createForProject(analyzedProject);
    dbClient.snapshotDao().insert(db.getSession(), snapshot);
    db.getSession().commit();

    WsTester.Result result = ws.newGetRequest("api/projects", "provisioned").execute();

    result.assertJson(getClass(), "all-projects.json");
    assertThat(result.outputAsString()).doesNotContain("analyzed-uuid-1");
  }

  @Test
  public void provisioned_projects_with_correct_pagination() throws Exception {
    userSessionRule.setGlobalPermissions(GlobalPermissions.PROVISIONING);
    for (int i = 1; i <= 10; i++) {
      componentDao.insert(db.getSession(), newProvisionedProject(String.valueOf(i)));
    }
    db.getSession().commit();

    WsTester.TestRequest request = ws.newGetRequest("api/projects", "provisioned")
      .setParam(Param.PAGE, "3")
      .setParam(Param.PAGE_SIZE, "4");

    String jsonOutput = request.execute().outputAsString();

    assertThat(StringUtils.countMatches(jsonOutput, "provisioned-uuid-")).isEqualTo(2);
  }

  @Test
  public void provisioned_projects_with_desired_fields() throws Exception {
    userSessionRule.setGlobalPermissions(GlobalPermissions.PROVISIONING);
    componentDao.insert(db.getSession(), newProvisionedProject("1"));
    db.getSession().commit();

    String jsonOutput = ws.newGetRequest("api/projects", "provisioned")
      .setParam(Param.FIELDS, "key")
      .execute().outputAsString();

    assertThat(jsonOutput).contains("uuid", "key")
      .doesNotContain("name")
      .doesNotContain("creationDate");
  }

  @Test
  public void provisioned_projects_with_query() throws Exception {
    userSessionRule.setGlobalPermissions(GlobalPermissions.PROVISIONING);
    componentDao.insert(db.getSession(), newProvisionedProject("1"), newProvisionedProject("2"));
    db.getSession().commit();

    String jsonOutput = ws.newGetRequest("api/projects", "provisioned")
      .setParam(Param.TEXT_QUERY, "PROVISIONED-name-2")
      .execute().outputAsString();

    assertThat(jsonOutput)
      .contains("provisioned-name-2", "provisioned-uuid-2")
      .doesNotContain("provisioned-uuid-1");
    assertThat(componentDao.countProvisionedProjects(db.getSession(), "name-2")).isEqualTo(1);
    assertThat(componentDao.countProvisionedProjects(db.getSession(), "key-2")).isEqualTo(1);
    assertThat(componentDao.countProvisionedProjects(db.getSession(), "visioned-name-")).isEqualTo(2);
  }

  @Test
  public void provisioned_projects_as_defined_in_the_example() throws Exception {
    userSessionRule.setGlobalPermissions(GlobalPermissions.PROVISIONING);
    ComponentDto hBaseProject = ComponentTesting.newProjectDto("ce4c03d6-430f-40a9-b777-ad877c00aa4d")
      .setKey("org.apache.hbas:hbase")
      .setName("HBase")
      .setCreatedAt(DateUtils.parseDateTime("2015-03-04T23:03:44+0100"));
    ComponentDto roslynProject = ComponentTesting.newProjectDto("c526ef20-131b-4486-9357-063fa64b5079")
      .setKey("com.microsoft.roslyn:roslyn")
      .setName("Roslyn")
      .setCreatedAt(DateUtils.parseDateTime("2013-03-04T23:03:44+0100"));
    componentDao.insert(db.getSession(), hBaseProject, roslynProject);
    db.getSession().commit();

    WsTester.Result result = ws.newGetRequest("api/projects", "provisioned").execute();

    JsonAssert.assertJson(result.outputAsString()).isSimilarTo(Resources.getResource(getClass(), "projects-example-provisioned.json"));
  }

  @Test
  public void fail_when_not_enough_privileges() throws Exception {
    expectedException.expect(ForbiddenException.class);
    userSessionRule.setGlobalPermissions(GlobalPermissions.SCAN_EXECUTION);
    componentDao.insert(db.getSession(), newProvisionedProject("1"));

    ws.newGetRequest("api/projects", "provisioned").execute();
  }

  private static ComponentDto newProvisionedProject(String uuid) {
    return ComponentTesting
      .newProjectDto("provisioned-uuid-" + uuid)
      .setName("provisioned-name-" + uuid)
      .setKey("provisioned-key-" + uuid);
  }
}
