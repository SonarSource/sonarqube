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
import org.sonar.api.resources.Languages;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbTester;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.permission.OrganizationPermission;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.language.LanguageTesting;
import org.sonar.server.organization.TestDefaultOrganizationProvider;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.Qualityprofiles.SearchGroupsResponse;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonar.api.server.ws.WebService.Param.PAGE;
import static org.sonar.api.server.ws.WebService.Param.PAGE_SIZE;
import static org.sonar.api.server.ws.WebService.Param.SELECTED;
import static org.sonar.api.server.ws.WebService.Param.TEXT_QUERY;
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER_QUALITY_PROFILES;
import static org.sonar.db.user.GroupTesting.newGroupDto;
import static org.sonar.test.JsonAssert.assertJson;
import static org.sonarqube.ws.MediaTypes.JSON;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_LANGUAGE;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_ORGANIZATION;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_QUALITY_PROFILE;

public class SearchGroupsActionTest {

  private static final String XOO = "xoo";
  private static final String FOO = "foo";
  private static final Languages LANGUAGES = LanguageTesting.newLanguages(XOO, FOO);

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create();

  private QProfileWsSupport wsSupport = new QProfileWsSupport(db.getDbClient(), userSession, TestDefaultOrganizationProvider.from(db));

  private WsActionTester ws = new WsActionTester(new SearchGroupsAction(db.getDbClient(), wsSupport, LANGUAGES));

  @Test
  public void test_definition() {
    WebService.Action def = ws.getDef();
    assertThat(def.key()).isEqualTo("search_groups");
    assertThat(def.isPost()).isFalse();
    assertThat(def.isInternal()).isTrue();
    assertThat(def.params()).extracting(WebService.Param::key)
      .containsExactlyInAnyOrder("organization", "qualityProfile", "language", "selected", "q", "p", "ps");
  }

  @Test
  public void test_example() {
    OrganizationDto organization = db.organizations().insert();
    QProfileDto profile = db.qualityProfiles().insert(organization, p -> p.setLanguage(XOO));
    GroupDto group1 = db.users().insertGroup(newGroupDto().setName("users").setDescription("Users").setOrganizationUuid(organization.getUuid()));
    GroupDto group2 = db.users().insertGroup(newGroupDto().setName("administrators").setDescription("Administrators").setOrganizationUuid(organization.getUuid()));
    db.qualityProfiles().addGroupPermission(profile, group1);
    userSession.logIn().addPermission(ADMINISTER_QUALITY_PROFILES, organization);

    String result = ws.newRequest()
      .setParam(PARAM_ORGANIZATION, organization.getKey())
      .setParam(PARAM_QUALITY_PROFILE, profile.getName())
      .setParam(PARAM_LANGUAGE, XOO)
      .setParam(SELECTED, "all")
      .setMediaType(JSON)
      .execute()
      .getInput();

    assertJson(ws.getDef().responseExampleAsString()).isSimilarTo(result);
  }

  @Test
  public void search_all_groups() {
    OrganizationDto organization = db.organizations().insert();
    QProfileDto profile = db.qualityProfiles().insert(organization, p -> p.setLanguage(XOO));
    GroupDto group1 = db.users().insertGroup(organization);
    GroupDto group2 = db.users().insertGroup(organization);
    db.qualityProfiles().addGroupPermission(profile, group1);
    userSession.logIn().addPermission(ADMINISTER_QUALITY_PROFILES, organization);

    SearchGroupsResponse response = ws.newRequest()
      .setParam(PARAM_ORGANIZATION, organization.getKey())
      .setParam(PARAM_QUALITY_PROFILE, profile.getName())
      .setParam(PARAM_LANGUAGE, XOO)
      .setParam(SELECTED, "all")
      .executeProtobuf(SearchGroupsResponse.class);

    assertThat(response.getGroupsList()).extracting(SearchGroupsResponse.Group::getName, SearchGroupsResponse.Group::getDescription, SearchGroupsResponse.Group::getSelected)
      .containsExactlyInAnyOrder(
        tuple(group1.getName(), group1.getDescription(), true),
        tuple(group2.getName(), group2.getDescription(), false));
  }

