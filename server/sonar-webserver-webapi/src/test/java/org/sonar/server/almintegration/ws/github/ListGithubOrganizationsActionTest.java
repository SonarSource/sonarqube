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
package org.sonar.server.almintegration.ws.github;

import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;
import org.sonar.alm.client.github.GithubApplicationClient;
import org.sonar.alm.client.github.GithubApplicationClientImpl;
import org.sonar.alm.client.github.security.UserAccessToken;
import org.sonar.api.config.internal.Encryption;
import org.sonar.api.config.internal.Settings;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.alm.pat.AlmPatDto;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.permission.GlobalPermission;
import org.sonar.db.user.UserDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.AlmIntegrations.GithubOrganization;
import org.sonarqube.ws.AlmIntegrations.ListGithubOrganizationsWsResponse;
import org.sonarqube.ws.Common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.server.almintegration.ws.github.ListGithubOrganizationsAction.PARAM_ALM_SETTING;
import static org.sonar.server.almintegration.ws.github.ListGithubOrganizationsAction.PARAM_TOKEN;
import static org.sonar.server.tester.UserSessionRule.standalone;

public class ListGithubOrganizationsActionTest {
  private static final Encryption encryption = mock(Encryption.class);
  private static final Settings settings = mock(Settings.class);

  @Rule
  public UserSessionRule userSession = standalone();

  private final System2 system2 = mock(System2.class);
  private final GithubApplicationClientImpl appClient = mock(GithubApplicationClientImpl.class);

  @Rule
  public DbTester db = DbTester.create(system2);

  private final WsActionTester ws = new WsActionTester(new ListGithubOrganizationsAction(db.getDbClient(), settings, userSession, appClient));

  @BeforeClass
  public static void setUp() {
    when(settings.getEncryption()).thenReturn(encryption);
  }

  @Test
  public void fail_when_missing_create_project_permission() {
    TestRequest request = ws.newRequest();

    assertThatThrownBy(request::execute).isInstanceOf(UnauthorizedException.class);
  }

