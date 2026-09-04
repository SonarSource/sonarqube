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
package org.sonar.alm.client.gitlab;

import java.util.List;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sonar.api.config.internal.Encryption;
import org.sonar.api.config.internal.Settings;
import org.sonar.auth.gitlab.GsonUser;
import org.sonar.db.alm.setting.AlmSettingDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatException;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.alm.client.gitlab.GitlabGlobalSettingsValidator.ValidationMode.AUTH_ONLY;
import static org.sonar.alm.client.gitlab.GitlabGlobalSettingsValidator.ValidationMode.COMPLETE;

public class GitlabGlobalSettingsValidatorTest {
  private static final Encryption encryption = mock(Encryption.class);
  private static final Settings settings = mock(Settings.class);
  private static final String GITLAB_API_URL = "https://gitlab.com/api";
  private static final String ACCESS_TOKEN = "access-token";
  private static final String ENCRYPTED_TOKEN = "encrypted-token";

  private final GitlabApplicationClient gitlabHttpClient = mock(GitlabApplicationClient.class);

  private final GitlabGlobalSettingsValidator underTest = new GitlabGlobalSettingsValidator(gitlabHttpClient, settings);

  @BeforeClass
  public static void setUp() {
    when(encryption.isEncrypted(ENCRYPTED_TOKEN)).thenReturn(true);
    when(encryption.decrypt(ENCRYPTED_TOKEN)).thenReturn(ACCESS_TOKEN);
    when(settings.getEncryption()).thenReturn(encryption);
  }

  @Test
  public void validate_success() {
    String token = "personal-access-token";
    AlmSettingDto almSettingDto = new AlmSettingDto()
      .setUrl(GITLAB_API_URL)
      .setPersonalAccessToken("personal-access-token");
    when(encryption.isEncrypted(token)).thenReturn(false);

    underTest.validate(almSettingDto);
    verify(gitlabHttpClient).checkUrl(almSettingDto.getUrl());
    verify(gitlabHttpClient).checkToken(almSettingDto.getUrl(), almSettingDto.getDecryptedPersonalAccessToken(encryption));
    verify(gitlabHttpClient).checkReadPermission(almSettingDto.getUrl(),
      almSettingDto.getDecryptedPersonalAccessToken(encryption));
    verify(gitlabHttpClient).checkWritePermission(almSettingDto.getUrl(), almSettingDto.getDecryptedPersonalAccessToken(encryption));
  }

  @Test
  public void validate_success_with_encrypted_token() {
    AlmSettingDto almSettingDto = new AlmSettingDto()
      .setUrl(GITLAB_API_URL)
      .setPersonalAccessToken(ENCRYPTED_TOKEN);

    underTest.validate(almSettingDto);

    verify(gitlabHttpClient).checkUrl(almSettingDto.getUrl());
    verify(gitlabHttpClient).checkToken(almSettingDto.getUrl(), ACCESS_TOKEN);
    verify(gitlabHttpClient).checkReadPermission(almSettingDto.getUrl(), ACCESS_TOKEN);
    verify(gitlabHttpClient).checkWritePermission(almSettingDto.getUrl(), ACCESS_TOKEN);
  }

  @Test
  public void validate_fail_url_not_set() {
    AlmSettingDto almSettingDto = new AlmSettingDto()
      .setUrl(null)
      .setPersonalAccessToken("personal-access-token");

    assertThatThrownBy(() -> underTest.validate(almSettingDto))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Your Gitlab global configuration is incomplete. The GitLab URL must be set.");
  }

  @Test
  public void validate_fail_pat_not_set() {
    AlmSettingDto almSettingDto = new AlmSettingDto()
      .setUrl(GITLAB_API_URL)
      .setPersonalAccessToken(null);

    assertThatThrownBy(() -> underTest.validate(almSettingDto))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Your Gitlab global configuration is incomplete. The GitLab access token must be set.");
  }

  @Test
  public void validate_forAuthOnlyWhenUrlIsNull_throwsException() {
    assertThatIllegalArgumentException()
      .isThrownBy(() -> underTest.validate(AUTH_ONLY, null, null))
      .withMessage("Your Gitlab global configuration is incomplete. The GitLab URL must be set.");
  }

