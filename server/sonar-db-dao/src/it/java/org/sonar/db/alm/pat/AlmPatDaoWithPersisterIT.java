/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.ArgumentCaptor;
import org.sonar.api.impl.utils.TestSystem2;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.audit.AuditPersister;
import org.sonar.db.audit.model.PersonalAccessTokenNewValue;
import org.sonar.db.user.UserDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.db.alm.integration.pat.AlmPatsTesting.newAlmPatDto;

class AlmPatDaoWithPersisterIT {

  private static final long NOW = 1000000L;
  private static final String A_UUID = "SOME_UUID";

  private final AuditPersister auditPersister = mock(AuditPersister.class);
  private final ArgumentCaptor<PersonalAccessTokenNewValue> newValueCaptor = ArgumentCaptor.forClass(PersonalAccessTokenNewValue.class);
  private final TestSystem2 system2 = new TestSystem2().setNow(NOW);
  @RegisterExtension
  private final DbTester db = DbTester.create(system2, auditPersister);

  private final DbSession dbSession = db.getSession();
  private final UuidFactory uuidFactory = mock(UuidFactory.class);

  private final AlmPatDao underTest = db.getDbClient().almPatDao();

  @Test
  void insertAndUpdateArePersisted() {
    when(uuidFactory.create()).thenReturn(A_UUID);

    AlmPatDto almPatDto = newAlmPatDto();
    underTest.insert(dbSession, almPatDto, "user-login", "alm-key");

    verify(auditPersister).addPersonalAccessToken(eq(dbSession), newValueCaptor.capture());
    PersonalAccessTokenNewValue newValue = newValueCaptor.getValue();
    assertThat(newValue)
      .extracting(PersonalAccessTokenNewValue::getPatUuid, PersonalAccessTokenNewValue::getUserUuid,
        PersonalAccessTokenNewValue::getAlmSettingUuid, PersonalAccessTokenNewValue::getUserLogin,
        PersonalAccessTokenNewValue::getAlmSettingKey)
      .containsExactly(almPatDto.getUuid(), almPatDto.getUserUuid(), almPatDto.getAlmSettingUuid(),
        "user-login", "alm-key");
    assertThat(newValue.toString()).contains("userLogin");

    String updated_pat = "updated pat";
    almPatDto.setPersonalAccessToken(updated_pat);
    system2.setNow(NOW + 1);
    underTest.update(dbSession, almPatDto, null, null);

    verify(auditPersister).updatePersonalAccessToken(eq(dbSession), newValueCaptor.capture());
  }

  @Test
  void deleteIsPersisted() {
    when(uuidFactory.create()).thenReturn(A_UUID);

    AlmPatDto almPat = newAlmPatDto();
    underTest.insert(dbSession, almPat, null, null);
    verify(auditPersister).addPersonalAccessToken(eq(dbSession), newValueCaptor.capture());

    underTest.delete(dbSession, almPat, null, null);

    verify(auditPersister).deletePersonalAccessToken(eq(dbSession), newValueCaptor.capture());
    PersonalAccessTokenNewValue newValue = newValueCaptor.getValue();
    assertThat(newValue)
      .extracting(PersonalAccessTokenNewValue::getPatUuid, PersonalAccessTokenNewValue::getUserUuid,
        PersonalAccessTokenNewValue::getAlmSettingUuid, PersonalAccessTokenNewValue::getUserLogin,
        PersonalAccessTokenNewValue::getAlmSettingKey)
      .containsExactly(almPat.getUuid(), almPat.getUserUuid(), almPat.getAlmSettingUuid(),
        null, null);
    assertThat(newValue.toString()).doesNotContain("userLogin");
  }

  @Test
  void deleteWithoutAffectedRowsIsNotPersisted() {
    AlmPatDto almPat = newAlmPatDto();

    underTest.delete(dbSession, almPat, null, null);

    verifyNoInteractions(auditPersister);
  }

  @Test
  void deleteByUserIsPersisted() {
    when(uuidFactory.create()).thenReturn(A_UUID);
    UserDto userDto = db.users().insertUser();
    AlmPatDto almPat = newAlmPatDto();
    almPat.setUserUuid(userDto.getUuid());
    underTest.insert(dbSession, almPat, userDto.getLogin(), null);
    verify(auditPersister).addPersonalAccessToken(eq(dbSession), newValueCaptor.capture());

    underTest.deleteByUser(dbSession, userDto);

    verify(auditPersister).deletePersonalAccessToken(eq(dbSession), newValueCaptor.capture());
    PersonalAccessTokenNewValue newValue = newValueCaptor.getValue();
    assertThat(newValue)
      .extracting(PersonalAccessTokenNewValue::getPatUuid, PersonalAccessTokenNewValue::getUserUuid,
        PersonalAccessTokenNewValue::getUserLogin, PersonalAccessTokenNewValue::getAlmSettingKey)
      .containsExactly(null, userDto.getUuid(), userDto.getLogin(), null);
    assertThat(newValue.toString()).doesNotContain("patUuid");
  }

  @Test
  void deleteByUserWithoutAffectedRowsIsNotPersisted() {
    UserDto userDto = db.users().insertUser();

    underTest.deleteByUser(dbSession, userDto);

    verify(auditPersister).addUser(any(), any());
    verifyNoMoreInteractions(auditPersister);
  }

  @Test
  void deleteByAlmSettingIsPersisted() {
    when(uuidFactory.create()).thenReturn(A_UUID);
    AlmSettingDto almSettingDto = db.almSettings().insertBitbucketAlmSetting();
    AlmPatDto almPat = newAlmPatDto();
    almPat.setAlmSettingUuid(almSettingDto.getUuid());
    underTest.insert(dbSession, almPat, null, almSettingDto.getKey());
    verify(auditPersister).addPersonalAccessToken(eq(dbSession), newValueCaptor.capture());

    underTest.deleteByAlmSetting(dbSession, almSettingDto);

    verify(auditPersister).deletePersonalAccessToken(eq(dbSession), newValueCaptor.capture());
    PersonalAccessTokenNewValue newValue = newValueCaptor.getValue();
    assertThat(newValue)
      .extracting(PersonalAccessTokenNewValue::getPatUuid, PersonalAccessTokenNewValue::getAlmSettingUuid,
        PersonalAccessTokenNewValue::getAlmSettingKey)
      .containsExactly(null, almPat.getAlmSettingUuid(), almSettingDto.getKey());
    assertThat(newValue.toString()).doesNotContain("userUuid");
  }

  @Test
  void deleteByAlmSettingWithoutAffectedRowsIsNotPersisted() {
    AlmSettingDto almSettingDto = db.almSettings().insertBitbucketAlmSetting();
    clearInvocations(auditPersister);
    underTest.deleteByAlmSetting(dbSession, almSettingDto);

    verifyNoInteractions(auditPersister);
  }

}
