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
package org.sonar.server.qualitygate.ws;

import org.junit.Rule;
import org.junit.Test;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.qualitygate.QualityGateDto;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.TokenType;
import org.sonar.db.user.UserDto;
import org.sonar.db.user.UserTokenDto;
import org.sonar.server.component.TestComponentFinder;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.tester.TokenUserSessionTesting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class QualityGatesWsSupportIT {

  @Rule
  public DbTester db = DbTester.create();

  @Test
  public void checkCanLimitedEdit_succeeds_for_delegated_user_authenticated_with_user_token() {
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();
    UserDto user = db.users().insertUser();
    db.qualityGates().addUserPermission(qualityGate, user);
    QualityGatesWsSupport underTest = newSupport(user, db.users().insertToken(user));

    underTest.checkCanLimitedEdit(db.getSession(), qualityGate);

    assertThat(underTest.hasLimitedPermission(db.getSession(), qualityGate)).isTrue();
  }

  @Test
  public void checkCanLimitedEdit_succeeds_for_delegated_group_member_authenticated_with_user_token() {
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();
    UserDto user = db.users().insertUser();
    GroupDto group = db.users().insertGroup();
    db.users().insertMember(group, user);
    db.qualityGates().addGroupPermission(qualityGate, group);
    QualityGatesWsSupport underTest = newSupport(user, db.users().insertToken(user));

    underTest.checkCanLimitedEdit(db.getSession(), qualityGate);

    assertThat(underTest.hasLimitedPermission(db.getSession(), qualityGate)).isTrue();
  }

  @Test
  public void checkCanLimitedEdit_fails_for_delegated_user_authenticated_with_project_analysis_token() {
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();
    UserDto user = db.users().insertUser();
    db.qualityGates().addUserPermission(qualityGate, user);
    QualityGatesWsSupport underTest = newSupport(user, db.users().insertProjectAnalysisToken(user));

    DbSession dbSession = db.getSession();

    assertThat(underTest.hasLimitedPermission(dbSession, qualityGate)).isFalse();
    assertThatThrownBy(() -> underTest.checkCanLimitedEdit(dbSession, qualityGate))
      .isInstanceOf(ForbiddenException.class);
  }

  @Test
  public void checkCanLimitedEdit_fails_for_delegated_user_authenticated_with_global_analysis_token() {
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();
    UserDto user = db.users().insertUser();
    db.qualityGates().addUserPermission(qualityGate, user);
    UserTokenDto token = db.users().insertToken(user, t -> t.setType(TokenType.GLOBAL_ANALYSIS_TOKEN.name()));
    QualityGatesWsSupport underTest = newSupport(user, token);

    DbSession dbSession = db.getSession();

    assertThat(underTest.hasLimitedPermission(dbSession, qualityGate)).isFalse();
    assertThatThrownBy(() -> underTest.checkCanLimitedEdit(dbSession, qualityGate))
      .isInstanceOf(ForbiddenException.class);
  }

  @Test
  public void checkCanLimitedEdit_fails_for_delegated_group_member_authenticated_with_project_analysis_token() {
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();
    UserDto user = db.users().insertUser();
    GroupDto group = db.users().insertGroup();
    db.users().insertMember(group, user);
    db.qualityGates().addGroupPermission(qualityGate, group);
    QualityGatesWsSupport underTest = newSupport(user, db.users().insertProjectAnalysisToken(user));

    DbSession dbSession = db.getSession();

    assertThat(underTest.hasLimitedPermission(dbSession, qualityGate)).isFalse();
    assertThatThrownBy(() -> underTest.checkCanLimitedEdit(dbSession, qualityGate))
      .isInstanceOf(ForbiddenException.class);
  }

  private QualityGatesWsSupport newSupport(UserDto user, UserTokenDto token) {
    return new QualityGatesWsSupport(db.getDbClient(), TokenUserSessionTesting.newTokenSession(db.getDbClient(), user, token), TestComponentFinder.from(db));
  }
}
