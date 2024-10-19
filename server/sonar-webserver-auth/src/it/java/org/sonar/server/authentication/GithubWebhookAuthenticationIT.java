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
package org.sonar.server.authentication;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Optional;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;
import org.sonar.api.config.internal.Encryption;
import org.sonar.api.config.internal.Settings;
import org.sonar.api.server.http.HttpRequest;
import org.sonar.api.testfixtures.log.LogAndArguments;
import org.sonar.api.testfixtures.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.db.DbTester;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.server.authentication.event.AuthenticationEvent;
import org.sonar.server.authentication.event.AuthenticationException;
import org.springframework.lang.Nullable;

import static java.lang.String.format;
import static java.util.Objects.requireNonNullElse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.server.authentication.GithubWebhookAuthentication.GITHUB_APP_ID_HEADER;
import static org.sonar.server.authentication.GithubWebhookAuthentication.GITHUB_SIGNATURE_HEADER;
import static org.sonar.server.authentication.GithubWebhookAuthentication.MSG_AUTHENTICATION_FAILED;
import static org.sonar.server.authentication.GithubWebhookAuthentication.MSG_NO_BODY_FOUND;
import static org.sonar.server.authentication.GithubWebhookAuthentication.MSG_NO_WEBHOOK_SECRET_FOUND;
import static org.sonar.server.authentication.GithubWebhookAuthentication.MSG_UNAUTHENTICATED_GITHUB_CALLS_DENIED;

public class GithubWebhookAuthenticationIT {

