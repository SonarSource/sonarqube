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
package org.sonar.server.almsettings.ws;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.sonar.api.platform.Server;
import org.sonar.api.server.http.HttpRequest;
import org.sonar.api.server.http.HttpResponse;
import org.sonar.api.utils.System2;
import org.sonar.api.web.FilterChain;
import org.sonar.alm.client.github.GithubGlobalSettingsValidator;
import org.sonar.db.DbTester;
import org.sonar.db.user.UserDto;
import org.sonar.auth.github.GithubAppCredentials;
import org.sonar.auth.github.GithubApplicationClient;
import org.sonar.server.almsettings.MultipleAlmFeature;
import org.sonar.server.common.github.config.GithubConfigurationService;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.component.ComponentTypes;
import org.sonar.server.management.ManagedInstanceService;
import org.sonar.server.setting.ThreadLocalSettings;
import org.sonar.server.tester.UserSessionRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.auth.github.GitHubSettings.GITHUB_ENABLED;

public class GithubManifestCallbackFilterIT {

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create();

  private final MultipleAlmFeature multipleAlmFeature = mock(MultipleAlmFeature.class);
  private final Server server = mock(Server.class);
  private final GithubApplicationClient githubApplicationClient = mock(GithubApplicationClient.class);
  // Real service (not a mock) so the auth configuration is actually written to the DB, letting the
  // transactional rollback be observed when the DevOps binding fails.
  private final GithubConfigurationService githubConfigurationService = new GithubConfigurationService(db.getDbClient(),
    mock(ManagedInstanceService.class), mock(GithubGlobalSettingsValidator.class), mock(ThreadLocalSettings.class));
  private final GithubManifestStateStore stateStore = new GithubManifestStateStore(System2.INSTANCE);
  private final AlmSettingsSupport almSettingsSupport = new AlmSettingsSupport(db.getDbClient(), userSession,
    new ComponentFinder(db.getDbClient(), mock(ComponentTypes.class)), multipleAlmFeature);
  private final GithubAppManifestGenerator manifestGenerator = new GithubAppManifestGenerator(server);

  private final HttpRequest request = mock(HttpRequest.class);
  private final HttpResponse response = mock(HttpResponse.class);
  private final FilterChain chain = mock(FilterChain.class);

  private final GithubManifestCallbackFilter underTest = new GithubManifestCallbackFilter(db.getDbClient(), userSession,
    almSettingsSupport, manifestGenerator, stateStore, githubApplicationClient, githubConfigurationService);

  @Before
  public void setUp() {
    when(server.getPublicRootUrl()).thenReturn("https://sonarqube.example.com");
  }

  @Test
  public void doGetPattern_matchesCallbackPath() {
    assertThat(underTest.doGetPattern().matches("/github/manifest/callback")).isTrue();
  }

  @Test
  public void doFilter_whenMissingCodeOrState_redirectsToError() throws Exception {
    when(request.getParameter("code")).thenReturn(null);
    when(request.getParameter("state")).thenReturn(null);

    underTest.doFilter(request, response, chain);

    assertThat(captureRedirect()).contains("almManifestResult=error");
  }

  @Test
  public void doFilter_whenNotSystemAdministrator_redirectsToError() throws Exception {
    when(request.getParameter("code")).thenReturn("the-code");
    when(request.getParameter("state")).thenReturn("the-state");

    underTest.doFilter(request, response, chain);

    assertThat(captureRedirect()).contains("almManifestResult=error").contains("system+administrator");
  }

  @Test
  public void doFilter_whenStateIsUnknown_redirectsToError() throws Exception {
    UserDto user = db.users().insertUser();
    userSession.logIn(user).setSystemAdministrator();
    when(request.getParameter("code")).thenReturn("the-code");
    when(request.getParameter("state")).thenReturn("unknown-state");

    underTest.doFilter(request, response, chain);

    assertThat(captureRedirect()).contains("almManifestResult=error").contains("invalid+or+has+expired");
  }

