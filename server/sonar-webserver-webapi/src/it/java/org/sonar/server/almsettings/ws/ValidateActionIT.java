/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.server.almsettings.ws;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.sonar.alm.client.azure.AzureDevOpsHttpClient;
import org.sonar.alm.client.azure.AzureDevOpsValidator;
import org.sonar.alm.client.bitbucket.bitbucketcloud.BitbucketCloudRestClient;
import org.sonar.alm.client.bitbucket.bitbucketcloud.BitbucketCloudValidator;
import org.sonar.api.config.internal.Encryption;
import org.sonar.api.config.internal.Settings;
import org.sonar.server.component.ComponentTypes;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbTester;
import org.sonar.db.alm.setting.ALM;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.user.UserDto;
import org.sonar.alm.client.bitbucketserver.BitbucketServerSettingsValidator;
import org.sonar.alm.client.github.GithubGlobalSettingsValidator;
import org.sonar.alm.client.gitlab.GitlabGlobalSettingsValidator;
import org.sonar.server.almsettings.MultipleAlmFeature;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ValidateActionIT {
  private static final Encryption encryption = mock(Encryption.class);
  private static final Settings settings = mock(Settings.class);

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create();

  private final MultipleAlmFeature multipleAlmFeature = mock(MultipleAlmFeature.class);
  private final ComponentFinder componentFinder = new ComponentFinder(db.getDbClient(), mock(ComponentTypes.class));
  private final AlmSettingsSupport almSettingsSupport = new AlmSettingsSupport(db.getDbClient(), userSession, componentFinder, multipleAlmFeature);
  private final AzureDevOpsHttpClient azureDevOpsHttpClient = mock(AzureDevOpsHttpClient.class);
  private final BitbucketCloudRestClient bitbucketCloudRestClient = mock(BitbucketCloudRestClient.class);
  private final GitlabGlobalSettingsValidator gitlabSettingsValidator = mock(GitlabGlobalSettingsValidator.class);
  private final GithubGlobalSettingsValidator githubGlobalSettingsValidator = mock(GithubGlobalSettingsValidator.class);
  private final BitbucketServerSettingsValidator bitbucketServerSettingsValidator = mock(BitbucketServerSettingsValidator.class);
  private final BitbucketCloudValidator bitbucketCloudValidator = new BitbucketCloudValidator(bitbucketCloudRestClient, settings);
  private final AzureDevOpsValidator azureDevOpsValidator = new AzureDevOpsValidator(azureDevOpsHttpClient, settings);
  private final WsActionTester ws = new WsActionTester(
    new ValidateAction(db.getDbClient(), userSession, almSettingsSupport, githubGlobalSettingsValidator,
      gitlabSettingsValidator, bitbucketServerSettingsValidator, bitbucketCloudValidator, azureDevOpsValidator));

  @BeforeClass
  public static void setUp() {
    when(settings.getEncryption()).thenReturn(encryption);
  }

  @Test
  public void fail_when_key_does_not_match_existing_alm_setting() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user).setSystemAdministrator();
    TestRequest request = ws.newRequest()
      .setParam("key", "unknown");

    assertThatThrownBy(request::execute)
      .isInstanceOf(NotFoundException.class)
      .hasMessage("DevOps Platform setting with key 'unknown' cannot be found");
  }

  @Test
  public void fail_when_missing_administer_system_permission() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user);
    TestRequest request = ws.newRequest()
      .setParam("key", "any key");

    assertThatThrownBy(request::execute).isInstanceOf(ForbiddenException.class);
  }

  @Test
  public void gitlab_validation_checks() {
    AlmSettingDto almSetting = insertAlmSetting(db.almSettings().insertGitlabAlmSetting());
    when(encryption.isEncrypted(any())).thenReturn(false);

    ws.newRequest()
      .setParam("key", almSetting.getKey())
      .execute();

    verify(gitlabSettingsValidator).validate(any(AlmSettingDto.class));
  }

  @Test
  public void github_validation_checks() {
    AlmSettingDto almSetting = insertAlmSetting(db.almSettings().insertGitHubAlmSetting(settings -> settings.setClientId("clientId")
      .setClientSecret("clientSecret")));
    when(encryption.isEncrypted(any())).thenReturn(false);

    ws.newRequest()
      .setParam("key", almSetting.getKey())
      .execute();

    ArgumentCaptor<AlmSettingDto> almSettingDtoArgumentCaptor = ArgumentCaptor.forClass(AlmSettingDto.class);
    verify(githubGlobalSettingsValidator).validate(almSettingDtoArgumentCaptor.capture());
    assertThat(almSettingDtoArgumentCaptor.getAllValues()).hasSize(1);
    assertThat(almSettingDtoArgumentCaptor.getValue().getClientId()).isEqualTo(almSetting.getClientId());
    assertThat(almSettingDtoArgumentCaptor.getValue().getDecryptedClientSecret(encryption)).isEqualTo(almSetting.getDecryptedClientSecret(encryption));
    assertThat(almSettingDtoArgumentCaptor.getValue().getAlm()).isEqualTo(almSetting.getAlm());
    assertThat(almSettingDtoArgumentCaptor.getValue().getAppId()).isEqualTo(almSetting.getAppId());
  }

  @Test
  public void github_validation_checks_with_encrypted_secret() {
    String secret = "encrypted-secret";
    String decryptedSecret = "decrypted-secret";
    AlmSettingDto almSetting = insertAlmSetting(db.almSettings().insertGitHubAlmSetting(settings -> settings.setClientId("clientId")
      .setClientSecret(secret)));
    when(encryption.isEncrypted(secret)).thenReturn(true);
    when(encryption.decrypt(secret)).thenReturn(decryptedSecret);

    ws.newRequest()
      .setParam("key", almSetting.getKey())
      .execute();

    ArgumentCaptor<AlmSettingDto> almSettingDtoArgumentCaptor = ArgumentCaptor.forClass(AlmSettingDto.class);
    verify(githubGlobalSettingsValidator).validate(almSettingDtoArgumentCaptor.capture());
    assertThat(almSettingDtoArgumentCaptor.getAllValues()).hasSize(1);
    assertThat(almSettingDtoArgumentCaptor.getValue().getClientId()).isEqualTo(almSetting.getClientId());
    assertThat(almSettingDtoArgumentCaptor.getValue().getDecryptedClientSecret(encryption)).isEqualTo(decryptedSecret);
    assertThat(almSettingDtoArgumentCaptor.getValue().getAlm()).isEqualTo(almSetting.getAlm());
    assertThat(almSettingDtoArgumentCaptor.getValue().getAppId()).isEqualTo(almSetting.getAppId());
  }

  @Test
  public void bitbucketServer_validation_checks() {
    AlmSettingDto almSetting = insertAlmSetting(db.almSettings().insertBitbucketAlmSetting());
    when(encryption.isEncrypted(any())).thenReturn(false);

    ws.newRequest()
      .setParam("key", almSetting.getKey())
      .execute();

    ArgumentCaptor<AlmSettingDto> almSettingDtoArgumentCaptor = ArgumentCaptor.forClass(AlmSettingDto.class);
    verify(bitbucketServerSettingsValidator).validate(almSettingDtoArgumentCaptor.capture());
    assertThat(almSettingDtoArgumentCaptor.getAllValues()).hasSize(1);
    assertThat(almSettingDtoArgumentCaptor.getValue().getKey()).isEqualTo(almSetting.getKey());
    assertThat(almSettingDtoArgumentCaptor.getValue().getAlm()).isEqualTo(ALM.BITBUCKET);
  }

  @Test
  public void azure_devops_validation_checks() {
    AlmSettingDto almSetting = insertAlmSetting(db.almSettings().insertAzureAlmSetting());
    when(encryption.isEncrypted(any())).thenReturn(false);

    ws.newRequest()
      .setParam("key", almSetting.getKey())
      .execute();

    verify(azureDevOpsHttpClient).checkPAT(almSetting.getUrl(), almSetting.getDecryptedPersonalAccessToken(encryption));
  }

  @Test
  public void azure_devops_validation_checks_with_encrypted_token() {
    AlmSettingDto almSetting = insertAlmSetting(db.almSettings().insertAzureAlmSetting());
    String decryptedToken = "decrypted-token";
    when(encryption.isEncrypted(any())).thenReturn(true);
    when(encryption.decrypt(any())).thenReturn(decryptedToken);

    ws.newRequest()
      .setParam("key", almSetting.getKey())
      .execute();

    verify(azureDevOpsHttpClient).checkPAT(almSetting.getUrl(), decryptedToken);
  }

  @Test
  public void azure_devops_validation_check_fails() {
    AlmSettingDto almSetting = insertAlmSetting(db.almSettings().insertAzureAlmSetting());

    doThrow(IllegalArgumentException.class)
      .when(azureDevOpsHttpClient).checkPAT(any(), any());

    assertThatThrownBy(() -> ws.newRequest()
      .setParam("key", almSetting.getKey())
      .execute()).isInstanceOf(IllegalArgumentException.class).hasMessage("Invalid Azure URL or Personal Access Token");
  }

  @Test
  public void bitbucketcloud_validation_checks() {
    AlmSettingDto almSetting = insertAlmSetting(db.almSettings().insertBitbucketCloudAlmSetting());
    when(encryption.isEncrypted(any())).thenReturn(false);

    ws.newRequest()
      .setParam("key", almSetting.getKey())
      .execute();

    verify(bitbucketCloudRestClient).validate(almSetting.getClientId(), almSetting.getDecryptedClientSecret(encryption), almSetting.getAppId());
  }

  @Test
  public void bitbucketcloud_validation_checks_with_encrypted_secret() {
    String decryptedSecret = "decrypted-secret";
    AlmSettingDto almSetting = insertAlmSetting(db.almSettings().insertBitbucketCloudAlmSetting());
    when(encryption.isEncrypted(any())).thenReturn(true);
    when(encryption.decrypt(any())).thenReturn(decryptedSecret);

    ws.newRequest()
      .setParam("key", almSetting.getKey())
      .execute();

    verify(bitbucketCloudRestClient).validate(almSetting.getClientId(), decryptedSecret, almSetting.getAppId());
  }

  @Test
  public void bitbucketcloud_validation_check_fails() {
    AlmSettingDto almSetting = insertAlmSetting(db.almSettings().insertBitbucketCloudAlmSetting());
    when(encryption.isEncrypted(any())).thenReturn(false);

    doThrow(IllegalArgumentException.class)
      .when(bitbucketCloudRestClient).validate(any(), any(), any());

    TestRequest request = ws.newRequest()
      .setParam("key", almSetting.getKey());
    assertThatThrownBy(request::execute).isInstanceOf(IllegalArgumentException.class);
  }

  private AlmSettingDto insertAlmSetting(AlmSettingDto almSettingDto) {
    UserDto user = db.users().insertUser();
    userSession.logIn(user).setSystemAdministrator();
    return almSettingDto;
  }

  @Test
  public void definition() {
    WebService.Action def = ws.getDef();

    assertThat(def.since()).isEqualTo("8.6");
    assertThat(def.isPost()).isFalse();
    assertThat(def.params())
      .extracting(WebService.Param::key, WebService.Param::isRequired)
      .containsExactlyInAnyOrder(tuple("key", true));
  }

}
