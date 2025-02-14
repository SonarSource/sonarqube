/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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

import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth20Service;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.String.format;
import static org.sonar.auth.OAuthRestClient.executePaginatedRequest;
import static org.sonar.auth.OAuthRestClient.executeRequest;

public class GitHubRestClient {

  private static final Logger LOGGER = LoggerFactory.getLogger(GitHubRestClient.class);

  private final GitHubSettings settings;

  public GitHubRestClient(GitHubSettings settings) {
    this.settings = settings;
  }

  public GsonUser getUser(OAuth20Service scribe, OAuth2AccessToken accessToken) throws IOException {
    String responseBody = executeRequest(settings.apiURL() + "user", scribe, accessToken).getBody();
    LOGGER.trace("User response received : {}", responseBody);
    return GsonUser.parse(responseBody);
  }

  public String getEmail(OAuth20Service scribe, OAuth2AccessToken accessToken) throws IOException {
    String responseBody = executeRequest(settings.apiURL() + "user/emails", scribe, accessToken).getBody();
    LOGGER.trace("Emails response received : {}", responseBody);
    List<GsonEmail> emails = GsonEmail.parse(responseBody);
    return emails.stream()
      .filter(email -> email.isPrimary() && email.isVerified())
      .findFirst()
      .map(GsonEmail::getEmail)
      .orElse(null);
  }

  public List<GsonOrganization> getUserOrganizations(OAuth20Service scribe, OAuth2AccessToken accessToken) {
    List<GsonOrganization> gsonOrganizations = executePaginatedRequest(settings.apiURL() + "user/orgs", scribe, accessToken, GsonOrganization::parse);
    LOGGER.trace("Organizations response received : {}", gsonOrganizations);
    return gsonOrganizations;
  }

  public List<GsonTeam> getTeams(OAuth20Service scribe, OAuth2AccessToken accessToken) {
    return executePaginatedRequest(settings.apiURL() + "user/teams", scribe, accessToken, GsonTeam::parse);
  }
}
