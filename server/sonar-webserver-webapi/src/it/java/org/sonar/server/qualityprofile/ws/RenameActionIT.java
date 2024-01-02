/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.db.qualityprofile.QualityProfileTesting;
import org.sonar.db.user.UserDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;

import static java.util.Optional.ofNullable;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.sonar.db.permission.GlobalPermission.ADMINISTER_QUALITY_PROFILES;
import static org.sonar.db.permission.GlobalPermission.SCAN;

public class RenameActionIT {

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  private DbClient dbClient = db.getDbClient();

  private WsActionTester ws;

  private String xoo1Key = "xoo1";
  private String xoo2Key = "xoo2";

  @Before
  public void setUp() {
    QProfileWsSupport wsSupport = new QProfileWsSupport(dbClient, userSession);
    RenameAction underTest = new RenameAction(dbClient, userSession, wsSupport);
    ws = new WsActionTester(underTest);

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
      .setLanguage("xoo")
      .setName("Old, valid name");
    db.qualityProfiles().insert(qualityProfile1);
    String qualityProfileKey1 = qualityProfile1.getKee();

    QProfileDto qualityProfile2 = QualityProfileTesting.newQualityProfileDto()
      .setLanguage("xoo")
      .setName("Invalid, duplicated name");
    db.qualityProfiles().insert(qualityProfile2);
    String qualityProfileKey2 = qualityProfile2.getKee();

    assertThatThrownBy(() -> {
      call(qualityProfileKey1, "Invalid, duplicated name");
    })
      .isInstanceOf(BadRequestException.class)
      .hasMessage("Quality profile already exists: Invalid, duplicated name");
  }

  @Test
  public void as_qprofile_editor() {
    QProfileDto qualityProfile = db.qualityProfiles().insert();
    UserDto user = db.users().insertUser();
    db.qualityProfiles().addUserPermission(qualityProfile, user);
    userSession.logIn(user);

    call(qualityProfile.getKee(), "the new name");

    QProfileDto reloaded = db.getDbClient().qualityProfileDao().selectByUuid(db.getSession(), qualityProfile.getKee());
    assertThat(reloaded.getName()).isEqualTo("the new name");
  }

  @Test
  public void fail_if_parameter_profile_is_missing() {
    logInAsQProfileAdministrator();

    assertThatThrownBy(() -> {
      call(null, "Other Sonar Way");
    })
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("The 'key' parameter is missing");
  }

  @Test
  public void fail_if_parameter_name_is_missing() {
    logInAsQProfileAdministrator();

    assertThatThrownBy(() -> {
      call("sonar-way-xoo1-13245", null);
    })
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("The 'name' parameter is missing");
  }

  @Test
  public void fail_if_not_profile_administrator() {
    userSession.logIn(db.users().insertUser())
      .addPermission(SCAN);

    QProfileDto qualityProfile = QualityProfileTesting.newQualityProfileDto();
    db.qualityProfiles().insert(qualityProfile);
    String qualityProfileKey = qualityProfile.getKee();

    assertThatThrownBy(() -> {
      call(qualityProfileKey, "Hey look I am not quality profile admin!");
    })
      .isInstanceOf(ForbiddenException.class)
      .hasMessage("Insufficient privileges");
  }

  @Test
  public void fail_if_not_logged_in() {
    assertThatThrownBy(() -> {
      call("sonar-way-xoo1-13245", "Not logged in");
    })
      .isInstanceOf(UnauthorizedException.class)
      .hasMessage("Authentication is required");
  }

  @Test
  public void fail_if_profile_does_not_exist() {
    logInAsQProfileAdministrator();

    assertThatThrownBy(() -> {
      call("polop", "Uh oh, I don't know this profile");
    })
      .isInstanceOf(NotFoundException.class)
      .hasMessage("Quality Profile with key 'polop' does not exist");
  }

  @Test
  public void fail_if_profile_is_built_in() {
    logInAsQProfileAdministrator();
    String qualityProfileKey = db.qualityProfiles().insert(p -> p.setIsBuiltIn(true)).getKee();

    assertThatThrownBy(() -> {
      call(qualityProfileKey, "the new name");
    })
      .isInstanceOf(BadRequestException.class);
  }

  @Test
  public void fail_if_blank_renaming() {
    String qualityProfileKey = createNewValidQualityProfileKey();

    assertThatThrownBy(() -> {
      call(qualityProfileKey, " ");
    })
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("The 'name' parameter is missing");
  }

  @Test
  public void fail_renaming_if_profile_not_found() {
    logInAsQProfileAdministrator();

    assertThatThrownBy(() -> {
      call("unknown", "the new name");
    })
      .isInstanceOf(NotFoundException.class)
      .hasMessage("Quality Profile with key 'unknown' does not exist");
  }

  @Test
  public void definition() {
    WebService.Action definition = ws.getDef();

    assertThat(definition.key()).isEqualTo("rename");
    assertThat(definition.isPost()).isTrue();
    assertThat(definition.params()).extracting(Param::key).containsExactlyInAnyOrder("key", "name");
    Param profile = definition.param("key");
    assertThat(profile.deprecatedKey()).isNullOrEmpty();
  }

  private String createNewValidQualityProfileKey() {
    QProfileDto qualityProfile = QualityProfileTesting.newQualityProfileDto();
    db.qualityProfiles().insert(qualityProfile);
    return qualityProfile.getKee();
  }

  private void createProfiles() {
    db.qualityProfiles().insert(p -> p.setKee("sonar-way-xoo1-12345").setLanguage(xoo1Key).setName("Sonar way"));

    QProfileDto parentXoo2 = db.qualityProfiles().insert(p -> p.setKee("sonar-way-xoo2-23456").setLanguage(xoo2Key).setName("Sonar way"));

    db.qualityProfiles().insert(p -> p.setKee("my-sonar-way-xoo2-34567").setLanguage(xoo2Key).setName("My Sonar way").setParentKee(parentXoo2.getKee()));
  }

  private void logInAsQProfileAdministrator() {
    userSession.logIn(db.users().insertUser()).addPermission(ADMINISTER_QUALITY_PROFILES);
  }

  private void call(@Nullable String key, @Nullable String name) {
    TestRequest request = ws.newRequest()
      .setMethod("POST");

    ofNullable(key).ifPresent(k -> request.setParam("key", k));
    ofNullable(name).ifPresent(n -> request.setParam("name", n));

    request.execute();
  }
}
