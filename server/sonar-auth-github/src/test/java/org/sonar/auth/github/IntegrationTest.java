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
package org.sonar.auth.github;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.server.authentication.OAuth2IdentityProvider;
import org.sonar.api.server.authentication.UnauthorizedException;
import org.sonar.api.server.authentication.UserIdentity;
import org.sonar.api.utils.System2;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class IntegrationTest {

  private static final String CALLBACK_URL = "http://localhost/oauth/callback/github";

  @Rule
  public MockWebServer github = new MockWebServer();

  // load settings with default values
  private MapSettings settings = new MapSettings(new PropertyDefinitions(System2.INSTANCE, GitHubSettings.definitions()));
  private GitHubSettings gitHubSettings = new GitHubSettings(settings.asConfig());
  private UserIdentityFactoryImpl userIdentityFactory = new UserIdentityFactoryImpl();
  private ScribeGitHubApi scribeApi = new ScribeGitHubApi(gitHubSettings);
  private GitHubRestClient gitHubRestClient = new GitHubRestClient(gitHubSettings);

  private String gitHubUrl;

  private GitHubIdentityProvider underTest = new GitHubIdentityProvider(gitHubSettings, userIdentityFactory, scribeApi, gitHubRestClient);

  @Before
  public void enable() {
    gitHubUrl = format("http://%s:%d", github.getHostName(), github.getPort());
    settings.setProperty("sonar.auth.github.clientId.secured", "the_id");
    settings.setProperty("sonar.auth.github.clientSecret.secured", "the_secret");
    settings.setProperty("sonar.auth.github.enabled", true);
    settings.setProperty("sonar.auth.github.apiUrl", gitHubUrl);
    settings.setProperty("sonar.auth.github.webUrl", gitHubUrl);
  }

  /**
   * First phase: SonarQube redirects browser to GitHub authentication form, requesting the
   * minimal access rights ("scope") to get user profile (login, name, email and others).
   */
  @Test
  public void redirect_browser_to_github_authentication_form() throws Exception {
    DumbInitContext context = new DumbInitContext("the-csrf-state");
    underTest.init(context);

    assertThat(context.redirectedTo).isEqualTo(
      gitHubSettings.webURL() +
        "login/oauth/authorize" +
        "?response_type=code" +
        "&client_id=the_id" +
        "&redirect_uri=" + URLEncoder.encode(CALLBACK_URL, StandardCharsets.UTF_8.name()) +
        "&scope=" + URLEncoder.encode("user:email", StandardCharsets.UTF_8.name()) +
        "&state=the-csrf-state");
  }

  /**
   * Second phase: GitHub redirects browser to SonarQube at /oauth/callback/github?code={the verifier code}.
   * This SonarQube web service sends two requests to GitHub:
   * <ul>
   *   <li>get an access token</li>
   *   <li>get the profile of the authenticated user</li>
   * </ul>
   */
  @Test
  public void callback_on_successful_authentication() throws IOException, InterruptedException {
    github.enqueue(newSuccessfulAccessTokenResponse());
    // response of api.github.com/user
    github.enqueue(new MockResponse().setBody("{\"id\":\"ABCD\", \"login\":\"octocat\", \"name\":\"monalisa octocat\",\"email\":\"octocat@github.com\"}"));

    HttpServletRequest request = newRequest("the-verifier-code");
    DumbCallbackContext callbackContext = new DumbCallbackContext(request);
    underTest.callback(callbackContext);

    assertThat(callbackContext.csrfStateVerified.get()).isTrue();
    assertThat(callbackContext.userIdentity.getProviderId()).isEqualTo("ABCD");
    assertThat(callbackContext.userIdentity.getProviderLogin()).isEqualTo("octocat");
    assertThat(callbackContext.userIdentity.getName()).isEqualTo("monalisa octocat");
    assertThat(callbackContext.userIdentity.getEmail()).isEqualTo("octocat@github.com");
    assertThat(callbackContext.redirectedToRequestedPage.get()).isTrue();

    // Verify the requests sent to GitHub
    RecordedRequest accessTokenGitHubRequest = github.takeRequest();
    assertThat(accessTokenGitHubRequest.getMethod()).isEqualTo("POST");
    assertThat(accessTokenGitHubRequest.getPath()).isEqualTo("/login/oauth/access_token");
    assertThat(accessTokenGitHubRequest.getBody().readUtf8()).isEqualTo(
      "code=the-verifier-code" +
        "&redirect_uri=" + URLEncoder.encode(CALLBACK_URL, StandardCharsets.UTF_8.name()) +
        "&grant_type=authorization_code");

    RecordedRequest profileGitHubRequest = github.takeRequest();
    assertThat(profileGitHubRequest.getMethod()).isEqualTo("GET");
    assertThat(profileGitHubRequest.getPath()).isEqualTo("/user");
  }

  @Test
  public void should_retrieve_private_primary_verified_email_address() {
    github.enqueue(newSuccessfulAccessTokenResponse());
    // response of api.github.com/user
    github.enqueue(new MockResponse().setBody("{\"id\":\"ABCD\", \"login\":\"octocat\", \"name\":\"monalisa octocat\",\"email\":null}"));
    // response of api.github.com/user/emails
    github.enqueue(new MockResponse().setBody(
      "[\n" +
        "  {\n" +
        "    \"email\": \"support@github.com\",\n" +
        "    \"verified\": false,\n" +
        "    \"primary\": false\n" +
        "  },\n" +
        "  {\n" +
        "    \"email\": \"octocat@github.com\",\n" +
        "    \"verified\": true,\n" +
        "    \"primary\": true\n" +
        "  },\n" +
        "]"));

    HttpServletRequest request = newRequest("the-verifier-code");
    DumbCallbackContext callbackContext = new DumbCallbackContext(request);
    underTest.callback(callbackContext);

    assertThat(callbackContext.csrfStateVerified.get()).isTrue();
    assertThat(callbackContext.userIdentity.getProviderId()).isEqualTo("ABCD");
    assertThat(callbackContext.userIdentity.getProviderLogin()).isEqualTo("octocat");
    assertThat(callbackContext.userIdentity.getName()).isEqualTo("monalisa octocat");
    assertThat(callbackContext.userIdentity.getEmail()).isEqualTo("octocat@github.com");
    assertThat(callbackContext.redirectedToRequestedPage.get()).isTrue();
  }

  @Test
  public void should_not_fail_if_no_email() {
    github.enqueue(newSuccessfulAccessTokenResponse());
    // response of api.github.com/user
    github.enqueue(new MockResponse().setBody("{\"id\":\"ABCD\", \"login\":\"octocat\", \"name\":\"monalisa octocat\",\"email\":null}"));
    // response of api.github.com/user/emails
    github.enqueue(new MockResponse().setBody("[]"));

    HttpServletRequest request = newRequest("the-verifier-code");
    DumbCallbackContext callbackContext = new DumbCallbackContext(request);
    underTest.callback(callbackContext);

    assertThat(callbackContext.csrfStateVerified.get()).isTrue();
    assertThat(callbackContext.userIdentity.getProviderId()).isEqualTo("ABCD");
    assertThat(callbackContext.userIdentity.getProviderLogin()).isEqualTo("octocat");
    assertThat(callbackContext.userIdentity.getName()).isEqualTo("monalisa octocat");
    assertThat(callbackContext.userIdentity.getEmail()).isNull();
    assertThat(callbackContext.redirectedToRequestedPage.get()).isTrue();
  }

  @Test
  public void redirect_browser_to_github_authentication_form_with_group_sync() throws Exception {
    settings.setProperty("sonar.auth.github.groupsSync", true);
    DumbInitContext context = new DumbInitContext("the-csrf-state");
    underTest.init(context);
    assertThat(context.redirectedTo).isEqualTo(
      gitHubSettings.webURL() +
        "login/oauth/authorize" +
        "?response_type=code" +
        "&client_id=the_id" +
        "&redirect_uri=" + URLEncoder.encode(CALLBACK_URL, StandardCharsets.UTF_8.name()) +
        "&scope=" + URLEncoder.encode("user:email,read:org", StandardCharsets.UTF_8.name()) +
        "&state=the-csrf-state");
  }

  @Test
  public void callback_on_successful_authentication_with_group_sync() {
    settings.setProperty("sonar.auth.github.groupsSync", true);

    github.enqueue(newSuccessfulAccessTokenResponse());
    // response of api.github.com/user
    github.enqueue(new MockResponse().setBody("{\"id\":\"ABCD\", \"login\":\"octocat\", \"name\":\"monalisa octocat\",\"email\":\"octocat@github.com\"}"));
    // response of api.github.com/user/teams
    github.enqueue(new MockResponse().setBody("[\n" +
      "  {\n" +
      "    \"slug\": \"developers\",\n" +
      "    \"organization\": {\n" +
      "      \"login\": \"SonarSource\"\n" +
      "    }\n" +
      "  }\n" +
      "]"));

    HttpServletRequest request = newRequest("the-verifier-code");
    DumbCallbackContext callbackContext = new DumbCallbackContext(request);
    underTest.callback(callbackContext);

    assertThat(callbackContext.userIdentity.getGroups()).containsOnly("SonarSource/developers");
  }

  @Test
  public void callback_on_successful_authentication_with_group_sync_on_many_pages() {
    settings.setProperty("sonar.auth.github.groupsSync", true);

    github.enqueue(newSuccessfulAccessTokenResponse());
    // response of api.github.com/user
    github.enqueue(new MockResponse().setBody("{\"id\":\"ABCD\", \"login\":\"octocat\", \"name\":\"monalisa octocat\",\"email\":\"octocat@github.com\"}"));
    // responses of api.github.com/user/teams
    github.enqueue(new MockResponse()
      .setHeader("Link", "<" + gitHubUrl + "/user/teams?per_page=100&page=2>; rel=\"next\", <" + gitHubUrl + "/user/teams?per_page=100&page=2>; rel=\"last\"")
      .setBody("[\n" +
        "  {\n" +
        "    \"slug\": \"developers\",\n" +
        "    \"organization\": {\n" +
        "      \"login\": \"SonarSource\"\n" +
        "    }\n" +
        "  }\n" +
        "]"));
    github.enqueue(new MockResponse()
      .setHeader("Link", "<" + gitHubUrl + "/user/teams?per_page=100&page=1>; rel=\"prev\", <" + gitHubUrl + "/user/teams?per_page=100&page=1>; rel=\"first\"")
      .setBody("[\n" +
        "  {\n" +
        "    \"slug\": \"sonarsource-developers\",\n" +
        "    \"organization\": {\n" +
        "      \"login\": \"SonarQubeCommunity\"\n" +
        "    }\n" +
        "  }\n" +
        "]"));

    HttpServletRequest request = newRequest("the-verifier-code");
    DumbCallbackContext callbackContext = new DumbCallbackContext(request);
    underTest.callback(callbackContext);

    assertThat(new TreeSet<>(callbackContext.userIdentity.getGroups())).containsOnly("SonarQubeCommunity/sonarsource-developers", "SonarSource/developers");
  }

  @Test
  public void redirect_browser_to_github_authentication_form_with_organizations() throws Exception {
    settings.setProperty("sonar.auth.github.organizations", "example0, example1");
    DumbInitContext context = new DumbInitContext("the-csrf-state");
    underTest.init(context);
    assertThat(context.redirectedTo).isEqualTo(
      gitHubSettings.webURL() +
        "login/oauth/authorize" +
        "?response_type=code" +
        "&client_id=the_id" +
        "&redirect_uri=" + URLEncoder.encode(CALLBACK_URL, StandardCharsets.UTF_8.name()) +
        "&scope=" + URLEncoder.encode("user:email,read:org", StandardCharsets.UTF_8.name()) +
        "&state=the-csrf-state");
  }

  @Test
  public void callback_on_successful_authentication_with_organizations_with_membership() {
    settings.setProperty("sonar.auth.github.organizations", "example0, example1");

    github.enqueue(newSuccessfulAccessTokenResponse());
    // response of api.github.com/user
    github.enqueue(new MockResponse().setBody("{\"id\":\"ABCD\", \"login\":\"octocat\", \"name\":\"monalisa octocat\",\"email\":\"octocat@github.com\"}"));
    // response of api.github.com/orgs/example0/members/user
    github.enqueue(new MockResponse().setResponseCode(204));

    HttpServletRequest request = newRequest("the-verifier-code");
    DumbCallbackContext callbackContext = new DumbCallbackContext(request);
    underTest.callback(callbackContext);

    assertThat(callbackContext.csrfStateVerified.get()).isTrue();
    assertThat(callbackContext.userIdentity).isNotNull();
    assertThat(callbackContext.redirectedToRequestedPage.get()).isTrue();
  }

  @Test
  public void callback_on_successful_authentication_with_organizations_without_membership() {
    settings.setProperty("sonar.auth.github.organizations", "first_org,second_org");

    github.enqueue(newSuccessfulAccessTokenResponse());
    // response of api.github.com/user
    github.enqueue(new MockResponse().setBody("{\"id\":\"ABCD\", \"login\":\"octocat\", \"name\":\"monalisa octocat\",\"email\":\"octocat@github.com\"}"));
    // response of api.github.com/orgs/first_org/members/user
    github.enqueue(new MockResponse().setResponseCode(404).setBody("{}"));
    // response of api.github.com/orgs/second_org/members/user
    github.enqueue(new MockResponse().setResponseCode(404).setBody("{}"));

    HttpServletRequest request = newRequest("the-verifier-code");
    DumbCallbackContext callbackContext = new DumbCallbackContext(request);
    try {
      underTest.callback(callbackContext);
      fail("exception expected");
    } catch (UnauthorizedException e) {
      assertThat(e.getMessage()).isEqualTo("'octocat' must be a member of at least one organization: 'first_org', 'second_org'");
    }
  }

  @Test
  public void callback_throws_ISE_if_error_when_requesting_user_profile() {
    github.enqueue(newSuccessfulAccessTokenResponse());
    // api.github.com/user crashes
    github.enqueue(new MockResponse().setResponseCode(500).setBody("{error}"));

    DumbCallbackContext callbackContext = new DumbCallbackContext(newRequest("the-verifier-code"));
    try {
      underTest.callback(callbackContext);
      fail("exception expected");
    } catch (IllegalStateException e) {
      assertThat(e.getMessage()).isEqualTo("Fail to execute request '" + gitHubSettings.apiURL() + "user'. HTTP code: 500, response: {error}");
    }

    assertThat(callbackContext.csrfStateVerified.get()).isTrue();
    assertThat(callbackContext.userIdentity).isNull();
    assertThat(callbackContext.redirectedToRequestedPage.get()).isFalse();
  }

  @Test
  public void callback_throws_ISE_if_error_when_checking_membership() {
    settings.setProperty("sonar.auth.github.organizations", "example");

    github.enqueue(newSuccessfulAccessTokenResponse());
    // response of api.github.com/user
    github.enqueue(new MockResponse().setBody("{\"id\":\"ABCD\", \"login\":\"octocat\", \"name\":\"monalisa octocat\",\"email\":\"octocat@github.com\"}"));
    // crash of api.github.com/orgs/example/members/user
    github.enqueue(new MockResponse().setResponseCode(500).setBody("{error}"));

    HttpServletRequest request = newRequest("the-verifier-code");
    DumbCallbackContext callbackContext = new DumbCallbackContext(request);
    try {
      underTest.callback(callbackContext);
      fail("exception expected");
    } catch (IllegalStateException e) {
      assertThat(e.getMessage()).isEqualTo("Fail to execute request '" + gitHubSettings.apiURL() + "orgs/example/members/octocat'. HTTP code: 500, response: {error}");
    }
  }

  /**
   * Response sent by GitHub to SonarQube when generating an access token
   */
  private static MockResponse newSuccessfulAccessTokenResponse() {
    // github does not return the standard JSON format but plain-text
    // see https://developer.github.com/v3/oauth/
    return new MockResponse().setBody("access_token=e72e16c7e42f292c6912e7710c838347ae178b4a&scope=user%2Cgist&token_type=bearer");
  }

  private static HttpServletRequest newRequest(String verifierCode) {
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getParameter("code")).thenReturn(verifierCode);
    return request;
  }

  private static class DumbCallbackContext implements OAuth2IdentityProvider.CallbackContext {
    final HttpServletRequest request;
    final AtomicBoolean csrfStateVerified = new AtomicBoolean(false);
    final AtomicBoolean redirectedToRequestedPage = new AtomicBoolean(false);
    UserIdentity userIdentity = null;

    public DumbCallbackContext(HttpServletRequest request) {
      this.request = request;
    }

    @Override
    public void verifyCsrfState() {
      this.csrfStateVerified.set(true);
    }

    @Override
    public void verifyCsrfState(String parameterName) {
      throw new UnsupportedOperationException("not used");
    }

    @Override
    public void redirectToRequestedPage() {
      redirectedToRequestedPage.set(true);
    }

    @Override
    public void authenticate(UserIdentity userIdentity) {
      this.userIdentity = userIdentity;
    }

    @Override
    public String getCallbackUrl() {
      return CALLBACK_URL;
    }

    @Override
    public HttpServletRequest getRequest() {
      return request;
    }

    @Override
    public HttpServletResponse getResponse() {
      throw new UnsupportedOperationException("not used");
    }
  }

  private static class DumbInitContext implements OAuth2IdentityProvider.InitContext {
    String redirectedTo = null;
    private final String generatedCsrfState;

    public DumbInitContext(String generatedCsrfState) {
      this.generatedCsrfState = generatedCsrfState;
    }

    @Override
    public String generateCsrfState() {
      return generatedCsrfState;
    }

    @Override
    public void redirectTo(String url) {
      this.redirectedTo = url;
    }

    @Override
    public String getCallbackUrl() {
      return CALLBACK_URL;
    }

    @Override
    public HttpServletRequest getRequest() {
      return null;
    }

    @Override
    public HttpServletResponse getResponse() {
      return null;
    }
  }
}
