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
package org.sonar.alm.client.github;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.io.IOException;
import java.util.Optional;
import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sonar.alm.client.github.config.GithubAppConfiguration;
import org.sonar.alm.client.github.security.AccessToken;
import org.sonar.alm.client.github.security.AppToken;
import org.sonar.alm.client.github.security.GithubAppSecurity;
import org.sonar.alm.client.github.security.UserAccessToken;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;

import static java.net.HttpURLConnection.HTTP_FORBIDDEN;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;
import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(DataProviderRunner.class)
public class GithubApplicationClientImplTest {

  @ClassRule
  public static LogTester logTester = new LogTester().setLevel(LoggerLevel.WARN);

  private GithubApplicationHttpClientImpl httpClient = mock(GithubApplicationHttpClientImpl.class);
  private GithubAppSecurity appSecurity = mock(GithubAppSecurity.class);
  private GithubAppConfiguration githubAppConfiguration = mock(GithubAppConfiguration.class);
  private GithubApplicationClient underTest;

  private String appUrl = "Any URL";

  @Before
  public void setup() {
    when(githubAppConfiguration.getApiEndpoint()).thenReturn(appUrl);
    underTest = new GithubApplicationClientImpl(httpClient, appSecurity);
    logTester.clear();
  }

  @Test
  @UseDataProvider("invalidApiEndpoints")
  public void checkApiEndpoint_Invalid(String url, String expectedMessage) {
    GithubAppConfiguration configuration = new GithubAppConfiguration(1L, "", url);

    assertThatThrownBy(() -> underTest.checkApiEndpoint(configuration))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage(expectedMessage);
  }

  @DataProvider
  public static Object[][] invalidApiEndpoints() {
    return new Object[][] {
      {"", "Missing URL"},
      {"ftp://api.github.com", "Only http and https schemes are supported"},
      {"https://github.com", "Invalid GitHub URL"}
    };
  }

  @Test
  @UseDataProvider("validApiEndpoints")
  public void checkApiEndpoint(String url) {
    GithubAppConfiguration configuration = new GithubAppConfiguration(1L, "", url);

    assertThatCode(() -> underTest.checkApiEndpoint(configuration)).isNull();
  }

  @DataProvider
  public static Object[][] validApiEndpoints() {
    return new Object[][] {
      {"https://github.sonarsource.com/api/v3"},
      {"https://api.github.com"},
      {"https://github.sonarsource.com/api/v3/"},
      {"https://api.github.com/"},
      {"HTTPS://api.github.com/"},
      {"HTTP://api.github.com/"},
      {"HtTpS://github.SonarSource.com/api/v3"},
      {"HtTpS://github.sonarsource.com/api/V3"},
      {"HtTpS://github.sonarsource.COM/ApI/v3"}
    };
  }

  @Test
  public void checkAppPermissions_IOException() throws IOException {
    AppToken appToken = mockAppToken();

    when(httpClient.get(appUrl, appToken, "/app")).thenThrow(new IOException("OOPS"));

    assertThatThrownBy(() -> underTest.checkAppPermissions(githubAppConfiguration))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Failed to validate configuration, check URL and Private Key");
  }

  @Test
  @UseDataProvider("checkAppPermissionsErrorCodes")
  public void checkAppPermissions_ErrorCodes(int errorCode, String expectedMessage) throws IOException {
    AppToken appToken = mockAppToken();

    when(httpClient.get(appUrl, appToken, "/app")).thenReturn(new ErrorGetResponse(errorCode, null));

    assertThatThrownBy(() -> underTest.checkAppPermissions(githubAppConfiguration))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage(expectedMessage);
  }

  @DataProvider
  public static Object[][] checkAppPermissionsErrorCodes() {
    return new Object[][] {
      {HTTP_UNAUTHORIZED, "Authentication failed, verify the Client Id, Client Secret and Private Key fields"},
      {HTTP_FORBIDDEN, "Authentication failed, verify the Client Id, Client Secret and Private Key fields"},
      {HTTP_NOT_FOUND, "Failed to check permissions with Github, check the configuration"}
    };
  }

  @Test
  public void checkAppPermissions_MissingPermissions() throws IOException {
    AppToken appToken = mockAppToken();

    when(httpClient.get(appUrl, appToken, "/app")).thenReturn(new OkGetResponse("{}"));

    assertThatThrownBy(() -> underTest.checkAppPermissions(githubAppConfiguration))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Failed to get app permissions, unexpected response body");
  }

  @Test
  public void checkAppPermissions_IncorrectPermissions() throws IOException {
    AppToken appToken = mockAppToken();

    String json = "{"
      + "      \"permissions\": {\n"
      + "        \"checks\": \"read\",\n"
      + "        \"metadata\": \"read\",\n"
      + "        \"statuses\": \"read\",\n"
      + "        \"pull_requests\": \"read\"\n"
      + "      }\n"
      + "}";

    when(httpClient.get(appUrl, appToken, "/app")).thenReturn(new OkGetResponse(json));

    assertThatThrownBy(() -> underTest.checkAppPermissions(githubAppConfiguration))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Missing permissions; permission granted on pull_requests is 'read', should be 'write', checks is 'read', should be 'write'");
  }

