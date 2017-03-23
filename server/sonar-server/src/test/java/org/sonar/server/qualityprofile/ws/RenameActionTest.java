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
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.qualityprofile.QualityProfileDto;
import org.sonar.db.qualityprofile.QualityProfileTesting;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.organization.TestDefaultOrganizationProvider;
import org.sonar.server.qualityprofile.index.ActiveRuleIndexer;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER_QUALITY_PROFILES;

public class RenameActionTest {

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private String xoo1Key = "xoo1";
  private String xoo2Key = "xoo2";
  private DbClient dbClient;
  private WsTester tester;
  private OrganizationDto organization;
  private RenameAction underTest;
  private ActiveRuleIndexer activeRuleIndexer = mock(ActiveRuleIndexer.class);

  @Before
  public void setUp() {
    dbClient = db.getDbClient();
    TestDefaultOrganizationProvider defaultOrganizationProvider = TestDefaultOrganizationProvider.from(db);
    QProfileWsSupport wsSupport = new QProfileWsSupport(dbClient, userSessionRule, defaultOrganizationProvider);
    underTest = new RenameAction(
      dbClient,
      userSessionRule);
    tester = new WsTester(new QProfilesWs(
      underTest));
    organization = db.organizations().insert();

    createProfiles();
  }

  @Test
  public void rename() {
    logInAsQProfileAdministrator();
    String qualityProfileKey = createNewValidQualityProfileKey();

    underTest.doHandle("the new name", qualityProfileKey);

    QualityProfileDto reloaded = db.getDbClient().qualityProfileDao().selectByKey(db.getSession(), qualityProfileKey);
    assertThat(reloaded.getName()).isEqualTo("the new name");
  }

  @Test
  public void fail_renaming_if_name_already_exists() {
    logInAsQProfileAdministrator();

    QualityProfileDto qualityProfile1 = QualityProfileTesting.newQualityProfileDto()
      .setOrganizationUuid(organization.getUuid())
      .setLanguage("xoo")
      .setName("Old, valid name");
    db.qualityProfiles().insertQualityProfile(qualityProfile1);
    String qualityProfileKey1 = qualityProfile1.getKey();

    QualityProfileDto qualityProfile2 = QualityProfileTesting.newQualityProfileDto()
      .setOrganizationUuid(organization.getUuid())
      .setLanguage("xoo")
      .setName("Invalid, duplicated name");
    db.qualityProfiles().insertQualityProfile(qualityProfile2);
    String qualityProfileKey2 = qualityProfile2.getKey();

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Quality profile already exists: Invalid, duplicated name");

    underTest.doHandle("Invalid, duplicated name", qualityProfileKey1);
  }

  @Test
  public void allow_same_name_in_different_organizations() {
    OrganizationDto organizationX = db.organizations().insert();
    OrganizationDto organizationY = db.organizations().insert();
    userSessionRule.logIn()
      .addPermission(ADMINISTER_QUALITY_PROFILES, organizationX);

    QualityProfileDto qualityProfile1 = QualityProfileTesting.newQualityProfileDto()
      .setOrganizationUuid(organizationX.getUuid())
      .setLanguage("xoo")
      .setName("Old, unique name");
    db.qualityProfiles().insertQualityProfile(qualityProfile1);
    String qualityProfileKey1 = qualityProfile1.getKey();

    QualityProfileDto qualityProfile2 = QualityProfileTesting.newQualityProfileDto()
      .setOrganizationUuid(organizationY.getUuid())
      .setLanguage("xoo")
      .setName("Duplicated name");
    db.qualityProfiles().insertQualityProfile(qualityProfile2);
    String qualityProfileKey2 = qualityProfile2.getKey();

    underTest.doHandle("Duplicated name", qualityProfileKey1);

    QualityProfileDto reloaded = db.getDbClient().qualityProfileDao().selectByKey(db.getSession(), qualityProfileKey1);
    assertThat(reloaded.getName()).isEqualTo("Duplicated name");
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
    OrganizationDto organizationX = db.organizations().insert();
    OrganizationDto organizationY = db.organizations().insert();
    userSessionRule.logIn()
      .addPermission(ADMINISTER_QUALITY_PROFILES, organizationX);

    QualityProfileDto qualityProfile = QualityProfileTesting.newQualityProfileDto()
      .setOrganizationUuid(organizationY.getUuid());
    db.qualityProfiles().insertQualityProfile(qualityProfile);
    String qualityProfileKey = qualityProfile.getKey();

    expectedException.expect(ForbiddenException.class);
    expectedException.expectMessage("Insufficient privileges");

    underTest.doHandle("Hey look I am not quality profile admin!", qualityProfileKey);
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

  @Test
  public void allow_100_characters_as_name_and_not_more() throws Exception {
    logInAsQProfileAdministrator();
    String qualityProfileKey = createNewValidQualityProfileKey();

    String a100charName = "1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890";
    underTest.doHandle(a100charName, qualityProfileKey);

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Name is too long (>100 characters)");

    String a101charName = "12345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901";
    underTest.doHandle(a101charName, qualityProfileKey);
  }

  @Test
  public void fail_if_blank_renaming() {
    String qualityProfileKey = createNewValidQualityProfileKey();

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Name must be set");

    underTest.doHandle(" ", qualityProfileKey);
  }

  @Test
  public void fail_renaming_if_profile_not_found() {
    logInAsQProfileAdministrator();

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("Quality profile not found: unknown");

    underTest.doHandle("the new name", "unknown");
  }

  private String createNewValidQualityProfileKey() {
    QualityProfileDto qualityProfile = QualityProfileTesting.newQualityProfileDto()
      .setOrganizationUuid(organization.getUuid());
    db.qualityProfiles().insertQualityProfile(qualityProfile);
    return qualityProfile.getKey();
  }

  private void createProfiles() {
    String orgUuid = organization.getUuid();
    dbClient.qualityProfileDao().insert(db.getSession(),
      QualityProfileDto.createFor("sonar-way-xoo1-12345")
        .setOrganizationUuid(orgUuid)
        .setLanguage(xoo1Key)
        .setName("Sonar way")
        .setDefault(true),

      QualityProfileDto.createFor("sonar-way-xoo2-23456")
        .setOrganizationUuid(orgUuid)
        .setLanguage(xoo2Key)
        .setName("Sonar way"),

      QualityProfileDto.createFor("my-sonar-way-xoo2-34567")
        .setOrganizationUuid(orgUuid)
        .setLanguage(xoo2Key)
        .setName("My Sonar way")
        .setParentKee("sonar-way-xoo2-23456")
        .setDefault(true));
    db.commit();
  }

  private void logInAsQProfileAdministrator() {
    userSessionRule
      .logIn()
      .addPermission(ADMINISTER_QUALITY_PROFILES, organization);
  }
}
