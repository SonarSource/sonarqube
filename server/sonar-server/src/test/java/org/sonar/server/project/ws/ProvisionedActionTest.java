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
package org.sonar.server.project.ws;

import org.apache.commons.lang.StringUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.server.ws.Change;
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
import org.sonar.server.organization.BillingValidationsProxy;
import org.sonar.server.organization.TestDefaultOrganizationProvider;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.mock;
import static org.sonar.db.permission.OrganizationPermission.PROVISION_PROJECTS;
import static org.sonar.db.permission.OrganizationPermission.SCAN;
import static org.sonar.test.JsonAssert.assertJson;

public class ProvisionedActionTest {

  private static final String PARAM_ORGANIZATION = "organization";

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private TestDefaultOrganizationProvider defaultOrganizationProvider = TestDefaultOrganizationProvider.from(db);
  private DbClient dbClient = db.getDbClient();

  private WsActionTester ws = new WsActionTester(
    new ProvisionedAction(new ProjectsWsSupport(dbClient, defaultOrganizationProvider, mock(BillingValidationsProxy.class)), dbClient, userSessionRule));

  @Test
  public void definition() {
    WebService.Action action = ws.getDef();

    assertThat(action.description()).isEqualTo("Get the list of provisioned projects.<br> " +
      "Web service is deprecated. Use api/projects/search instead, with onProvisionedOnly=true.<br> " +
      "Require 'Create Projects' permission.");
    assertThat(action.since()).isEqualTo("5.2");
    assertThat(action.changelog()).extracting(Change::getVersion, Change::getDescription).containsExactlyInAnyOrder(
      tuple("6.4", "The 'uuid' field is deprecated in the response"),
      tuple("6.4", "Paging response fields is now in a Paging object"));
    assertThat(action.deprecatedSince()).isEqualTo("6.6");

    assertThat(action.params()).hasSize(5);

    Param organization = action.param(PARAM_ORGANIZATION);
    assertThat(organization.description()).isEqualTo("The key of the organization");
    assertThat(organization.isInternal()).isTrue();
    assertThat(organization.isRequired()).isFalse();
    assertThat(organization.since()).isEqualTo("6.3");
  }

  @Test
  public void all_provisioned_projects_without_analyzed_projects() {
    OrganizationDto org = db.organizations().insert();
    ComponentDto analyzedProject = ComponentTesting.newPrivateProjectDto(org, "analyzed-uuid-1");
    db.components().insertComponents(newProvisionedProject(org, "1"), newProvisionedProject(org, "2"), analyzedProject);
    db.components().insertSnapshot(SnapshotTesting.newAnalysis(analyzedProject));
    userSessionRule.logIn().addPermission(PROVISION_PROJECTS, org);

    TestResponse result = ws.newRequest()
      .setParam(PARAM_ORGANIZATION, org.getKey())
      .execute();

    String json = result.getInput();
    assertJson(json)
      .isSimilarTo("{" +
        "  \"projects\":[" +
        "    {" +
        "      \"uuid\":\"provisioned-uuid-1\"," +
        "      \"key\":\"provisioned-key-1\"," +
        "      \"name\":\"provisioned-name-1\"," +
        "      \"visibility\":\"private\"" +
        "    }," +
        "    {" +
        "      \"uuid\":\"provisioned-uuid-2\"," +
        "      \"key\":\"provisioned-key-2\"," +
        "      \"name\":\"provisioned-name-2\"," +
        "      \"visibility\":\"private\"" +
        "    }" +
        "  ]" +
        "}");
    assertThat(json).doesNotContain("analyzed-uuid-1");
  }

