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

import java.util.Date;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.GroupTesting;

import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;

public class QualityGateGroupPermissionsDaoTest {

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  private DbSession dbSession = dbTester.getSession();
  private QualityGateGroupPermissionsDao underTest = dbTester.getDbClient().qualityGateGroupPermissionsDao();

  @Test
  public void itInsertsAndExistsReturnsTrue() {
    GroupDto group = GroupTesting.newGroupDto();
    QualityGateDto qualityGateDto = insertQualityGate();
    QualityGateGroupPermissionsDto qualityGateGroupPermission = insertQualityGateGroupPermission(qualityGateDto.getUuid(), group.getUuid());

    assertThat(qualityGateGroupPermission.getUuid()).isNotNull();
    assertThat(underTest.exists(dbSession, qualityGateDto, group)).isTrue();
    assertThat(underTest.exists(dbSession, qualityGateDto.getUuid(), group.getUuid())).isTrue();
  }

  @Test
  public void existsReturnsTrueForListOfGroups() {
    GroupDto group1 = GroupTesting.newGroupDto();
    GroupDto group2 = GroupTesting.newGroupDto();
    QualityGateDto qualityGateDto = insertQualityGate();
    QualityGateGroupPermissionsDto qualityGateGroupPermission = insertQualityGateGroupPermission(qualityGateDto.getUuid(), group1.getUuid());

    assertThat(qualityGateGroupPermission.getUuid()).isNotNull();
    assertThat(underTest.exists(dbSession, qualityGateDto, List.of(group1, group2))).isTrue();
    assertThat(underTest.exists(dbSession, qualityGateDto.getUuid(), group1.getUuid())).isTrue();
    assertThat(underTest.exists(dbSession, qualityGateDto.getUuid(), group2.getUuid())).isFalse();
  }

  @Test
  public void existsReturnsFalseWhenQGEditGroupsDoesNotExist() {
    assertThat(underTest.exists(dbSession, randomAlphabetic(5), randomAlphabetic(5))).isFalse();
  }

  private QualityGateDto insertQualityGate() {
    QualityGateDto qg = new QualityGateDto()
      .setUuid(randomAlphabetic(5))
      .setName(randomAlphabetic(5));
    dbTester.getDbClient().qualityGateDao().insert(dbTester.getSession(), qg);
    dbTester.commit();
    return qg;
  }

  private QualityGateGroupPermissionsDto insertQualityGateGroupPermission(String qualityGateUuid, String groupUuid) {
    QualityGateGroupPermissionsDto qgg = new QualityGateGroupPermissionsDto()
      .setUuid(Uuids.create())
      .setQualityGateUuid(qualityGateUuid)
      .setGroupUuid(groupUuid)
      .setCreatedAt(new Date());
    underTest.insert(dbTester.getSession(), qgg);
    dbTester.commit();
    return qgg;
  }
}
