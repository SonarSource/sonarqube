/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.server.project.ws;

import com.google.common.io.Resources;
import java.util.function.Consumer;
import org.apache.commons.lang.StringUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.component.SnapshotTesting;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.organization.TestDefaultOrganizationProvider;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.component.SnapshotDto.STATUS_PROCESSED;
import static org.sonar.db.component.SnapshotDto.STATUS_UNPROCESSED;
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER;
import static org.sonar.test.JsonAssert.assertJson;

public class GhostsActionTest {

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private DefaultOrganizationProvider defaultOrganizationProvider = TestDefaultOrganizationProvider.from(db);
  private DbClient dbClient = db.getDbClient();
  private WsActionTester underTest = new WsActionTester(new GhostsAction(dbClient, userSessionRule, defaultOrganizationProvider));

  @Test
  public void verify_definition() {
    WebService.Action action = underTest.getDef();
    assertThat(action.description()).isEqualTo("List ghost projects.<br /> Requires 'Administer System' permission.");
    assertThat(action.since()).isEqualTo("5.2");
    assertThat(action.isInternal()).isFalse();

    assertThat(action.params()).hasSize(5);

    Param organization = action.param("organization");
    assertThat(organization.description()).isEqualTo("Organization key");
    assertThat(organization.since()).isEqualTo("6.3");
    assertThat(organization.isRequired()).isFalse();
    assertThat(organization.isInternal()).isTrue();
  }

  @Test
  public void ghost_projects_without_analyzed_projects() throws Exception {
    OrganizationDto organization = db.organizations().insert();
    ComponentDto ghost1 = insertGhostProject(organization);
    ComponentDto ghost2 = insertGhostProject(organization);
    ComponentDto activeProject = insertActiveProject(organization);
    userSessionRule.logIn().addPermission(ADMINISTER, organization);

    TestResponse result = underTest.newRequest()
      .setParam("organization", organization.getKey())
      .execute();

    String json = result.getInput();
    assertJson(json).isSimilarTo("{" +
      "  \"projects\": [" +
      "    {" +
      "      \"uuid\": \"" + ghost1.uuid() + "\"," +
      "      \"key\": \"" + ghost1.key() + "\"," +
      "      \"name\": \"" + ghost1.name() + "\"," +
      "      \"visibility\": \"private\"" +
      "    }," +
      "    {" +
      "      \"uuid\": \"" + ghost2.uuid() + "\"," +
      "      \"key\": \"" + ghost2.key() + "\"," +
      "      \"name\": \"" + ghost2.name() + "\"," +
      "      \"visibility\": \"private\"" +
      "    }" +
      "  ]" +
      "}");
    assertThat(json).doesNotContain(activeProject.uuid());
  }

  @Test
  public void ghost_projects_with_correct_pagination() throws Exception {
    OrganizationDto organization = db.organizations().insert();
    for (int i = 1; i <= 10; i++) {
      int count = i;
      insertGhostProject(organization, dto -> dto.setKey("ghost-key-" + count));
    }
    userSessionRule.logIn().addPermission(ADMINISTER, organization);

    TestResponse result = underTest.newRequest()
      .setParam("organization", organization.getKey())
      .setParam(Param.PAGE, "3")
      .setParam(Param.PAGE_SIZE, "4")
      .execute();

    String json = result.getInput();
    assertJson(json).isSimilarTo("{" +
      "  \"p\": 3," +
      "  \"ps\": 4," +
      "  \"total\": 10" +
      "}");
    assertThat(StringUtils.countMatches(json, "ghost-key-")).isEqualTo(2);
  }

  @Test
  public void ghost_projects_with_chosen_fields() throws Exception {
    OrganizationDto organization = db.organizations().insert();
    insertGhostProject(organization);
    userSessionRule.logIn().addPermission(ADMINISTER, organization);

    TestResponse result = underTest.newRequest()
      .setParam("organization", organization.getKey())
      .setParam(Param.FIELDS, "name")
      .execute();

    assertThat(result.getInput())
      .contains("uuid", "name")
      .doesNotContain("key")
      .doesNotContain("creationDate");
  }