  @Test
  public void checkAppPermissions() throws IOException {
    AppToken appToken = mockAppToken();

    String json = "{"
      + "      \"permissions\": {\n"
      + "        \"checks\": \"write\",\n"
      + "        \"metadata\": \"read\",\n"
      + "        \"statuses\": \"read\",\n"
      + "        \"pull_requests\": \"write\"\n"
      + "      }\n"
      + "}";

    when(httpClient.get(appUrl, appToken, "/app")).thenReturn(new OkGetResponse(json));

    assertThatCode(() -> underTest.checkAppPermissions(githubAppConfiguration)).isNull();
  }

  @Test
  @UseDataProvider("githubServers")
  public void createUserAccessToken_returns_empty_if_access_token_cant_be_created(String apiUrl, String appUrl) throws IOException {
    when(httpClient.post(appUrl, null, "/login/oauth/access_token?client_id=clientId&client_secret=clientSecret&code=code"))
      .thenReturn(new Response(400, null));

    assertThatThrownBy(() -> underTest.createUserAccessToken(appUrl, "clientId", "clientSecret", "code"))
      .isInstanceOf(IllegalStateException.class);
    verify(httpClient).post(appUrl, null, "/login/oauth/access_token?client_id=clientId&client_secret=clientSecret&code=code");
  }

  @Test
  @UseDataProvider("githubServers")
  public void createUserAccessToken_fail_if_access_token_request_fails(String apiUrl, String appUrl) throws IOException {
    when(httpClient.post(appUrl, null, "/login/oauth/access_token?client_id=clientId&client_secret=clientSecret&code=code"))
      .thenThrow(new IOException("OOPS"));

    assertThatThrownBy(() -> underTest.createUserAccessToken(apiUrl, "clientId", "clientSecret", "code"))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Failed to create GitHub's user access token");

    verify(httpClient).post(appUrl, null, "/login/oauth/access_token?client_id=clientId&client_secret=clientSecret&code=code");
  }

  @Test
  @UseDataProvider("githubServers")
  public void createUserAccessToken_throws_illegal_argument_exception_if_access_token_code_is_expired(String apiUrl, String appUrl) throws IOException {
    when(httpClient.post(appUrl, null, "/login/oauth/access_token?client_id=clientId&client_secret=clientSecret&code=code"))
      .thenReturn(new OkGetResponse("error_code=100&error=expired_or_invalid"));

    assertThatThrownBy(() -> underTest.createUserAccessToken(apiUrl, "clientId", "clientSecret", "code"))
      .isInstanceOf(IllegalArgumentException.class);

    verify(httpClient).post(appUrl, null, "/login/oauth/access_token?client_id=clientId&client_secret=clientSecret&code=code");
  }

  @Test
  @UseDataProvider("githubServers")
  public void createUserAccessToken_from_authorization_code_returns_access_token(String apiUrl, String appUrl) throws IOException {
    String token = randomAlphanumeric(10);
    when(httpClient.post(appUrl, null, "/login/oauth/access_token?client_id=clientId&client_secret=clientSecret&code=code"))
      .thenReturn(new OkGetResponse("access_token=" + token + "&status="));

    UserAccessToken userAccessToken = underTest.createUserAccessToken(apiUrl, "clientId", "clientSecret", "code");

    assertThat(userAccessToken)
      .extracting(UserAccessToken::getValue, UserAccessToken::getAuthorizationHeaderPrefix)
      .containsOnly(token, "token");
    verify(httpClient).post(appUrl, null, "/login/oauth/access_token?client_id=clientId&client_secret=clientSecret&code=code");
  }

  @DataProvider
  public static Object[][] githubServers() {
    return new Object[][] {
      {"https://github.sonarsource.com/api/v3", "https://github.sonarsource.com"},
      {"https://api.github.com", "https://github.com"},
      {"https://github.sonarsource.com/api/v3/", "https://github.sonarsource.com"},
      {"https://api.github.com/", "https://github.com"},
    };
  }

  @Test
  public void listOrganizations_fail_on_failure() throws IOException {
    String appUrl = "https://github.sonarsource.com";
    AccessToken accessToken = new UserAccessToken(randomAlphanumeric(10));

    when(httpClient.get(appUrl, accessToken, String.format("/user/installations?page=%s&per_page=%s", 1, 100)))
      .thenThrow(new IOException("OOPS"));

    assertThatThrownBy(() -> underTest.listOrganizations(appUrl, accessToken, 1, 100))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Failed to list all organizations accessible by user access token on %s", appUrl);
  }

  @Test
  public void listOrganizations_fail_if_pageIndex_out_of_bounds() {
    UserAccessToken token = new UserAccessToken("token");
    assertThatThrownBy(() -> underTest.listOrganizations(appUrl, token, 0, 100))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("'page' must be larger than 0.");
  }

  @Test
  public void listOrganizations_fail_if_pageSize_out_of_bounds() {
    UserAccessToken token = new UserAccessToken("token");
    assertThatThrownBy(() -> underTest.listOrganizations(appUrl, token, 1, 0))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("'pageSize' must be a value larger than 0 and smaller or equal to 100.");
    assertThatThrownBy(() -> underTest.listOrganizations("", token, 1, 101))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("'pageSize' must be a value larger than 0 and smaller or equal to 100.");
  }

  @Test
  public void listOrganizations_returns_no_installations() throws IOException {
    String appUrl = "https://github.sonarsource.com";
    AccessToken accessToken = new UserAccessToken(randomAlphanumeric(10));
    String responseJson = "{\n"
      + "  \"total_count\": 0\n"
      + "} ";

    when(httpClient.get(appUrl, accessToken, String.format("/user/installations?page=%s&per_page=%s", 1, 100)))
      .thenReturn(new OkGetResponse(responseJson));

    GithubApplicationClient.Organizations organizations = underTest.listOrganizations(appUrl, accessToken, 1, 100);

    assertThat(organizations.getTotal()).isZero();
    assertThat(organizations.getOrganizations()).isNull();
  }

