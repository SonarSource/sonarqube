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

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.impl.utils.TestSystem2;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.audit.NoOpAuditPersister;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.db.alm.setting.ALM.GITHUB;
import static org.sonar.db.alm.setting.ALM.GITLAB;
import static org.sonar.db.almsettings.AlmSettingsTesting.newGithubAlmSettingDto;

class AlmSettingDaoIT {

  private static final long NOW = 1000000L;
  private static final String A_UUID = "SOME_UUID";
  private static final AlmSettingDto ALM_SETTING_WITH_WEBHOOK_SECRET = newGithubAlmSettingDto().setWebhookSecret("webhook_secret");

  private final TestSystem2 system2 = new TestSystem2().setNow(NOW);
  @RegisterExtension
  private final DbTester db = DbTester.create(system2);

  private final DbSession dbSession = db.getSession();
  private final UuidFactory uuidFactory = mock(UuidFactory.class);

  private final AlmSettingDao underTest = new AlmSettingDao(system2, uuidFactory, new NoOpAuditPersister());

  @BeforeEach
  void setUp() {
    Iterator<Integer> values = Stream.iterate(0, i -> i + 1).iterator();
    when(uuidFactory.create()).thenAnswer(answer -> A_UUID + "_" + values.next());
  }

  @Test
  void selectByUuid() {
    AlmSettingDto expected = ALM_SETTING_WITH_WEBHOOK_SECRET;
    underTest.insert(dbSession, expected);

    AlmSettingDto result = underTest.selectByUuid(dbSession, expected.getUuid()).orElse(null);

    assertThat(result).usingRecursiveComparison().isEqualTo(expected);
  }

  @Test
  void selectByUuid_shouldNotFindResult_whenUuidIsNotPresent() {
   underTest.insert(dbSession, ALM_SETTING_WITH_WEBHOOK_SECRET);

    assertThat(underTest.selectByUuid(dbSession, "foo")).isNotPresent();
  }

  @Test
  void selectByKey() {
    AlmSettingDto expected = ALM_SETTING_WITH_WEBHOOK_SECRET;
    underTest.insert(dbSession, expected);

    AlmSettingDto result = underTest.selectByKey(dbSession, expected.getKey()).orElse(null);

    assertThat(result).usingRecursiveComparison().isEqualTo(expected);
  }

  @Test
  void selectByKey_shouldNotFindResult_whenKeyIsNotPresent() {
   underTest.insert(dbSession, ALM_SETTING_WITH_WEBHOOK_SECRET);

    assertThat(underTest.selectByKey(dbSession, "foo")).isNotPresent();
  }

  @Test
  void selectByKey_withEmptySecrets() {
    AlmSettingDto expected = newGithubAlmSettingDto().setWebhookSecret(null);
    underTest.insert(dbSession, expected);

    AlmSettingDto result = underTest.selectByKey(dbSession, expected.getKey()).orElse(null);

    assertThat(result).usingRecursiveComparison().isEqualTo(expected);
  }

  @Test
  void selectByAlm() {
    AlmSettingDto gitHubAlmSetting1 = db.almSettings().insertGitHubAlmSetting();
    AlmSettingDto gitHubAlmSetting2 = db.almSettings().insertGitHubAlmSetting();
    db.almSettings().insertAzureAlmSetting();

    List<AlmSettingDto> almSettings = underTest.selectByAlm(dbSession, GITHUB);

    assertThat(almSettings)
      .extracting(AlmSettingDto::getUuid)
      .containsExactlyInAnyOrder(gitHubAlmSetting1.getUuid(), gitHubAlmSetting2.getUuid());
  }

  @Test
  void selectAll() {
    underTest.insert(dbSession, newGithubAlmSettingDto());
    when(uuidFactory.create()).thenReturn(A_UUID + "bis");
    underTest.insert(dbSession, newGithubAlmSettingDto());

    List<AlmSettingDto> almSettings = underTest.selectAll(dbSession);

    assertThat(almSettings).size().isEqualTo(2);
  }

  @Test
  void update() {
    //GIVEN
    AlmSettingDto expected = newGithubAlmSettingDto();
    underTest.insert(dbSession, expected);

    expected.setPrivateKey("updated private key");
    expected.setAppId("updated app id");
    expected.setUrl("updated url");
    expected.setPersonalAccessToken("updated pat");
    expected.setKey("updated key");
    expected.setWebhookSecret("updated webhook secret");

    system2.setNow(NOW + 1);
    //WHEN
    underTest.update(dbSession, expected, false);
    //THEN
    AlmSettingDto result = underTest.selectByUuid(dbSession, expected.getUuid()).orElse(null);
    assertThat(result).usingRecursiveComparison().isEqualTo(expected);
  }

  @Test
  void delete() {
    AlmSettingDto almSettingDto = newGithubAlmSettingDto();
    underTest.insert(dbSession, almSettingDto);

    underTest.delete(dbSession, almSettingDto);

    assertThat(underTest.selectByKey(dbSession, almSettingDto.getKey())).isNotPresent();
  }

  @Test
  void selectByAlmAndAppId_whenSingleMatch_returnsCorrectObject() {
    String appId = "APP_ID";
    AlmSettingDto expectedAlmSettingDto = db.almSettings().insertGitHubAlmSetting(almSettingDto -> almSettingDto.setAppId(appId));
    db.almSettings().insertGitHubAlmSetting(almSettingDto -> almSettingDto.setAppId(null));

    Optional<AlmSettingDto> result = underTest.selectByAlmAndAppId(dbSession, GITHUB, appId);

    assertThat(result).isPresent();
    assertThat(result.get()).usingRecursiveComparison().isEqualTo(expectedAlmSettingDto);
  }

  @Test
  void selectByAlmAndAppId_whenAppIdSharedWithAnotherAlm_returnsCorrectOne() {
    String appId = "APP_ID";
    db.almSettings().insertGitHubAlmSetting(almSettingDto -> almSettingDto.setAppId(appId));
    AlmSettingDto gitLabAlmSettingDto = db.almSettings().insertGitlabAlmSetting(almSettingDto -> almSettingDto.setAppId(appId));

    Optional<AlmSettingDto> result = underTest.selectByAlmAndAppId(dbSession, GITLAB, appId);

    assertThat(result).isPresent();
    assertThat(result.get()).usingRecursiveComparison().isEqualTo(gitLabAlmSettingDto);
  }

  @Test
  void selectByAlmAndAppId_withMultipleConfigurationWithSameAppId_returnsAnyAndDoesNotFail() {
    String appId = "APP_ID";
    IntStream.of(1, 10).forEach(i -> db.almSettings().insertGitHubAlmSetting(almSettingDto -> almSettingDto.setAppId(appId)));
    IntStream.of(1, 5).forEach(i -> db.almSettings().insertGitHubAlmSetting(almSettingDto -> almSettingDto.setAppId(null)));

    Optional<AlmSettingDto> result = underTest.selectByAlmAndAppId(dbSession, GITHUB, appId);

    assertThat(result).isPresent();
    assertThat(result.get().getAppId()).isEqualTo(appId);
  }

}