  @Test
  public void search_selected_groups() {
    OrganizationDto organization = db.organizations().insert();
    QProfileDto profile = db.qualityProfiles().insert(organization, p -> p.setLanguage(XOO));
    GroupDto group1 = db.users().insertGroup(organization);
    GroupDto group2 = db.users().insertGroup(organization);
    db.qualityProfiles().addGroupPermission(profile, group1);
    userSession.logIn().addPermission(ADMINISTER_QUALITY_PROFILES, organization);

    SearchGroupsResponse response = ws.newRequest()
      .setParam(PARAM_ORGANIZATION, organization.getKey())
      .setParam(PARAM_QUALITY_PROFILE, profile.getName())
      .setParam(PARAM_LANGUAGE, XOO)
      .setParam(SELECTED, "selected")
      .executeProtobuf(SearchGroupsResponse.class);

    assertThat(response.getGroupsList()).extracting(SearchGroupsResponse.Group::getName, SearchGroupsResponse.Group::getDescription, SearchGroupsResponse.Group::getSelected)
      .containsExactlyInAnyOrder(
        tuple(group1.getName(), group1.getDescription(), true));
  }

  @Test
  public void search_deselected_groups() {
    OrganizationDto organization = db.organizations().insert();
    QProfileDto profile = db.qualityProfiles().insert(organization, p -> p.setLanguage(XOO));
    GroupDto group1 = db.users().insertGroup(organization);
    GroupDto group2 = db.users().insertGroup(organization);
    db.qualityProfiles().addGroupPermission(profile, group1);
    userSession.logIn().addPermission(ADMINISTER_QUALITY_PROFILES, organization);

    SearchGroupsResponse response = ws.newRequest()
      .setParam(PARAM_ORGANIZATION, organization.getKey())
      .setParam(PARAM_QUALITY_PROFILE, profile.getName())
      .setParam(PARAM_LANGUAGE, XOO)
      .setParam(SELECTED, "deselected")
      .executeProtobuf(SearchGroupsResponse.class);

    assertThat(response.getGroupsList()).extracting(SearchGroupsResponse.Group::getName, SearchGroupsResponse.Group::getDescription, SearchGroupsResponse.Group::getSelected)
      .containsExactlyInAnyOrder(
        tuple(group2.getName(), group2.getDescription(), false));
  }

  @Test
  public void search_by_name() {
    OrganizationDto organization = db.organizations().insert();
    QProfileDto profile = db.qualityProfiles().insert(organization, p -> p.setLanguage(XOO));
    GroupDto group1 = db.users().insertGroup(organization, "sonar-users-project");
    GroupDto group2 = db.users().insertGroup(organization, "sonar-users-qprofile");
    GroupDto group3 = db.users().insertGroup(organization, "sonar-admin");
    db.qualityProfiles().addGroupPermission(profile, group1);
    db.qualityProfiles().addGroupPermission(profile, group2);
    db.qualityProfiles().addGroupPermission(profile, group3);
    userSession.logIn().addPermission(ADMINISTER_QUALITY_PROFILES, organization);

    SearchGroupsResponse response = ws.newRequest()
      .setParam(PARAM_ORGANIZATION, organization.getKey())
      .setParam(PARAM_QUALITY_PROFILE, profile.getName())
      .setParam(PARAM_LANGUAGE, XOO)
      .setParam(TEXT_QUERY, "UsErS")
      .setParam(SELECTED, "all")
      .executeProtobuf(SearchGroupsResponse.class);

    assertThat(response.getGroupsList()).extracting(SearchGroupsResponse.Group::getName)
      .containsExactlyInAnyOrder(group1.getName(), group2.getName());
  }

  @Test
  public void group_without_description() {
    OrganizationDto organization = db.organizations().insert();
    QProfileDto profile = db.qualityProfiles().insert(organization, p -> p.setLanguage(XOO));
    GroupDto group = db.users().insertGroup(newGroupDto().setDescription(null).setOrganizationUuid(organization.getUuid()));
    db.qualityProfiles().addGroupPermission(profile, group);
    userSession.logIn().addPermission(ADMINISTER_QUALITY_PROFILES, organization);

    SearchGroupsResponse response = ws.newRequest()
      .setParam(PARAM_ORGANIZATION, organization.getKey())
      .setParam(PARAM_QUALITY_PROFILE, profile.getName())
      .setParam(PARAM_LANGUAGE, XOO)
      .setParam(SELECTED, "all")
      .executeProtobuf(SearchGroupsResponse.class);

    assertThat(response.getGroupsList()).extracting(SearchGroupsResponse.Group::getName, SearchGroupsResponse.Group::hasDescription)
      .containsExactlyInAnyOrder(tuple(group.getName(), false));
  }

