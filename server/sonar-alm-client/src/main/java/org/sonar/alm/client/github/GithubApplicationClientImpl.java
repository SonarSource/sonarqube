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

import com.google.gson.Gson;
import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.sonar.alm.client.github.GithubApplicationHttpClient.GetResponse;
import org.sonar.alm.client.github.GithubBinding.GsonGithubRepository;
import org.sonar.alm.client.github.GithubBinding.GsonInstallations;
import org.sonar.alm.client.github.GithubBinding.GsonRepositorySearch;
import org.sonar.alm.client.github.security.AccessToken;
import org.sonar.alm.client.github.security.UserAccessToken;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.String.format;
import static java.net.HttpURLConnection.HTTP_OK;

public class GithubApplicationClientImpl implements GithubApplicationClient {
  private static final Logger LOG = Loggers.get(GithubApplicationClientImpl.class);
  private static final Gson GSON = new Gson();

  private final GithubApplicationHttpClient appHttpClient;

  public GithubApplicationClientImpl(GithubApplicationHttpClient appHttpClient) {
    this.appHttpClient = appHttpClient;
  }

  private static void checkPageArgs(int page, int pageSize) {
    checkArgument(page > 0, "'page' must be larger than 0.");
    checkArgument(pageSize > 0 && pageSize <= 100, "'pageSize' must be a value larger than 0 and smaller or equal to 100.");
  }

  @Override
  public Organizations listOrganizations(String appUrl, AccessToken accessToken, int page, int pageSize) {
    checkPageArgs(page, pageSize);

    try {
      Organizations organizations = new Organizations();
      GetResponse response = appHttpClient.get(appUrl, accessToken, String.format("/user/installations?page=%s&per_page=%s", page, pageSize));
      Optional<GsonInstallations> gsonInstallations = response.getContent().map(content -> GSON.fromJson(content, GsonInstallations.class));

      if (!gsonInstallations.isPresent()) {
        return organizations;
      }

      organizations.setTotal(gsonInstallations.get().totalCount);
      if (gsonInstallations.get().installations != null) {
        organizations.setOrganizations(gsonInstallations.get().installations.stream()
          .map(gsonInstallation -> new Organization(gsonInstallation.account.id, gsonInstallation.account.login, null, null, null, null, null,
            gsonInstallation.targetType))
          .collect(Collectors.toList()));
      }

      return organizations;
    } catch (IOException e) {
      throw new IllegalStateException(format("Failed to list all organizations accessible by user access token on %s", appUrl), e);
    }
  }

  @Override
  public Repositories listRepositories(String appUrl, AccessToken accessToken, String organization, @Nullable String query, int page, int pageSize) {
    checkPageArgs(page, pageSize);
    String searchQuery = "org:" + organization;
    if (query != null) {
      searchQuery = query.replace(" ", "+") + "+" + searchQuery;
    }
    try {
      Repositories repositories = new Repositories();
      GetResponse response = appHttpClient.get(appUrl, accessToken, String.format("/search/repositories?q=%s&page=%s&per_page=%s", searchQuery, page, pageSize));
      Optional<GsonRepositorySearch> gsonRepositories = response.getContent().map(content -> GSON.fromJson(content, GsonRepositorySearch.class));
      if (!gsonRepositories.isPresent()) {
        return repositories;
      }

      repositories.setTotal(gsonRepositories.get().totalCount);

      if (gsonRepositories.get().items != null) {
        repositories.setRepositories(gsonRepositories.get().items.stream()
          .map(gsonRepository -> new Repository(gsonRepository.id, gsonRepository.name, gsonRepository.isPrivate, gsonRepository.fullName, gsonRepository.url))
          .collect(Collectors.toList()));
      }

      return repositories;
    } catch (Exception e) {
      throw new IllegalStateException(format("Failed to list all repositories of '%s' accessible by user access token on '%s' using query '%s'", organization, appUrl, searchQuery),
        e);
    }
  }

  @Override
  public Optional<Repository> getRepository(String appUrl, AccessToken accessToken, String organization, String repositoryKey) {
    try {
      GetResponse response = appHttpClient.get(appUrl, accessToken, String.format("/repos/%s", repositoryKey));
      return response.getContent()
        .map(content -> GSON.fromJson(content, GsonGithubRepository.class))
        .map(repository -> new Repository(repository.id, repository.name, repository.isPrivate, repository.fullName, repository.url));
    } catch (Exception e) {
      throw new IllegalStateException(format("Failed to get repository '%s' of '%s' accessible by user access token on '%s'", repositoryKey, organization, appUrl), e);
    }
  }

  @Override
  public UserAccessToken createUserAccessToken(String appUrl, String clientId, String clientSecret, String code) {
    try {
      String endpoint = "/login/oauth/access_token?client_id=" + clientId + "&client_secret=" + clientSecret + "&code=" + code;

      String baseAppUrl;
      int apiIndex = appUrl.indexOf("/api/v3");
      if (apiIndex > 0) {
        baseAppUrl = appUrl.substring(0, apiIndex);
      } else if (appUrl.startsWith("https://api.github.com")) {
        baseAppUrl = "https://github.com";
      } else {
        baseAppUrl = appUrl;
      }

      GithubApplicationHttpClient.Response response = appHttpClient.post(baseAppUrl, null, endpoint);

      if (response.getCode() != HTTP_OK) {
        throw new IllegalStateException("Failed to create GitHub's user access token. GitHub returned code " + code + ". " + response.getContent().orElse(""));
      }

      Optional<String> content = response.getContent();
      Optional<UserAccessToken> accessToken = content.flatMap(c -> Arrays.stream(c.split("&"))
        .filter(t -> t.startsWith("access_token="))
        .map(t -> t.split("=")[1])
        .findAny())
        .map(UserAccessToken::new);

      if (accessToken.isPresent()) {
        return accessToken.get();
      }

      // If token is not in the 200's body, it's because the client ID or client secret are incorrect
      LOG.error("Failed to create GitHub's user access token. GitHub's response: " + content);
      throw new IllegalArgumentException();
    } catch (IOException e) {
      throw new IllegalStateException("Failed to create GitHub's user access token", e);
    }
  }
}
