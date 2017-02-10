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
package org.sonar.server.qualityprofile.ws;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.qualityprofile.QualityProfileDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.organization.TestDefaultOrganizationProvider;
import org.sonar.server.qualityprofile.QProfileFactory;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class RenameActionTest {

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private DbClient dbClient = db.getDbClient();
  private String xoo1Key = "xoo1";
  private String xoo2Key = "xoo2";
  private WsTester tester;
  private DefaultOrganizationProvider defaultOrganizationProvider = TestDefaultOrganizationProvider.fromUuid("ORG1");
  private QProfileWsSupport wsSupport = new QProfileWsSupport(userSessionRule, defaultOrganizationProvider);

  @Before
  public void setUp() {
    createProfiles();

    tester = new WsTester(new QProfilesWs(
      mock(RuleActivationActions.class),
      mock(BulkRuleActivationActions.class),
      new RenameAction(new QProfileFactory(dbClient), wsSupport)));
  }

  @Test
  public void rename_nominal() throws Exception {
    logInAsQProfileAdministrator();

    tester.newPostRequest("api/qualityprofiles", "rename")
      .setParam("key", "sonar-way-xoo2-23456")
      .setParam("name", "Other Sonar Way")
      .execute().assertNoContent();

    assertThat(dbClient.qualityProfileDao().selectOrFailByKey(db.getSession(), "sonar-way-xoo2-23456").getName()).isEqualTo("Other Sonar Way");
  }

  @Test
  public void fail_if_profile_with_same_name_exists() throws Exception {
    logInAsQProfileAdministrator();

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Quality profile already exists: My Sonar way");

    tester.newPostRequest("api/qualityprofiles", "rename")
      .setParam("key", "sonar-way-xoo2-23456")
      .setParam("name", "My Sonar way")
      .execute();
  }

  @Test
  public void fail_if_parameter_key_is_missing() throws Exception {
    logInAsQProfileAdministrator();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("The 'key' parameter is missing");

    tester.newPostRequest("api/qualityprofiles", "rename")
      .setParam("name", "Other Sonar Way")
      .execute();
  }

  @Test
  public void fail_if_parameter_name_is_missing() throws Exception {
    logInAsQProfileAdministrator();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("The 'name' parameter is missing");

    tester.newPostRequest("api/qualityprofiles", "rename")
      .setParam("key", "sonar-way-xoo1-13245")
      .execute();
  }

  @Test
  public void throw_ForbiddenException_if_not_profile_administrator() throws Exception {
    userSessionRule.logIn();

    expectedException.expect(ForbiddenException.class);
    expectedException.expectMessage("Insufficient privileges");

    tester.newPostRequest("api/qualityprofiles", "rename")
      .setParam("key", "sonar-way-xoo1-13245")
      .setParam("name", "Hey look I am not quality profile admin!")
      .execute();
  }

  @Test
  public void throw_UnauthorizedException_if_not_logged_in() throws Exception {
    expectedException.expect(UnauthorizedException.class);
    expectedException.expectMessage("Authentication is required");

    tester.newPostRequest("api/qualityprofiles", "rename")
      .setParam("key", "sonar-way-xoo1-13245")
      .setParam("name", "Not logged in")
      .execute();
  }

  @Test
  public void fail_if_profile_does_not_exist() throws Exception {
    logInAsQProfileAdministrator();

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("Quality profile not found: polop");

    tester.newPostRequest("api/qualityprofiles", "rename")
      .setParam("key", "polop")
      .setParam("name", "Uh oh, I don't know this profile")
      .execute();
  }

  private void createProfiles() {
    dbClient.qualityProfileDao().insert(db.getSession(),
      QualityProfileDto.createFor("sonar-way-xoo1-12345").setLanguage(xoo1Key).setName("Sonar way").setDefault(true),
      QualityProfileDto.createFor("sonar-way-xoo2-23456").setLanguage(xoo2Key).setName("Sonar way"),
      QualityProfileDto.createFor("my-sonar-way-xoo2-34567").setLanguage(xoo2Key).setName("My Sonar way").setParentKee("sonar-way-xoo2-23456").setDefault(true));
    db.commit();
  }

  private void logInAsQProfileAdministrator() {
    userSessionRule
      .logIn()
      .addOrganizationPermission(defaultOrganizationProvider.get().getUuid(), GlobalPermissions.QUALITY_PROFILE_ADMIN);
  }
}
