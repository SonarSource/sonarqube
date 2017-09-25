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
import org.sonar.db.user.GroupDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

public class QProfileEditGroupsDaoTest {

  private static final long NOW = 10_000_000_000L;

  private System2 system2 = new TestSystem2().setNow(NOW);

  @Rule
  public DbTester db = DbTester.create(system2);

  private QProfileEditGroupsDao underTest = db.getDbClient().qProfileEditGroupsDao();

  @Test
  public void exists() {
    OrganizationDto organization = db.organizations().insert();
    QProfileDto profile = db.qualityProfiles().insert(organization);
    QProfileDto anotherProfile = db.qualityProfiles().insert(organization);
    GroupDto group = db.users().insertGroup(organization);
    GroupDto anotherGroup = db.users().insertGroup(organization);
    db.qualityProfiles().addGroupPermission(profile, group);

    assertThat(underTest.exists(db.getSession(), profile, group)).isTrue();
    assertThat(underTest.exists(db.getSession(), profile, anotherGroup)).isFalse();
    assertThat(underTest.exists(db.getSession(), anotherProfile, group)).isFalse();
    assertThat(underTest.exists(db.getSession(), anotherProfile, anotherGroup)).isFalse();
  }

  @Test
  public void insert() {
    underTest.insert(db.getSession(), new QProfileEditGroupsDto()
      .setUuid("ABCD")
      .setGroupId(100)
      .setQProfileUuid("QPROFILE")
    );

    assertThat(db.selectFirst(db.getSession(), "select uuid as \"uuid\", group_id as \"groupId\", qprofile_uuid as \"qProfileUuid\", created_at as \"createdAt\" from qprofile_edit_groups")).contains(
      entry("uuid", "ABCD"),
      entry("groupId", 100L),
      entry("qProfileUuid", "QPROFILE"),
      entry("createdAt", NOW));
  }

}
