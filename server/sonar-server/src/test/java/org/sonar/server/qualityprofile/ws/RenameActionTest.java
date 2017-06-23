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

import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.db.qualityprofile.QualityProfileTesting;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.organization.TestDefaultOrganizationProvider;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.core.util.Protobuf.setNullable;
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER_QUALITY_PROFILES;

public class RenameActionTest {

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private DbClient dbClient = db.getDbClient();

  private WsActionTester ws;

  private String xoo1Key = "xoo1";
  private String xoo2Key = "xoo2";
  private OrganizationDto organization;

  @Before
  public void setUp() {
    TestDefaultOrganizationProvider defaultOrganizationProvider = TestDefaultOrganizationProvider.from(db);
    QProfileWsSupport wsSupport = new QProfileWsSupport(dbClient, userSessionRule, defaultOrganizationProvider);
    RenameAction underTest = new RenameAction(dbClient, userSessionRule, wsSupport);
    ws = new WsActionTester(underTest);

    organization = db.organizations().insert();
    createProfiles();
  }

  @Test
  public void rename() {
    logInAsQProfileAdministrator();
    String qualityProfileKey = createNewValidQualityProfileKey();

    call(qualityProfileKey, "the new name");

    QProfileDto reloaded = db.getDbClient().qualityProfileDao().selectByUuid(db.getSession(), qualityProfileKey);
    assertThat(reloaded.getName()).isEqualTo("the new name");
  }

  @Test
  public void fail_renaming_if_name_already_exists() {
    logInAsQProfileAdministrator();

    QProfileDto qualityProfile1 = QualityProfileTesting.newQualityProfileDto()
      .setOrganizationUuid(organization.getUuid())
      .setLanguage("xoo")
      .setName("Old, valid name");
    db.qualityProfiles().insert(qualityProfile1);
    String qualityProfileKey1 = qualityProfile1.getKee();

    QProfileDto qualityProfile2 = QualityProfileTesting.newQualityProfileDto()
      .setOrganizationUuid(organization.getUuid())
      .setLanguage("xoo")
      .setName("Invalid, duplicated name");
    db.qualityProfiles().insert(qualityProfile2);
    String qualityProfileKey2 = qualityProfile2.getKee();

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Quality profile already exists: Invalid, duplicated name");

    call(qualityProfileKey1, "Invalid, duplicated name");
  }

  @Test
  public void allow_same_name_in_different_organizations() {
    OrganizationDto organizationX = db.organizations().insert();
    OrganizationDto organizationY = db.organizations().insert();
    userSessionRule.logIn()
      .addPermission(ADMINISTER_QUALITY_PROFILES, organizationX);

    QProfileDto qualityProfile1 = QualityProfileTesting.newQualityProfileDto()
      .setOrganizationUuid(organizationX.getUuid())
      .setLanguage("xoo")
      .setName("Old, unique name");
    db.qualityProfiles().insert(qualityProfile1);
    String qualityProfileKey1 = qualityProfile1.getKee();

    QProfileDto qualityProfile2 = QualityProfileTesting.newQualityProfileDto()
      .setOrganizationUuid(organizationY.getUuid())
      .setLanguage("xoo")
      .setName("Duplicated name");
    db.qualityProfiles().insert(qualityProfile2);
    String qualityProfileKey2 = qualityProfile2.getKee();

    call(qualityProfileKey1, "Duplicated name");

    QProfileDto reloaded = db.getDbClient().qualityProfileDao().selectByUuid(db.getSession(), qualityProfileKey1);
    assertThat(reloaded.getName()).isEqualTo("Duplicated name");
  }

  @Test
  public void fail_if_parameter_profile_is_missing() throws Exception {
    logInAsQProfileAdministrator();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("The 'profile' parameter is missing");

    call(null, "Other Sonar Way");
  }

  @Test
  public void fail_if_parameter_name_is_missing() throws Exception {
    logInAsQProfileAdministrator();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("The 'name' parameter is missing");

    call("sonar-way-xoo1-13245", null);
  }

