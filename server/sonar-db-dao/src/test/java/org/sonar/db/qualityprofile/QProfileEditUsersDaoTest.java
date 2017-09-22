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
package org.sonar.db.qualityprofile;

import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.internal.TestSystem2;
import org.sonar.db.DbTester;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.user.UserDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

public class QProfileEditUsersDaoTest {

  private static final long NOW = 10_000_000_000L;

  private System2 system2 = new TestSystem2().setNow(NOW);

  @Rule
  public DbTester db = DbTester.create(system2);

  private QProfileEditUsersDao underTest = db.getDbClient().qProfileEditUsersDao();

  @Test
  public void exists() {
    OrganizationDto organization = db.organizations().insert();
    QProfileDto profile = db.qualityProfiles().insert(organization);
    QProfileDto anotherProfile = db.qualityProfiles().insert(organization);
    UserDto user = db.users().insertUser();
    UserDto anotherUser = db.users().insertUser();
    db.qualityProfiles().addUserPermission(profile, user);

    assertThat(underTest.exists(db.getSession(), profile, user)).isTrue();
    assertThat(underTest.exists(db.getSession(), profile, anotherUser)).isFalse();
    assertThat(underTest.exists(db.getSession(), anotherProfile, user)).isFalse();
    assertThat(underTest.exists(db.getSession(), anotherProfile, anotherUser)).isFalse();
  }

  @Test
  public void insert() {
    underTest.insert(db.getSession(), new QProfileEditUsersDto()
      .setUuid("ABCD")
      .setUserId(100)
      .setQProfileUuid("QPROFILE")
    );

    assertThat(db.selectFirst(db.getSession(), "select uuid as \"uuid\", user_id as \"userId\", qprofile_uuid as \"qProfileUuid\", created_at as \"createdAt\" from qprofile_edit_users")).contains(
      entry("uuid", "ABCD"),
      entry("userId", 100L),
      entry("qProfileUuid", "QPROFILE"),
      entry("createdAt", NOW));
  }

  @Test
  public void deleteByQProfileAndUser() {
    OrganizationDto organization = db.organizations().insert();
    QProfileDto profile = db.qualityProfiles().insert(organization);
    UserDto user = db.users().insertUser();
    db.qualityProfiles().addUserPermission(profile, user);
    assertThat(underTest.exists(db.getSession(), profile, user)).isTrue();

    underTest.deleteByQProfileAndUser(db.getSession(), profile, user);

    assertThat(underTest.exists(db.getSession(), profile, user)).isFalse();
  }

}