  @Test
  public void validate_forAuthOnly_onlyValidatesUrl() {
    underTest.validate(AUTH_ONLY, GITLAB_API_URL, null);
    verify(gitlabHttpClient).checkUrl(GITLAB_API_URL);
  }

  @Test
  public void validate_whenCompleteMode_validatesUrl() {
    assertThatIllegalArgumentException()
      .isThrownBy(() -> underTest.validate(COMPLETE, null, null))
      .withMessage("Your Gitlab global configuration is incomplete. The GitLab URL must be set.");
  }

  @Test
  public void validate_whenCompleteMode_validatesTokenNotNull() {
    assertThatIllegalArgumentException()
      .isThrownBy(() -> underTest.validate(COMPLETE, GITLAB_API_URL, null))
      .withMessage("Your Gitlab global configuration is incomplete. The GitLab access token must be set.");
  }

  @Test
  public void validate_whenCompleteMode_validatesToken() {
    underTest.validate(COMPLETE, GITLAB_API_URL, ACCESS_TOKEN);

    verify(gitlabHttpClient).checkUrl(GITLAB_API_URL);
    verify(gitlabHttpClient).checkToken(GITLAB_API_URL, ACCESS_TOKEN);
    verify(gitlabHttpClient).checkReadPermission(GITLAB_API_URL, ACCESS_TOKEN);
    verify(gitlabHttpClient).checkWritePermission(GITLAB_API_URL, ACCESS_TOKEN);
  }
  @Test
  public void validate_whenCompleteModeAndTokenIsEncrypted_decryptAndValidatesToken() {
    underTest.validate(COMPLETE, GITLAB_API_URL, ENCRYPTED_TOKEN);

    verify(gitlabHttpClient).checkUrl(GITLAB_API_URL);
    verify(gitlabHttpClient).checkToken(GITLAB_API_URL, ACCESS_TOKEN);
    verify(gitlabHttpClient).checkReadPermission(GITLAB_API_URL, ACCESS_TOKEN);
    verify(gitlabHttpClient).checkWritePermission(GITLAB_API_URL, ACCESS_TOKEN);
  }

  @Test
  public void validate_whenCompleteModeAndError_reThrowsError() {
    IllegalStateException exception = new IllegalStateException("bla");
    doThrow(exception).when(gitlabHttpClient).checkReadPermission(GITLAB_API_URL, ACCESS_TOKEN);

    assertThatException()
      .isThrownBy(() -> underTest.validate(COMPLETE, GITLAB_API_URL, ACCESS_TOKEN))
      .isEqualTo(exception);
  }

  @Test
  public void isBotToken_whenGitlabFlagsUserAsBot_returnsTrue() {
    AlmSettingDto almSettingDto = new AlmSettingDto().setUrl(GITLAB_API_URL).setPersonalAccessToken(ACCESS_TOKEN);
    when(encryption.isEncrypted(ACCESS_TOKEN)).thenReturn(false);
    GsonUser botUser = mock();
    when(botUser.isBot()).thenReturn(true);
    when(gitlabHttpClient.checkToken(GITLAB_API_URL, ACCESS_TOKEN)).thenReturn(botUser);

    assertThat(underTest.isBotToken(almSettingDto)).isTrue();
  }

  @Test
  public void isBotToken_whenUsernameMatchesLegacyProjectBotPattern_returnsTrue() {
    AlmSettingDto almSettingDto = new AlmSettingDto().setUrl(GITLAB_API_URL).setPersonalAccessToken(ACCESS_TOKEN);
    when(encryption.isEncrypted(ACCESS_TOKEN)).thenReturn(false);
    GsonUser botUser = mock();
    when(botUser.isBot()).thenReturn(false);
    when(botUser.getUsername()).thenReturn("project_123_bot");
    when(gitlabHttpClient.checkToken(GITLAB_API_URL, ACCESS_TOKEN)).thenReturn(botUser);

    assertThat(underTest.isBotToken(almSettingDto)).isTrue();
  }