  private static final String GITHUB_SIGNATURE = "sha256=1cd2d35d7bb5fc5738672bcdc959c4bc94ba308eb7008938a2f22b5713571925";
  private static final String GITHUB_PAYLOAD = "{\"action\":\"closed_by_user\",\"alert\":{\"number\":2,\"created_at\":\"2022-08-16T13:02:17Z\",\"updated_at\":\"2022-09-05T08:35:05Z\",\"url\":\"https://api.github.com/repos/aurelien-poscia-sonarsource/mmf-2821-github-scanning-alerts/code-scanning/alerts/2\",\"html_url\":\"https://github.com/aurelien-poscia-sonarsource/mmf-2821-github-scanning-alerts/security/code-scanning/2\",\"state\":\"dismissed\",\"fixed_at\":null,\"dismissed_by\":{\"login\":\"aurelien-poscia-sonarsource\",\"id\":100427063,\"node_id\":\"U_kgDOBfxlNw\",\"avatar_url\":\"https://avatars.githubusercontent.com/u/100427063?v=4\",\"gravatar_id\":\"\",\"url\":\"https://api.github.com/users/aurelien-poscia-sonarsource\",\"html_url\":\"https://github.com/aurelien-poscia-sonarsource\",\"followers_url\":\"https://api.github.com/users/aurelien-poscia-sonarsource/followers\",\"following_url\":\"https://api.github.com/users/aurelien-poscia-sonarsource/following{/other_user}\",\"gists_url\":\"https://api.github.com/users/aurelien-poscia-sonarsource/gists{/gist_id}\",\"starred_url\":\"https://api.github.com/users/aurelien-poscia-sonarsource/starred{/owner}{/repo}\",\"subscriptions_url\":\"https://api.github.com/users/aurelien-poscia-sonarsource/subscriptions\",\"organizations_url\":\"https://api.github.com/users/aurelien-poscia-sonarsource/orgs\",\"repos_url\":\"https://api.github.com/users/aurelien-poscia-sonarsource/repos\",\"events_url\":\"https://api.github.com/users/aurelien-poscia-sonarsource/events{/privacy}\",\"received_events_url\":\"https://api.github.com/users/aurelien-poscia-sonarsource/received_events\",\"type\":\"User\",\"site_admin\":false},\"dismissed_at\":\"2022-09-05T08:35:05Z\",\"dismissed_reason\":\"false positive\",\"dismissed_comment\":\"c'est pas un buuuuug\",\"rule\":{\"id\":\"js/inconsistent-use-of-new\",\"severity\":\"warning\",\"description\":\"Inconsistent use of 'new'\",\"name\":\"js/inconsistent-use-of-new\",\"tags\":[\"correctness\",\"language-features\",\"reliability\"],\"full_description\":\"If a function is intended to be a constructor, it should always be invoked with 'new'. Otherwise, it should always be invoked as a normal function, that is, without 'new'.\",\"help\":\"\",\"help_uri\":\"\"},\"tool\":{\"name\":\"CodeQL\",\"guid\":null,\"version\":\"2.0.0\"},\"most_recent_instance\":{\"ref\":\"refs/heads/main\",\"analysis_key\":\"(default)\",\"environment\":\"{}\",\"category\":\"\",\"state\":\"dismissed\",\"commit_sha\":\"b4f17b0f7612575b70e9708e0aeda7e8067e8404\",\"message\":{\"text\":\"Function resolvingPromise is sometimes invoked as a constructor (for example here), and sometimes as a normal function (for example here).\"},\"location\":{\"path\":\"src/promiseUtils.js\",\"start_line\":2,\"end_line\":2,\"start_column\":8,\"end_column\":37},\"classifications\":[]},\"instances_url\":\"https://api.github.com/repos/aurelien-poscia-sonarsource/mmf-2821-github-scanning-alerts/code-scanning/alerts/2/instances\"},\"ref\":\"\",\"commit_oid\":\"\",\"repository\":{\"id\":525365935,\"node_id\":\"R_kgDOH1Byrw\",\"name\":\"mmf-2821-github-scanning-alerts\",\"full_name\":\"aurelien-poscia-sonarsource/mmf-2821-github-scanning-alerts\",\"private\":false,\"owner\":{\"login\":\"aurelien-poscia-sonarsource\",\"id\":100427063,\"node_id\":\"U_kgDOBfxlNw\",\"avatar_url\":\"https://avatars.githubusercontent.com/u/100427063?v=4\",\"gravatar_id\":\"\",\"url\":\"https://api.github.com/users/aurelien-poscia-sonarsource\",\"html_url\":\"https://github.com/aurelien-poscia-sonarsource\",\"followers_url\":\"https://api.github.com/users/aurelien-poscia-sonarsource/followers\",\"following_url\":\"https://api.github.com/users/aurelien-poscia-sonarsource/following{/other_user}\",\"gists_url\":\"https://api.github.com/users/aurelien-poscia-sonarsource/gists{/gist_id}\",\"starred_url\":\"https://api.github.com/users/aurelien-poscia-sonarsource/starred{/owner}{/repo}\",\"subscriptions_url\":\"https://api.github.com/users/aurelien-poscia-sonarsource/subscriptions\",\"organizations_url\":\"https://api.github.com/users/aurelien-poscia-sonarsource/orgs\",\"repos_url\":\"https://api.github.com/users/aurelien-poscia-sonarsource/repos\",\"events_url\":\"https://api.github.com/users/aurelien-poscia-sonarsource/events{/privacy}\",\"received_events_url\":\"https://api.github.com/users/aurelien-poscia-sonarsource/received_events\",\"type\":\"User\",\"site_admin\":false},\"html_url\":\"https://github.com/aurelien-poscia-sonarsource/mmf-2821-github-scanning-alerts\",\"description\":null,\"fork\":false,\"url\":\"https://api.github.com/repos/aurelien-poscia-sonarsource/mmf-2821-github-scanning-alerts\",\"forks_url\":\"https://api.github.com/repos/aurelien-poscia-sonarsource/mmf-2821-github-scanning-alerts/forks\",\"keys_url\":\"https://api.github.com/repos/aurelien-poscia-sonarsource/mmf-2821-github-scanning-alerts/keys{/key_id}\",\"collaborators_url\":\"https://api.github.com/repos/aurelien-poscia-sonarsource/mmf-2821-github-scanning-alerts/collaborators{/collaborator}\",\"teams_url\":\"https://api.github.com/repos/aurelien-poscia-sonarsource/mmf-2821-github-scanning-alerts/teams\",\"hooks_url\":\"https://api.github.com/repos/aurelien-poscia-sonarsource/mmf-2821-github-scanning-alerts/hooks\",\"issue_events_url\":\"https://api.github.com/repos/aurelien-poscia-sonarsource/mmf-2821-github-scanning-alerts/issues/events{/number}\",\"events_url\":\"https://api.github.com/repos/aurelien-poscia-sonarsource/mmf-2821-github-scanning-alerts/events\",\"assignees_url\":\"https://api.github.com/repos/aurelien-poscia-sonarsource/mmf-2821-github-scanning-alerts/assignees{/user}\",\"branches_url\":\"https://api.github.com/repos/aurelien-poscia-sonarsource/mmf-2821-github-scanning-alerts/branches{/branch}\",\"tags_url\":\"https://api.github.com/repos/aurelien-poscia-sonarsource/mmf-2821-github-scanning-alerts/tags\",\"blobs_url\":\"https://api.github.com/repos/aurelien-poscia-sonarsource/mmf-2821-github-scanning-alerts/git/blobs{/sha}\",\"git_tags_url\":\"https://api.github.com/repos/aurelien-poscia-sonarsource/mmf-2821-github-scanning-alerts/git/tags{/sha}\",\"git_refs_url\":\"https://api.github.com/repos/aurelien-poscia-sonarsource/mmf-2821-github-scanning-alerts/git/refs{/sha}\",\"trees_url\":\"https://api.github.com/repos/aurelien-poscia-sonarsource/mmf-2821-github-scanning-alerts/git/trees{/sha}\",\"statuses_url\":\"https://api.github.com/repos/aurelien-poscia-sonarsource/mmf-2821-github-scanning-alerts/statuses/{sha}\",\"languages_url\":\"https://api.github.com/repos/aurelien-poscia-sonarsource/mmf-2821-github-scanning-alerts/languages\",\"stargazers_url\":\"https://api.github.com/repos/aurelien-poscia-sonarsource/mmf-2821-github-scanning-alerts/stargazers\",\"contributors_url\":\"https://api.github.com/repos/aurelien-poscia-sonarsource/mmf-2821-github-scanning-alerts/contributors\",\"subscribers_url\":\"https://api.github.com/repos/aurelien-poscia-sonarsource/mmf-2821-github-scanning-alerts/subscribers\",\"subscription_url\":\"https://api.github.com/repos/aurelien-poscia-sonarsource/mmf-2821-github-scanning-alerts/subscription\",\"commits_url\":\"https://api.github.com/repos/aurelien-poscia-sonarsource/mmf-2821-github-scanning-alerts/commits{/sha}\",\"git_commits_url\":\"https://api.github.com/repos/aurelien-poscia-sonarsource/mmf-2821-github-scanning-alerts/git/commits{/sha}\",\"comments_url\":\"https://api.github.com/repos/aurelien-poscia-sonarsource/mmf-2821-github-scanning-alerts/comments{/number}\",\"issue_comment_url\":\"https://api.github.com/repos/aurelien-poscia-sonarsource/mmf-2821-github-scanning-alerts/issues/comments{/number}\",\"contents_url\":\"https://api.github.com/repos/aurelien-poscia-sonarsource/mmf-2821-github-scanning-alerts/contents/{+path}\",\"compare_url\":\"https://api.github.com/repos/aurelien-poscia-sonarsource/mmf-2821-github-scanning-alerts/compare/{base}...{head}\",\"merges_url\":\"https://api.github.com/repos/aurelien-poscia-sonarsource/mmf-2821-github-scanning-alerts/merges\",\"archive_url\":\"https://api.github.com/repos/aurelien-poscia-sonarsource/mmf-2821-github-scanning-alerts/{archive_format}{/ref}\",\"downloads_url\":\"https://api.github.com/repos/aurelien-poscia-sonarsource/mmf-2821-github-scanning-alerts/downloads\",\"issues_url\":\"https://api.github.com/repos/aurelien-poscia-sonarsource/mmf-2821-github-scanning-alerts/issues{/number}\",\"pulls_url\":\"https://api.github.com/repos/aurelien-poscia-sonarsource/mmf-2821-github-scanning-alerts/pulls{/number}\",\"milestones_url\":\"https://api.github.com/repos/aurelien-poscia-sonarsource/mmf-2821-github-scanning-alerts/milestones{/number}\",\"notifications_url\":\"https://api.github.com/repos/aurelien-poscia-sonarsource/mmf-2821-github-scanning-alerts/notifications{?since,all,participating}\",\"labels_url\":\"https://api.github.com/repos/aurelien-poscia-sonarsource/mmf-2821-github-scanning-alerts/labels{/name}\",\"releases_url\":\"https://api.github.com/repos/aurelien-poscia-sonarsource/mmf-2821-github-scanning-alerts/releases{/id}\",\"deployments_url\":\"https://api.github.com/repos/aurelien-poscia-sonarsource/mmf-2821-github-scanning-alerts/deployments\",\"created_at\":\"2022-08-16T12:16:56Z\",\"updated_at\":\"2022-08-16T12:16:56Z\",\"pushed_at\":\"2022-08-18T10:08:48Z\",\"git_url\":\"git://github.com/aurelien-poscia-sonarsource/mmf-2821-github-scanning-alerts.git\",\"ssh_url\":\"git@github.com:aurelien-poscia-sonarsource/mmf-2821-github-scanning-alerts.git\",\"clone_url\":\"https://github.com/aurelien-poscia-sonarsource/mmf-2821-github-scanning-alerts.git\",\"svn_url\":\"https://github.com/aurelien-poscia-sonarsource/mmf-2821-github-scanning-alerts\",\"homepage\":null,\"size\":2,\"stargazers_count\":0,\"watchers_count\":0,\"language\":null,\"has_issues\":true,\"has_projects\":true,\"has_downloads\":true,\"has_wiki\":true,\"has_pages\":false,\"forks_count\":0,\"mirror_url\":null,\"archived\":false,\"disabled\":false,\"open_issues_count\":0,\"license\":null,\"allow_forking\":true,\"is_template\":false,\"web_commit_signoff_required\":false,\"topics\":[],\"visibility\":\"public\",\"forks\":0,\"open_issues\":0,\"watchers\":0,\"default_branch\":\"main\"},\"sender\":{\"login\":\"aurelien-poscia-sonarsource\",\"id\":100427063,\"node_id\":\"U_kgDOBfxlNw\",\"avatar_url\":\"https://avatars.githubusercontent.com/u/100427063?v=4\",\"gravatar_id\":\"\",\"url\":\"https://api.github.com/users/aurelien-poscia-sonarsource\",\"html_url\":\"https://github.com/aurelien-poscia-sonarsource\",\"followers_url\":\"https://api.github.com/users/aurelien-poscia-sonarsource/followers\",\"following_url\":\"https://api.github.com/users/aurelien-poscia-sonarsource/following{/other_user}\",\"gists_url\":\"https://api.github.com/users/aurelien-poscia-sonarsource/gists{/gist_id}\",\"starred_url\":\"https://api.github.com/users/aurelien-poscia-sonarsource/starred{/owner}{/repo}\",\"subscriptions_url\":\"https://api.github.com/users/aurelien-poscia-sonarsource/subscriptions\",\"organizations_url\":\"https://api.github.com/users/aurelien-poscia-sonarsource/orgs\",\"repos_url\":\"https://api.github.com/users/aurelien-poscia-sonarsource/repos\",\"events_url\":\"https://api.github.com/users/aurelien-poscia-sonarsource/events{/privacy}\",\"received_events_url\":\"https://api.github.com/users/aurelien-poscia-sonarsource/received_events\",\"type\":\"User\",\"site_admin\":false},\"installation\":{\"id\":28299870,\"node_id\":\"MDIzOkludGVncmF0aW9uSW5zdGFsbGF0aW9uMjgyOTk4NzA=\"}}";
  private static final String APP_ID = "APP_ID";
  private static final String WEBHOOK_SECRET = "toto_secret";

