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
package org.sonar.auth.bitbucket;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
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
import static java.net.URLEncoder.encode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class IntegrationTest {

  private static final String CALLBACK_URL = "http://localhost/oauth/callback/bitbucket";

  @Rule
  public MockWebServer bitbucket = new MockWebServer();

  // load settings with default values
  private final MapSettings settings = new MapSettings(new PropertyDefinitions(System2.INSTANCE, BitbucketSettings.definitions()));

  private final BitbucketSettings bitbucketSettings = spy(new BitbucketSettings(settings.asConfig()));
  private final UserIdentityFactory userIdentityFactory = new UserIdentityFactory();
  private final BitbucketScribeApi scribeApi = new BitbucketScribeApi(bitbucketSettings);
  private final BitbucketIdentityProvider underTest = new BitbucketIdentityProvider(bitbucketSettings, userIdentityFactory, scribeApi);

  @Before
  public void setUp() {
    settings.setProperty("sonar.auth.bitbucket.clientId.secured", "the_id");
    settings.setProperty("sonar.auth.bitbucket.clientSecret.secured", "the_secret");
    settings.setProperty("sonar.auth.bitbucket.enabled", true);
    when(bitbucketSettings.webURL()).thenReturn(format("http://%s:%d/", bitbucket.getHostName(), bitbucket.getPort()));
    when(bitbucketSettings.apiURL()).thenReturn(format("http://%s:%d/", bitbucket.getHostName(), bitbucket.getPort()));
  }

  /**
   * First phase: SonarQube redirects browser to Bitbucket authentication form, requesting the
   * minimal access rights ("scope") to get user profile.
   */
  @Test
  public void redirect_browser_to_bitbucket_authentication_form() throws Exception {
    DumbInitContext context = new DumbInitContext("the-csrf-state");
    underTest.init(context);
    assertThat(context.redirectedTo)
      .startsWith(bitbucket.url("site/oauth2/authorize").toString())
      .contains("scope=" + encode("account", StandardCharsets.UTF_8.name()));
  }

  /**
   * Second phase: Bitbucket redirects browser to SonarQube at /oauth/callback/bitbucket?code={the verifier code}.
   * This SonarQube web service sends three requests to Bitbucket:
   * <ul>
   *   <li>get an access token</li>
   *   <li>get the profile (login, name) of the authenticated user</li>
   *   <li>get the emails of the authenticated user</li>
   * </ul>
   */
  @Test
  public void authenticate_successfully() throws Exception {
    bitbucket.enqueue(newSuccessfulAccessTokenResponse());
    bitbucket.enqueue(newUserResponse("john", "John", "john-uuid"));
    bitbucket.enqueue(newPrimaryEmailResponse("john@bitbucket.org"));

    HttpServletRequest request = newRequest("the-verifier-code");
    DumbCallbackContext callbackContext = new DumbCallbackContext(request);
    underTest.callback(callbackContext);

    assertThat(callbackContext.csrfStateVerified.get()).isTrue();
    assertThat(callbackContext.userIdentity.getName()).isEqualTo("John");
    assertThat(callbackContext.userIdentity.getEmail()).isEqualTo("john@bitbucket.org");
    assertThat(callbackContext.redirectedToRequestedPage.get()).isTrue();

    // Verify the requests sent to Bitbucket
    RecordedRequest accessTokenRequest = bitbucket.takeRequest();
    assertThat(accessTokenRequest.getPath()).startsWith("/site/oauth2/access_token");
    RecordedRequest userRequest = bitbucket.takeRequest();
    assertThat(userRequest.getPath()).startsWith("/2.0/user");
    RecordedRequest emailRequest = bitbucket.takeRequest();
    assertThat(emailRequest.getPath()).startsWith("/2.0/user/emails");
    // do not request user workspaces, workspace restriction is disabled by default
    assertThat(bitbucket.getRequestCount()).isEqualTo(3);
  }

  @Test
  public void callback_throws_ISE_if_error_when_requesting_user_profile() {
    bitbucket.enqueue(newSuccessfulAccessTokenResponse());
    // https://api.bitbucket.org/2.0/user fails
    bitbucket.enqueue(new MockResponse().setResponseCode(500).setBody("{error}"));

    DumbCallbackContext callbackContext = new DumbCallbackContext(newRequest("the-verifier-code"));

    assertThatThrownBy(() -> underTest.callback(callbackContext))
      .hasMessage("Can not get Bitbucket user profile. HTTP code: 500, response: {error}")
      .isInstanceOf(IllegalStateException.class);

    assertThat(callbackContext.csrfStateVerified.get()).isTrue();
    assertThat(callbackContext.userIdentity).isNull();
    assertThat(callbackContext.redirectedToRequestedPage.get()).isFalse();
  }

  @Test
  public void allow_authentication_if_user_is_member_of_one_restricted_workspace() {
    settings.setProperty("sonar.auth.bitbucket.workspaces", new String[] {"workspace1", "workspace2"});

    bitbucket.enqueue(newSuccessfulAccessTokenResponse());
    bitbucket.enqueue(newUserResponse("john", "John", "john-uuid"));
    bitbucket.enqueue(newPrimaryEmailResponse("john@bitbucket.org"));
    bitbucket.enqueue(newWorkspacesResponse("workspace3", "workspace2"));

    HttpServletRequest request = newRequest("the-verifier-code");
    DumbCallbackContext callbackContext = new DumbCallbackContext(request);
    underTest.callback(callbackContext);

    assertThat(callbackContext.userIdentity.getEmail()).isEqualTo("john@bitbucket.org");
    assertThat(callbackContext.userIdentity.getProviderLogin()).isEqualTo("john");
    assertThat(callbackContext.userIdentity.getProviderId()).isEqualTo("john-uuid");
    assertThat(callbackContext.redirectedToRequestedPage.get()).isTrue();
  }

  @Test
  public void forbid_authentication_if_user_is_not_member_of_one_restricted_workspace() {
    settings.setProperty("sonar.auth.bitbucket.workspaces", new String[] {"workspace1", "workspace2"});

    bitbucket.enqueue(newSuccessfulAccessTokenResponse());
    bitbucket.enqueue(newUserResponse("john", "John", "john-uuid"));
    bitbucket.enqueue(newPrimaryEmailResponse("john@bitbucket.org"));
    bitbucket.enqueue(newWorkspacesResponse("workspace3"));
    DumbCallbackContext context = new DumbCallbackContext(newRequest("the-verifier-code"));

    assertThatThrownBy(() -> underTest.callback(context))
      .isInstanceOf(UnauthorizedException.class);
  }

  @Test
  public void forbid_authentication_if_user_is_not_member_of_any_workspace() {
    settings.setProperty("sonar.auth.bitbucket.workspaces", new String[] {"workspace1", "workspace2"});

    bitbucket.enqueue(newSuccessfulAccessTokenResponse());
    bitbucket.enqueue(newUserResponse("john", "John", "john-uuid"));
    bitbucket.enqueue(newPrimaryEmailResponse("john@bitbucket.org"));
    bitbucket.enqueue(newWorkspacesResponse(/* no workspaces */));
    DumbCallbackContext context = new DumbCallbackContext(newRequest("the-verifier-code"));

    assertThatThrownBy(() -> underTest.callback(context))
      .isInstanceOf(UnauthorizedException.class);
  }

  /**
   * Response sent by Bitbucket to SonarQube when generating an access token
   */
  private static MockResponse newSuccessfulAccessTokenResponse() {
    return new MockResponse().setBody("{\"access_token\":\"e72e16c7e42f292c6912e7710c838347ae178b4a\",\"scope\":\"user\"}");
  }

  /**
   * Response of https://api.bitbucket.org/2.0/user
   */
  private static MockResponse newUserResponse(String login, String name, String uuid) {
    return new MockResponse().setBody("{\"username\":\"" + login + "\", \"display_name\":\"" + name + "\", \"uuid\":\"" + uuid + "\"}");
  }

  /**
   * Response of https://api.bitbucket.org/2.0/user/permissions/workspaces?q=permission="member"
   */
  private static MockResponse newWorkspacesResponse(String... workspaces) {
    String s = Arrays.stream(workspaces)
      .map(w -> "{\"workspace\":{\"name\":\"" + w + "\",\"slug\":\"" + w + "\"}}")
      .collect(Collectors.joining(","));
    return new MockResponse().setBody("{\"values\":[" + s + "]}");
  }

  /**
   * Response of https://api.bitbucket.org/2.0/user/emails
   */
  private static MockResponse newPrimaryEmailResponse(String email) {
    return new MockResponse().setBody("{\"values\":[{\"active\": true,\"email\":\"" + email + "\",\"is_primary\": true}]}");
  }

  private static HttpServletRequest newRequest(String verifierCode) {
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getParameter("code")).thenReturn(verifierCode);
    return request;
  }

  private static class DumbCallbackContext implements OAuth2IdentityProvider.CallbackContext {
    final HttpServletRequest request;
    final AtomicBoolean csrfStateVerified = new AtomicBoolean(true);
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
    public void verifyCsrfState(String s) {
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
