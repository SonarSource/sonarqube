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
package org.sonar.db.alm.pat;

import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.impl.utils.TestSystem2;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.alm.setting.AlmSettingDao;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.audit.NoOpAuditPersister;
import org.sonar.db.user.UserDto;

import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.db.alm.integration.pat.AlmPatsTesting.newAlmPatDto;
import static org.sonar.db.almsettings.AlmSettingsTesting.newGithubAlmSettingDto;

public class AlmPatDaoIT {

  private static final long NOW = 1000000L;
  private static final String A_UUID = "SOME_UUID";
  private TestSystem2 system2 = new TestSystem2().setNow(NOW);
  @Rule
  public DbTester db = DbTester.create(system2);

  private DbSession dbSession = db.getSession();
  private UuidFactory uuidFactory = mock(UuidFactory.class);
  private AlmSettingDao almSettingDao = new AlmSettingDao(system2, uuidFactory, new NoOpAuditPersister());

  private AlmPatDao underTest = new AlmPatDao(system2, uuidFactory, new NoOpAuditPersister());

  @Test
  public void selectByUuid() {
    when(uuidFactory.create()).thenReturn(A_UUID);

    AlmPatDto almPatDto = newAlmPatDto();
    underTest.insert(dbSession, almPatDto, null, null);

    assertThat(underTest.selectByUuid(dbSession, A_UUID).get())
      .extracting(AlmPatDto::getUuid, AlmPatDto::getPersonalAccessToken,
        AlmPatDto::getUserUuid, AlmPatDto::getAlmSettingUuid,
        AlmPatDto::getUpdatedAt, AlmPatDto::getCreatedAt)
      .containsExactly(A_UUID, almPatDto.getPersonalAccessToken(),
        almPatDto.getUserUuid(), almPatDto.getAlmSettingUuid(),
        NOW, NOW);

    assertThat(underTest.selectByUuid(dbSession, "foo")).isNotPresent();
  }

  @Test
  public void selectByAlmSetting() {
    when(uuidFactory.create()).thenReturn(A_UUID);

    AlmSettingDto almSetting = newGithubAlmSettingDto();
    almSettingDao.insert(dbSession, almSetting);
    AlmPatDto almPatDto = newAlmPatDto();
    almPatDto.setAlmSettingUuid(almSetting.getUuid());

    String userUuid = randomAlphanumeric(40);
    almPatDto.setUserUuid(userUuid);
    underTest.insert(dbSession, almPatDto, null, null);

    assertThat(underTest.selectByUserAndAlmSetting(dbSession, userUuid, almSetting).get())
      .extracting(AlmPatDto::getUuid, AlmPatDto::getPersonalAccessToken,
        AlmPatDto::getUserUuid, AlmPatDto::getAlmSettingUuid,
        AlmPatDto::getCreatedAt, AlmPatDto::getUpdatedAt)
      .containsExactly(A_UUID, almPatDto.getPersonalAccessToken(),
        userUuid, almSetting.getUuid(), NOW, NOW);

    assertThat(underTest.selectByUserAndAlmSetting(dbSession, randomAlphanumeric(40), newGithubAlmSettingDto())).isNotPresent();
  }

  @Test
  public void update() {
    when(uuidFactory.create()).thenReturn(A_UUID);
    AlmPatDto almPatDto = newAlmPatDto();
    underTest.insert(dbSession, almPatDto, null, null);

    String updated_pat = "updated pat";
    almPatDto.setPersonalAccessToken(updated_pat);

    system2.setNow(NOW + 1);
    underTest.update(dbSession, almPatDto, null, null);

    AlmPatDto result = underTest.selectByUuid(dbSession, A_UUID).get();
    assertThat(result)
      .extracting(AlmPatDto::getUuid, AlmPatDto::getPersonalAccessToken,
        AlmPatDto::getUserUuid, AlmPatDto::getAlmSettingUuid,
        AlmPatDto::getCreatedAt, AlmPatDto::getUpdatedAt)
      .containsExactly(A_UUID, updated_pat, almPatDto.getUserUuid(),
        almPatDto.getAlmSettingUuid(),
        NOW, NOW + 1);
  }

  @Test
  public void delete() {
    when(uuidFactory.create()).thenReturn(A_UUID);
    AlmPatDto almPat = newAlmPatDto();
    underTest.insert(dbSession, almPat, null, null);

    underTest.delete(dbSession, almPat, null, null);

    assertThat(underTest.selectByUuid(dbSession, almPat.getUuid())).isNotPresent();
  }

  @Test
  public void deleteByUser() {
    when(uuidFactory.create()).thenReturn(A_UUID);
    UserDto userDto = db.users().insertUser();
    AlmPatDto almPat = newAlmPatDto();
    almPat.setUserUuid(userDto.getUuid());
    underTest.insert(dbSession, almPat, userDto.getLogin(), null);

    underTest.deleteByUser(dbSession, userDto);

    assertThat(underTest.selectByUuid(dbSession, almPat.getUuid())).isNotPresent();
  }

  @Test
  public void deleteByAlmSetting() {
    when(uuidFactory.create()).thenReturn(A_UUID);
    AlmSettingDto almSettingDto = db.almSettings().insertBitbucketAlmSetting();
    AlmPatDto almPat = newAlmPatDto();
    almPat.setAlmSettingUuid(almSettingDto.getUuid());
    underTest.insert(dbSession, almPat, null, null);

    underTest.deleteByAlmSetting(dbSession, almSettingDto);

    assertThat(underTest.selectByUuid(dbSession, almPat.getUuid())).isNotPresent();
  }

}
