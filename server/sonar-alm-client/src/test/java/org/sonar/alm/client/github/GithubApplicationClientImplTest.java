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
package org.sonar.alm.client.github;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import javax.annotation.Nullable;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.slf4j.event.Level;
import org.sonar.alm.client.ApplicationHttpClient.RateLimit;
import org.sonar.alm.client.github.security.AppToken;
import org.sonar.alm.client.github.security.GithubAppSecurity;
import org.sonar.api.testfixtures.log.LogAndArguments;
import org.sonar.api.testfixtures.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.auth.github.AppInstallationToken;
import org.sonar.auth.github.ExpiringAppInstallationToken;
import org.sonar.auth.github.GitHubSettings;
import org.sonar.auth.github.GithubAppConfiguration;
import org.sonar.auth.github.GithubAppInstallation;
import org.sonar.auth.github.GithubBinding;
import org.sonar.auth.github.GsonRepositoryCollaborator;
import org.sonar.auth.github.GsonRepositoryPermissions;
import org.sonar.auth.github.GsonRepositoryTeam;
import org.sonar.auth.github.client.GithubApplicationClient;
import org.sonar.auth.github.security.AccessToken;
import org.sonar.auth.github.security.UserAccessToken;
import org.sonarqube.ws.client.HttpException;

import static java.lang.String.format;
import static java.net.HttpURLConnection.HTTP_CREATED;
import static java.net.HttpURLConnection.HTTP_FORBIDDEN;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.alm.client.ApplicationHttpClient.GetResponse;

@RunWith(DataProviderRunner.class)
public class GithubApplicationClientImplTest {
  private static final String ORG_NAME = "ORG_NAME";
  private static final String TEAM_NAME = "team1";
  private static final String REPO_NAME = "repo1";
  private static final String APP_URL = "https://github.com/";
  private static final String REPO_TEAMS_ENDPOINT = "/repos/ORG_NAME/repo1/teams";
  private static final String REPO_COLLABORATORS_ENDPOINT = "/repos/ORG_NAME/repo1/collaborators?affiliation=direct";
  private static final int INSTALLATION_ID = 1;
  private static final String APP_JWT_TOKEN = "APP_TOKEN_JWT";
  private static final String PAYLOAD_2_ORGS = """
    [
      {
        "id": 1,
        "account": {
          "login": "org1",
          "type": "Organization"
        },
        "target_type": "Organization",
        "permissions": {
          "members": "read",
          "metadata": "read"
        },
        "suspended_at": "2023-05-30T08:40:55Z"
      },
      {
        "id": 2,
        "account": {
          "login": "org2",
          "type": "Organization"
        },
        "target_type": "Organization",
        "permissions": {
          "members": "read",
          "metadata": "read"
        }
      }
    ]""";

  private static final RateLimit RATE_LIMIT = new RateLimit(Integer.MAX_VALUE, Integer.MAX_VALUE, 0L);

  @ClassRule
  public static LogTester logTester = new LogTester().setLevel(LoggerLevel.WARN);

  private GithubApplicationHttpClient githubApplicationHttpClient = mock();
  private GithubAppSecurity appSecurity = mock();
  private GithubAppConfiguration githubAppConfiguration = mock();
  private GitHubSettings gitHubSettings = mock();

  private GithubPaginatedHttpClient githubPaginatedHttpClient = mock();
  private AppInstallationToken appInstallationToken = mock();
  private GithubApplicationClient underTest;

  private Clock clock = Clock.fixed(Instant.EPOCH, ZoneId.systemDefault());
  private String appUrl = "Any URL";

  @Before
  public void setup() {
    when(githubAppConfiguration.getApiEndpoint()).thenReturn(appUrl);
    underTest = new GithubApplicationClientImpl(clock, githubApplicationHttpClient, appSecurity, gitHubSettings, githubPaginatedHttpClient);
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

    when(githubApplicationHttpClient.get(appUrl, appToken, "/app")).thenThrow(new IOException("OOPS"));

    assertThatThrownBy(() -> underTest.checkAppPermissions(githubAppConfiguration))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Failed to validate configuration, check URL and Private Key");
  }

  @Test
  @UseDataProvider("checkAppPermissionsErrorCodes")
  public void checkAppPermissions_ErrorCodes(int errorCode, String expectedMessage) throws IOException {
    AppToken appToken = mockAppToken();

    when(githubApplicationHttpClient.get(appUrl, appToken, "/app")).thenReturn(new ErrorGetResponse(errorCode, null));

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

    when(githubApplicationHttpClient.get(appUrl, appToken, "/app")).thenReturn(new OkGetResponse("{}"));

    assertThatThrownBy(() -> underTest.checkAppPermissions(githubAppConfiguration))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Failed to get app permissions, unexpected response body");
  }

