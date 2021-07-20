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
package org.sonar.db.alm.setting;

import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.sonar.api.impl.utils.TestSystem2;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.audit.AuditPersister;
import org.sonar.db.audit.model.DevOpsPlatformSettingNewValue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.db.almsettings.AlmSettingsTesting.newGithubAlmSettingDto;

public class AlmSettingDaoWithPersisterTest {

  private static final long NOW = 1000000L;
  private static final String A_UUID = "SOME_UUID";

  private final ArgumentCaptor<DevOpsPlatformSettingNewValue> newValueCaptor = ArgumentCaptor.forClass(DevOpsPlatformSettingNewValue.class);
  private final AuditPersister auditPersister = mock(AuditPersister.class);
  private final TestSystem2 system2 = new TestSystem2().setNow(NOW);
  @Rule
  public final DbTester db = DbTester.create(system2, auditPersister);

  private final DbSession dbSession = db.getSession();
  private final UuidFactory uuidFactory = mock(UuidFactory.class);

  private final AlmSettingDao underTest = db.getDbClient().almSettingDao();

  @Test
  public void insertAndUpdateArePersisted() {
    when(uuidFactory.create()).thenReturn(A_UUID);
    AlmSettingDto almSettingDto = newGithubAlmSettingDto();

    underTest.insert(dbSession, almSettingDto);

    verify(auditPersister).addDevOpsPlatformSetting(eq(dbSession), newValueCaptor.capture());
    DevOpsPlatformSettingNewValue newValue = newValueCaptor.getValue();
    assertThat(newValue)
      .extracting(DevOpsPlatformSettingNewValue::getDevOpsPlatformSettingUuid, DevOpsPlatformSettingNewValue::getKey)
      .containsExactly(almSettingDto.getUuid(), almSettingDto.getKey());
    assertThat(newValue.toString()).doesNotContain("url");

    almSettingDto.setPrivateKey("updated private key");
    almSettingDto.setAppId("updated app id");
    almSettingDto.setUrl("updated url");
    almSettingDto.setPersonalAccessToken("updated pat");
    almSettingDto.setKey("updated key");
    system2.setNow(NOW + 1);

    underTest.update(dbSession, almSettingDto);

    verify(auditPersister).updateDevOpsPlatformSetting(eq(dbSession), newValueCaptor.capture());
    newValue = newValueCaptor.getValue();
    assertThat(newValue)
      .extracting(DevOpsPlatformSettingNewValue::getDevOpsPlatformSettingUuid, DevOpsPlatformSettingNewValue::getKey,
        DevOpsPlatformSettingNewValue::getAppId, DevOpsPlatformSettingNewValue::getDevOpsPlatformName,
        DevOpsPlatformSettingNewValue::getUrl, DevOpsPlatformSettingNewValue::getClientId)
      .containsExactly(almSettingDto.getUuid(), almSettingDto.getKey(), almSettingDto.getAppId(), almSettingDto.getAppId(),
        almSettingDto.getUrl(), almSettingDto.getClientId());
  }

  @Test
  public void deleteIsPersisted() {
    when(uuidFactory.create()).thenReturn(A_UUID);
    AlmSettingDto almSettingDto = newGithubAlmSettingDto();
    underTest.insert(dbSession, almSettingDto);

    underTest.delete(dbSession, almSettingDto);

    verify(auditPersister).deleteDevOpsPlatformSetting(eq(dbSession), newValueCaptor.capture());
    DevOpsPlatformSettingNewValue newValue = newValueCaptor.getValue();
    assertThat(newValue)
      .extracting(DevOpsPlatformSettingNewValue::getDevOpsPlatformSettingUuid, DevOpsPlatformSettingNewValue::getKey)
      .containsExactly(almSettingDto.getUuid(), almSettingDto.getKey());
    assertThat(newValue.toString()).doesNotContain("url");
  }
}
