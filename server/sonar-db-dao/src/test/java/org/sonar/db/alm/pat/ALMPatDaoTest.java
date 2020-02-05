/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.alm.setting.AlmSettingDao;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.user.UserDto;

import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.db.alm.integration.pat.AlmPatsTesting.newAlmPatDto;
import static org.sonar.db.almsettings.AlmSettingsTesting.newGithubAlmSettingDto;

public class ALMPatDaoTest {

  private static final long NOW = 1000000L;
  private static final String A_UUID = "SOME_UUID";
  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  private System2 system2 = mock(System2.class);
  @Rule
  public DbTester db = DbTester.create(system2);

  private DbSession dbSession = db.getSession();
  private UuidFactory uuidFactory = mock(UuidFactory.class);
  private AlmPatDao underTest = new AlmPatDao(system2, uuidFactory);
  private AlmSettingDao almSettingDao = new AlmSettingDao(system2, uuidFactory);

  @Test
  public void selectByUuid() {
    when(uuidFactory.create()).thenReturn(A_UUID);
    when(system2.now()).thenReturn(NOW);

    AlmPatDto almPatDto = newAlmPatDto();
    underTest.insert(dbSession, almPatDto);

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
    when(system2.now()).thenReturn(NOW);

    AlmSettingDto almSetting = newGithubAlmSettingDto();
    almSettingDao.insert(dbSession, almSetting);
    AlmPatDto almPatDto = newAlmPatDto();
    almPatDto.setAlmSettingUuid(almSetting.getUuid());

    String userUuid = randomAlphanumeric(40);
    almPatDto.setUserUuid(userUuid);
    underTest.insert(dbSession, almPatDto);

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
    when(system2.now()).thenReturn(NOW);
    AlmPatDto almPatDto = newAlmPatDto();
    underTest.insert(dbSession, almPatDto);

    String updated_pat = "updated pat";
    almPatDto.setPersonalAccessToken(updated_pat);

    when(system2.now()).thenReturn(NOW + 1);
    underTest.update(dbSession, almPatDto);

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
    when(system2.now()).thenReturn(NOW);
    AlmPatDto almPat = newAlmPatDto();
    underTest.insert(dbSession, almPat);

    underTest.delete(dbSession, almPat);

    assertThat(underTest.selectByUuid(dbSession, almPat.getUuid()).isPresent()).isFalse();
  }

  @Test
  public void deleteByUser() {
    when(uuidFactory.create()).thenReturn(A_UUID);
    when(system2.now()).thenReturn(NOW);
    UserDto userDto = db.users().insertUser();
    AlmPatDto almPat = newAlmPatDto();
    almPat.setUserUuid(userDto.getUuid());
    underTest.insert(dbSession, almPat);

    underTest.deleteByUser(dbSession, userDto);

    assertThat(underTest.selectByUuid(dbSession, almPat.getUuid()).isPresent()).isFalse();
  }

  @Test
  public void deleteByAlmSetting() {
    when(uuidFactory.create()).thenReturn(A_UUID);
    when(system2.now()).thenReturn(NOW);
    AlmSettingDto almSettingDto = db.almSettings().insertBitbucketAlmSetting();
    AlmPatDto almPat = newAlmPatDto();
    almPat.setAlmSettingUuid(almSettingDto.getUuid());
    underTest.insert(dbSession, almPat);

    underTest.deleteByAlmSetting(dbSession, almSettingDto);

    assertThat(underTest.selectByUuid(dbSession, almPat.getUuid()).isPresent()).isFalse();
  }

}