  @Test
  public void listOrganizations_returns_pages_results() throws IOException {
    String appUrl = "https://github.sonarsource.com";
    AccessToken accessToken = new UserAccessToken(randomAlphanumeric(10));
    String responseJson = "{\n"
      + "  \"total_count\": 2,\n"
      + "  \"installations\": [\n"
      + "    {\n"
      + "      \"id\": 1,\n"
      + "      \"account\": {\n"
      + "        \"login\": \"github\",\n"
      + "        \"id\": 1,\n"
      + "        \"node_id\": \"MDEyOk9yZ2FuaXphdGlvbjE=\",\n"
      + "        \"url\": \"https://github.sonarsource.com/api/v3/orgs/github\",\n"
      + "        \"repos_url\": \"https://github.sonarsource.com/api/v3/orgs/github/repos\",\n"
      + "        \"events_url\": \"https://github.sonarsource.com/api/v3/orgs/github/events\",\n"
      + "        \"hooks_url\": \"https://github.sonarsource.com/api/v3/orgs/github/hooks\",\n"
      + "        \"issues_url\": \"https://github.sonarsource.com/api/v3/orgs/github/issues\",\n"
      + "        \"members_url\": \"https://github.sonarsource.com/api/v3/orgs/github/members{/member}\",\n"
      + "        \"public_members_url\": \"https://github.sonarsource.com/api/v3/orgs/github/public_members{/member}\",\n"
      + "        \"avatar_url\": \"https://github.com/images/error/octocat_happy.gif\",\n"
      + "        \"description\": \"A great organization\"\n"
      + "      },\n"
      + "      \"access_tokens_url\": \"https://github.sonarsource.com/api/v3/app/installations/1/access_tokens\",\n"
      + "      \"repositories_url\": \"https://github.sonarsource.com/api/v3/installation/repositories\",\n"
      + "      \"html_url\": \"https://github.com/organizations/github/settings/installations/1\",\n"
      + "      \"app_id\": 1,\n"
      + "      \"target_id\": 1,\n"
      + "      \"target_type\": \"Organization\",\n"
      + "      \"permissions\": {\n"
      + "        \"checks\": \"write\",\n"
      + "        \"metadata\": \"read\",\n"
      + "        \"contents\": \"read\"\n"
      + "      },\n"
      + "      \"events\": [\n"
      + "        \"push\",\n"
      + "        \"pull_request\"\n"
      + "      ],\n"
      + "      \"single_file_name\": \"config.yml\"\n"
      + "    },\n"
      + "    {\n"
      + "      \"id\": 3,\n"
      + "      \"account\": {\n"
      + "        \"login\": \"octocat\",\n"
      + "        \"id\": 2,\n"
      + "        \"node_id\": \"MDQ6VXNlcjE=\",\n"
      + "        \"avatar_url\": \"https://github.com/images/error/octocat_happy.gif\",\n"
      + "        \"gravatar_id\": \"\",\n"
      + "        \"url\": \"https://github.sonarsource.com/api/v3/users/octocat\",\n"
      + "        \"html_url\": \"https://github.com/octocat\",\n"
      + "        \"followers_url\": \"https://github.sonarsource.com/api/v3/users/octocat/followers\",\n"
      + "        \"following_url\": \"https://github.sonarsource.com/api/v3/users/octocat/following{/other_user}\",\n"
      + "        \"gists_url\": \"https://github.sonarsource.com/api/v3/users/octocat/gists{/gist_id}\",\n"
      + "        \"starred_url\": \"https://github.sonarsource.com/api/v3/users/octocat/starred{/owner}{/repo}\",\n"
      + "        \"subscriptions_url\": \"https://github.sonarsource.com/api/v3/users/octocat/subscriptions\",\n"
      + "        \"organizations_url\": \"https://github.sonarsource.com/api/v3/users/octocat/orgs\",\n"
      + "        \"repos_url\": \"https://github.sonarsource.com/api/v3/users/octocat/repos\",\n"
      + "        \"events_url\": \"https://github.sonarsource.com/api/v3/users/octocat/events{/privacy}\",\n"
      + "        \"received_events_url\": \"https://github.sonarsource.com/api/v3/users/octocat/received_events\",\n"
      + "        \"type\": \"User\",\n"
      + "        \"site_admin\": false\n"
      + "      },\n"
      + "      \"access_tokens_url\": \"https://github.sonarsource.com/api/v3/app/installations/1/access_tokens\",\n"
      + "      \"repositories_url\": \"https://github.sonarsource.com/api/v3/installation/repositories\",\n"
      + "      \"html_url\": \"https://github.com/organizations/github/settings/installations/1\",\n"
      + "      \"app_id\": 1,\n"
      + "      \"target_id\": 1,\n"
      + "      \"target_type\": \"Organization\",\n"
      + "      \"permissions\": {\n"
      + "        \"checks\": \"write\",\n"
      + "        \"metadata\": \"read\",\n"
      + "        \"contents\": \"read\"\n"
      + "      },\n"
      + "      \"events\": [\n"
      + "        \"push\",\n"
      + "        \"pull_request\"\n"
      + "      ],\n"
      + "      \"single_file_name\": \"config.yml\"\n"
      + "    }\n"
      + "  ]\n"
      + "} ";

    when(httpClient.get(appUrl, accessToken, String.format("/user/installations?page=%s&per_page=%s", 1, 100)))
      .thenReturn(new OkGetResponse(responseJson));

    GithubApplicationClient.Organizations organizations = underTest.listOrganizations(appUrl, accessToken, 1, 100);

    assertThat(organizations.getTotal()).isEqualTo(2);
    assertThat(organizations.getOrganizations()).extracting(GithubApplicationClient.Organization::getLogin).containsOnly("github", "octocat");
  }

