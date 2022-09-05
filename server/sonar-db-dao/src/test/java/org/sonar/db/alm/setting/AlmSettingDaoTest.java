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
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.impl.utils.TestSystem2;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.audit.NoOpAuditPersister;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.db.alm.setting.ALM.GITHUB;
import static org.sonar.db.almsettings.AlmSettingsTesting.newAlmSettingDtoWithEmptySecrets;
import static org.sonar.db.almsettings.AlmSettingsTesting.newGithubAlmSettingDto;
import static org.sonar.db.almsettings.AlmSettingsTesting.newGithubAlmSettingDtoWithNonRequiredField;

public class AlmSettingDaoTest {

  private static final long NOW = 1000000L;
  private static final String A_UUID = "SOME_UUID";
  private final TestSystem2 system2 = new TestSystem2().setNow(NOW);
  @Rule
  public DbTester db = DbTester.create(system2);

  private final DbSession dbSession = db.getSession();
  private final UuidFactory uuidFactory = mock(UuidFactory.class);

  private final AlmSettingDao underTest = new AlmSettingDao(system2, uuidFactory, new NoOpAuditPersister());

  @Before
  public void setUp() {
    when(uuidFactory.create()).thenReturn(A_UUID);
  }

  @Test
  public void selectByUuid() {
    AlmSettingDto expected = newGithubAlmSettingDtoWithNonRequiredField();
    underTest.insert(dbSession, expected);

    AlmSettingDto result = underTest.selectByUuid(dbSession, A_UUID).orElse(null);

    assertThat(result).usingRecursiveComparison().isEqualTo(expected);
  }

  @Test
  public void selectByUuid_shouldNotFindResult_whenUuidIsNotPresent() {
    AlmSettingDto expected = newGithubAlmSettingDtoWithNonRequiredField();
    underTest.insert(dbSession, expected);

    assertThat(underTest.selectByUuid(dbSession, "foo")).isNotPresent();
  }

  @Test
  public void selectByKey() {
    AlmSettingDto expected = newGithubAlmSettingDtoWithNonRequiredField();
    underTest.insert(dbSession, expected);

    AlmSettingDto result = underTest.selectByKey(dbSession, expected.getKey()).orElse(null);

    assertThat(result).usingRecursiveComparison().isEqualTo(expected);
  }

  @Test
  public void selectByKey_shouldNotFindResult_whenKeyIsNotPresent() {
    AlmSettingDto expected = newGithubAlmSettingDtoWithNonRequiredField();
    underTest.insert(dbSession, expected);

    assertThat(underTest.selectByKey(dbSession, "foo")).isNotPresent();
  }

  @Test
  public void selectByKey_withEmptySecrets() {
    AlmSettingDto expected = newAlmSettingDtoWithEmptySecrets();
    underTest.insert(dbSession, expected);

    AlmSettingDto result = underTest.selectByKey(dbSession, expected.getKey()).orElse(null);

    assertThat(result).usingRecursiveComparison().isEqualTo(expected);
  }

  @Test
  public void selectByAlm() {
    AlmSettingDto gitHubAlmSetting1 = db.almSettings().insertGitHubAlmSetting();
    AlmSettingDto gitHubAlmSetting2 = db.almSettings().insertGitHubAlmSetting();
    db.almSettings().insertAzureAlmSetting();

    List<AlmSettingDto> almSettings = underTest.selectByAlm(dbSession, GITHUB);

    assertThat(almSettings)
      .extracting(AlmSettingDto::getUuid)
      .containsExactlyInAnyOrder(gitHubAlmSetting1.getUuid(), gitHubAlmSetting2.getUuid());
  }

  @Test
  public void selectAll() {
    underTest.insert(dbSession, newGithubAlmSettingDto());
    when(uuidFactory.create()).thenReturn(A_UUID + "bis");
    underTest.insert(dbSession, newGithubAlmSettingDto());

    List<AlmSettingDto> almSettings = underTest.selectAll(dbSession);

    assertThat(almSettings).size().isEqualTo(2);
  }

  @Test
  public void update() {
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
    AlmSettingDto result = underTest.selectByUuid(dbSession, A_UUID).orElse(null);
    assertThat(result).usingRecursiveComparison().isEqualTo(expected);
  }

  @Test
  public void delete() {
    AlmSettingDto almSettingDto = newGithubAlmSettingDto();
    underTest.insert(dbSession, almSettingDto);

    underTest.delete(dbSession, almSettingDto);

    assertThat(underTest.selectByKey(dbSession, almSettingDto.getKey())).isNotPresent();
  }

}