  @Test
  public void isBotToken_whenUsernameMatchesGroupBotPatternWithRandomSuffix_returnsTrue() {
    AlmSettingDto almSettingDto = new AlmSettingDto().setUrl(GITLAB_API_URL).setPersonalAccessToken(ACCESS_TOKEN);
    when(encryption.isEncrypted(ACCESS_TOKEN)).thenReturn(false);
    GsonUser botUser = mock();
    when(botUser.isBot()).thenReturn(false);
    when(botUser.getUsername()).thenReturn("group_456_bot_4ffca233d8298ea1");
    when(gitlabHttpClient.checkToken(GITLAB_API_URL, ACCESS_TOKEN)).thenReturn(botUser);

    assertThat(underTest.isBotToken(almSettingDto)).isTrue();
  }

  @Test
  public void isBotToken_whenGenuinePersonalAccessToken_returnsFalse() {
    AlmSettingDto almSettingDto = new AlmSettingDto().setUrl(GITLAB_API_URL).setPersonalAccessToken(ACCESS_TOKEN);
    when(encryption.isEncrypted(ACCESS_TOKEN)).thenReturn(false);
    GsonUser humanUser = mock();
    when(humanUser.isBot()).thenReturn(false);
    when(humanUser.getUsername()).thenReturn("jane-doe");
    when(gitlabHttpClient.checkToken(GITLAB_API_URL, ACCESS_TOKEN)).thenReturn(humanUser);

    assertThat(underTest.isBotToken(almSettingDto)).isFalse();
  }

  @Test
  public void isBotToken_whenUsernameStartsWithBotButIsHumanName_returnsFalse() {
    AlmSettingDto almSettingDto = new AlmSettingDto().setUrl(GITLAB_API_URL).setPersonalAccessToken(ACCESS_TOKEN);
    when(encryption.isEncrypted(ACCESS_TOKEN)).thenReturn(false);
    GsonUser humanUser = mock();
    when(humanUser.isBot()).thenReturn(false);
    when(humanUser.getUsername()).thenReturn("group_99_botanist");
    when(gitlabHttpClient.checkToken(GITLAB_API_URL, ACCESS_TOKEN)).thenReturn(humanUser);

    assertThat(underTest.isBotToken(almSettingDto)).isFalse();
  }

  @Test
  public void isBotToken_whenCheckTokenReturnsNull_returnsFalse() {
    AlmSettingDto almSettingDto = new AlmSettingDto().setUrl(GITLAB_API_URL).setPersonalAccessToken(ACCESS_TOKEN);
    when(encryption.isEncrypted(ACCESS_TOKEN)).thenReturn(false);
    when(gitlabHttpClient.checkToken(GITLAB_API_URL, ACCESS_TOKEN)).thenReturn(null);

    assertThat(underTest.isBotToken(almSettingDto)).isFalse();
  }

  @Test
  public void isBotToken_whenUsernameIsNull_returnsFalse() {
    AlmSettingDto almSettingDto = new AlmSettingDto().setUrl(GITLAB_API_URL).setPersonalAccessToken(ACCESS_TOKEN);
    when(encryption.isEncrypted(ACCESS_TOKEN)).thenReturn(false);
    GsonUser userWithNullUsername = mock();
    when(userWithNullUsername.isBot()).thenReturn(false);
    when(userWithNullUsername.getUsername()).thenReturn(null);
    when(gitlabHttpClient.checkToken(GITLAB_API_URL, ACCESS_TOKEN)).thenReturn(userWithNullUsername);

    assertThat(underTest.isBotToken(almSettingDto)).isFalse();
  }

  @Test
  public void isBotToken_whenUrlIsNull_returnsFalseWithoutCallingClient() {
    AlmSettingDto almSettingDto = new AlmSettingDto().setUrl(null).setPersonalAccessToken(ACCESS_TOKEN);

    assertThat(underTest.isBotToken(almSettingDto)).isFalse();
    verify(gitlabHttpClient, never()).checkToken(any(), any());
  }