  @Test
  public void listRepositories_fail_on_failure() throws IOException {
    String appUrl = "https://github.sonarsource.com";
    AccessToken accessToken = new UserAccessToken(randomAlphanumeric(10));

    when(httpClient.get(appUrl, accessToken, String.format("/search/repositories?q=%s&page=%s&per_page=%s", "org:test", 1, 100)))
      .thenThrow(new IOException("OOPS"));

    assertThatThrownBy(() -> underTest.listRepositories(appUrl, accessToken, "test", null, 1, 100))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Failed to list all repositories of 'test' accessible by user access token on 'https://github.sonarsource.com' using query 'fork:true+org:test'");
  }

  @Test
  public void listRepositories_fail_if_pageIndex_out_of_bounds() {
    UserAccessToken token = new UserAccessToken("token");
    assertThatThrownBy(() -> underTest.listRepositories(appUrl, token, "test", null, 0, 100))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("'page' must be larger than 0.");
  }

  @Test
  public void listRepositories_fail_if_pageSize_out_of_bounds() {
    UserAccessToken token = new UserAccessToken("token");
    assertThatThrownBy(() -> underTest.listRepositories(appUrl, token, "test", null, 1, 0))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("'pageSize' must be a value larger than 0 and smaller or equal to 100.");
    assertThatThrownBy(() -> underTest.listRepositories("", token, "test", null, 1, 101))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("'pageSize' must be a value larger than 0 and smaller or equal to 100.");
  }

  @Test
  public void listRepositories_returns_empty_results() throws IOException {
    String appUrl = "https://github.sonarsource.com";
    AccessToken accessToken = new UserAccessToken(randomAlphanumeric(10));
    String responseJson = "{\n"
      + "  \"total_count\": 0\n"
      + "}";

    when(httpClient.get(appUrl, accessToken, String.format("/search/repositories?q=%s&page=%s&per_page=%s", "fork:true+org:github", 1, 100)))
      .thenReturn(new OkGetResponse(responseJson));

    GithubApplicationClient.Repositories repositories = underTest.listRepositories(appUrl, accessToken, "github", null, 1, 100);

    assertThat(repositories.getTotal()).isZero();
    assertThat(repositories.getRepositories()).isNull();
  }