  @Test
  public void paging_search() {
    OrganizationDto organization = db.organizations().insert();
    QProfileDto profile = db.qualityProfiles().insert(organization, p -> p.setLanguage(XOO));
    GroupDto group3 = db.users().insertGroup(organization, "group3");
    GroupDto group1 = db.users().insertGroup(organization, "group1");
    GroupDto group2 = db.users().insertGroup(organization, "group2");
    db.qualityProfiles().addGroupPermission(profile, group1);
    db.qualityProfiles().addGroupPermission(profile, group2);
    userSession.logIn().addPermission(ADMINISTER_QUALITY_PROFILES, organization);

    assertThat(ws.newRequest()
      .setParam(PARAM_ORGANIZATION, organization.getKey())
      .setParam(PARAM_QUALITY_PROFILE, profile.getName())
      .setParam(PARAM_LANGUAGE, XOO)
      .setParam(SELECTED, "all")
      .setParam(PAGE, "1")
      .setParam(PAGE_SIZE, "1")
      .executeProtobuf(SearchGroupsResponse.class).getGroupsList())
      .extracting(SearchGroupsResponse.Group::getName)
      .containsExactly(group1.getName());

    assertThat(ws.newRequest()
      .setParam(PARAM_ORGANIZATION, organization.getKey())
      .setParam(PARAM_QUALITY_PROFILE, profile.getName())
      .setParam(PARAM_LANGUAGE, XOO)
      .setParam(SELECTED, "all")
      .setParam(PAGE, "3")
      .setParam(PAGE_SIZE, "1")
      .executeProtobuf(SearchGroupsResponse.class).getGroupsList())
      .extracting(SearchGroupsResponse.Group::getName)
      .containsExactly(group3.getName());

    assertThat(ws.newRequest()
      .setParam(PARAM_ORGANIZATION, organization.getKey())
      .setParam(PARAM_QUALITY_PROFILE, profile.getName())
      .setParam(PARAM_LANGUAGE, XOO)
      .setParam(SELECTED, "all")
      .setParam(PAGE, "1")
      .setParam(PAGE_SIZE, "10")
      .executeProtobuf(SearchGroupsResponse.class).getGroupsList())
      .extracting(SearchGroupsResponse.Group::getName)
      .containsExactly(group1.getName(), group2.getName(), group3.getName());
  }

  @Test
  public void uses_default_organization_when_no_organization() {
    OrganizationDto organization = db.getDefaultOrganization();
    QProfileDto profile = db.qualityProfiles().insert(organization, p -> p.setLanguage(XOO));
    GroupDto group = db.users().insertGroup(organization);
    db.qualityProfiles().addGroupPermission(profile, group);
    userSession.logIn().addPermission(ADMINISTER_QUALITY_PROFILES, organization);

    SearchGroupsResponse response = ws.newRequest()
      .setParam(PARAM_QUALITY_PROFILE, profile.getName())
      .setParam(PARAM_LANGUAGE, XOO)
      .setParam(SELECTED, "all")
      .executeProtobuf(SearchGroupsResponse.class);

    assertThat(response.getGroupsList()).extracting(SearchGroupsResponse.Group::getName).containsExactlyInAnyOrder(group.getName());
  }

  @Test
  public void qp_administers_can_search_groups() {
    OrganizationDto organization = db.organizations().insert();
    QProfileDto profile = db.qualityProfiles().insert(organization, p -> p.setLanguage(XOO));
    GroupDto group = db.users().insertGroup(organization);
    db.qualityProfiles().addGroupPermission(profile, group);
    userSession.logIn().addPermission(OrganizationPermission.ADMINISTER_QUALITY_PROFILES, organization);

    SearchGroupsResponse response = ws.newRequest()
      .setParam(PARAM_ORGANIZATION, organization.getKey())
      .setParam(PARAM_QUALITY_PROFILE, profile.getName())
      .setParam(PARAM_LANGUAGE, XOO)
      .setParam(SELECTED, "all")
      .executeProtobuf(SearchGroupsResponse.class);

    assertThat(response.getGroupsList()).extracting(SearchGroupsResponse.Group::getName).containsExactlyInAnyOrder(group.getName());
  }

