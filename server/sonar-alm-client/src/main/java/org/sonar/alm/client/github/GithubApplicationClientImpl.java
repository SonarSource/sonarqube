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
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

import org.sonar.alm.client.github.GithubApplicationHttpClient.GetResponse;
import org.sonar.alm.client.github.GithubBinding.GsonGithubRepository;
import org.sonar.alm.client.github.GithubBinding.GsonInstallations;
import org.sonar.alm.client.github.GithubBinding.GsonRepositorySearch;
import org.sonar.alm.client.github.config.GithubAppConfiguration;
import org.sonar.alm.client.github.security.AccessToken;
import org.sonar.alm.client.github.security.AppToken;
import org.sonar.alm.client.github.security.GithubAppSecurity;
import org.sonar.alm.client.github.security.UserAccessToken;
import org.sonar.alm.client.gitlab.GsonApp;
import org.sonar.api.internal.apachecommons.lang.StringUtils;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.String.format;
import static java.net.HttpURLConnection.HTTP_FORBIDDEN;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;
import static java.util.stream.Collectors.toList;

public class GithubApplicationClientImpl implements GithubApplicationClient {
  private static final Logger LOG = Loggers.get(GithubApplicationClientImpl.class);
  protected static final Gson GSON = new Gson();

  protected static final String WRITE_PERMISSION_NAME = "write";
  protected static final String READ_PERMISSION_NAME = "read";
  protected static final String FAILED_TO_REQUEST_BEGIN_MSG = "Failed to request ";

  protected final GithubApplicationHttpClient appHttpClient;
  protected final GithubAppSecurity appSecurity;

  public GithubApplicationClientImpl(GithubApplicationHttpClient appHttpClient, GithubAppSecurity appSecurity) {
    this.appHttpClient = appHttpClient;
    this.appSecurity = appSecurity;
  }

  private static void checkPageArgs(int page, int pageSize) {
    checkArgument(page > 0, "'page' must be larger than 0.");
    checkArgument(pageSize > 0 && pageSize <= 100, "'pageSize' must be a value larger than 0 and smaller or equal to 100.");
  }


  @Override
  public void checkApiEndpoint(GithubAppConfiguration githubAppConfiguration) {
    if (StringUtils.isBlank(githubAppConfiguration.getApiEndpoint())) {
      throw new IllegalArgumentException("Missing URL");
    }

    URI apiEndpoint;
    try {
      apiEndpoint = URI.create(githubAppConfiguration.getApiEndpoint());
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("Invalid URL, " + e.getMessage());
    }

    if (!"http".equalsIgnoreCase(apiEndpoint.getScheme()) && !"https".equalsIgnoreCase(apiEndpoint.getScheme())) {
      throw new IllegalArgumentException("Only http and https schemes are supported");
    } else if (!"api.github.com".equalsIgnoreCase(apiEndpoint.getHost()) && !apiEndpoint.getPath().toLowerCase(Locale.ENGLISH).startsWith("/api/v3")) {
      throw new IllegalArgumentException("Invalid GitHub URL");
    }
  }

  @Override
  public void checkAppPermissions(GithubAppConfiguration githubAppConfiguration) {
    AppToken appToken = appSecurity.createAppToken(githubAppConfiguration.getId(), githubAppConfiguration.getPrivateKey());

    Map<String, String> permissions = new HashMap<>();
    permissions.put("checks", WRITE_PERMISSION_NAME);
    permissions.put("pull_requests", WRITE_PERMISSION_NAME);
    permissions.put("statuses", READ_PERMISSION_NAME);
    permissions.put("metadata", READ_PERMISSION_NAME);

    String endPoint = "/app";
    GetResponse response;
    try {
      response = appHttpClient.get(githubAppConfiguration.getApiEndpoint(), appToken, endPoint);
    } catch (IOException e) {
      LOG.warn(FAILED_TO_REQUEST_BEGIN_MSG + githubAppConfiguration.getApiEndpoint() + endPoint, e);
      throw new IllegalArgumentException("Failed to validate configuration, check URL and Private Key");
    }
    if (response.getCode() == HTTP_OK) {
      Map<String, String> perms = handleResponse(response, endPoint, GsonApp.class)
        .map(GsonApp::getPermissions)
        .orElseThrow(() -> new IllegalArgumentException("Failed to get app permissions, unexpected response body"));
      List<String> missingPermissions = permissions.entrySet().stream()
        .filter(permission -> !Objects.equals(permission.getValue(), perms.get(permission.getKey())))
        .map(Map.Entry::getKey)
        .collect(toList());

      if (!missingPermissions.isEmpty()) {
        String message = missingPermissions.stream()
          .map(perm -> perm + " is '" + perms.get(perm) + "', should be '" + permissions.get(perm) + "'")
          .collect(Collectors.joining(", "));

        throw new IllegalArgumentException("Missing permissions; permission granted on " + message);
      }
    } else if (response.getCode() == HTTP_UNAUTHORIZED || response.getCode() == HTTP_FORBIDDEN) {
      throw new IllegalArgumentException("Authentication failed, verify the Client Id, Client Secret and Private Key fields");
    } else {
      throw new IllegalArgumentException("Failed to check permissions with Github, check the configuration");
    }
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
          .collect(toList()));
      }

      return organizations;
    } catch (IOException e) {
      throw new IllegalStateException(format("Failed to list all organizations accessible by user access token on %s", appUrl), e);
    }
  }

  @Override
  public Repositories listRepositories(String appUrl, AccessToken accessToken, String organization, @Nullable String query, int page, int pageSize) {
    checkPageArgs(page, pageSize);
    String searchQuery = "fork:true+org:" + organization;
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
          .map(GsonGithubRepository::toRepository)
          .collect(toList()));
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
        .map(GsonGithubRepository::toRepository);
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


  protected static <T> Optional<T> handleResponse(GithubApplicationHttpClient.Response response, String endPoint, Class<T> gsonClass) {
    try {
      return response.getContent().map(c -> GSON.fromJson(c, gsonClass));
    } catch (Exception e) {
      LOG.warn(FAILED_TO_REQUEST_BEGIN_MSG + endPoint, e);
      return Optional.empty();
    }
  }
}