  @Test
  public void doFilter_devopsHappyPath_persistsSettingAndRedirectsToInstall() throws Exception {
    UserDto user = db.users().insertUser();
    userSession.logIn(user).setSystemAdministrator();
    String state = stateStore.create("my-key", null, user.getUuid(), true, false);
    GithubAppCredentials credentials = new GithubAppCredentials(12345L, "sonarqube", "client-id", "client-secret",
      "webhook-secret", "the-pem", "https://github.com/apps/sonarqube");
    when(githubApplicationClient.convertAppManifest("https://api.github.com", "the-code")).thenReturn(credentials);
    when(request.getParameter("code")).thenReturn("the-code");
    when(request.getParameter("state")).thenReturn(state);

    underTest.doFilter(request, response, chain);

    assertThat(db.getDbClient().almSettingDao().selectByKey(db.getSession(), "my-key"))
      .isPresent()
      .hasValueSatisfying(s -> assertThat(s.getAppId()).isEqualTo("12345"));
    assertThat(captureRedirect()).isEqualTo("https://github.com/apps/sonarqube/installations/new");
  }

  @Test
  public void doFilter_whenAuthAndDevopsSucceed_persistsBoth() throws Exception {
    UserDto user = db.users().insertUser();
    userSession.logIn(user).setSystemAdministrator();
    String state = stateStore.create("my-key", "my-org", user.getUuid(), true, true);
    GithubAppCredentials credentials = new GithubAppCredentials(12345L, "sonarqube", "client-id", "client-secret",
      "webhook-secret", "the-pem", "https://github.com/apps/sonarqube");
    when(githubApplicationClient.convertAppManifest("https://api.github.com", "the-code")).thenReturn(credentials);
    when(request.getParameter("code")).thenReturn("the-code");
    when(request.getParameter("state")).thenReturn(state);

    underTest.doFilter(request, response, chain);

    assertThat(db.getDbClient().almSettingDao().selectByKey(db.getSession(), "my-key")).isPresent();
    assertThat(db.getDbClient().propertiesDao().selectGlobalProperty(db.getSession(), GITHUB_ENABLED)).isNotNull();
    assertThat(captureRedirect()).isEqualTo("https://github.com/apps/sonarqube/installations/new");
  }

  @Test
  public void doFilter_whenDevopsBindingFails_rollsBackAuthenticationConfiguration() throws Exception {
    UserDto user = db.users().insertUser();
    userSession.logIn(user).setSystemAdministrator();
    // Pre-existing setting with the same key makes the DevOps binding fail, after the auth config has
    // already been written in the same transaction.
    db.almSettings().insertGitHubAlmSetting(s -> s.setKey("my-key"));
    String state = stateStore.create("my-key", "my-org", user.getUuid(), true, true);
    GithubAppCredentials credentials = new GithubAppCredentials(12345L, "sonarqube", "client-id", "client-secret",
      "webhook-secret", "the-pem", "https://github.com/apps/sonarqube");
    when(githubApplicationClient.convertAppManifest("https://api.github.com", "the-code")).thenReturn(credentials);
    when(request.getParameter("code")).thenReturn("the-code");
    when(request.getParameter("state")).thenReturn(state);

    underTest.doFilter(request, response, chain);

    // The whole transaction is rolled back: no GitHub authentication configuration is left behind.
    assertThat(db.getDbClient().propertiesDao().selectGlobalProperty(db.getSession(), GITHUB_ENABLED)).isNull();
    assertThat(captureRedirect()).contains("almManifestResult=error");
  }

  @Test
  public void doFilter_whenAuthOnlyFlowFails_redirectsErrorToAuthenticationSettings() throws Exception {
    UserDto user = db.users().insertUser();
    userSession.logIn(user).setSystemAdministrator();
    String state = stateStore.create(null, null, user.getUuid(), false, true);
    when(githubApplicationClient.convertAppManifest("https://api.github.com", "the-code"))
      .thenThrow(new IllegalStateException("boom"));
    when(request.getParameter("code")).thenReturn("the-code");
    when(request.getParameter("state")).thenReturn(state);

    underTest.doFilter(request, response, chain);

    assertThat(captureRedirect())
      .contains("category=authentication")
      .contains("almManifestResult=error")
      .doesNotContain("category=almintegration");
  }

  private String captureRedirect() throws Exception {
    ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
    verify(response).sendRedirect(captor.capture());
    return captor.getValue();
  }
}