  @Rule
  public LogTester logTester = new LogTester();

  @Rule
  public final DbTester db = DbTester.create();

  private final AuthenticationEvent authenticationEvent = mock(AuthenticationEvent.class);

  private final Settings settings = mock(Settings.class);

  private final Encryption encryption = mock(Encryption.class);

  private GithubWebhookAuthentication githubWebhookAuthentication;

  private AlmSettingDto almSettingDto;

  @Before
  public void setUp() {
    when(settings.getEncryption()).thenReturn(encryption);
    githubWebhookAuthentication = new GithubWebhookAuthentication(authenticationEvent, db.getDbClient(), settings);
    almSettingDto = db.almSettings().insertGitHubAlmSetting(
      setting -> setting.setAppId(APP_ID),
      setting -> setting.setWebhookSecret(WEBHOOK_SECRET));
  }

  @Test
  public void authenticate_withComputedSignatureMatchingGithubSignature_returnsAuthentication() {
    HttpRequest request = mockRequest(GITHUB_PAYLOAD, GITHUB_SIGNATURE);

    Optional<UserAuthResult> authentication = githubWebhookAuthentication.authenticate(request);
    assertThat(authentication).isPresent();

    UserAuthResult userAuthResult = authentication.get();
    assertThat(userAuthResult.getUserDto()).isNull();
    assertThat(userAuthResult.getAuthType()).isEqualTo(UserAuthResult.AuthType.GITHUB_WEBHOOK);
    assertThat(userAuthResult.getTokenDto()).isNull();

    verify(authenticationEvent).loginSuccess(request, "github-webhook", AuthenticationEvent.Source.githubWebhook());
    assertThat(logTester.getLogs()).isEmpty();
  }