  @Test
  public void listRepositories_returns_pages_results() throws IOException {
    String appUrl = "https://github.sonarsource.com";
    AccessToken accessToken = new UserAccessToken(randomAlphanumeric(10));
    String responseJson = "{\n"
      + "  \"total_count\": 2,\n"
      + "  \"incomplete_results\": false,\n"
      + "  \"items\": [\n"
      + "    {\n"
      + "      \"id\": 3081286,\n"
      + "      \"node_id\": \"MDEwOlJlcG9zaXRvcnkzMDgxMjg2\",\n"
      + "      \"name\": \"HelloWorld\",\n"
      + "      \"full_name\": \"github/HelloWorld\",\n"
      + "      \"owner\": {\n"
      + "        \"login\": \"github\",\n"
      + "        \"id\": 872147,\n"
      + "        \"node_id\": \"MDQ6VXNlcjg3MjE0Nw==\",\n"
      + "        \"avatar_url\": \"https://github.sonarsource.com/images/error/octocat_happy.gif\",\n"
      + "        \"gravatar_id\": \"\",\n"
      + "        \"url\": \"https://github.sonarsource.com/api/v3/users/github\",\n"
      + "        \"received_events_url\": \"https://github.sonarsource.com/api/v3/users/github/received_events\",\n"
      + "        \"type\": \"User\"\n"
      + "      },\n"
      + "      \"private\": false,\n"
      + "      \"html_url\": \"https://github.com/github/HelloWorld\",\n"
      + "      \"description\": \"A C implementation of HelloWorld\",\n"
      + "      \"fork\": false,\n"
      + "      \"url\": \"https://github.sonarsource.com/api/v3/repos/github/HelloWorld\",\n"
      + "      \"created_at\": \"2012-01-01T00:31:50Z\",\n"
      + "      \"updated_at\": \"2013-01-05T17:58:47Z\",\n"
      + "      \"pushed_at\": \"2012-01-01T00:37:02Z\",\n"
      + "      \"homepage\": \"\",\n"
      + "      \"size\": 524,\n"
      + "      \"stargazers_count\": 1,\n"
      + "      \"watchers_count\": 1,\n"
      + "      \"language\": \"Assembly\",\n"
      + "      \"forks_count\": 0,\n"
      + "      \"open_issues_count\": 0,\n"
      + "      \"master_branch\": \"master\",\n"
      + "      \"default_branch\": \"master\",\n"
      + "      \"score\": 1.0\n"
      + "    },\n"
      + "    {\n"
      + "      \"id\": 3081286,\n"
      + "      \"node_id\": \"MDEwOlJlcG9zaXRvcnkzMDgxMjg2\",\n"
      + "      \"name\": \"HelloUniverse\",\n"
      + "      \"full_name\": \"github/HelloUniverse\",\n"
      + "      \"owner\": {\n"
      + "        \"login\": \"github\",\n"
      + "        \"id\": 872147,\n"
      + "        \"node_id\": \"MDQ6VXNlcjg3MjE0Nw==\",\n"
      + "        \"avatar_url\": \"https://github.sonarsource.com/images/error/octocat_happy.gif\",\n"
      + "        \"gravatar_id\": \"\",\n"
      + "        \"url\": \"https://github.sonarsource.com/api/v3/users/github\",\n"
      + "        \"received_events_url\": \"https://github.sonarsource.com/api/v3/users/github/received_events\",\n"
      + "        \"type\": \"User\"\n"
      + "      },\n"
      + "      \"private\": false,\n"
      + "      \"html_url\": \"https://github.com/github/HelloUniverse\",\n"
      + "      \"description\": \"A C implementation of HelloUniverse\",\n"
      + "      \"fork\": false,\n"
      + "      \"url\": \"https://github.sonarsource.com/api/v3/repos/github/HelloUniverse\",\n"
      + "      \"created_at\": \"2012-01-01T00:31:50Z\",\n"
      + "      \"updated_at\": \"2013-01-05T17:58:47Z\",\n"
      + "      \"pushed_at\": \"2012-01-01T00:37:02Z\",\n"
      + "      \"homepage\": \"\",\n"
      + "      \"size\": 524,\n"
      + "      \"stargazers_count\": 1,\n"
      + "      \"watchers_count\": 1,\n"
      + "      \"language\": \"Assembly\",\n"
      + "      \"forks_count\": 0,\n"
      + "      \"open_issues_count\": 0,\n"
      + "      \"master_branch\": \"master\",\n"
      + "      \"default_branch\": \"master\",\n"
      + "      \"score\": 1.0\n"
      + "    }\n"
      + "  ]\n"
      + "}";

    when(httpClient.get(appUrl, accessToken, String.format("/search/repositories?q=%s&page=%s&per_page=%s", "fork:true+org:github", 1, 100)))
      .thenReturn(new OkGetResponse(responseJson));
    GithubApplicationClient.Repositories repositories = underTest.listRepositories(appUrl, accessToken, "github", null, 1, 100);

    assertThat(repositories.getTotal()).isEqualTo(2);
    assertThat(repositories.getRepositories())
      .extracting(GithubApplicationClient.Repository::getName, GithubApplicationClient.Repository::getFullName)
      .containsOnly(tuple("HelloWorld", "github/HelloWorld"), tuple("HelloUniverse", "github/HelloUniverse"));
  }

  @Test
  public void listRepositories_returns_search_results() throws IOException {
    String appUrl = "https://github.sonarsource.com";
    AccessToken accessToken = new UserAccessToken(randomAlphanumeric(10));
    String responseJson = "{\n"
      + "  \"total_count\": 2,\n"
      + "  \"incomplete_results\": false,\n"
      + "  \"items\": [\n"
      + "    {\n"
      + "      \"id\": 3081286,\n"
      + "      \"node_id\": \"MDEwOlJlcG9zaXRvcnkzMDgxMjg2\",\n"
      + "      \"name\": \"HelloWorld\",\n"
      + "      \"full_name\": \"github/HelloWorld\",\n"
      + "      \"owner\": {\n"
      + "        \"login\": \"github\",\n"
      + "        \"id\": 872147,\n"
      + "        \"node_id\": \"MDQ6VXNlcjg3MjE0Nw==\",\n"
      + "        \"avatar_url\": \"https://github.sonarsource.com/images/error/octocat_happy.gif\",\n"
      + "        \"gravatar_id\": \"\",\n"
      + "        \"url\": \"https://github.sonarsource.com/api/v3/users/github\",\n"
      + "        \"received_events_url\": \"https://github.sonarsource.com/api/v3/users/github/received_events\",\n"
      + "        \"type\": \"User\"\n"
      + "      },\n"
      + "      \"private\": false,\n"
      + "      \"html_url\": \"https://github.com/github/HelloWorld\",\n"
      + "      \"description\": \"A C implementation of HelloWorld\",\n"
      + "      \"fork\": false,\n"
      + "      \"url\": \"https://github.sonarsource.com/api/v3/repos/github/HelloWorld\",\n"
      + "      \"created_at\": \"2012-01-01T00:31:50Z\",\n"
      + "      \"updated_at\": \"2013-01-05T17:58:47Z\",\n"
      + "      \"pushed_at\": \"2012-01-01T00:37:02Z\",\n"
      + "      \"homepage\": \"\",\n"
      + "      \"size\": 524,\n"
      + "      \"stargazers_count\": 1,\n"
      + "      \"watchers_count\": 1,\n"
      + "      \"language\": \"Assembly\",\n"
      + "      \"forks_count\": 0,\n"
      + "      \"open_issues_count\": 0,\n"
      + "      \"master_branch\": \"master\",\n"
      + "      \"default_branch\": \"master\",\n"
      + "      \"score\": 1.0\n"
      + "    }\n"
      + "  ]\n"
      + "}";

    when(httpClient.get(appUrl, accessToken, String.format("/search/repositories?q=%s&page=%s&per_page=%s", "world+fork:true+org:github", 1, 100)))
      .thenReturn(new GithubApplicationHttpClient.GetResponse() {
        @Override
        public Optional<String> getNextEndPoint() {
          return Optional.empty();
        }

        @Override
        public int getCode() {
          return 200;
        }

        @Override
        public Optional<String> getContent() {
          return Optional.of(responseJson);
        }
      });

    GithubApplicationClient.Repositories repositories = underTest.listRepositories(appUrl, accessToken, "github", "world", 1, 100);

    assertThat(repositories.getTotal()).isEqualTo(2);
    assertThat(repositories.getRepositories())
      .extracting(GithubApplicationClient.Repository::getName, GithubApplicationClient.Repository::getFullName)
      .containsOnly(tuple("HelloWorld", "github/HelloWorld"));
  }

