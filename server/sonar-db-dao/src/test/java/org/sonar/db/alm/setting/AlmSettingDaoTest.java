/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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

import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.internal.Encryption;
import org.sonar.api.impl.utils.TestSystem2;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.almsettings.AlmSettingsTesting;
import org.sonar.db.audit.NoOpAuditPersister;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.db.almsettings.AlmSettingsTesting.newGithubAlmSettingDto;

public class AlmSettingDaoTest {

  private static final long NOW = 1000000L;
  private static final String A_UUID = "SOME_UUID";
  private TestSystem2 system2 = new TestSystem2().setNow(NOW);
  @Rule
  public DbTester db = DbTester.create(system2);

  private final Encryption encryption = mock(Encryption.class);

  private DbSession dbSession = db.getSession();
  private UuidFactory uuidFactory = mock(UuidFactory.class);

  private AlmSettingDao underTest = new AlmSettingDao(system2, uuidFactory, new NoOpAuditPersister());

  @Test
  public void selectByUuid() {
    when(uuidFactory.create()).thenReturn(A_UUID);
    when(encryption.isEncrypted(any())).thenReturn(false);

    AlmSettingDto almSettingDto = newGithubAlmSettingDto();
    underTest.insert(dbSession, almSettingDto);

    assertThat(underTest.selectByUuid(dbSession, A_UUID).get())
      .extracting(AlmSettingDto::getUuid, AlmSettingDto::getKey, AlmSettingDto::getRawAlm, AlmSettingDto::getUrl,
        AlmSettingDto::getAppId, AlmSettingDto::getCreatedAt, AlmSettingDto::getUpdatedAt,
        s -> almSettingDto.getDecryptedPrivateKey(encryption),
        s -> almSettingDto.getDecryptedPersonalAccessToken(encryption),
        s -> almSettingDto.getDecryptedClientSecret(encryption))
      .containsExactly(A_UUID, almSettingDto.getKey(), ALM.GITHUB.getId(), almSettingDto.getUrl(),
        almSettingDto.getAppId(), NOW, NOW,
        almSettingDto.getDecryptedPrivateKey(encryption),
        almSettingDto.getDecryptedPersonalAccessToken(encryption),
        almSettingDto.getDecryptedClientSecret(encryption));

    assertThat(underTest.selectByUuid(dbSession, "foo")).isNotPresent();
  }

  @Test
  public void selectByKey() {
    when(uuidFactory.create()).thenReturn(A_UUID);
    String decrypted = "decrypted";
    when(encryption.isEncrypted(any())).thenReturn(true);
    when(encryption.decrypt(any())).thenReturn(decrypted);

    AlmSettingDto almSettingDto = AlmSettingsTesting.newGithubAlmSettingDto();
    underTest.insert(dbSession, almSettingDto);

    assertThat(underTest.selectByKey(dbSession, almSettingDto.getKey()).get())
      .extracting(AlmSettingDto::getUuid, AlmSettingDto::getKey, AlmSettingDto::getRawAlm, AlmSettingDto::getUrl,
        AlmSettingDto::getAppId, AlmSettingDto::getCreatedAt, AlmSettingDto::getUpdatedAt,
        s -> almSettingDto.getDecryptedPrivateKey(encryption),
        s -> almSettingDto.getDecryptedPersonalAccessToken(encryption),
        s -> almSettingDto.getDecryptedClientSecret(encryption))
      .containsExactly(A_UUID, almSettingDto.getKey(), ALM.GITHUB.getId(), almSettingDto.getUrl(),
        almSettingDto.getAppId(), NOW, NOW,
        almSettingDto.getDecryptedPrivateKey(encryption),
        null,
        almSettingDto.getDecryptedClientSecret(encryption));

    assertThat(underTest.selectByKey(dbSession, "foo")).isNotPresent();
  }

