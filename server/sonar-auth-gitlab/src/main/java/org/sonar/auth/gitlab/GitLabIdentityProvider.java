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
package org.sonar.auth.gitlab;

import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.OAuthConstants;
import com.github.scribejava.core.oauth.OAuth20Service;
import com.google.common.annotations.VisibleForTesting;
import jakarta.inject.Inject;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.server.authentication.Display;
import org.sonar.api.server.authentication.OAuth2IdentityProvider;
import org.sonar.api.server.authentication.UnauthorizedException;
import org.sonar.api.server.authentication.UserIdentity;
import org.sonar.api.server.http.HttpRequest;

import static com.google.common.base.Preconditions.checkState;
import static java.util.stream.Collectors.toSet;

public class GitLabIdentityProvider implements OAuth2IdentityProvider {

  private static final Logger LOG = LoggerFactory.getLogger(GitLabIdentityProvider.class);

  public static final String KEY = "gitlab";

  private static final int MAX_CONCURRENT_REQUESTS = 5;
  private final GitLabSettings gitLabSettings;
  private final ScribeGitLabOauth2Api scribeApi;
  private final GitLabRestClient gitLabRestClient;
  private final GitLabGraphQlClient gitLabGraphQlClient;
  private final ScribeFactory scribeFactory;

  @Inject
  public GitLabIdentityProvider(GitLabSettings gitLabSettings, GitLabRestClient gitLabRestClient, GitLabGraphQlClient gitLabGraphQlClient,
    ScribeGitLabOauth2Api scribeApi) {
    this(gitLabSettings, gitLabRestClient, gitLabGraphQlClient, scribeApi, new ScribeFactory());
  }

  @VisibleForTesting
  GitLabIdentityProvider(GitLabSettings gitLabSettings, GitLabRestClient gitLabRestClient, GitLabGraphQlClient gitLabGraphQlClient,
    ScribeGitLabOauth2Api scribeApi, ScribeFactory scribeFactory) {
    this.gitLabSettings = gitLabSettings;
    this.scribeApi = scribeApi;
    this.gitLabRestClient = gitLabRestClient;
    this.gitLabGraphQlClient = gitLabGraphQlClient;
    this.scribeFactory = scribeFactory;
  }

  @Override
  public String getKey() {
    return KEY;
  }

  @Override
  public String getName() {
    return "GitLab";
  }

  @Override
  public Display getDisplay() {
    return Display.builder()
      .setIconPath("/images/alm/gitlab.svg")
      .setBackgroundColor("#6a4fbb")
      .build();
  }

  @Override
  public boolean isEnabled() {
    return gitLabSettings.isEnabled();
  }

  @Override
  public boolean allowsUsersToSignUp() {
    return gitLabSettings.allowUsersToSignUp();
  }

  @Override
  public void init(InitContext context) {
    String state = context.generateCsrfState();
    try (OAuth20Service scribe = scribeFactory.newScribe(gitLabSettings, context.getCallbackUrl(), scribeApi)) {
      String url = scribe.getAuthorizationUrl(state);
      context.redirectTo(url);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  public void callback(CallbackContext context) {
    try (OAuth20Service scribe = scribeFactory.newScribe(gitLabSettings, context.getCallbackUrl(), scribeApi)) {
      onCallback(context, scribe);
    } catch (IOException | ExecutionException e) {
      throw new IllegalStateException(e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(e);
    }
  }

  private void onCallback(CallbackContext context, OAuth20Service scribe) throws InterruptedException, ExecutionException, IOException {
    HttpRequest request = context.getHttpRequest();
    String code = request.getParameter(OAuthConstants.CODE);
    OAuth2AccessToken accessToken = scribe.getAccessToken(code);
    GsonUser user = gitLabRestClient.getUser(scribe, accessToken);

    UserIdentity.Builder builder = UserIdentity.builder()
      .setProviderId(Long.toString(user.getId()))
      .setProviderLogin(user.getUsername())
      .setName(user.getName())
      .setEmail(user.getEmail());

    if (gitLabSettings.syncUserGroups()) {
      Set<String> userGroups = getGroups(accessToken);
      validateUserInAllowedGroups(user.getUsername(), userGroups);
      builder.setGroups(userGroups);
    }
    context.authenticate(builder.build());
    context.redirectToRequestedPage();
  }

  private void validateUserInAllowedGroups(String gitlabUserName, Set<String> userGroups) {
    if (gitLabSettings.allowedGroups().isEmpty()) {
      return;
    }

    boolean allowedUser = userGroups.stream()
      .anyMatch(gitLabSettings::isAllowedGroup);

    if (!allowedUser) {
      LOG.info("Login for user with GitLab user name {} rejected, as the user do not belong to the allowlisted groups", gitlabUserName);
      throw new UnauthorizedException("You are not allowed to authenticate");
    }
  }

  private Set<String> getGroups(OAuth2AccessToken accessToken) {
    Set<String> allowedGroups = gitLabSettings.allowedGroups();
    List<GsonGroup> groups;
    if (allowedGroups.isEmpty()) {
      groups = gitLabGraphQlClient.getGroups(accessToken.getAccessToken(), null);
    } else {
      groups = findGroupsUsingGraphQlApiInParallel(accessToken, allowedGroups);
    }
    return groups.stream()
      .map(GsonGroup::getFullPath)
      .collect(toSet());
  }

  private List<GsonGroup> findGroupsUsingGraphQlApiInParallel(OAuth2AccessToken accessToken, Set<String> allowedGroups) {
    try (var executor = Executors.newFixedThreadPool(MAX_CONCURRENT_REQUESTS)) {
      List<Future<List<GsonGroup>>> futures = allowedGroups.stream()
        .map(group -> executor.submit(() -> gitLabGraphQlClient.getGroups(accessToken.getAccessToken(), group)))
        .toList();
      return futures.stream()
        .flatMap(future -> getResult(future).stream())
        .toList();
    }
  }

  private static <T> T getResult(Future<T> future) {
    try {
      return future.get();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(e);
    } catch (ExecutionException e) {
      throw new IllegalStateException(e.getCause());
    }
  }

  static class ScribeFactory {

    private static final String API_SCOPE = "api";
    private static final String READ_USER_SCOPE = "read_user";

    OAuth20Service newScribe(GitLabSettings gitLabSettings, String callbackUrl, ScribeGitLabOauth2Api scribeApi) {
      checkState(gitLabSettings.isEnabled(), "GitLab authentication is disabled");
      return new ServiceBuilder(gitLabSettings.applicationId())
        .apiSecret(gitLabSettings.secret())
        .defaultScope(gitLabSettings.syncUserGroups() ? API_SCOPE : READ_USER_SCOPE)
        .callback(callbackUrl)
        .build(scribeApi);
    }
  }

}