  @Test
  public void getRepository_returns_empty_when_repository_doesnt_exist() throws IOException {
    when(httpClient.get(any(), any(), any()))
      .thenReturn(new Response(404, null));

    Optional<GithubApplicationClient.Repository> repository = underTest.getRepository(appUrl, new UserAccessToken("temp"), "octocat", "octocat/Hello-World");

    assertThat(repository).isEmpty();
  }

  @Test
  public void getRepository_fails_on_failure() throws IOException {
    String repositoryKey = "octocat/Hello-World";
    String organization = "octocat";

    when(httpClient.get(any(), any(), any()))
      .thenThrow(new IOException("OOPS"));

    UserAccessToken token = new UserAccessToken("temp");
    assertThatThrownBy(() -> underTest.getRepository(appUrl, token, organization, repositoryKey))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Failed to get repository '%s' of '%s' accessible by user access token on '%s'", repositoryKey, organization, appUrl);
  }

  @Test
  public void getRepository_returns_repository() throws IOException {
    String appUrl = "https://github.sonarsource.com";
    AccessToken accessToken = new UserAccessToken(randomAlphanumeric(10));
    String responseJson = "{\n"
      + "  \"id\": 1296269,\n"
      + "  \"node_id\": \"MDEwOlJlcG9zaXRvcnkxMjk2MjY5\",\n"
      + "  \"name\": \"Hello-World\",\n"
      + "  \"full_name\": \"octocat/Hello-World\",\n"
      + "  \"owner\": {\n"
      + "    \"login\": \"octocat\",\n"
      + "    \"id\": 1,\n"
      + "    \"node_id\": \"MDQ6VXNlcjE=\",\n"
      + "    \"avatar_url\": \"https://github.sonarsource.com/images/error/octocat_happy.gif\",\n"
      + "    \"gravatar_id\": \"\",\n"
      + "    \"url\": \"https://github.sonarsource.com/api/v3/users/octocat\",\n"
      + "    \"html_url\": \"https://github.com/octocat\",\n"
      + "    \"followers_url\": \"https://github.sonarsource.com/api/v3/users/octocat/followers\",\n"
      + "    \"following_url\": \"https://github.sonarsource.com/api/v3/users/octocat/following{/other_user}\",\n"
      + "    \"gists_url\": \"https://github.sonarsource.com/api/v3/users/octocat/gists{/gist_id}\",\n"
      + "    \"starred_url\": \"https://github.sonarsource.com/api/v3/users/octocat/starred{/owner}{/repo}\",\n"
      + "    \"subscriptions_url\": \"https://github.sonarsource.com/api/v3/users/octocat/subscriptions\",\n"
      + "    \"organizations_url\": \"https://github.sonarsource.com/api/v3/users/octocat/orgs\",\n"
      + "    \"repos_url\": \"https://github.sonarsource.com/api/v3/users/octocat/repos\",\n"
      + "    \"events_url\": \"https://github.sonarsource.com/api/v3/users/octocat/events{/privacy}\",\n"
      + "    \"received_events_url\": \"https://github.sonarsource.com/api/v3/users/octocat/received_events\",\n"
      + "    \"type\": \"User\",\n"
      + "    \"site_admin\": false\n"
      + "  },\n"
      + "  \"private\": false,\n"
      + "  \"html_url\": \"https://github.com/octocat/Hello-World\",\n"
      + "  \"description\": \"This your first repo!\",\n"
      + "  \"fork\": false,\n"
      + "  \"url\": \"https://github.sonarsource.com/api/v3/repos/octocat/Hello-World\",\n"
      + "  \"archive_url\": \"https://github.sonarsource.com/api/v3/repos/octocat/Hello-World/{archive_format}{/ref}\",\n"
      + "  \"assignees_url\": \"https://github.sonarsource.com/api/v3/repos/octocat/Hello-World/assignees{/user}\",\n"
      + "  \"blobs_url\": \"https://github.sonarsource.com/api/v3/repos/octocat/Hello-World/git/blobs{/sha}\",\n"
      + "  \"branches_url\": \"https://github.sonarsource.com/api/v3/repos/octocat/Hello-World/branches{/branch}\",\n"
      + "  \"collaborators_url\": \"https://github.sonarsource.com/api/v3/repos/octocat/Hello-World/collaborators{/collaborator}\",\n"
      + "  \"comments_url\": \"https://github.sonarsource.com/api/v3/repos/octocat/Hello-World/comments{/number}\",\n"
      + "  \"commits_url\": \"https://github.sonarsource.com/api/v3/repos/octocat/Hello-World/commits{/sha}\",\n"
      + "  \"compare_url\": \"https://github.sonarsource.com/api/v3/repos/octocat/Hello-World/compare/{base}...{head}\",\n"
      + "  \"contents_url\": \"https://github.sonarsource.com/api/v3/repos/octocat/Hello-World/contents/{+path}\",\n"
      + "  \"contributors_url\": \"https://github.sonarsource.com/api/v3/repos/octocat/Hello-World/contributors\",\n"
      + "  \"deployments_url\": \"https://github.sonarsource.com/api/v3/repos/octocat/Hello-World/deployments\",\n"
      + "  \"downloads_url\": \"https://github.sonarsource.com/api/v3/repos/octocat/Hello-World/downloads\",\n"
      + "  \"events_url\": \"https://github.sonarsource.com/api/v3/repos/octocat/Hello-World/events\",\n"
      + "  \"forks_url\": \"https://github.sonarsource.com/api/v3/repos/octocat/Hello-World/forks\",\n"
      + "  \"git_commits_url\": \"https://github.sonarsource.com/api/v3/repos/octocat/Hello-World/git/commits{/sha}\",\n"
      + "  \"git_refs_url\": \"https://github.sonarsource.com/api/v3/repos/octocat/Hello-World/git/refs{/sha}\",\n"
      + "  \"git_tags_url\": \"https://github.sonarsource.com/api/v3/repos/octocat/Hello-World/git/tags{/sha}\",\n"
      + "  \"git_url\": \"git:github.com/octocat/Hello-World.git\",\n"
      + "  \"issue_comment_url\": \"https://github.sonarsource.com/api/v3/repos/octocat/Hello-World/issues/comments{/number}\",\n"
      + "  \"issue_events_url\": \"https://github.sonarsource.com/api/v3/repos/octocat/Hello-World/issues/events{/number}\",\n"
      + "  \"issues_url\": \"https://github.sonarsource.com/api/v3/repos/octocat/Hello-World/issues{/number}\",\n"
      + "  \"keys_url\": \"https://github.sonarsource.com/api/v3/repos/octocat/Hello-World/keys{/key_id}\",\n"
      + "  \"labels_url\": \"https://github.sonarsource.com/api/v3/repos/octocat/Hello-World/labels{/name}\",\n"
      + "  \"languages_url\": \"https://github.sonarsource.com/api/v3/repos/octocat/Hello-World/languages\",\n"
      + "  \"merges_url\": \"https://github.sonarsource.com/api/v3/repos/octocat/Hello-World/merges\",\n"
      + "  \"milestones_url\": \"https://github.sonarsource.com/api/v3/repos/octocat/Hello-World/milestones{/number}\",\n"
      + "  \"notifications_url\": \"https://github.sonarsource.com/api/v3/repos/octocat/Hello-World/notifications{?since,all,participating}\",\n"
      + "  \"pulls_url\": \"https://github.sonarsource.com/api/v3/repos/octocat/Hello-World/pulls{/number}\",\n"
      + "  \"releases_url\": \"https://github.sonarsource.com/api/v3/repos/octocat/Hello-World/releases{/id}\",\n"
      + "  \"ssh_url\": \"git@github.com:octocat/Hello-World.git\",\n"
      + "  \"stargazers_url\": \"https://github.sonarsource.com/api/v3/repos/octocat/Hello-World/stargazers\",\n"
      + "  \"statuses_url\": \"https://github.sonarsource.com/api/v3/repos/octocat/Hello-World/statuses/{sha}\",\n"
      + "  \"subscribers_url\": \"https://github.sonarsource.com/api/v3/repos/octocat/Hello-World/subscribers\",\n"
      + "  \"subscription_url\": \"https://github.sonarsource.com/api/v3/repos/octocat/Hello-World/subscription\",\n"
      + "  \"tags_url\": \"https://github.sonarsource.com/api/v3/repos/octocat/Hello-World/tags\",\n"
      + "  \"teams_url\": \"https://github.sonarsource.com/api/v3/repos/octocat/Hello-World/teams\",\n"
      + "  \"trees_url\": \"https://github.sonarsource.com/api/v3/repos/octocat/Hello-World/git/trees{/sha}\",\n"
      + "  \"clone_url\": \"https://github.com/octocat/Hello-World.git\",\n"
      + "  \"mirror_url\": \"git:git.example.com/octocat/Hello-World\",\n"
      + "  \"hooks_url\": \"https://github.sonarsource.com/api/v3/repos/octocat/Hello-World/hooks\",\n"
      + "  \"svn_url\": \"https://svn.github.com/octocat/Hello-World\",\n"
      + "  \"homepage\": \"https://github.com\",\n"
      + "  \"language\": null,\n"
      + "  \"forks_count\": 9,\n"
      + "  \"stargazers_count\": 80,\n"
      + "  \"watchers_count\": 80,\n"
      + "  \"size\": 108,\n"
      + "  \"default_branch\": \"master\",\n"
      + "  \"open_issues_count\": 0,\n"
      + "  \"is_template\": true,\n"
      + "  \"topics\": [\n"
      + "    \"octocat\",\n"
      + "    \"atom\",\n"
      + "    \"electron\",\n"
      + "    \"api\"\n"
      + "  ],\n"
      + "  \"has_issues\": true,\n"
      + "  \"has_projects\": true,\n"
      + "  \"has_wiki\": true,\n"
      + "  \"has_pages\": false,\n"
      + "  \"has_downloads\": true,\n"
      + "  \"archived\": false,\n"
      + "  \"disabled\": false,\n"
      + "  \"visibility\": \"public\",\n"
      + "  \"pushed_at\": \"2011-01-26T19:06:43Z\",\n"
      + "  \"created_at\": \"2011-01-26T19:01:12Z\",\n"
      + "  \"updated_at\": \"2011-01-26T19:14:43Z\",\n"
      + "  \"permissions\": {\n"
      + "    \"admin\": false,\n"
      + "    \"push\": false,\n"
      + "    \"pull\": true\n"
      + "  },\n"
      + "  \"allow_rebase_merge\": true,\n"
      + "  \"template_repository\": null,\n"
      + "  \"allow_squash_merge\": true,\n"
      + "  \"allow_merge_commit\": true,\n"
      + "  \"subscribers_count\": 42,\n"
      + "  \"network_count\": 0,\n"
      + "  \"anonymous_access_enabled\": false,\n"
      + "  \"license\": {\n"
      + "    \"key\": \"mit\",\n"
      + "    \"name\": \"MIT License\",\n"
      + "    \"spdx_id\": \"MIT\",\n"
      + "    \"url\": \"https://github.sonarsource.com/api/v3/licenses/mit\",\n"
      + "    \"node_id\": \"MDc6TGljZW5zZW1pdA==\"\n"
      + "  },\n"
      + "  \"organization\": {\n"
      + "    \"login\": \"octocat\",\n"
      + "    \"id\": 1,\n"
      + "    \"node_id\": \"MDQ6VXNlcjE=\",\n"
      + "    \"avatar_url\": \"https://github.com/images/error/octocat_happy.gif\",\n"
      + "    \"gravatar_id\": \"\",\n"
      + "    \"url\": \"https://github.sonarsource.com/api/v3/users/octocat\",\n"
      + "    \"html_url\": \"https://github.com/octocat\",\n"
      + "    \"followers_url\": \"https://github.sonarsource.com/api/v3/users/octocat/followers\",\n"
      + "    \"following_url\": \"https://github.sonarsource.com/api/v3/users/octocat/following{/other_user}\",\n"
      + "    \"gists_url\": \"https://github.sonarsource.com/api/v3/users/octocat/gists{/gist_id}\",\n"
      + "    \"starred_url\": \"https://github.sonarsource.com/api/v3/users/octocat/starred{/owner}{/repo}\",\n"
      + "    \"subscriptions_url\": \"https://github.sonarsource.com/api/v3/users/octocat/subscriptions\",\n"
      + "    \"organizations_url\": \"https://github.sonarsource.com/api/v3/users/octocat/orgs\",\n"
      + "    \"repos_url\": \"https://github.sonarsource.com/api/v3/users/octocat/repos\",\n"
      + "    \"events_url\": \"https://github.sonarsource.com/api/v3/users/octocat/events{/privacy}\",\n"
      + "    \"received_events_url\": \"https://github.sonarsource.com/api/v3/users/octocat/received_events\",\n"
      + "    \"type\": \"Organization\",\n"
      + "    \"site_admin\": false\n"
      + "  }"
      + "}";

    when(httpClient.get(appUrl, accessToken, "/repos/octocat/Hello-World"))
      .thenReturn(new GithubApplicationHttpClient.GetResponse() {
        @Override
        public Optional<String> getNextEndPoint() {
          return Optional.empty();
        }

        @Override
        public int getCode() {
          return 200;
        }

        @Override
        public Optional<String> getContent() {
          return Optional.of(responseJson);
        }
      });

    Optional<GithubApplicationClient.Repository> repository = underTest.getRepository(appUrl, accessToken, "octocat", "octocat/Hello-World");

    assertThat(repository)
      .isPresent()
      .get()
      .extracting(GithubApplicationClient.Repository::getId, GithubApplicationClient.Repository::getName, GithubApplicationClient.Repository::getFullName,
        GithubApplicationClient.Repository::getUrl, GithubApplicationClient.Repository::isPrivate, GithubApplicationClient.Repository::getDefaultBranch)
      .containsOnly(1296269L, "Hello-World", "octocat/Hello-World", "https://github.com/octocat/Hello-World", false, "master");
  }

  private AppToken mockAppToken() {
    String jwt = randomAlphanumeric(5);
    when(appSecurity.createAppToken(githubAppConfiguration.getId(), githubAppConfiguration.getPrivateKey())).thenReturn(new AppToken(jwt));
    return new AppToken(jwt);
  }

  private static class OkGetResponse extends Response {
    private OkGetResponse(String content) {
      super(200, content);
    }
  }

  private static class ErrorGetResponse extends Response {
    ErrorGetResponse() {
      super(401, null);
    }

    ErrorGetResponse(int code, String content) {
      super(code, content);
    }
  }

  private static class Response implements GithubApplicationHttpClient.GetResponse {
    private final int code;
    private final String content;
    private final String nextEndPoint;

    private Response(int code, @Nullable String content) {
      this(code, content, null);
    }

    private Response(int code, @Nullable String content, @Nullable String nextEndPoint) {
      this.code = code;
      this.content = content;
      this.nextEndPoint = nextEndPoint;
    }

    @Override
    public int getCode() {
      return code;
    }

    @Override
    public Optional<String> getContent() {
      return Optional.ofNullable(content);
    }

    @Override
    public Optional<String> getNextEndPoint() {
      return Optional.ofNullable(nextEndPoint);
    }
  }
}