  @Test
  public void checkAppPermissions_IncorrectPermissions() throws IOException {
    AppToken appToken = mockAppToken();

    String json = """
      {
            "permissions": {
              "checks": "read",
              "metadata": "read",
              "pull_requests": "read"
            }
      }
      """;

    when(githubApplicationHttpClient.get(appUrl, appToken, "/app")).thenReturn(new OkGetResponse(json));

    assertThatThrownBy(() -> underTest.checkAppPermissions(githubAppConfiguration))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Missing permissions; permission granted on pull_requests is 'read', should be 'write', checks is 'read', should be 'write'");
  }

  @Test
  public void checkAppPermissions() throws IOException {
    AppToken appToken = mockAppToken();

    String json = """
      {
            "permissions": {
              "checks": "write",
              "metadata": "read",
              "pull_requests": "write"
            }
      }
      """;

    when(githubApplicationHttpClient.get(appUrl, appToken, "/app")).thenReturn(new OkGetResponse(json));

    assertThatCode(() -> underTest.checkAppPermissions(githubAppConfiguration)).isNull();
  }

  @Test
  public void getInstallationId_returns_installation_id_of_given_account() throws IOException {
    AppToken appToken = new AppToken(APP_JWT_TOKEN);
    when(appSecurity.createAppToken(githubAppConfiguration.getId(), githubAppConfiguration.getPrivateKey())).thenReturn(appToken);
    when(githubApplicationHttpClient.get(appUrl, appToken, "/repos/torvalds/linux/installation"))
      .thenReturn(new OkGetResponse("""
        {
          "id": 2,
          "account": {
            "login": "torvalds"
          }
        }"""));

    assertThat(underTest.getInstallationId(githubAppConfiguration, "torvalds/linux")).hasValue(2L);
  }