  @Test
  public void provisioned_projects_with_correct_pagination() {
    OrganizationDto org = db.organizations().insert();
    for (int i = 1; i <= 10; i++) {
      db.components().insertComponent(newProvisionedProject(org, String.valueOf(i)));
    }
    userSessionRule.logIn().addPermission(PROVISION_PROJECTS, org);

    TestRequest request = ws.newRequest()
      .setParam(PARAM_ORGANIZATION, org.getKey())
      .setParam(Param.PAGE, "3")
      .setParam(Param.PAGE_SIZE, "4");

    String jsonOutput = request.execute().getInput();

    assertThat(StringUtils.countMatches(jsonOutput, "provisioned-uuid-")).isEqualTo(2);
  }

  @Test
  public void provisioned_projects_with_desired_fields() {
    OrganizationDto organization = db.organizations().insert();
    db.components().insertComponent(newProvisionedProject(organization, "1"));
    userSessionRule.logIn().addPermission(PROVISION_PROJECTS, organization);

    String jsonOutput = ws.newRequest()
      .setParam(PARAM_ORGANIZATION, organization.getKey())
      .setParam(Param.FIELDS, "key")
      .execute().getInput();

    assertThat(jsonOutput).contains("uuid", "key")
      .doesNotContain("name")
      .doesNotContain("creationDate");
  }

  @Test
  public void provisioned_projects_with_query() {
    OrganizationDto org = db.organizations().insert();
    db.components().insertComponents(newProvisionedProject(org, "1"), newProvisionedProject(org, "2"));
    userSessionRule.logIn().addPermission(PROVISION_PROJECTS, org);

    String jsonOutput = ws.newRequest()
      .setParam(PARAM_ORGANIZATION, org.getKey())
      .setParam(Param.TEXT_QUERY, "PROVISIONED-name-2")
      .execute().getInput();

    assertThat(jsonOutput)
      .contains("provisioned-name-2", "provisioned-uuid-2")
      .doesNotContain("provisioned-uuid-1");
  }

  @Test
  public void provisioned_projects_as_defined_in_the_example() {
    OrganizationDto org = db.organizations().insert();
    ComponentDto hBaseProject = ComponentTesting.newPrivateProjectDto(org, "ce4c03d6-430f-40a9-b777-ad877c00aa4d")
      .setDbKey("org.apache.hbas:hbase")
      .setName("HBase")
      .setCreatedAt(DateUtils.parseDateTime("2015-03-04T23:03:44+0100"))
      .setPrivate(false);
    ComponentDto roslynProject = ComponentTesting.newPrivateProjectDto(org, "c526ef20-131b-4486-9357-063fa64b5079")
      .setDbKey("com.microsoft.roslyn:roslyn")
      .setName("Roslyn")
      .setCreatedAt(DateUtils.parseDateTime("2013-03-04T23:03:44+0100"));
    db.components().insertComponents(hBaseProject, roslynProject);
    userSessionRule.logIn().addPermission(PROVISION_PROJECTS, org);

    TestResponse result = ws.newRequest()
      .setParam(PARAM_ORGANIZATION, org.getKey())
      .execute();

    assertJson(result.getInput()).isSimilarTo(ws.getDef().responseExampleAsString());
  }

  @Test
  public void fail_when_not_enough_privileges() {
    OrganizationDto organization = db.organizations().insert();
    db.components().insertComponent(newProvisionedProject(organization, "1"));
    userSessionRule.logIn().addPermission(SCAN, organization);

    expectedException.expect(ForbiddenException.class);

    ws.newRequest().execute();
  }

  @Test
  public void fail_with_NotFoundException_when_organization_with_specified_key_does_not_exist() {
    TestRequest request = ws.newRequest()
      .setParam(PARAM_ORGANIZATION, "foo");
    userSessionRule.logIn();

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("No organization for key 'foo'");

    request.execute();
  }

  private static ComponentDto newProvisionedProject(OrganizationDto organizationDto, String uuid) {
    return ComponentTesting
      .newPrivateProjectDto(organizationDto, "provisioned-uuid-" + uuid)
      .setName("provisioned-name-" + uuid)
      .setDbKey("provisioned-key-" + uuid);
  }
}