  @Test
  public void qp_editors_can_search_groups() {
    OrganizationDto organization = db.organizations().insert();
    QProfileDto profile = db.qualityProfiles().insert(organization, p -> p.setLanguage(XOO));
    GroupDto group = db.users().insertGroup(organization);
    db.qualityProfiles().addGroupPermission(profile, group);
    UserDto userAllowedToEditProfile = db.users().insertUser();
    db.qualityProfiles().addUserPermission(profile, userAllowedToEditProfile);
    userSession.logIn(userAllowedToEditProfile);

    SearchGroupsResponse response = ws.newRequest()
      .setParam(PARAM_ORGANIZATION, organization.getKey())
      .setParam(PARAM_QUALITY_PROFILE, profile.getName())
      .setParam(PARAM_LANGUAGE, XOO)
      .setParam(SELECTED, "all")
      .executeProtobuf(SearchGroupsResponse.class);

    assertThat(response.getGroupsList()).extracting(SearchGroupsResponse.Group::getName).containsExactlyInAnyOrder(group.getName());
  }

  @Test
  public void fail_when_qprofile_does_not_exist() {
    OrganizationDto organization = db.organizations().insert();
    userSession.logIn().addPermission(OrganizationPermission.ADMINISTER_QUALITY_PROFILES, organization);

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage(format("Quality Profile for language 'xoo' and name 'unknown' does not exist in organization '%s'", organization.getKey()));

    ws.newRequest()
      .setParam(PARAM_QUALITY_PROFILE, "unknown")
      .setParam(PARAM_LANGUAGE, XOO)
      .setParam(PARAM_ORGANIZATION, organization.getKey())
      .execute();
  }

  @Test
  public void fail_when_qprofile_does_not_belong_to_organization() {
    OrganizationDto organization = db.organizations().insert();
    OrganizationDto anotherOrganization = db.organizations().insert();
    QProfileDto profile = db.qualityProfiles().insert(anotherOrganization, p -> p.setLanguage(XOO));
    userSession.logIn().addPermission(OrganizationPermission.ADMINISTER_QUALITY_PROFILES, organization);

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage(format("Quality Profile for language 'xoo' and name '%s' does not exist in organization '%s'", profile.getName(), organization.getKey()));

    ws.newRequest()
      .setParam(PARAM_QUALITY_PROFILE, profile.getName())
      .setParam(PARAM_LANGUAGE, XOO)
      .setParam(PARAM_ORGANIZATION, organization.getKey())
      .execute();
  }

  @Test
  public void fail_when_wrong_language() {
    OrganizationDto organization = db.organizations().insert();
    QProfileDto profile = db.qualityProfiles().insert(organization, p -> p.setLanguage(XOO));
    userSession.logIn().addPermission(ADMINISTER_QUALITY_PROFILES, organization);

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage(format("Quality Profile for language 'foo' and name '%s' does not exist in organization '%s'", profile.getName(), organization.getKey()));

    ws.newRequest()
      .setParam(PARAM_ORGANIZATION, organization.getKey())
      .setParam(PARAM_QUALITY_PROFILE, profile.getName())
      .setParam(PARAM_LANGUAGE, FOO)
      .executeProtobuf(SearchGroupsResponse.class);
  }

  @Test
  public void fail_when_not_enough_permission() {
    OrganizationDto organization = db.organizations().insert();
    QProfileDto profile = db.qualityProfiles().insert(organization, p -> p.setLanguage(XOO));
    userSession.logIn(db.users().insertUser()).addPermission(OrganizationPermission.ADMINISTER_QUALITY_GATES, organization);

    expectedException.expect(ForbiddenException.class);

    ws.newRequest()
      .setParam(PARAM_QUALITY_PROFILE, profile.getName())
      .setParam(PARAM_LANGUAGE, XOO)
      .setParam(PARAM_ORGANIZATION, organization.getKey())
      .execute();
  }
}
