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
import org.sonar.db.audit.model.SecretNewValue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.db.almsettings.AlmSettingsTesting.newGithubAlmSettingDto;

public class AlmSettingDaoWithPersisterIT {

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
    ArgumentCaptor<SecretNewValue> secretNewValueCaptor = ArgumentCaptor.forClass(SecretNewValue.class);
    when(uuidFactory.create()).thenReturn(A_UUID);
    AlmSettingDto almSettingDto = newGithubAlmSettingDto()
      .setKey("key")
      .setAppId("id1")
      .setClientId("cid1")
      .setUrl("url");

    underTest.insert(dbSession, almSettingDto);

    verify(auditPersister).addDevOpsPlatformSetting(eq(dbSession), newValueCaptor.capture());
    DevOpsPlatformSettingNewValue newValue = newValueCaptor.getValue();
    assertThat(newValue)
      .extracting("devOpsPlatformSettingUuid", "key")
      .containsExactly(almSettingDto.getUuid(), almSettingDto.getKey());
    assertThat(newValue)
      .hasToString("{\"devOpsPlatformSettingUuid\": \"1\", \"key\": \"key\", \"devOpsPlatformName\": \"id1\", \"url\": \"url\", \"appId\": \"id1\", \"clientId\": \"cid1\" }");

    almSettingDto.setPrivateKey("updated private key");
    almSettingDto.setAppId("updated app id");
    almSettingDto.setUrl("updated url");
    almSettingDto.setPersonalAccessToken("updated pat");
    almSettingDto.setKey("updated key");

    underTest.update(dbSession, almSettingDto, true);

    verify(auditPersister).updateDevOpsPlatformSecret(eq(dbSession), secretNewValueCaptor.capture());
    SecretNewValue secretNewValue = secretNewValueCaptor.getValue();
    assertThat(secretNewValue).hasToString(String.format("{\"DevOpsPlatform\":\"%s\"}", almSettingDto.getRawAlm()));

    verify(auditPersister).updateDevOpsPlatformSetting(eq(dbSession), newValueCaptor.capture());
    newValue = newValueCaptor.getValue();
    assertThat(newValue)
      .extracting("devOpsPlatformSettingUuid", "key", "appId", "devOpsPlatformName", "url", "clientId")
      .containsExactly(almSettingDto.getUuid(), almSettingDto.getKey(), almSettingDto.getAppId(), almSettingDto.getAppId(), almSettingDto.getUrl(), almSettingDto.getClientId());
    assertThat(newValue).hasToString("{\"devOpsPlatformSettingUuid\": \"1\", \"key\": \"updated key\", \"devOpsPlatformName\": \"updated app id\", "
      + "\"url\": \"updated url\", \"appId\": \"updated app id\", \"clientId\": \"cid1\" }");
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
      .extracting("devOpsPlatformSettingUuid", "key")
      .containsExactly(almSettingDto.getUuid(), almSettingDto.getKey());
    assertThat(newValue.getUrl()).isNullOrEmpty();
  }
}
