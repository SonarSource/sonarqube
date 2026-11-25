/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.server.common.almsettings.bitbucketserver;

import java.util.Optional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.sonar.alm.client.bitbucketserver.BitbucketServerException;
import org.sonar.alm.client.bitbucketserver.BitbucketServerSettingsValidator;
import org.sonar.api.config.internal.Encryption;
import org.sonar.api.config.internal.Settings;
import org.sonar.core.scm.ScmAccessToken;
import org.sonar.db.DbClient;
import org.sonar.db.alm.setting.ALM;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.alm.setting.ProjectAlmSettingDto;
import org.sonar.db.project.ProjectDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BitbucketServerScmAccessTokenProviderTest {

  private static final String PROJECT_KEY = "my-project";
  private static final String ALM_SETTING_UUID = "almSettingUuid";
  private static final String DECRYPTED_PAT = "bitbucket-server-pat";

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private DbClient dbClient;
  @Mock
  private BitbucketServerSettingsValidator bitbucketServerSettingsValidator;
  @Mock
  private Settings settings;
  @Mock
  private Encryption encryption;

  private BitbucketServerScmAccessTokenProvider underTest;

  @org.junit.Before
  public void setUp() {
    when(settings.getEncryption()).thenReturn(encryption);
    underTest = new BitbucketServerScmAccessTokenProvider(dbClient, bitbucketServerSettingsValidator, settings);
  }

  @Test
  public void mint_whenProjectUnknown_shouldReturnEmpty() {
    when(dbClient.projectDao().selectProjectByKey(any(), eq(PROJECT_KEY))).thenReturn(Optional.empty());

    assertThat(underTest.mint(PROJECT_KEY)).isEmpty();
  }

  @Test
  public void mint_whenProjectNotBoundToAnyDevOpsPlatform_shouldReturnEmpty() {
    ProjectDto project = mockProject();
    when(dbClient.projectAlmSettingDao().selectByProject(any(), eq(project))).thenReturn(Optional.empty());

    assertThat(underTest.mint(PROJECT_KEY)).isEmpty();
  }

  @Test
  public void mint_whenAlmSettingIsNotBitbucketServer_shouldReturnEmpty() {
    mockProjectAlmSetting();
    AlmSettingDto almSetting = mock();
    when(almSetting.getAlm()).thenReturn(ALM.BITBUCKET_CLOUD);
    when(dbClient.almSettingDao().selectByUuid(any(), eq(ALM_SETTING_UUID))).thenReturn(Optional.of(almSetting));

    assertThat(underTest.mint(PROJECT_KEY)).isEmpty();
  }

  @Test
  public void mint_whenAlmSettingUuidNotFound_shouldReturnEmpty() {
    mockProjectAlmSetting();
    when(dbClient.almSettingDao().selectByUuid(any(), eq(ALM_SETTING_UUID))).thenReturn(Optional.empty());

    assertThat(underTest.mint(PROJECT_KEY)).isEmpty();
  }

  @Test
  public void mint_whenBitbucketServerConfigurationIsIncomplete_shouldThrow() {
    mockProjectAlmSetting();
    AlmSettingDto almSetting = mockBitbucketServerAlmSetting();
    IllegalArgumentException cause = new IllegalArgumentException("Your global Bitbucket Server configuration is incomplete.");
    doThrow(cause).when(bitbucketServerSettingsValidator).validate(almSetting);

    assertThatThrownBy(() -> underTest.mint(PROJECT_KEY))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("Cannot provide a Bitbucket Server access token for project '%s'".formatted(PROJECT_KEY))
      .hasCause(cause);
  }

  @Test
  public void mint_whenBitbucketServerRejectsPersonalAccessToken_shouldThrow() {
    mockProjectAlmSetting();
    AlmSettingDto almSetting = mockBitbucketServerAlmSetting();
    BitbucketServerException cause = new BitbucketServerException(401, "Invalid personal access token");
    doThrow(cause).when(bitbucketServerSettingsValidator).validate(almSetting);

    assertThatThrownBy(() -> underTest.mint(PROJECT_KEY))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("invalid Bitbucket Server configuration: Invalid personal access token")
      .hasCause(cause);
  }

  @Test
  public void mint_whenEverythingSucceeds_shouldReturnStoredPersonalAccessTokenAfterValidating() {
    mockProjectAlmSetting();
    AlmSettingDto almSetting = mockBitbucketServerAlmSetting();

    Optional<ScmAccessToken> result = underTest.mint(PROJECT_KEY);

    assertThat(result).contains(new ScmAccessToken(ALM.BITBUCKET.getId(), "", DECRYPTED_PAT, null));
    verify(bitbucketServerSettingsValidator).validate(almSetting);
  }

  private ProjectDto mockProject() {
    ProjectDto project = mock();
    when(dbClient.projectDao().selectProjectByKey(any(), eq(PROJECT_KEY))).thenReturn(Optional.of(project));
    return project;
  }

  private ProjectAlmSettingDto mockProjectAlmSetting() {
    ProjectDto project = mockProject();
    ProjectAlmSettingDto projectAlmSetting = mock();
    when(projectAlmSetting.getAlmSettingUuid()).thenReturn(ALM_SETTING_UUID);
    when(dbClient.projectAlmSettingDao().selectByProject(any(), eq(project))).thenReturn(Optional.of(projectAlmSetting));
    return projectAlmSetting;
  }

  private AlmSettingDto mockBitbucketServerAlmSetting() {
    AlmSettingDto almSetting = mock();
    when(almSetting.getAlm()).thenReturn(ALM.BITBUCKET);
    when(almSetting.getDecryptedPersonalAccessToken(encryption)).thenReturn(DECRYPTED_PAT);
    when(dbClient.almSettingDao().selectByUuid(any(), eq(ALM_SETTING_UUID))).thenReturn(Optional.of(almSetting));
    return almSetting;
  }
}
