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
package org.sonar.server.common.almsettings.bitbucketcloud;

import java.time.Instant;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.sonar.alm.client.bitbucket.bitbucketcloud.BitbucketCloudRestClient;
import org.sonar.alm.client.bitbucket.bitbucketcloud.BitbucketCloudRestClient.BitbucketCloudException;
import org.sonar.alm.client.bitbucket.bitbucketcloud.Token;
import org.sonar.api.config.internal.Encryption;
import org.sonar.api.config.internal.Settings;
import org.sonar.api.utils.System2;
import org.sonar.core.scm.ScmAccessToken;
import org.sonar.db.DbClient;
import org.sonar.db.alm.setting.ALM;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.alm.setting.ProjectAlmSettingDto;
import org.sonar.db.project.ProjectDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BitbucketCloudScmAccessTokenProviderTest {

  private static final String PROJECT_KEY = "my-project";
  private static final String ALM_SETTING_UUID = "almSettingUuid";
  private static final String CLIENT_ID = "client-id";
  private static final String DECRYPTED_CLIENT_SECRET = "client-secret";
  private static final String ACCESS_TOKEN = "bitbucket-access-token";
  private static final Instant NOW = Instant.parse("2026-11-19T10:00:00Z");

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private DbClient dbClient;
  @Mock
  private BitbucketCloudRestClient bitbucketCloudRestClient;
  @Mock
  private Settings settings;
  @Mock
  private Encryption encryption;
  @Mock
  private System2 system2;

  private BitbucketCloudScmAccessTokenProvider underTest;

  @Before
  public void setUp() {
    when(settings.getEncryption()).thenReturn(encryption);
    underTest = new BitbucketCloudScmAccessTokenProvider(dbClient, bitbucketCloudRestClient, settings, system2);
  }

  @Test
  public void mint_whenProjectUnknown_shouldReturnEmpty() {
    when(dbClient.projectDao().selectProjectByKey(any(), eq(PROJECT_KEY))).thenReturn(Optional.empty());

    assertThat(underTest.mint(PROJECT_KEY)).isEmpty();
    verifyNoInteractions(bitbucketCloudRestClient);
  }

  @Test
  public void mint_whenProjectNotBoundToAnyDevOpsPlatform_shouldReturnEmpty() {
    ProjectDto project = mockProject();
    when(dbClient.projectAlmSettingDao().selectByProject(any(), eq(project))).thenReturn(Optional.empty());

    assertThat(underTest.mint(PROJECT_KEY)).isEmpty();
    verifyNoInteractions(bitbucketCloudRestClient);
  }

  @Test
  public void mint_whenAlmSettingIsNotBitbucketCloud_shouldReturnEmpty() {
    mockProjectAlmSetting();
    AlmSettingDto almSetting = mock();
    when(almSetting.getAlm()).thenReturn(ALM.BITBUCKET);
    when(dbClient.almSettingDao().selectByUuid(any(), eq(ALM_SETTING_UUID))).thenReturn(Optional.of(almSetting));

    assertThat(underTest.mint(PROJECT_KEY)).isEmpty();
    verifyNoInteractions(bitbucketCloudRestClient);
  }

  @Test
  public void mint_whenAlmSettingUuidNotFound_shouldReturnEmpty() {
    mockProjectAlmSetting();
    when(dbClient.almSettingDao().selectByUuid(any(), eq(ALM_SETTING_UUID))).thenReturn(Optional.empty());

    assertThat(underTest.mint(PROJECT_KEY)).isEmpty();
  }

  @Test
  public void mint_whenOAuthConsumerSecretIsMissing_shouldThrow() {
    mockProjectAlmSetting();
    AlmSettingDto almSetting = mockBitbucketCloudAlmSetting();
    when(almSetting.getDecryptedClientSecret(encryption)).thenReturn(null);

    assertThatThrownBy(() -> underTest.mint(PROJECT_KEY))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("Cannot mint a Bitbucket Cloud access token for project '%s'".formatted(PROJECT_KEY));
    verifyNoInteractions(bitbucketCloudRestClient);
  }

  @Test
  public void mint_whenTokenEndpointRejectsOAuthConsumer_shouldThrowIllegalArgumentException() {
    mockProjectAlmSetting();
    mockBitbucketCloudAlmSetting();
    BitbucketCloudException cause = new BitbucketCloudException("Error returned by Bitbucket Cloud [HTTP 400]", 400);
    when(bitbucketCloudRestClient.createToken(CLIENT_ID, DECRYPTED_CLIENT_SECRET)).thenThrow(cause);

    assertThatThrownBy(() -> underTest.mint(PROJECT_KEY))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("Cannot mint a Bitbucket Cloud access token for project '%s': invalid Bitbucket Cloud configuration".formatted(PROJECT_KEY))
      .hasCause(cause);
  }

  @Test
  public void mint_whenTokenEndpointIsRateLimited_shouldRethrowAsIs() {
    mockProjectAlmSetting();
    mockBitbucketCloudAlmSetting();
    BitbucketCloudException cause = new BitbucketCloudException("Error returned by Bitbucket Cloud [HTTP 429]", 429);
    when(bitbucketCloudRestClient.createToken(CLIENT_ID, DECRYPTED_CLIENT_SECRET)).thenThrow(cause);

    assertThatThrownBy(() -> underTest.mint(PROJECT_KEY)).isSameAs(cause);
  }

  @Test
  public void mint_whenTokenEndpointFailsWithServerError_shouldRethrowAsIs() {
    mockProjectAlmSetting();
    mockBitbucketCloudAlmSetting();
    BitbucketCloudException cause = new BitbucketCloudException("Error returned by Bitbucket Cloud [HTTP 503]", 503);
    when(bitbucketCloudRestClient.createToken(CLIENT_ID, DECRYPTED_CLIENT_SECRET)).thenThrow(cause);

    assertThatThrownBy(() -> underTest.mint(PROJECT_KEY)).isSameAs(cause);
  }

  @Test
  public void mint_whenEverythingSucceeds_shouldReturnMintedAccessTokenWithExpiry() {
    mockProjectAlmSetting();
    mockBitbucketCloudAlmSetting();
    mockToken(7200);
    when(system2.now()).thenReturn(NOW.toEpochMilli());

    Optional<ScmAccessToken> result = underTest.mint(PROJECT_KEY);

    assertThat(result).contains(new ScmAccessToken(ALM.BITBUCKET_CLOUD.getId(), "x-token-auth", ACCESS_TOKEN, "2026-11-19T12:00:00Z"));
  }

  @Test
  public void mint_whenTokenLifetimeIsUnknown_shouldReturnTokenWithoutExpiry() {
    mockProjectAlmSetting();
    mockBitbucketCloudAlmSetting();
    mockToken(0);

    Optional<ScmAccessToken> result = underTest.mint(PROJECT_KEY);

    assertThat(result).contains(new ScmAccessToken(ALM.BITBUCKET_CLOUD.getId(), "x-token-auth", ACCESS_TOKEN, null));
  }

  private void mockToken(long expiresIn) {
    Token token = mock();
    when(token.getAccessToken()).thenReturn(ACCESS_TOKEN);
    when(token.getExpiresIn()).thenReturn(expiresIn);
    when(bitbucketCloudRestClient.createToken(anyString(), anyString())).thenReturn(token);
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

  private AlmSettingDto mockBitbucketCloudAlmSetting() {
    AlmSettingDto almSetting = mock();
    when(almSetting.getAlm()).thenReturn(ALM.BITBUCKET_CLOUD);
    when(almSetting.getClientId()).thenReturn(CLIENT_ID);
    when(almSetting.getDecryptedClientSecret(encryption)).thenReturn(DECRYPTED_CLIENT_SECRET);
    when(dbClient.almSettingDao().selectByUuid(any(), eq(ALM_SETTING_UUID))).thenReturn(Optional.of(almSetting));
    return almSetting;
  }
}