  @Test
  public void isBotToken_whenTokenIsNull_returnsFalseWithoutCallingClient() {
    AlmSettingDto almSettingDto = new AlmSettingDto().setUrl(GITLAB_API_URL).setPersonalAccessToken(null);

    assertThat(underTest.isBotToken(almSettingDto)).isFalse();
    verify(gitlabHttpClient, never()).checkToken(any(), any());
  }

  @Test
  public void hasApiScope_whenTokenHasApiScope_returnsTrue() {
    AlmSettingDto almSettingDto = new AlmSettingDto().setUrl(GITLAB_API_URL).setPersonalAccessToken(ACCESS_TOKEN);
    when(encryption.isEncrypted(ACCESS_TOKEN)).thenReturn(false);
    when(gitlabHttpClient.getPersonalAccessTokenInfo(GITLAB_API_URL, ACCESS_TOKEN))
      .thenReturn(new GsonPersonalAccessTokenInfo(List.of("api")));

    assertThat(underTest.hasApiScope(almSettingDto)).isTrue();
  }

  @Test
  public void hasApiScope_whenTokenHasOnlyReadApiScope_returnsFalse() {
    AlmSettingDto almSettingDto = new AlmSettingDto().setUrl(GITLAB_API_URL).setPersonalAccessToken(ACCESS_TOKEN);
    when(encryption.isEncrypted(ACCESS_TOKEN)).thenReturn(false);
    when(gitlabHttpClient.getPersonalAccessTokenInfo(GITLAB_API_URL, ACCESS_TOKEN))
      .thenReturn(new GsonPersonalAccessTokenInfo(List.of("read_api")));

    assertThat(underTest.hasApiScope(almSettingDto)).isFalse();
  }

  @Test
  public void hasApiScope_whenTokenInfoIsNull_returnsFalse() {
    AlmSettingDto almSettingDto = new AlmSettingDto().setUrl(GITLAB_API_URL).setPersonalAccessToken(ACCESS_TOKEN);
    when(encryption.isEncrypted(ACCESS_TOKEN)).thenReturn(false);
    when(gitlabHttpClient.getPersonalAccessTokenInfo(GITLAB_API_URL, ACCESS_TOKEN)).thenReturn(null);

    assertThat(underTest.hasApiScope(almSettingDto)).isFalse();
  }

  @Test
  public void hasApiScope_whenScopesAreNull_returnsFalse() {
    AlmSettingDto almSettingDto = new AlmSettingDto().setUrl(GITLAB_API_URL).setPersonalAccessToken(ACCESS_TOKEN);
    when(encryption.isEncrypted(ACCESS_TOKEN)).thenReturn(false);
    when(gitlabHttpClient.getPersonalAccessTokenInfo(GITLAB_API_URL, ACCESS_TOKEN))
      .thenReturn(new GsonPersonalAccessTokenInfo(null));

    assertThat(underTest.hasApiScope(almSettingDto)).isFalse();
  }

  @Test
  public void hasApiScope_whenUrlIsNull_throwsWithoutCallingClient() {
    AlmSettingDto almSettingDto = new AlmSettingDto().setUrl(null).setPersonalAccessToken(ACCESS_TOKEN);

    assertThatIllegalArgumentException()
      .isThrownBy(() -> underTest.hasApiScope(almSettingDto))
      .withMessage("Your Gitlab global configuration is incomplete. The GitLab URL must be set.");
    verify(gitlabHttpClient, never()).getPersonalAccessTokenInfo(any(), any());
  }

  @Test
  public void hasApiScope_whenTokenIsNull_throwsWithoutCallingClient() {
    AlmSettingDto almSettingDto = new AlmSettingDto().setUrl(GITLAB_API_URL).setPersonalAccessToken(null);

    assertThatIllegalArgumentException()
      .isThrownBy(() -> underTest.hasApiScope(almSettingDto))
      .withMessage("Your Gitlab global configuration is incomplete. The GitLab access token must be set.");
    verify(gitlabHttpClient, never()).getPersonalAccessTokenInfo(any(), any());
  }

}
