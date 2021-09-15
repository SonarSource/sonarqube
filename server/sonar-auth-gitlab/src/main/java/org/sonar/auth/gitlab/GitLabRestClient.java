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
package org.sonar.auth.gitlab;

import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.oauth.OAuth20Service;
import java.io.IOException;
import java.util.List;
import org.sonar.auth.OAuthRestClient;

public class GitLabRestClient {

  private static final String API_SUFFIX = "/api/v4";

  private final GitLabSettings settings;

  public GitLabRestClient(GitLabSettings settings) {
    this.settings = settings;
  }

  GsonUser getUser(OAuth20Service scribe, OAuth2AccessToken accessToken) {
    try (Response response = OAuthRestClient.executeRequest(settings.url() + API_SUFFIX + "/user", scribe, accessToken)) {
      String responseBody = response.getBody();
      return GsonUser.parse(responseBody);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to get gitlab user", e);
    }
  }

  List<GsonGroup> getGroups(OAuth20Service scribe, OAuth2AccessToken accessToken) {
    return OAuthRestClient.executePaginatedRequest(settings.url() + API_SUFFIX + "/groups?min_access_level=10", scribe, accessToken, GsonGroup::parse);
  }
}