  @Test
  public void selectByKey_withEmptySecrets() {
    when(uuidFactory.create()).thenReturn(A_UUID);
    String decrypted = "decrypted";
    when(encryption.isEncrypted(any())).thenReturn(true);
    when(encryption.decrypt(any())).thenReturn(decrypted);

    AlmSettingDto almSettingDto = AlmSettingsTesting.newAlmSettingDtoWithEmptySecrets();
    underTest.insert(dbSession, almSettingDto);

    assertThat(underTest.selectByKey(dbSession, almSettingDto.getKey()).get())
      .extracting(AlmSettingDto::getUuid, AlmSettingDto::getKey, AlmSettingDto::getRawAlm, AlmSettingDto::getUrl,
        AlmSettingDto::getAppId, AlmSettingDto::getCreatedAt, AlmSettingDto::getUpdatedAt,
        s -> almSettingDto.getDecryptedPrivateKey(encryption),
        s -> almSettingDto.getDecryptedPersonalAccessToken(encryption),
        s -> almSettingDto.getDecryptedClientSecret(encryption))
      .containsExactly(A_UUID, almSettingDto.getKey(), ALM.GITHUB.getId(), almSettingDto.getUrl(),
        almSettingDto.getAppId(), NOW, NOW, null, null, null);

    assertThat(underTest.selectByKey(dbSession, "foo")).isNotPresent();
  }

  @Test
  public void selectByAlm() {
    when(uuidFactory.create()).thenReturn(A_UUID);
    AlmSettingDto gitHubAlmSetting1 = db.almSettings().insertGitHubAlmSetting();
    AlmSettingDto gitHubAlmSetting2 = db.almSettings().insertGitHubAlmSetting();
    AlmSettingDto azureAlmSetting2 = db.almSettings().insertAzureAlmSetting();

    List<AlmSettingDto> almSettings = underTest.selectByAlm(dbSession, ALM.GITHUB);

    assertThat(almSettings)
      .extracting(AlmSettingDto::getUuid)
      .containsExactlyInAnyOrder(gitHubAlmSetting1.getUuid(), gitHubAlmSetting2.getUuid());
  }

  @Test
  public void selectAll() {
    when(uuidFactory.create()).thenReturn(A_UUID);
    underTest.insert(dbSession, newGithubAlmSettingDto());
    when(uuidFactory.create()).thenReturn(A_UUID + "bis");
    underTest.insert(dbSession, newGithubAlmSettingDto());

    List<AlmSettingDto> almSettings = underTest.selectAll(dbSession);

    assertThat(almSettings).size().isEqualTo(2);
  }

  @Test
  public void update() {
    when(uuidFactory.create()).thenReturn(A_UUID);
    AlmSettingDto almSettingDto = newGithubAlmSettingDto();
    underTest.insert(dbSession, almSettingDto);

    almSettingDto.setPrivateKey("updated private key");
    almSettingDto.setAppId("updated app id");
    almSettingDto.setUrl("updated url");
    almSettingDto.setPersonalAccessToken("updated pat");
    almSettingDto.setKey("updated key");

    system2.setNow(NOW + 1);
    underTest.update(dbSession, almSettingDto, false);

    AlmSettingDto result = underTest.selectByUuid(dbSession, A_UUID).get();
    assertThat(result)
      .extracting(AlmSettingDto::getUuid, AlmSettingDto::getKey, AlmSettingDto::getRawAlm, AlmSettingDto::getUrl,
        AlmSettingDto::getAppId,
        s -> almSettingDto.getDecryptedPrivateKey(encryption),
        s -> almSettingDto.getDecryptedPersonalAccessToken(encryption),
        AlmSettingDto::getCreatedAt, AlmSettingDto::getUpdatedAt)
      .containsExactly(A_UUID, almSettingDto.getKey(), ALM.GITHUB.getId(), almSettingDto.getUrl(),
        almSettingDto.getAppId(), almSettingDto.getDecryptedPrivateKey(encryption),
        almSettingDto.getDecryptedPersonalAccessToken(encryption), NOW, NOW + 1);
  }

  @Test
  public void delete() {
    when(uuidFactory.create()).thenReturn(A_UUID);
    AlmSettingDto almSettingDto = newGithubAlmSettingDto();
    underTest.insert(dbSession, almSettingDto);

    underTest.delete(dbSession, almSettingDto);

    assertThat(underTest.selectByKey(dbSession, almSettingDto.getKey())).isNotPresent();
  }

}
