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
package org.sonar.server.almsettings.ws;

import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.sonar.alm.client.azure.AzureDevOpsHttpClient;
import org.sonar.alm.client.bitbucket.bitbucketcloud.BitbucketCloudRestClient;
import org.sonar.alm.client.bitbucketserver.BitbucketServerRestClient;
import org.sonar.alm.client.gitlab.GitlabHttpClient;
import org.sonar.api.resources.ResourceTypes;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbTester;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.almintegration.validator.GithubGlobalSettingsValidator;
import org.sonar.server.almsettings.MultipleAlmFeatureProvider;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class ValidateActionTest {

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create();

  private final MultipleAlmFeatureProvider multipleAlmFeatureProvider = mock(MultipleAlmFeatureProvider.class);
  private final ComponentFinder componentFinder = new ComponentFinder(db.getDbClient(), mock(ResourceTypes.class));
  private final AlmSettingsSupport almSettingsSupport = new AlmSettingsSupport(db.getDbClient(), userSession, componentFinder, multipleAlmFeatureProvider);
  private final AzureDevOpsHttpClient azureDevOpsHttpClient = mock(AzureDevOpsHttpClient.class);
  private final GitlabHttpClient gitlabHttpClient = mock(GitlabHttpClient.class);
  private final GithubGlobalSettingsValidator githubGlobalSettingsValidator = mock(GithubGlobalSettingsValidator.class);
  private final BitbucketServerRestClient bitbucketServerRestClient = mock(BitbucketServerRestClient.class);
  private final BitbucketCloudRestClient bitbucketCloudRestClient = mock(BitbucketCloudRestClient.class);
  private final WsActionTester ws = new WsActionTester(
    new ValidateAction(db.getDbClient(), userSession, almSettingsSupport, azureDevOpsHttpClient, githubGlobalSettingsValidator, gitlabHttpClient,
      bitbucketServerRestClient, bitbucketCloudRestClient));

  @Test
  public void fail_when_key_does_not_match_existing_alm_setting() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user).setSystemAdministrator();
    TestRequest request = ws.newRequest()
      .setParam("key", "unknown");

    assertThatThrownBy(request::execute)
      .isInstanceOf(NotFoundException.class)
      .hasMessage("ALM setting with key 'unknown' cannot be found");
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

    ws.newRequest()
      .setParam("key", almSetting.getKey())
      .execute();

    verify(gitlabHttpClient).checkUrl(almSetting.getUrl());
    verify(gitlabHttpClient).checkToken(almSetting.getUrl(), almSetting.getPersonalAccessToken());
    verify(gitlabHttpClient).checkReadPermission(almSetting.getUrl(), almSetting.getPersonalAccessToken());
    verify(gitlabHttpClient).checkWritePermission(almSetting.getUrl(), almSetting.getPersonalAccessToken());
  }

  @Test
  public void github_validation_checks() {
    AlmSettingDto almSetting = insertAlmSetting(db.almSettings().insertGitHubAlmSetting(settings -> settings.setClientId("clientId")
      .setClientSecret("clientSecret")));

    ws.newRequest()
      .setParam("key", almSetting.getKey())
      .execute();

    ArgumentCaptor<AlmSettingDto> almSettingDtoArgumentCaptor = ArgumentCaptor.forClass(AlmSettingDto.class);
    verify(githubGlobalSettingsValidator).validate(almSettingDtoArgumentCaptor.capture());
    assertThat(almSettingDtoArgumentCaptor.getAllValues()).hasSize(1);
    assertThat(almSettingDtoArgumentCaptor.getValue().getClientId()).isEqualTo(almSetting.getClientId());
    assertThat(almSettingDtoArgumentCaptor.getValue().getClientSecret()).isEqualTo(almSetting.getClientSecret());
    assertThat(almSettingDtoArgumentCaptor.getValue().getAlm()).isEqualTo(almSetting.getAlm());
    assertThat(almSettingDtoArgumentCaptor.getValue().getAppId()).isEqualTo(almSetting.getAppId());
  }

  @Test
  public void bitbucketServer_validation_checks() {
    AlmSettingDto almSetting = insertAlmSetting(db.almSettings().insertBitbucketAlmSetting());

    ws.newRequest()
      .setParam("key", almSetting.getKey())
      .execute();

    verify(bitbucketServerRestClient).validateUrl(almSetting.getUrl());
    verify(bitbucketServerRestClient).validateToken(almSetting.getUrl(), almSetting.getPersonalAccessToken());
    verify(bitbucketServerRestClient).validateReadPermission(almSetting.getUrl(), almSetting.getPersonalAccessToken());
  }

  @Test
  public void azure_devops_validation_checks() {
    AlmSettingDto almSetting = insertAlmSetting(db.almSettings().insertAzureAlmSetting());

    ws.newRequest()
      .setParam("key", almSetting.getKey())
      .execute();

    verify(azureDevOpsHttpClient).checkPAT(almSetting.getUrl(), almSetting.getPersonalAccessToken());
  }

  @Test
  public void azure_devops_validation_check_fails() {
    AlmSettingDto almSetting = insertAlmSetting(db.almSettings().insertAzureAlmSetting());

    doThrow(IllegalArgumentException.class)
      .when(azureDevOpsHttpClient).checkPAT(almSetting.getUrl(), almSetting.getPersonalAccessToken());

    assertThatThrownBy(() -> ws.newRequest()
      .setParam("key", almSetting.getKey())
      .execute()).isInstanceOf(IllegalArgumentException.class).hasMessage("Invalid Azure URL or Personal Access Token");
  }

  @Test
  public void bitbucketcloud_validation_checks() {
    AlmSettingDto almSetting = insertAlmSetting(db.almSettings().insertBitbucketCloudAlmSetting());

    ws.newRequest()
      .setParam("key", almSetting.getKey())
      .execute();

    verify(bitbucketCloudRestClient).validate(almSetting.getClientId(), almSetting.getClientSecret(), almSetting.getAppId());
  }

  @Test
  public void bitbucketcloud_validation_check_fails() {
    AlmSettingDto almSetting = insertAlmSetting(db.almSettings().insertBitbucketCloudAlmSetting());

    doThrow(IllegalArgumentException.class)
      .when(bitbucketCloudRestClient).validate(almSetting.getClientId(), almSetting.getClientSecret(), almSetting.getAppId());

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