  @Test
  public void authenticate_withoutGithubSignatureHeader_throws() {
    HttpRequest request = mockRequest(GITHUB_PAYLOAD, null);

    String expectedMessage = format(MSG_UNAUTHENTICATED_GITHUB_CALLS_DENIED, APP_ID);
    assertThatExceptionOfType(AuthenticationException.class)
      .isThrownBy(() -> githubWebhookAuthentication.authenticate(request))
      .withMessage(expectedMessage);
    assertThat(logTester.getLogs(LoggerLevel.WARN)).extracting(LogAndArguments::getFormattedMsg).contains(expectedMessage);
  }

  @Test
  public void authenticate_withoutBody_throws() {
    HttpRequest request = mockRequest(null, GITHUB_SIGNATURE);

    assertThatExceptionOfType(AuthenticationException.class)
      .isThrownBy(() -> githubWebhookAuthentication.authenticate(request))
      .withMessage(MSG_AUTHENTICATION_FAILED);
    assertThat(logTester.getLogs(LoggerLevel.WARN)).extracting(LogAndArguments::getFormattedMsg).contains(MSG_AUTHENTICATION_FAILED);
  }

  @Test
  public void authenticate_withExceptionWhileReadingBody_throws() throws IOException {
    HttpRequest request = mockRequest(GITHUB_PAYLOAD, GITHUB_SIGNATURE);
    when(request.getReader()).thenThrow(new IOException());

    assertThatExceptionOfType(AuthenticationException.class)
      .isThrownBy(() -> githubWebhookAuthentication.authenticate(request))
      .withMessage(MSG_NO_BODY_FOUND);
    assertThat(logTester.getLogs(LoggerLevel.WARN)).extracting(LogAndArguments::getFormattedMsg).contains(MSG_NO_BODY_FOUND);
  }

