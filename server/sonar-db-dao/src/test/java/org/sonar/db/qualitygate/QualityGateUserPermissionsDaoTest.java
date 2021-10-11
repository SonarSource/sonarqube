/*
 * SonarQube
 * Copyright (C) 2009-2021 SonarSource SA
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
package org.sonar.db.qualitygate;

import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.user.UserDbTester;
import org.sonar.db.user.UserDto;

import static org.assertj.core.api.Assertions.assertThat;

public class QualityGateUserPermissionsDaoTest {
  @Rule
  public final DbTester db = DbTester.create(System2.INSTANCE);

  private final DbSession dbSession = db.getSession();
  private final UserDbTester userDbTester = new UserDbTester(db);
  private final QualityGateDbTester qualityGateDbTester = new QualityGateDbTester(db);
  private final QualityGateUserPermissionsDao underTest = db.getDbClient().qualityGateUserPermissionDao();

  @Test
  public void insert() {
    UserDto user = userDbTester.insertUser();
    QualityGateDto qualityGate = qualityGateDbTester.insertQualityGate();
    QualityGateUserPermissionsDto qualityGateUserPermissions = new QualityGateUserPermissionsDto("uuid", user.getUuid(), qualityGate.getUuid());
    underTest.insert(dbSession, qualityGateUserPermissions);
    dbSession.commit();

    QualityGateUserPermissionsDto fromDB = underTest.selectByQualityGateAndUser(dbSession, qualityGate.getUuid(), user.getUuid());
    assertThat(fromDB.getQualityGateUuid()).isEqualTo(qualityGate.getUuid());
    assertThat(fromDB.getUserUuid()).isEqualTo(user.getUuid());
    assertThat(fromDB.getUuid()).isEqualTo("uuid");
  }

  @Test
  public void exist() {
    UserDto user = userDbTester.insertUser();
    QualityGateDto qualityGate = qualityGateDbTester.insertQualityGate();
    QualityGateUserPermissionsDto qualityGateUserPermissions = new QualityGateUserPermissionsDto("uuid", user.getUuid(), qualityGate.getUuid());
    underTest.insert(dbSession, qualityGateUserPermissions);
    dbSession.commit();

    assertThat(underTest.exists(dbSession, qualityGate.getUuid(), user.getUuid())).isTrue();
    assertThat(underTest.exists(dbSession, qualityGate, user)).isTrue();
  }

}