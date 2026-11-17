/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.db.qualityprofile.QualityProfileTesting;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.TokenType;
import org.sonar.db.user.UserDto;
import org.sonar.db.user.UserTokenDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.tester.TokenUserSessionTesting;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class QProfileWsSupportIT {

  @Rule
  public DbTester db = DbTester.create();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  private final QProfileWsSupport underTest = new QProfileWsSupport(db.getDbClient(), userSession);

  @Test
  public void getProfile_returns_the_profile_specified_by_key() {
    QProfileDto profile = db.qualityProfiles().insert();

    QProfileDto loaded = underTest.getProfile(db.getSession(), QProfileReference.fromKey(profile.getKee()));

    assertThat(loaded.getKee()).isEqualTo(profile.getKee());
    assertThat(loaded.getLanguage()).isEqualTo(profile.getLanguage());
    assertThat(loaded.getName()).isEqualTo(profile.getName());
  }

  @Test
  public void getProfile_throws_NotFoundException_if_specified_key_does_not_exist() {
    assertThatThrownBy(() -> underTest.getProfile(db.getSession(), QProfileReference.fromKey("missing")))
      .isInstanceOf(NotFoundException.class)
      .hasMessage("Quality Profile with key 'missing' does not exist");
  }

  @Test
  public void getProfile_returns_the_profile_specified_by_name() {
    QProfileDto profile = QualityProfileTesting.newQualityProfileDto();
    db.qualityProfiles().insert(profile);

    QProfileDto loaded = underTest.getProfile(db.getSession(), QProfileReference.fromName(profile.getLanguage(), profile.getName()));

    assertThat(loaded.getKee()).isEqualTo(profile.getKee());
    assertThat(loaded.getLanguage()).isEqualTo(profile.getLanguage());
    assertThat(loaded.getName()).isEqualTo(profile.getName());
  }

  @Test
  public void getProfile_throws_NotFoundException_if_specified_name_does_not_exist() {
    QProfileDto profile = QualityProfileTesting.newQualityProfileDto();
    db.qualityProfiles().insert(profile);

    assertThatThrownBy(() -> underTest.getProfile(db.getSession(), QProfileReference.fromName("java", "missing")))
      .isInstanceOf(NotFoundException.class)
      .hasMessage("Quality Profile for language 'java' and name 'missing' does not exist");
  }

  @Test
  public void getProfile_whenCalledWithNameAndLanguage_andNameIsTheDisplayNameOfABuiltInProfile_resolvesItByInternalName() {
    QProfileDto sonarWay = db.qualityProfiles().insert(p -> p.setName("Sonar way").setLanguage("xoo").setIsBuiltIn(true));

    QProfileDto loaded = underTest.getProfile(db.getSession(), "Sonar way comprehensive", "xoo");

    assertThat(loaded.getKee()).isEqualTo(sonarWay.getKee());
  }

  @Test
  public void getRule_throws_BadRequest_if_rule_is_external() {
    RuleDto rule = db.rules().insert(r -> r.setIsExternal(true));

    assertThatThrownBy(() -> underTest.getRule(db.getSession(), rule.getKey()))
      .isInstanceOf(BadRequestException.class)
      .hasMessage(format("Operation forbidden for rule '%s' imported from an external rule engine.", rule.getKey()));
  }

  @Test
  public void canEdit_returns_true_for_delegated_user_authenticated_with_user_token() {
    QProfileDto profile = db.qualityProfiles().insert();
    UserDto user = db.users().insertUser();
    db.qualityProfiles().addUserPermission(profile, user);
    QProfileWsSupport support = newSupport(user, db.users().insertToken(user));

    assertThat(support.canEdit(db.getSession(), profile)).isTrue();
  }

  @Test
  public void canEdit_returns_true_for_delegated_group_member_authenticated_with_user_token() {
    QProfileDto profile = db.qualityProfiles().insert();
    UserDto user = db.users().insertUser();
    GroupDto group = db.users().insertGroup();
    db.users().insertMember(group, user);
    db.qualityProfiles().addGroupPermission(profile, group);
    QProfileWsSupport support = newSupport(user, db.users().insertToken(user));

    assertThat(support.canEdit(db.getSession(), profile)).isTrue();
  }

  @Test
  public void canEdit_returns_false_for_delegated_user_authenticated_with_project_analysis_token() {
    QProfileDto profile = db.qualityProfiles().insert();
    UserDto user = db.users().insertUser();
    db.qualityProfiles().addUserPermission(profile, user);
    QProfileWsSupport support = newSupport(user, db.users().insertProjectAnalysisToken(user));

    DbSession dbSession = db.getSession();

    assertThat(support.canEdit(dbSession, profile)).isFalse();
    assertThatThrownBy(() -> support.checkCanEdit(dbSession, profile))
      .isInstanceOf(ForbiddenException.class);
  }

  @Test
  public void canEdit_returns_false_for_delegated_user_authenticated_with_global_analysis_token() {
    QProfileDto profile = db.qualityProfiles().insert();
    UserDto user = db.users().insertUser();
    db.qualityProfiles().addUserPermission(profile, user);
    UserTokenDto token = db.users().insertToken(user, t -> t.setType(TokenType.GLOBAL_ANALYSIS_TOKEN.name()));
    QProfileWsSupport support = newSupport(user, token);

    assertThat(support.canEdit(db.getSession(), profile)).isFalse();
  }

  @Test
  public void canEdit_returns_false_for_delegated_group_member_authenticated_with_project_analysis_token() {
    QProfileDto profile = db.qualityProfiles().insert();
    UserDto user = db.users().insertUser();
    GroupDto group = db.users().insertGroup();
    db.users().insertMember(group, user);
    db.qualityProfiles().addGroupPermission(profile, group);
    QProfileWsSupport support = newSupport(user, db.users().insertProjectAnalysisToken(user));

    assertThat(support.canEdit(db.getSession(), profile)).isFalse();
  }

  private QProfileWsSupport newSupport(UserDto user, UserTokenDto token) {
    return new QProfileWsSupport(db.getDbClient(), TokenUserSessionTesting.newTokenSession(db.getDbClient(), user, token));
  }
}