  @Test
  public void authenticate_withoutAppId_returnsEmpty() {
    HttpRequest request = mockRequest(null, GITHUB_SIGNATURE);
    when(request.getHeader(GITHUB_APP_ID_HEADER)).thenReturn(null);

    assertThat(githubWebhookAuthentication.authenticate(request)).isEmpty();
    assertThat(logTester.getLogs()).isEmpty();
  }

  @Test
  public void authenticate_withWrongPayload_throws() {
    HttpRequest request = mockRequest(GITHUB_PAYLOAD + "_", GITHUB_SIGNATURE);

    assertThatExceptionOfType(AuthenticationException.class)
      .isThrownBy(() -> githubWebhookAuthentication.authenticate(request))
      .withMessage(MSG_AUTHENTICATION_FAILED);
    assertThat(logTester.getLogs(LoggerLevel.WARN)).extracting(LogAndArguments::getFormattedMsg).contains(MSG_AUTHENTICATION_FAILED);
  }

  @Test
  public void authenticate_withWrongSignature_throws() {
    HttpRequest request = mockRequest(GITHUB_PAYLOAD, GITHUB_SIGNATURE + "_");

    assertThatExceptionOfType(AuthenticationException.class)
      .isThrownBy(() -> githubWebhookAuthentication.authenticate(request))
      .withMessage(MSG_AUTHENTICATION_FAILED);
    assertThat(logTester.getLogs(LoggerLevel.WARN)).extracting(LogAndArguments::getFormattedMsg).contains(MSG_AUTHENTICATION_FAILED);
  }

  @Test
  public void authenticate_whenNoWebhookSecret_throws() {
    HttpRequest request = mockRequest(GITHUB_PAYLOAD, GITHUB_SIGNATURE);
    db.getDbClient().almSettingDao().update(db.getSession(), almSettingDto.setWebhookSecret(null), true);
    db.commit();

    String expectedMessage = format(MSG_NO_WEBHOOK_SECRET_FOUND, APP_ID);
    assertThatExceptionOfType(AuthenticationException.class)
      .isThrownBy(() -> githubWebhookAuthentication.authenticate(request))
      .withMessage(expectedMessage);
    assertThat(logTester.getLogs(LoggerLevel.WARN)).extracting(LogAndArguments::getFormattedMsg).contains(expectedMessage);
  }

  private static HttpRequest mockRequest(@Nullable String payload, @Nullable String gitHubSignature) {
    HttpRequest request = mock(HttpRequest.class, Mockito.RETURNS_DEEP_STUBS);
    try {
      StringReader stringReader = new StringReader(requireNonNullElse(payload, ""));
      BufferedReader bufferedReader = new BufferedReader(stringReader);
      when(request.getReader()).thenReturn(bufferedReader);
      when(request.getHeader(GITHUB_SIGNATURE_HEADER)).thenReturn(gitHubSignature);
      when(request.getHeader(GITHUB_APP_ID_HEADER)).thenReturn(APP_ID);
    } catch (IOException e) {
      fail("mockRequest threw an exception: " + e.getMessage());
    }
    return request;
  }

}
