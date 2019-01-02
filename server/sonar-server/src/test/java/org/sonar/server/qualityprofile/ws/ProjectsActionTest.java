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
package org.sonar.server.qualityprofile.ws;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.organization.TestDefaultOrganizationProvider;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.server.ws.WebService.Param.PAGE;
import static org.sonar.api.server.ws.WebService.Param.PAGE_SIZE;
import static org.sonar.api.server.ws.WebService.Param.TEXT_QUERY;
import static org.sonar.api.web.UserRole.USER;
import static org.sonar.db.organization.OrganizationDto.Subscription.PAID;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_KEY;

public class ProjectsActionTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  private WsActionTester ws = new WsActionTester(
    new ProjectsAction(db.getDbClient(), userSession, new QProfileWsSupport(db.getDbClient(), userSession, TestDefaultOrganizationProvider.from(db))));

  @Test
  public void list_authorized_projects_only() {
    OrganizationDto organization = db.organizations().insert();
    ComponentDto project1 = db.components().insertPrivateProject(organization);
    ComponentDto project2 = db.components().insertPrivateProject(organization);
    QProfileDto qualityProfile = db.qualityProfiles().insert(organization);
    associateProjectsWithProfile(qualityProfile, project1, project2);
    // user only sees project1
    UserDto user = db.users().insertUser();
    db.users().insertProjectPermissionOnUser(user, USER, project1);
    userSession.logIn(user);

    ws.newRequest()
      .setParam(PARAM_KEY, qualityProfile.getKee())
      .setParam("selected", "selected")
      .execute()
      .assertJson("{\"results\":\n" +
        "  [\n" +
        "    {\n" +
        "      \"key\": \"" + project1.getKey() + "\",\n" +
        "      \"name\": \"" + project1.name() + "\",\n" +
        "      \"selected\": true\n" +
        "    }\n" +
        "  ]}");
  }

  @Test
  public void paginate() {
    OrganizationDto organization = db.organizations().insert();
    ComponentDto project1 = db.components().insertPublicProject(organization, p -> p.setName("Project One"));
    ComponentDto project2 = db.components().insertPublicProject(organization, p -> p.setName("Project Two"));
    ComponentDto project3 = db.components().insertPublicProject(organization, p -> p.setName("Project Three"));
    ComponentDto project4 = db.components().insertPublicProject(organization, p -> p.setName("Project Four"));
    QProfileDto qualityProfile = db.qualityProfiles().insert(organization);
    associateProjectsWithProfile(qualityProfile, project1, project2, project3, project4);

    ws.newRequest()
      .setParam(PARAM_KEY, qualityProfile.getKee())
      .setParam("selected", "selected")
      .setParam(PAGE_SIZE, "2")
      .execute()
      .assertJson("{\n" +
        "  \"results\":\n" +
        "  [\n" +
        "    {\n" +
        "      \"key\": \"" + project4.getKey() + "\",\n" +
        "      \"selected\": true\n" +
        "    },\n" +
        "    {\n" +
        "      \"key\": \"" + project1.getKey() + "\",\n" +
        "      \"selected\": true\n" +
        "    }\n" +
        "  ]\n" +
        "}\n");
    ws.newRequest()
      .setParam(PARAM_KEY, qualityProfile.getKee())
      .setParam("selected", "selected")
      .setParam(PAGE_SIZE, "2")
      .setParam(PAGE, "2")
      .execute()
      .assertJson("{\n" +
        "  \"results\":\n" +
        "  [\n" +
        "    {\n" +
        "      \"key\": \"" + project3.getKey() + "\",\n" +
        "      \"selected\": true\n" +
        "    },\n" +
        "    {\n" +
        "      \"key\": \"" + project2.getKey() + "\",\n" +
        "      \"selected\": true\n" +
        "    }\n" +
        "  ]\n" +
        "}\n");
    ws.newRequest()
      .setParam(PARAM_KEY, qualityProfile.getKee())
      .setParam("selected", "selected")
      .setParam(PAGE_SIZE, "2")
      .setParam(PAGE, "3")
      .execute()
      .assertJson("{\"results\":[]}");
    ws.newRequest()
      .setParam(PARAM_KEY, qualityProfile.getKee())
      .setParam("selected", "selected")
      .setParam(PAGE_SIZE, "2")
      .setParam(PAGE, "4")
      .execute()
      .assertJson("{\"results\":[]}");

    ws.newRequest()
      .setParam(PARAM_KEY, qualityProfile.getKee())
      .setParam("selected", "selected")
      .setParam(PAGE_SIZE, "3")
      .setParam(PAGE, "1")
      .execute()
      .assertJson("{\n" +
        "  \"results\":\n" +
        "  [\n" +
        "    {\n" +
        "      \"key\": \"" + project4.getKey() + "\",\n" +
        "      \"selected\": true\n" +
        "    },\n" +
        "    {\n" +
        "      \"key\": \"" + project1.getKey() + "\",\n" +
        "      \"selected\": true\n" +
        "    },\n" +
        "    {\n" +
        "      \"key\": \"" + project3.getKey() + "\",\n" +
        "      \"selected\": true\n" +
        "    }\n" +
        "  ]\n" +
        "}\n");
    ws.newRequest()
      .setParam(PARAM_KEY, qualityProfile.getKee())
      .setParam("selected", "selected")
      .setParam(PAGE_SIZE, "3")
      .setParam(PAGE, "2")
      .execute()
      .assertJson("{\n" +
        "  \"results\":\n" +
        "  [\n" +
        "    {\n" +
        "      \"key\": \"" + project2.getKey() + "\",\n" +
        "      \"selected\": true\n" +
        "    }\n" +
        "  ]\n" +
        "}\n");
    ws.newRequest()
      .setParam(PARAM_KEY, qualityProfile.getKee())
      .setParam("selected", "selected")
      .setParam(PAGE_SIZE, "3")
      .setParam(PAGE, "3")
      .execute()
      .assertJson("{\"results\":[]}");
  }

  @Test
  public void show_unselected() {
    OrganizationDto organization = db.organizations().insert();
    ComponentDto project1 = db.components().insertPublicProject(organization);
    ComponentDto project2 = db.components().insertPublicProject(organization);
    QProfileDto qualityProfile = db.qualityProfiles().insert(organization);
    associateProjectsWithProfile(qualityProfile, project1);

    ws.newRequest()
      .setParam(PARAM_KEY, qualityProfile.getKee())
      .setParam("selected", "deselected")
      .execute()
      .assertJson("{ \"results\":\n" +
        "  [\n" +
        "    {\n" +
        "      \"key\": \"" + project2.getKey() + "\",\n" +
        "      \"selected\": false\n" +
        "    }\n" +
        "  ]}");
  }

  @Test
  public void show_all() {
    OrganizationDto organization = db.organizations().insert();
    ComponentDto project1 = db.components().insertPublicProject(organization, p -> p.setName("Project 1"));
    ComponentDto project2 = db.components().insertPublicProject(organization, p -> p.setName("Project 2"));
    ComponentDto project3 = db.components().insertPublicProject(organization, p -> p.setName("Project 3"));
    ComponentDto project4 = db.components().insertPublicProject(organization, p -> p.setName("Project 4"));
    QProfileDto qualityProfile1 = db.qualityProfiles().insert(organization);
    associateProjectsWithProfile(qualityProfile1, project1, project2);
    QProfileDto qualityProfile2 = db.qualityProfiles().insert(organization);
    // project3 is associated with P2, must appear as not associated with xooP1
    associateProjectsWithProfile(qualityProfile2, project3);

    ws.newRequest()
      .setParam(PARAM_KEY, qualityProfile1.getKee())
      .setParam("selected", "all")
      .execute()
      .assertJson("{\n" +
        "  \"results\": [\n" +
        "    {\n" +
        "      \"key\": \"" + project1.getKey() + "\",\n" +
        "      \"selected\": true\n" +
        "    },\n" +
        "    {\n" +
        "      \"key\": \"" + project2.getKey() + "\",\n" +
        "      \"selected\": true\n" +
        "    },\n" +
        "    {\n" +
        "      \"key\": \"" + project3.getKey() + "\",\n" +
        "      \"selected\": false\n" +
        "    },\n" +
        "    {\n" +
        "      \"key\": \"" + project4.getKey() + "\",\n" +
        "      \"selected\": false\n" +
        "    }\n" +
        "  ]}\n");
  }

  @Test
  public void filter_on_name() {
    OrganizationDto organization = db.organizations().insert();
    ComponentDto project1 = db.components().insertPublicProject(organization, p -> p.setName("Project One"));
    ComponentDto project2 = db.components().insertPublicProject(organization, p -> p.setName("Project Two"));
    ComponentDto project3 = db.components().insertPublicProject(organization, p -> p.setName("Project Three"));
    ComponentDto project4 = db.components().insertPublicProject(organization, p -> p.setName("Project Four"));
    QProfileDto qualityProfile = db.qualityProfiles().insert(organization);
    associateProjectsWithProfile(qualityProfile, project1, project2);

    ws.newRequest()
      .setParam(PARAM_KEY, qualityProfile.getKee())
      .setParam("selected", "all")
      .setParam(TEXT_QUERY, "project t")
      .execute()
      .assertJson("{\n" +
        "  \"results\":\n" +
        "  [\n" +
        "    {\n" +
        "      \"key\": \"" + project3.getKey() + "\",\n" +
        "      \"name\": \"Project Three\",\n" +
        "      \"selected\": false\n" +
        "    },\n" +
        "    {\n" +
        "      \"key\": \"" + project2.getKey() + "\",\n" +
        "      \"name\": \"Project Two\",\n" +
        "      \"selected\": true\n" +
        "    }\n" +
        "  ]}\n");
  }

  @Test
  public void return_deprecated_uuid_field() {
    OrganizationDto organization = db.organizations().insert();
    ComponentDto project = db.components().insertPublicProject(organization);
    QProfileDto qualityProfile = db.qualityProfiles().insert(organization);
    associateProjectsWithProfile(qualityProfile, project);

    ws.newRequest()
      .setParam(PARAM_KEY, qualityProfile.getKee())
      .setParam("selected", "all")
      .execute()
      .assertJson("{\"results\":\n" +
        "  [\n" +
        "    {\n" +
        "      \"id\": \"" + project.uuid() + "\",\n" +
        "      \"key\": \"" + project.getKey() + "\",\n" +
        "    }\n" +
        "  ]}");
  }

  @Test
  public void projects_on_paid_organization() {
    OrganizationDto organization = db.organizations().insert();
    ComponentDto project = db.components().insertPublicProject(organization);
    QProfileDto qualityProfile = db.qualityProfiles().insert(organization);
    associateProjectsWithProfile(qualityProfile, project);
    UserDto user = db.users().insertUser();
    db.organizations().addMember(organization, user);
    userSession.logIn(user);

    ws.newRequest()
      .setParam(PARAM_KEY, qualityProfile.getKee())
      .setParam("selected", "all")
      .execute()
      .assertJson("{\"results\":\n" +
        "  [\n" +
        "    {\n" +
        "      \"key\": \"" + project.getKey() + "\",\n" +
        "    }\n" +
        "  ]}");
  }

  @Test
  public void fail_on_nonexistent_profile() {
    expectedException.expect(NotFoundException.class);

    ws.newRequest()
      .setParam(PARAM_KEY, "unknown")
      .execute();
  }

  @Test
  public void fail_on_paid_organization_when_not_member() {
    OrganizationDto organization = db.organizations().insert(o -> o.setSubscription(PAID));
    QProfileDto qualityProfile = db.qualityProfiles().insert(organization);

    expectedException.expect(ForbiddenException.class);
    expectedException.expectMessage(format("You're not member of organization '%s'", organization.getKey()));

    ws.newRequest()
      .setParam(PARAM_KEY, qualityProfile.getKee())
      .execute();
  }

  @Test
  public void definition() {
    WebService.Action definition = ws.getDef();

    assertThat(definition.key()).isEqualTo("projects");
    assertThat(definition.responseExampleAsString()).isNotEmpty();
    assertThat(definition.params()).extracting(Param::key).containsExactlyInAnyOrder("key", "p", "ps", "q", "selected");
    Param profile = definition.param("key");
    assertThat(profile.deprecatedKey()).isNullOrEmpty();
    Param page = definition.param("p");
    assertThat(page.deprecatedKey()).isEqualTo("page");
    Param pageSize = definition.param("ps");
    assertThat(pageSize.deprecatedKey()).isEqualTo("pageSize");
    Param query = definition.param("q");
    assertThat(query.deprecatedKey()).isEqualTo("query");
  }

  private void associateProjectsWithProfile(QProfileDto profile, ComponentDto... projects) {
    for (ComponentDto project : projects) {
      db.getDbClient().qualityProfileDao().insertProjectProfileAssociation(db.getSession(), project, profile);
    }
    db.commit();
  }
}