  @Test
  public void fail_when_almSetting_does_not_exist() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user).addPermission(GlobalPermission.PROVISION_PROJECTS);
    TestRequest request = ws.newRequest().setParam(PARAM_ALM_SETTING, "unknown");

    assertThatThrownBy(request::execute)
      .isInstanceOf(NotFoundException.class)
      .hasMessage("GitHub Setting 'unknown' not found");
  }

  @Test
  public void fail_when_unable_to_create_personal_access_token() {
    AlmSettingDto githubAlmSetting = setupAlm();
    when(appClient.createUserAccessToken(githubAlmSetting.getUrl(), githubAlmSetting.getClientId(),
      githubAlmSetting.getDecryptedClientSecret(encryption), "abc"))
      .thenThrow(IllegalStateException.class);
    TestRequest request = ws.newRequest()
      .setParam(PARAM_ALM_SETTING, githubAlmSetting.getKey())
      .setParam(PARAM_TOKEN, "abc");

    assertThatThrownBy(request::execute)
      .isInstanceOf(IllegalStateException.class)
      .hasMessage(null);
  }

  @Test
  public void fail_create_personal_access_token_because_of_invalid_settings() {
    AlmSettingDto githubAlmSetting = setupAlm();
    when(appClient.createUserAccessToken(githubAlmSetting.getUrl(), githubAlmSetting.getClientId(),
      githubAlmSetting.getDecryptedClientSecret(encryption), "abc"))
      .thenThrow(IllegalArgumentException.class);
    TestRequest request = ws.newRequest()
      .setParam(PARAM_ALM_SETTING, githubAlmSetting.getKey())
      .setParam(PARAM_TOKEN, "abc");

    assertThatThrownBy(request::execute)
      .isInstanceOf(BadRequestException.class)
      .hasMessage("Unable to authenticate with GitHub. Check the GitHub App client ID and client secret configured in the Global Settings and try again.");
  }

  @Test
  public void fail_when_personal_access_token_doesnt_exist() {
    AlmSettingDto githubAlmSetting = setupAlm();
    TestRequest request = ws.newRequest().setParam(PARAM_ALM_SETTING, githubAlmSetting.getKey());

    assertThatThrownBy(request::execute)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("No personal access token found");
  }

  @Test
  public void return_organizations_and_store_personal_access_token() {
    UserAccessToken accessToken = new UserAccessToken("token_for_abc");
    AlmSettingDto githubAlmSettings = setupAlm();
    when(encryption.isEncrypted(any())).thenReturn(false);

    when(appClient.createUserAccessToken(githubAlmSettings.getUrl(), githubAlmSettings.getClientId(),
      githubAlmSettings.getDecryptedClientSecret(encryption), "abc"))
      .thenReturn(accessToken);
    setupGhOrganizations(githubAlmSettings, accessToken.getValue());

    ListGithubOrganizationsWsResponse response = ws.newRequest()
      .setParam(PARAM_ALM_SETTING, githubAlmSettings.getKey())
      .setParam(PARAM_TOKEN, "abc")
      .executeProtobuf(ListGithubOrganizationsWsResponse.class);

    assertThat(response.getPaging())
      .extracting(Common.Paging::getPageIndex, Common.Paging::getPageSize, Common.Paging::getTotal)
      .containsOnly(1, 100, 2);
    assertThat(response.getOrganizationsList())
      .extracting(GithubOrganization::getKey, GithubOrganization::getName)
      .containsOnly(tuple("github", "github"), tuple("octacat", "octacat"));

    verify(appClient).createUserAccessToken(githubAlmSettings.getUrl(), githubAlmSettings.getClientId(),
      githubAlmSettings.getDecryptedClientSecret(encryption), "abc");
    verify(appClient).listOrganizations(githubAlmSettings.getUrl(), accessToken, 1, 100);
    Mockito.verifyNoMoreInteractions(appClient);
    assertThat(db.getDbClient().almPatDao().selectByUserAndAlmSetting(db.getSession(), userSession.getUuid(), githubAlmSettings).get().getPersonalAccessToken())
      .isEqualTo(accessToken.getValue());
  }

  @Test
  public void return_organizations_and_store_personal_access_token_with_encrypted_client_secret() {
    String decryptedSecret = "decrypted-secret";
    UserAccessToken accessToken = new UserAccessToken("token_for_abc");
    AlmSettingDto githubAlmSettings = setupAlm();
    when(encryption.isEncrypted(any())).thenReturn(true);
    when(encryption.decrypt(any())).thenReturn(decryptedSecret);

    when(appClient.createUserAccessToken(githubAlmSettings.getUrl(), githubAlmSettings.getClientId(), decryptedSecret, "abc"))
      .thenReturn(accessToken);
    setupGhOrganizations(githubAlmSettings, accessToken.getValue());

    ListGithubOrganizationsWsResponse response = ws.newRequest()
      .setParam(PARAM_ALM_SETTING, githubAlmSettings.getKey())
      .setParam(PARAM_TOKEN, "abc")
      .executeProtobuf(ListGithubOrganizationsWsResponse.class);

    assertThat(response.getPaging())
      .extracting(Common.Paging::getPageIndex, Common.Paging::getPageSize, Common.Paging::getTotal)
      .containsOnly(1, 100, 2);
    assertThat(response.getOrganizationsList())
      .extracting(GithubOrganization::getKey, GithubOrganization::getName)
      .containsOnly(tuple("github", "github"), tuple("octacat", "octacat"));

    verify(appClient).createUserAccessToken(githubAlmSettings.getUrl(), githubAlmSettings.getClientId(), decryptedSecret, "abc");
    verify(appClient).listOrganizations(githubAlmSettings.getUrl(), accessToken, 1, 100);
    Mockito.verifyNoMoreInteractions(appClient);
    assertThat(db.getDbClient().almPatDao().selectByUserAndAlmSetting(db.getSession(), userSession.getUuid(), githubAlmSettings).get().getPersonalAccessToken())
      .isEqualTo(accessToken.getValue());
  }

  @Test
  public void return_organizations_overriding_existing_personal_access_token() {

    AlmSettingDto githubAlmSettings = setupAlm();
    // old pat
    AlmPatDto pat = db.almPats().insert(p -> p.setAlmSettingUuid(githubAlmSettings.getUuid()).setUserUuid(userSession.getUuid()));

    // new pat
    UserAccessToken accessToken = new UserAccessToken("token_for_abc");
    when(appClient.createUserAccessToken(githubAlmSettings.getUrl(), githubAlmSettings.getClientId(),
      githubAlmSettings.getDecryptedClientSecret(encryption), "abc"))
      .thenReturn(accessToken);
    setupGhOrganizations(githubAlmSettings, accessToken.getValue());

    ListGithubOrganizationsWsResponse response = ws.newRequest()
      .setParam(PARAM_ALM_SETTING, githubAlmSettings.getKey())
      .setParam(PARAM_TOKEN, "abc")
      .executeProtobuf(ListGithubOrganizationsWsResponse.class);

    assertThat(response.getPaging())
      .extracting(Common.Paging::getPageIndex, Common.Paging::getPageSize, Common.Paging::getTotal)
      .containsOnly(1, 100, 2);
    assertThat(response.getOrganizationsList())
      .extracting(GithubOrganization::getKey, GithubOrganization::getName)
      .containsOnly(tuple("github", "github"), tuple("octacat", "octacat"));

    verify(appClient).createUserAccessToken(githubAlmSettings.getUrl(), githubAlmSettings.getClientId(),
      githubAlmSettings.getDecryptedClientSecret(encryption), "abc");
    verify(appClient).listOrganizations(eq(githubAlmSettings.getUrl()), argThat(token -> token.getValue().equals(accessToken.getValue())), eq(1), eq(100));
    Mockito.verifyNoMoreInteractions(appClient);
    assertThat(db.getDbClient().almPatDao().selectByUserAndAlmSetting(db.getSession(), userSession.getUuid(), githubAlmSettings).get().getPersonalAccessToken())
      .isEqualTo(accessToken.getValue());
  }

  @Test
  public void return_organizations_using_existing_personal_access_token() {
    AlmSettingDto githubAlmSettings = setupAlm();
    AlmPatDto pat = db.almPats().insert(p -> p.setAlmSettingUuid(githubAlmSettings.getUuid()).setUserUuid(userSession.getUuid()));
    setupGhOrganizations(githubAlmSettings, pat.getPersonalAccessToken());

    ListGithubOrganizationsWsResponse response = ws.newRequest()
      .setParam(PARAM_ALM_SETTING, githubAlmSettings.getKey())
      .executeProtobuf(ListGithubOrganizationsWsResponse.class);

    assertThat(response.getPaging())
      .extracting(Common.Paging::getPageIndex, Common.Paging::getPageSize, Common.Paging::getTotal)
      .containsOnly(1, 100, 2);
    assertThat(response.getOrganizationsList())
      .extracting(GithubOrganization::getKey, GithubOrganization::getName)
      .containsOnly(tuple("github", "github"), tuple("octacat", "octacat"));

    verify(appClient, never()).createUserAccessToken(any(), any(), any(), any());
    verify(appClient).listOrganizations(eq(githubAlmSettings.getUrl()), argThat(token -> token.getValue().equals(pat.getPersonalAccessToken())), eq(1), eq(100));
    Mockito.verifyNoMoreInteractions(appClient);
    assertThat(db.getDbClient().almPatDao().selectByUserAndAlmSetting(db.getSession(), userSession.getUuid(), githubAlmSettings).get().getPersonalAccessToken())
      .isEqualTo(pat.getPersonalAccessToken());
  }

  @Test
  public void definition() {
    WebService.Action def = ws.getDef();

    assertThat(def.since()).isEqualTo("8.4");
    assertThat(def.isPost()).isFalse();
    assertThat(def.isInternal()).isTrue();
    assertThat(def.responseExampleFormat()).isEqualTo("json");
    assertThat(def.params())
      .extracting(WebService.Param::key, WebService.Param::isRequired)
      .containsExactlyInAnyOrder(tuple("p", false), tuple("ps", false), tuple("almSetting", true), tuple("token", false));
  }

  private void setupGhOrganizations(AlmSettingDto almSetting, String pat) {
    when(appClient.listOrganizations(eq(almSetting.getUrl()), argThat(token -> token.getValue().equals(pat)), eq(1), eq(100)))
      .thenReturn(new GithubApplicationClient.Organizations()
        .setTotal(2)
        .setOrganizations(Stream.of("github", "octacat")
          .map(login -> new GithubApplicationClient.Organization(login.length(), login, login, null, null, null, null, "Organization"))
          .collect(Collectors.toList())));
  }

  private AlmSettingDto setupAlm() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user).addPermission(GlobalPermission.PROVISION_PROJECTS);
    return db.almSettings().insertGitHubAlmSetting(alm -> alm.setClientId("client_123").setClientSecret("client_secret_123"));
  }
}