  @Test
  public void throw_ForbiddenException_if_not_profile_administrator() throws Exception {
    OrganizationDto organizationX = db.organizations().insert();
    OrganizationDto organizationY = db.organizations().insert();
    userSessionRule.logIn()
      .addPermission(ADMINISTER_QUALITY_PROFILES, organizationX);

    QProfileDto qualityProfile = QualityProfileTesting.newQualityProfileDto()
      .setOrganizationUuid(organizationY.getUuid());
    db.qualityProfiles().insert(qualityProfile);
    String qualityProfileKey = qualityProfile.getKee();

    expectedException.expect(ForbiddenException.class);
    expectedException.expectMessage("Insufficient privileges");

    call(qualityProfileKey, "Hey look I am not quality profile admin!");
  }

  @Test
  public void throw_UnauthorizedException_if_not_logged_in() throws Exception {
    expectedException.expect(UnauthorizedException.class);
    expectedException.expectMessage("Authentication is required");

    call("sonar-way-xoo1-13245", "Not logged in");
  }

  @Test
  public void fail_if_profile_does_not_exist() throws Exception {
    logInAsQProfileAdministrator();

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("Quality Profile with key 'polop' does not exist");

    call("polop", "Uh oh, I don't know this profile");
  }

  @Test
  public void fail_if_profile_is_built_in() {
    logInAsQProfileAdministrator();
    String qualityProfileKey = db.qualityProfiles().insert(organization, p -> p.setIsBuiltIn(true)).getKee();

    expectedException.expect(BadRequestException.class);

    call(qualityProfileKey, "the new name");
  }

  @Test
  public void allow_100_characters_as_name_and_not_more() throws Exception {
    logInAsQProfileAdministrator();
    String qualityProfileKey = createNewValidQualityProfileKey();

    String a100charName = "1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890";
    call(qualityProfileKey, a100charName);

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Name is too long (>100 characters)");

    String a101charName = "12345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901";
    call(qualityProfileKey, a101charName);
  }

  @Test
  public void fail_if_blank_renaming() {
    String qualityProfileKey = createNewValidQualityProfileKey();

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Name must be set");

    call(qualityProfileKey, " ");
  }

  @Test
  public void fail_renaming_if_profile_not_found() {
    logInAsQProfileAdministrator();

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("Quality Profile with key 'unknown' does not exist");

    call("unknown", "the new name");
  }

  @Test
  public void definition() {
    WebService.Action definition = ws.getDef();

    assertThat(definition.key()).isEqualTo("rename");
    assertThat(definition.isPost()).isTrue();
    assertThat(definition.params()).extracting(Param::key).containsExactlyInAnyOrder("profile", "name");
    Param profile = definition.param("profile");
    assertThat(profile.deprecatedKey()).isEqualTo("key");
  }

  private String createNewValidQualityProfileKey() {
    QProfileDto qualityProfile = QualityProfileTesting.newQualityProfileDto()
      .setOrganizationUuid(organization.getUuid());
    db.qualityProfiles().insert(qualityProfile);
    return qualityProfile.getKee();
  }

  private void createProfiles() {
    db.qualityProfiles().insert(organization, p -> p.setKee("sonar-way-xoo1-12345").setLanguage(xoo1Key).setName("Sonar way"));

    QProfileDto parentXoo2 = db.qualityProfiles().insert(organization, p -> p.setKee("sonar-way-xoo2-23456").setLanguage(xoo2Key).setName("Sonar way"));

    db.qualityProfiles().insert(organization, p -> p.setKee("my-sonar-way-xoo2-34567").setLanguage(xoo2Key).setName("My Sonar way").setParentKee(parentXoo2.getKee()));
  }

  private void logInAsQProfileAdministrator() {
    userSessionRule
      .logIn()
      .addPermission(ADMINISTER_QUALITY_PROFILES, organization);
  }

  private void call(@Nullable String key, @Nullable String name) {
    TestRequest request = ws.newRequest()
      .setMethod("POST");

    setNullable(key, k -> request.setParam("profile", k));
    setNullable(name, n -> request.setParam("name", n));

    request.execute();
  }
}