  @Test
  public void getInstallationId_throws_IAE_if_fail_to_create_app_token() {
    when(appSecurity.createAppToken(githubAppConfiguration.getId(), githubAppConfiguration.getPrivateKey())).thenThrow(IllegalArgumentException.class);

    assertThatThrownBy(() -> underTest.getInstallationId(githubAppConfiguration, "torvalds"))
      .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void getInstallationId_return_empty_if_no_installation_found_for_githubAccount() throws IOException {
    AppToken appToken = new AppToken(APP_JWT_TOKEN);
    when(appSecurity.createAppToken(githubAppConfiguration.getId(), githubAppConfiguration.getPrivateKey())).thenReturn(appToken);
    when(githubApplicationHttpClient.get(appUrl, appToken, "/repos/torvalds/linux/installation"))
      .thenReturn(new ErrorGetResponse(404, null));

    assertThat(underTest.getInstallationId(githubAppConfiguration, "torvalds")).isEmpty();
  }

  @Test
  @UseDataProvider("githubServers")
  public void createUserAccessToken_returns_empty_if_access_token_cant_be_created(String apiUrl, String appUrl) throws IOException {
    when(githubApplicationHttpClient.post(appUrl, null, "/login/oauth/access_token?client_id=clientId&client_secret=clientSecret&code=code"))
      .thenReturn(new Response(400, null));

    assertThatThrownBy(() -> underTest.createUserAccessToken(appUrl, "clientId", "clientSecret", "code"))
      .isInstanceOf(IllegalStateException.class);
    verify(githubApplicationHttpClient).post(appUrl, null, "/login/oauth/access_token?client_id=clientId&client_secret=clientSecret&code=code");
  }

  @Test
  @UseDataProvider("githubServers")
  public void createUserAccessToken_fail_if_access_token_request_fails(String apiUrl, String appUrl) throws IOException {
    when(githubApplicationHttpClient.post(appUrl, null, "/login/oauth/access_token?client_id=clientId&client_secret=clientSecret&code=code"))
      .thenThrow(new IOException("OOPS"));

    assertThatThrownBy(() -> underTest.createUserAccessToken(apiUrl, "clientId", "clientSecret", "code"))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Failed to create GitHub's user access token");

    verify(githubApplicationHttpClient).post(appUrl, null, "/login/oauth/access_token?client_id=clientId&client_secret=clientSecret&code=code");
  }

  @Test
  @UseDataProvider("githubServers")
  public void createUserAccessToken_throws_illegal_argument_exception_if_access_token_code_is_expired(String apiUrl, String appUrl) throws IOException {
    when(githubApplicationHttpClient.post(appUrl, null, "/login/oauth/access_token?client_id=clientId&client_secret=clientSecret&code=code"))
      .thenReturn(new OkGetResponse("error_code=100&error=expired_or_invalid"));

    assertThatThrownBy(() -> underTest.createUserAccessToken(apiUrl, "clientId", "clientSecret", "code"))
      .isInstanceOf(IllegalArgumentException.class);

    verify(githubApplicationHttpClient).post(appUrl, null, "/login/oauth/access_token?client_id=clientId&client_secret=clientSecret&code=code");
  }

  @Test
  @UseDataProvider("githubServers")
  public void createUserAccessToken_from_authorization_code_returns_access_token(String apiUrl, String appUrl) throws IOException {
    String token = randomAlphanumeric(10);
    when(githubApplicationHttpClient.post(appUrl, null, "/login/oauth/access_token?client_id=clientId&client_secret=clientSecret&code=code"))
      .thenReturn(new OkGetResponse("access_token=" + token + "&status="));

    UserAccessToken userAccessToken = underTest.createUserAccessToken(apiUrl, "clientId", "clientSecret", "code");

    assertThat(userAccessToken)
      .extracting(UserAccessToken::getValue, UserAccessToken::getAuthorizationHeaderPrefix)
      .containsOnly(token, "token");
    verify(githubApplicationHttpClient).post(appUrl, null, "/login/oauth/access_token?client_id=clientId&client_secret=clientSecret&code=code");
  }

  @Test
  public void getApp_returns_id() throws IOException {
    AppToken appToken = new AppToken(APP_JWT_TOKEN);
    when(appSecurity.createAppToken(githubAppConfiguration.getId(), githubAppConfiguration.getPrivateKey())).thenReturn(appToken);
    when(githubApplicationHttpClient.get(appUrl, appToken, "/app"))
      .thenReturn(new OkGetResponse("{\"installations_count\": 2}"));

    assertThat(underTest.getApp(githubAppConfiguration).getInstallationsCount()).isEqualTo(2L);
  }

  @Test
  public void getApp_whenStatusCodeIsNotOk_shouldThrowHttpException() throws IOException {
    AppToken appToken = new AppToken(APP_JWT_TOKEN);
    when(appSecurity.createAppToken(githubAppConfiguration.getId(), githubAppConfiguration.getPrivateKey())).thenReturn(appToken);
    when(githubApplicationHttpClient.get(appUrl, appToken, "/app"))
      .thenReturn(new ErrorGetResponse(418, "I'm a teapot"));

    assertThatThrownBy(() -> underTest.getApp(githubAppConfiguration))
      .isInstanceOfSatisfying(HttpException.class, httpException -> {
        assertThat(httpException.code()).isEqualTo(418);
        assertThat(httpException.url()).isEqualTo("Any URL/app");
        assertThat(httpException.content()).isEqualTo("I'm a teapot");
      });
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

    when(githubApplicationHttpClient.get(appUrl, accessToken, format("/user/installations?page=%s&per_page=%s", 1, 100)))
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
    String responseJson = """
      {
        "total_count": 0
      }
      """;

    when(githubApplicationHttpClient.get(appUrl, accessToken, format("/user/installations?page=%s&per_page=%s", 1, 100)))
      .thenReturn(new OkGetResponse(responseJson));

    GithubApplicationClient.Organizations organizations = underTest.listOrganizations(appUrl, accessToken, 1, 100);

    assertThat(organizations.getTotal()).isZero();
    assertThat(organizations.getOrganizations()).isNull();
  }

  @Test
  public void listOrganizations_returns_pages_results() throws IOException {
    String appUrl = "https://github.sonarsource.com";
    AccessToken accessToken = new UserAccessToken(randomAlphanumeric(10));
    String responseJson = """
      {
        "total_count": 2,
        "installations": [
          {
            "id": 1,
            "account": {
              "login": "github",
              "id": 1,
              "node_id": "MDEyOk9yZ2FuaXphdGlvbjE=",
              "url": "https://github.sonarsource.com/api/v3/orgs/github",
              "repos_url": "https://github.sonarsource.com/api/v3/orgs/github/repos",
              "events_url": "https://github.sonarsource.com/api/v3/orgs/github/events",
              "hooks_url": "https://github.sonarsource.com/api/v3/orgs/github/hooks",
              "issues_url": "https://github.sonarsource.com/api/v3/orgs/github/issues",
              "members_url": "https://github.sonarsource.com/api/v3/orgs/github/members{/member}",
              "public_members_url": "https://github.sonarsource.com/api/v3/orgs/github/public_members{/member}",
              "avatar_url": "https://github.com/images/error/octocat_happy.gif",
              "description": "A great organization"
            },
            "access_tokens_url": "https://github.sonarsource.com/api/v3/app/installations/1/access_tokens",
            "repositories_url": "https://github.sonarsource.com/api/v3/installation/repositories",
            "html_url": "https://github.com/organizations/github/settings/installations/1",
            "app_id": 1,
            "target_id": 1,
            "target_type": "Organization",
            "permissions": {
              "checks": "write",
              "metadata": "read",
              "contents": "read"
            },
            "events": [
              "push",
              "pull_request"
            ],
            "single_file_name": "config.yml"
          },
          {
            "id": 3,
            "account": {
              "login": "octocat",
              "id": 2,
              "node_id": "MDQ6VXNlcjE=",
              "avatar_url": "https://github.com/images/error/octocat_happy.gif",
              "gravatar_id": "",
              "url": "https://github.sonarsource.com/api/v3/users/octocat",
              "html_url": "https://github.com/octocat",
              "followers_url": "https://github.sonarsource.com/api/v3/users/octocat/followers",
              "following_url": "https://github.sonarsource.com/api/v3/users/octocat/following{/other_user}",
              "gists_url": "https://github.sonarsource.com/api/v3/users/octocat/gists{/gist_id}",
              "starred_url": "https://github.sonarsource.com/api/v3/users/octocat/starred{/owner}{/repo}",
              "subscriptions_url": "https://github.sonarsource.com/api/v3/users/octocat/subscriptions",
              "organizations_url": "https://github.sonarsource.com/api/v3/users/octocat/orgs",
              "repos_url": "https://github.sonarsource.com/api/v3/users/octocat/repos",
              "events_url": "https://github.sonarsource.com/api/v3/users/octocat/events{/privacy}",
              "received_events_url": "https://github.sonarsource.com/api/v3/users/octocat/received_events",
              "type": "User",
              "site_admin": false
            },
            "access_tokens_url": "https://github.sonarsource.com/api/v3/app/installations/1/access_tokens",
            "repositories_url": "https://github.sonarsource.com/api/v3/installation/repositories",
            "html_url": "https://github.com/organizations/github/settings/installations/1",
            "app_id": 1,
            "target_id": 1,
            "target_type": "Organization",
            "permissions": {
              "checks": "write",
              "metadata": "read",
              "contents": "read"
            },
            "events": [
              "push",
              "pull_request"
            ],
            "single_file_name": "config.yml"
          }
        ]
      }
      """;

    when(githubApplicationHttpClient.get(appUrl, accessToken, format("/user/installations?page=%s&per_page=%s", 1, 100)))
      .thenReturn(new OkGetResponse(responseJson));

    GithubApplicationClient.Organizations organizations = underTest.listOrganizations(appUrl, accessToken, 1, 100);

    assertThat(organizations.getTotal()).isEqualTo(2);
    assertThat(organizations.getOrganizations()).extracting(GithubApplicationClient.Organization::getLogin).containsOnly("github", "octocat");
  }

  @Test
  public void getWhitelistedGithubAppInstallations_whenWhitelistNotSpecified_doesNotFilter() throws IOException {
    List<GithubAppInstallation> allOrgInstallations = getGithubAppInstallationsFromGithubResponse(PAYLOAD_2_ORGS);
    assertOrgDeserialization(allOrgInstallations);
  }

  private static void assertOrgDeserialization(List<GithubAppInstallation> orgs) {
    GithubAppInstallation org1 = orgs.get(0);
    assertThat(org1.installationId()).isEqualTo("1");
    assertThat(org1.organizationName()).isEqualTo("org1");
    assertThat(org1.permissions().getMembers()).isEqualTo("read");
    assertThat(org1.isSuspended()).isTrue();

    GithubAppInstallation org2 = orgs.get(1);
    assertThat(org2.installationId()).isEqualTo("2");
    assertThat(org2.organizationName()).isEqualTo("org2");
    assertThat(org2.permissions().getMembers()).isEqualTo("read");
    assertThat(org2.isSuspended()).isFalse();
  }

  @Test
  public void getWhitelistedGithubAppInstallations_whenWhitelistSpecified_filtersWhitelistedOrgs() throws IOException {
    when(gitHubSettings.getOrganizations()).thenReturn(Set.of("org2"));
    List<GithubAppInstallation> orgInstallations = getGithubAppInstallationsFromGithubResponse(PAYLOAD_2_ORGS);
    assertThat(orgInstallations)
      .hasSize(1)
      .extracting(GithubAppInstallation::organizationName)
      .containsExactlyInAnyOrder("org2");
  }

  @Test
  public void getWhitelistedGithubAppInstallations_whenEmptyResponse_shouldReturnEmpty() throws IOException {
    List<GithubAppInstallation> allOrgInstallations = getGithubAppInstallationsFromGithubResponse("[]");
    assertThat(allOrgInstallations).isEmpty();
  }

  @Test
  public void getWhitelistedGithubAppInstallations_whenNoOrganization_shouldReturnEmpty() throws IOException {
    List<GithubAppInstallation> allOrgInstallations = getGithubAppInstallationsFromGithubResponse("""
      [
        {
          "id": 1,
          "account": {
            "login": "user1",
            "type": "User"
          },
          "target_type": "User",
          "permissions": {
            "metadata": "read"
          }
        }
      ]""");
    assertThat(allOrgInstallations).isEmpty();
  }

  @SuppressWarnings("unchecked")
  private List<GithubAppInstallation> getGithubAppInstallationsFromGithubResponse(String content) throws IOException {
    AppToken appToken = new AppToken(APP_JWT_TOKEN);
    when(appSecurity.createAppToken(githubAppConfiguration.getId(), githubAppConfiguration.getPrivateKey())).thenReturn(appToken);
    when(githubPaginatedHttpClient.get(eq(appUrl), eq(appToken), eq("/app/installations"), any()))
      .thenAnswer(invocation -> {
        Function<String, List<GithubBinding.GsonInstallation>> deserializingFunction = invocation.getArgument(3, Function.class);
        return deserializingFunction.apply(content);
      });
    return underTest.getWhitelistedGithubAppInstallations(githubAppConfiguration);
  }

  @Test
  public void getWhitelistedGithubAppInstallations_whenGithubReturnsError_shouldReThrow() {
    AppToken appToken = new AppToken(APP_JWT_TOKEN);
    when(appSecurity.createAppToken(githubAppConfiguration.getId(), githubAppConfiguration.getPrivateKey())).thenReturn(appToken);
    when(githubPaginatedHttpClient.get(any(), any(), any(), any())).thenThrow(new IllegalStateException("exception"));

    assertThatThrownBy(() -> underTest.getWhitelistedGithubAppInstallations(githubAppConfiguration))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("exception");
  }

  @Test
  public void listRepositories_fail_on_failure() throws IOException {
    String appUrl = "https://github.sonarsource.com";
    AccessToken accessToken = new UserAccessToken(randomAlphanumeric(10));

    when(githubApplicationHttpClient.get(appUrl, accessToken, format("/search/repositories?q=%s&page=%s&per_page=%s", "org:test", 1, 100)))
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

    when(githubApplicationHttpClient.get(appUrl, accessToken, format("/search/repositories?q=%s&page=%s&per_page=%s", "fork:true+org:github", 1, 100)))
      .thenReturn(new OkGetResponse(responseJson));

    GithubApplicationClient.Repositories repositories = underTest.listRepositories(appUrl, accessToken, "github", null, 1, 100);

    assertThat(repositories.getTotal()).isZero();
    assertThat(repositories.getRepositories()).isNull();
  }

  @Test
  public void listRepositories_returns_pages_results() throws IOException {
    String appUrl = "https://github.sonarsource.com";
    AccessToken accessToken = new UserAccessToken(randomAlphanumeric(10));
    String responseJson = """
      {
        "total_count": 2,
        "incomplete_results": false,
        "items": [
          {
            "id": 3081286,
            "node_id": "MDEwOlJlcG9zaXRvcnkzMDgxMjg2",
            "name": "HelloWorld",
            "full_name": "github/HelloWorld",
            "owner": {
              "login": "github",
              "id": 872147,
              "node_id": "MDQ6VXNlcjg3MjE0Nw==",
              "avatar_url": "https://github.sonarsource.com/images/error/octocat_happy.gif",
              "gravatar_id": "",
              "url": "https://github.sonarsource.com/api/v3/users/github",
              "received_events_url": "https://github.sonarsource.com/api/v3/users/github/received_events",
              "type": "User"
            },
            "private": false,
            "html_url": "https://github.com/github/HelloWorld",
            "description": "A C implementation of HelloWorld",
            "fork": false,
            "url": "https://github.sonarsource.com/api/v3/repos/github/HelloWorld",
            "created_at": "2012-01-01T00:31:50Z",
            "updated_at": "2013-01-05T17:58:47Z",
            "pushed_at": "2012-01-01T00:37:02Z",
            "homepage": "",
            "size": 524,
            "stargazers_count": 1,
            "watchers_count": 1,
            "language": "Assembly",
            "forks_count": 0,
            "open_issues_count": 0,
            "master_branch": "master",
            "default_branch": "master",
            "score": 1.0
          },
          {
            "id": 3081286,
            "node_id": "MDEwOlJlcG9zaXRvcnkzMDgxMjg2",
            "name": "HelloUniverse",
            "full_name": "github/HelloUniverse",
            "owner": {
              "login": "github",
              "id": 872147,
              "node_id": "MDQ6VXNlcjg3MjE0Nw==",
              "avatar_url": "https://github.sonarsource.com/images/error/octocat_happy.gif",
              "gravatar_id": "",
              "url": "https://github.sonarsource.com/api/v3/users/github",
              "received_events_url": "https://github.sonarsource.com/api/v3/users/github/received_events",
              "type": "User"
            },
            "private": false,
            "html_url": "https://github.com/github/HelloUniverse",
            "description": "A C implementation of HelloUniverse",
            "fork": false,
            "url": "https://github.sonarsource.com/api/v3/repos/github/HelloUniverse",
            "created_at": "2012-01-01T00:31:50Z",
            "updated_at": "2013-01-05T17:58:47Z",
            "pushed_at": "2012-01-01T00:37:02Z",
            "homepage": "",
            "size": 524,
            "stargazers_count": 1,
            "watchers_count": 1,
            "language": "Assembly",
            "forks_count": 0,
            "open_issues_count": 0,
            "master_branch": "master",
            "default_branch": "master",
            "score": 1.0
          }
        ]
      }""";

    when(githubApplicationHttpClient.get(appUrl, accessToken, format("/search/repositories?q=%s&page=%s&per_page=%s", "fork:true+org:github", 1, 100)))
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
    String responseJson = """
      {
        "total_count": 2,
        "incomplete_results": false,
        "items": [
          {
            "id": 3081286,
            "node_id": "MDEwOlJlcG9zaXRvcnkzMDgxMjg2",
            "name": "HelloWorld",
            "full_name": "github/HelloWorld",
            "owner": {
              "login": "github",
              "id": 872147,
              "node_id": "MDQ6VXNlcjg3MjE0Nw==",
              "avatar_url": "https://github.sonarsource.com/images/error/octocat_happy.gif",
              "gravatar_id": "",
              "url": "https://github.sonarsource.com/api/v3/users/github",
              "received_events_url": "https://github.sonarsource.com/api/v3/users/github/received_events",
              "type": "User"
            },
            "private": false,
            "html_url": "https://github.com/github/HelloWorld",
            "description": "A C implementation of HelloWorld",
            "fork": false,
            "url": "https://github.sonarsource.com/api/v3/repos/github/HelloWorld",
            "created_at": "2012-01-01T00:31:50Z",
            "updated_at": "2013-01-05T17:58:47Z",
            "pushed_at": "2012-01-01T00:37:02Z",
            "homepage": "",
            "size": 524,
            "stargazers_count": 1,
            "watchers_count": 1,
            "language": "Assembly",
            "forks_count": 0,
            "open_issues_count": 0,
            "master_branch": "master",
            "default_branch": "master",
            "score": 1.0
          }
        ]
      }""";

    when(githubApplicationHttpClient.get(appUrl, accessToken, format("/search/repositories?q=%s&page=%s&per_page=%s", "world+fork:true+org:github", 1, 100)))
      .thenReturn(new GetResponse() {
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

        @Override
        public RateLimit getRateLimit() {
          return RATE_LIMIT;
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
    when(githubApplicationHttpClient.get(any(), any(), any()))
      .thenReturn(new Response(404, null));

    Optional<GithubApplicationClient.Repository> repository = underTest.getRepository(appUrl, new UserAccessToken("temp"), "octocat/Hello-World");

    assertThat(repository).isEmpty();
  }

  @Test
  public void getRepository_fails_on_failure() throws IOException {
    String repositoryKey = "octocat/Hello-World";

    when(githubApplicationHttpClient.get(any(), any(), any()))
      .thenThrow(new IOException("OOPS"));

    UserAccessToken token = new UserAccessToken("temp");
    assertThatThrownBy(() -> underTest.getRepository(appUrl, token, repositoryKey))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Failed to get repository 'octocat/Hello-World' on 'Any URL' (this might be related to the GitHub App installation scope)");
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

    when(githubApplicationHttpClient.get(appUrl, accessToken, "/repos/octocat/Hello-World"))
      .thenReturn(new GetResponse() {
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

        @Override
        public RateLimit getRateLimit() {
          return RATE_LIMIT;
        }
      });

    Optional<GithubApplicationClient.Repository> repository = underTest.getRepository(appUrl, accessToken, "octocat/Hello-World");

    assertThat(repository)
      .isPresent()
      .get()
      .extracting(GithubApplicationClient.Repository::getId, GithubApplicationClient.Repository::getName, GithubApplicationClient.Repository::getFullName,
        GithubApplicationClient.Repository::getUrl, GithubApplicationClient.Repository::isPrivate, GithubApplicationClient.Repository::getDefaultBranch)
      .containsOnly(1296269L, "Hello-World", "octocat/Hello-World", "https://github.com/octocat/Hello-World", false, "master");
  }

  @Test
  public void createAppInstallationToken_throws_IAE_if_application_token_cant_be_created() {
    mockNoApplicationJwtToken();

    assertThatThrownBy(() -> underTest.createAppInstallationToken(githubAppConfiguration, INSTALLATION_ID))
      .isInstanceOf(IllegalArgumentException.class);
  }

  private void mockNoApplicationJwtToken() {
    when(appSecurity.createAppToken(githubAppConfiguration.getId(), githubAppConfiguration.getPrivateKey())).thenThrow(IllegalArgumentException.class);
  }

  @Test
  public void createAppInstallationToken_returns_empty_if_post_throws_IOE() throws IOException {
    mockAppToken();
    when(githubApplicationHttpClient.post(anyString(), any(AccessToken.class), anyString())).thenThrow(IOException.class);
    Optional<ExpiringAppInstallationToken> accessToken = underTest.createAppInstallationToken(githubAppConfiguration, INSTALLATION_ID);

    assertThat(accessToken).isEmpty();
    assertThat(logTester.getLogs(Level.WARN)).extracting(LogAndArguments::getRawMsg).anyMatch(s -> s.startsWith("Failed to request"));
  }

  @Test
  public void createAppInstallationToken_returns_empty_if_access_token_cant_be_created() throws IOException {
    AppToken appToken = mockAppToken();
    mockAccessTokenCallingGithubFailure();

    Optional<ExpiringAppInstallationToken> accessToken = underTest.createAppInstallationToken(githubAppConfiguration, INSTALLATION_ID);

    assertThat(accessToken).isEmpty();
    verify(githubApplicationHttpClient).post(appUrl, appToken, "/app/installations/" + INSTALLATION_ID + "/access_tokens");
  }

  @Test
  public void createAppInstallationToken_from_installation_id_returns_access_token() throws IOException {
    AppToken appToken = mockAppToken();
    ExpiringAppInstallationToken installToken = mockCreateAccessTokenCallingGithub();

    Optional<ExpiringAppInstallationToken> accessToken = underTest.createAppInstallationToken(githubAppConfiguration, INSTALLATION_ID);

    assertThat(accessToken).hasValue(installToken);
    verify(githubApplicationHttpClient).post(appUrl, appToken, "/app/installations/" + INSTALLATION_ID + "/access_tokens");
  }

  @Test
  public void getRepositoryTeams_returnsRepositoryTeams() throws IOException {
    ArgumentCaptor<Function<String, List<GsonRepositoryTeam>>> deserializerCaptor = ArgumentCaptor.forClass(Function.class);

    when(githubPaginatedHttpClient.get(eq(APP_URL), eq(appInstallationToken), eq(REPO_TEAMS_ENDPOINT), deserializerCaptor.capture())).thenReturn(expectedTeams());

    Set<GsonRepositoryTeam> repoTeams = underTest.getRepositoryTeams(APP_URL, appInstallationToken, ORG_NAME, REPO_NAME);

    assertThat(repoTeams)
      .containsExactlyInAnyOrderElementsOf(expectedTeams());

    String responseContent = getResponseContent("repo-teams-full-response.json");
    assertThat(deserializerCaptor.getValue().apply(responseContent)).containsExactlyElementsOf(expectedTeams());
  }

  @Test
  public void getRepositoryTeams_whenGitHubCallThrowsException_shouldRethrow() {
    when(githubPaginatedHttpClient.get(eq(APP_URL), eq(appInstallationToken), eq(REPO_TEAMS_ENDPOINT), any())).thenThrow(new IllegalStateException("error"));

    assertThatIllegalStateException()
      .isThrownBy(() -> underTest.getRepositoryTeams(APP_URL, appInstallationToken, ORG_NAME, REPO_NAME))
      .withMessage("error");
  }

  private static List<GsonRepositoryTeam> expectedTeams() {
    return List.of(
      new GsonRepositoryTeam("team1", 1, "team1", "pull", new GsonRepositoryPermissions(true, true, true, true, true)),
      new GsonRepositoryTeam("team2", 2, "team2", "push", new GsonRepositoryPermissions(false, false, true, true, true)));
  }

  @Test
  public void getRepositoryCollaborators_returnsCollaboratorsFromGithub() throws IOException {
    ArgumentCaptor<Function<String, List<GsonRepositoryCollaborator>>> deserializerCaptor = ArgumentCaptor.forClass(Function.class);

    when(githubPaginatedHttpClient.get(eq(APP_URL), eq(appInstallationToken), eq(REPO_COLLABORATORS_ENDPOINT), deserializerCaptor.capture())).thenReturn(expectedCollaborators());

    Set<GsonRepositoryCollaborator> repoTeams = underTest.getRepositoryCollaborators(APP_URL, appInstallationToken, ORG_NAME, REPO_NAME);

    assertThat(repoTeams)
      .containsExactlyInAnyOrderElementsOf(expectedCollaborators());

    String responseContent = getResponseContent("repo-collaborators-full-response.json");
    assertThat(deserializerCaptor.getValue().apply(responseContent)).containsExactlyElementsOf(expectedCollaborators());

  }

  @Test
  public void getRepositoryCollaborators_whenGitHubCallThrowsException_shouldRethrow() {
    when(githubPaginatedHttpClient.get(eq(APP_URL), eq(appInstallationToken), eq(REPO_COLLABORATORS_ENDPOINT), any())).thenThrow(new IllegalStateException("error"));

    assertThatIllegalStateException()
      .isThrownBy(() -> underTest.getRepositoryCollaborators(APP_URL, appInstallationToken, ORG_NAME, REPO_NAME))
      .withMessage("error");
  }

  private static String getResponseContent(String path) throws IOException {
    return IOUtils.toString(GithubApplicationClientImplTest.class.getResourceAsStream(path), StandardCharsets.UTF_8);
  }

  private static List<GsonRepositoryCollaborator> expectedCollaborators() {
    return List.of(
      new GsonRepositoryCollaborator("jean-michel", 1, "role1", new GsonRepositoryPermissions(true, true, true, true, true)),
      new GsonRepositoryCollaborator("jean-pierre", 2, "role2", new GsonRepositoryPermissions(false, false, true, true, true)));
  }

  private void mockAccessTokenCallingGithubFailure() throws IOException {
    Response response = mock(Response.class);
    when(response.getContent()).thenReturn(Optional.empty());
    when(response.getCode()).thenReturn(HTTP_UNAUTHORIZED);
    when(githubApplicationHttpClient.post(eq(appUrl), any(AppToken.class), eq("/app/installations/" + INSTALLATION_ID + "/access_tokens"))).thenReturn(response);
  }

  private AppToken mockAppToken() {
    String jwt = randomAlphanumeric(5);
    when(appSecurity.createAppToken(githubAppConfiguration.getId(), githubAppConfiguration.getPrivateKey())).thenReturn(new AppToken(jwt));
    return new AppToken(jwt);
  }

  private ExpiringAppInstallationToken mockCreateAccessTokenCallingGithub() throws IOException {
    String token = randomAlphanumeric(5);
    Response response = mock(Response.class);
    when(response.getContent()).thenReturn(Optional.of(format("""
          {
        	"token": "%s",
        	"expires_at": "2024-08-28T10:44:51Z",
        	"permissions": {
        		"members": "read",
        		"organization_administration": "read",
        		"administration": "read",
        		"metadata": "read"
        	},
        	"repository_selection": "all"
        }
      """, token)));
    when(response.getCode()).thenReturn(HTTP_CREATED);
    when(githubApplicationHttpClient.post(eq(appUrl), any(AppToken.class), eq("/app/installations/" + INSTALLATION_ID + "/access_tokens"))).thenReturn(response);
    return new ExpiringAppInstallationToken(clock, token, "2024-08-28T10:44:51Z");
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

  private static class Response implements GetResponse {
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
    public RateLimit getRateLimit() {
      return RATE_LIMIT;
    }

    @Override
    public Optional<String> getNextEndPoint() {
      return Optional.ofNullable(nextEndPoint);
    }
  }
}