  @Test
  public void ghost_projects_with_partial_query_on_name() throws Exception {
    OrganizationDto organization = db.organizations().insert();
    insertGhostProject(organization, dto -> dto.setName("ghost-name-10"));
    insertGhostProject(organization, dto -> dto.setName("ghost-name-11"));
    insertGhostProject(organization, dto -> dto.setName("ghost-name-20"));

    userSessionRule.logIn().addPermission(ADMINISTER, organization);

    TestResponse result = underTest.newRequest()
      .setParam("organization", organization.getKey())
      .setParam(Param.TEXT_QUERY, "name-1")
      .execute();

    assertThat(result.getInput())
      .contains("ghost-name-10", "ghost-name-11")
      .doesNotContain("ghost-name-2");
  }

  @Test
  public void ghost_projects_with_partial_query_on_key() throws Exception {
    OrganizationDto organization = db.organizations().insert();
    insertGhostProject(organization, dto -> dto.setKey("ghost-key-1"));

    userSessionRule.logIn().addPermission(ADMINISTER, organization);

    TestResponse result = underTest.newRequest()
      .setParam("organization", organization.getKey())
      .setParam(Param.TEXT_QUERY, "GHOST-key")
      .execute();

    assertThat(result.getInput())
      .contains("ghost-key-1");
  }

  @Test
  public void ghost_projects_base_on_json_example() throws Exception {
    OrganizationDto organization = db.organizations().insert();
    ComponentDto hBaseProject = ComponentTesting.newPrivateProjectDto(organization, "ce4c03d6-430f-40a9-b777-ad877c00aa4d")
      .setKey("org.apache.hbas:hbase")
      .setName("HBase")
      .setCreatedAt(DateUtils.parseDateTime("2015-03-04T23:03:44+0100"))
      .setPrivate(false);
    dbClient.componentDao().insert(db.getSession(), hBaseProject);
    dbClient.snapshotDao().insert(db.getSession(), SnapshotTesting.newAnalysis(hBaseProject)
      .setStatus(STATUS_UNPROCESSED));
    ComponentDto roslynProject = ComponentTesting.newPrivateProjectDto(organization, "c526ef20-131b-4486-9357-063fa64b5079")
      .setKey("com.microsoft.roslyn:roslyn")
      .setName("Roslyn")
      .setCreatedAt(DateUtils.parseDateTime("2013-03-04T23:03:44+0100"));
    dbClient.componentDao().insert(db.getSession(), roslynProject);
    dbClient.snapshotDao().insert(db.getSession(), SnapshotTesting.newAnalysis(roslynProject)
      .setStatus(STATUS_UNPROCESSED));
    db.getSession().commit();
    userSessionRule.logIn().addPermission(ADMINISTER, organization);

    TestResponse result = underTest.newRequest()
      .setParam("organization", organization.getKey())
      .execute();

    assertJson(result.getInput())
      .isSimilarTo(Resources.getResource(getClass(), "projects-example-ghosts.json"));
  }

  @Test
  public void throws_ForbiddenException_if_not_administrator_of_organization() throws Exception {
    userSessionRule.logIn();

    expectedException.expect(ForbiddenException.class);
    expectedException.expectMessage("Insufficient privileges");

    underTest.newRequest().execute();
  }

  @Test
  public void fail_with_NotFoundException_when_organization_with_specified_key_does_not_exist() {
    userSessionRule.logIn();

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("No organization for key 'foo'");

    underTest.newRequest().setParam("organization", "foo").execute();
  }

  private ComponentDto insertGhostProject(OrganizationDto organization) {
    return insertGhostProject(organization, dto -> {
    });
  }

  private ComponentDto insertGhostProject(OrganizationDto organization, Consumer<ComponentDto> consumer) {
    ComponentDto project = db.components().insertPrivateProject(organization, consumer);
    db.components().insertSnapshot(project, dto -> dto.setStatus(STATUS_UNPROCESSED));
    return project;
  }

  private ComponentDto insertActiveProject(OrganizationDto organization) {
    ComponentDto project = db.components().insertPrivateProject(organization);
    db.components().insertSnapshot(project, dto -> dto.setStatus(STATUS_PROCESSED));
    return project;
  }

}
