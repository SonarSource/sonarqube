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

import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.builder.ServiceBuilderOAuth20;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.OAuthConstants;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth20Service;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import javax.annotation.CheckForNull;
import javax.servlet.http.HttpServletRequest;
import org.sonar.api.server.ServerSide;
import org.sonar.api.server.authentication.Display;
import org.sonar.api.server.authentication.OAuth2IdentityProvider;
import org.sonar.api.server.authentication.UnauthorizedException;
import org.sonar.api.server.authentication.UserIdentity;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toSet;

@ServerSide
public class BitbucketIdentityProvider implements OAuth2IdentityProvider {

  private static final Logger LOGGER = Loggers.get(BitbucketIdentityProvider.class);

  public static final String REQUIRED_SCOPE = "account";
  public static final String KEY = "bitbucket";

  private final BitbucketSettings settings;
  private final UserIdentityFactory userIdentityFactory;
  private final BitbucketScribeApi scribeApi;

  public BitbucketIdentityProvider(BitbucketSettings settings, UserIdentityFactory userIdentityFactory, BitbucketScribeApi scribeApi) {
    this.settings = settings;
    this.userIdentityFactory = userIdentityFactory;
    this.scribeApi = scribeApi;
  }

  @Override
  public String getKey() {
    return KEY;
  }

  @Override
  public String getName() {
    return "Bitbucket";
  }

  @Override
  public Display getDisplay() {
    return Display.builder()
      .setIconPath("/images/alm/bitbucket-white.svg")
      .setBackgroundColor("#0052cc")
      .build();
  }

  @Override
  public boolean isEnabled() {
    return settings.isEnabled();
  }

  @Override
  public boolean allowsUsersToSignUp() {
    return settings.allowUsersToSignUp();
  }

  @Override
  public void init(InitContext context) {
    String state = context.generateCsrfState();
    OAuth20Service scribe = newScribeBuilder(context).build(scribeApi);
    String url = scribe.getAuthorizationUrl(state);
    context.redirectTo(url);
  }

  private ServiceBuilderOAuth20 newScribeBuilder(OAuth2Context context) {
    checkState(isEnabled(), "Bitbucket authentication is disabled");
    return new ServiceBuilder(settings.clientId())
      .apiSecret(settings.clientSecret())
      .callback(context.getCallbackUrl())
      .defaultScope(REQUIRED_SCOPE);
  }

  @Override
  public void callback(CallbackContext context) {
    try {
      onCallback(context);
    } catch (IOException | ExecutionException e) {
      throw new IllegalStateException(e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(e);
    }
  }

  private void onCallback(CallbackContext context) throws InterruptedException, ExecutionException, IOException {
    HttpServletRequest request = context.getRequest();
    OAuth20Service scribe = newScribeBuilder(context).build(scribeApi);
    String code = request.getParameter(OAuthConstants.CODE);
    OAuth2AccessToken accessToken = scribe.getAccessToken(code);

    GsonUser gsonUser = requestUser(scribe, accessToken);
    GsonEmails gsonEmails = requestEmails(scribe, accessToken);

    checkTeamRestriction(scribe, accessToken, gsonUser);

    UserIdentity userIdentity = userIdentityFactory.create(gsonUser, gsonEmails);
    context.authenticate(userIdentity);
    context.redirectToRequestedPage();
  }

  private GsonUser requestUser(OAuth20Service service, OAuth2AccessToken accessToken) throws InterruptedException, ExecutionException, IOException {
    OAuthRequest userRequest = new OAuthRequest(Verb.GET, settings.apiURL() + "2.0/user");
    service.signRequest(accessToken, userRequest);
    Response userResponse = service.execute(userRequest);

    if (!userResponse.isSuccessful()) {
      throw new IllegalStateException(format("Can not get Bitbucket user profile. HTTP code: %s, response: %s",
        userResponse.getCode(), userResponse.getBody()));
    }
    String userResponseBody = userResponse.getBody();
    return GsonUser.parse(userResponseBody);
  }

  @CheckForNull
  private GsonEmails requestEmails(OAuth20Service service, OAuth2AccessToken accessToken) throws InterruptedException, ExecutionException, IOException {
    OAuthRequest userRequest = new OAuthRequest(Verb.GET, settings.apiURL() + "2.0/user/emails");
    service.signRequest(accessToken, userRequest);
    Response emailsResponse = service.execute(userRequest);
    if (emailsResponse.isSuccessful()) {
      return GsonEmails.parse(emailsResponse.getBody());
    }
    return null;
  }

  private void checkTeamRestriction(OAuth20Service service, OAuth2AccessToken accessToken, GsonUser user) throws InterruptedException, ExecutionException, IOException {
    String[] workspaceAllowed = settings.workspaceAllowedList();
    if (workspaceAllowed != null && workspaceAllowed.length > 0) {
      GsonWorkspaceMemberships userWorkspaces = requestWorkspaces(service, accessToken);
      String errorMessage = format("User %s is not part of allowed workspaces list", user.getUsername());
      if (userWorkspaces == null || userWorkspaces.getWorkspaces() == null) {
        throw new UnauthorizedException(errorMessage);
      } else {
        Set<String> uniqueUserWorkspaces = new HashSet<>();
        uniqueUserWorkspaces.addAll(userWorkspaces.getWorkspaces().stream().map(w -> w.getWorkspace().getName()).collect(toSet()));
        uniqueUserWorkspaces.addAll(userWorkspaces.getWorkspaces().stream().map(w -> w.getWorkspace().getSlug()).collect(toSet()));
        List<String> workspaceAllowedList = asList(workspaceAllowed);
        if (uniqueUserWorkspaces.stream().noneMatch(workspaceAllowedList::contains)) {
          throw new UnauthorizedException(errorMessage);
        }
      }
    }
  }

  @CheckForNull
  private GsonWorkspaceMemberships requestWorkspaces(OAuth20Service service, OAuth2AccessToken accessToken) throws InterruptedException, ExecutionException, IOException {
    OAuthRequest userRequest = new OAuthRequest(Verb.GET, settings.apiURL() + "2.0/user/permissions/workspaces?q=permission=\"member\"");
    service.signRequest(accessToken, userRequest);
    Response teamsResponse = service.execute(userRequest);
    if (teamsResponse.isSuccessful()) {
      return GsonWorkspaceMemberships.parse(teamsResponse.getBody());
    }
    LOGGER.warn("Fail to retrieve the teams of Bitbucket user: {}", teamsResponse.getBody());
    return null;
  }

}
