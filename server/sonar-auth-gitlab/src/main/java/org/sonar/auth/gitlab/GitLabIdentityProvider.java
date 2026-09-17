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
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;
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
  private static final char GROUP_PATH_DELIMITER = '/';
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
    context.verifyCsrfState();
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
    if (gitLabSettings.allowedGroups().isEmpty() || gitLabSettings.allowAllGroups()) {
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
    Set<String> searchTerms = allowedGroups.stream()
      .flatMap(GitLabIdentityProvider::pathAndAncestors)
      .collect(toSet());
    List<GsonGroup> directGroups;
    if (allowedGroups.isEmpty() || gitLabSettings.allowAllGroups() || hasShortGroupName(searchTerms)) {
      // GitLab GraphQL API requires a minimum of 3 characters for group search queries.
      // When any search term (allowed group or one of its ancestors) is shorter than 3
      // characters, targeted search cannot be used, so all user groups are fetched instead.
      directGroups = gitLabGraphQlClient.getGroups(accessToken.getAccessToken(), null);
    } else {
      directGroups = fetchInParallel(searchTerms, term -> gitLabGraphQlClient.getGroups(accessToken.getAccessToken(), term));
    }
    Set<String> directGroupPaths = directGroups.stream()
      .map(GsonGroup::getFullPath)
      .collect(toSet());
    return withInheritedDescendantGroups(accessToken, directGroupPaths);
  }

  private static boolean hasShortGroupName(Set<String> groupNames) {
    return groupNames.stream().anyMatch(g -> g.length() < 3);
  }

  private static Stream<String> pathAndAncestors(String path) {
    return Stream.concat(
      IntStream.iterate(path.indexOf(GROUP_PATH_DELIMITER), i -> i > 0, i -> path.indexOf(GROUP_PATH_DELIMITER, i + 1)).mapToObj(i -> path.substring(0, i)),
      Stream.of(path));
  }

  private Set<String> withInheritedDescendantGroups(OAuth2AccessToken accessToken, Set<String> directGroupPaths) {
    Set<String> pathsToExpand = directGroupPaths.stream()
      .filter(path -> directGroupPaths.stream().noneMatch(other -> path.startsWith(other + GROUP_PATH_DELIMITER)))
      .collect(toSet());
    Set<String> allGroupPaths = new HashSet<>(directGroupPaths);
    fetchInParallel(pathsToExpand, path -> gitLabGraphQlClient.getDescendantGroups(accessToken.getAccessToken(), path))
      .forEach(descendant -> allGroupPaths.add(descendant.getFullPath()));
    return allGroupPaths;
  }

  private static List<GsonGroup> fetchInParallel(Collection<String> inputs, Function<String, List<GsonGroup>> fetcher) {
    if (inputs.isEmpty()) {
      return List.of();
    }
    try (var executor = Executors.newFixedThreadPool(MAX_CONCURRENT_REQUESTS)) {
      List<Future<List<GsonGroup>>> futures = inputs.stream()
        .map(input -> executor.submit(() -> fetcher.apply(input)))
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
