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

import com.google.gson.annotations.SerializedName;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import org.sonar.alm.client.github.config.GithubAppConfiguration;
import org.sonar.alm.client.github.security.AccessToken;
import org.sonar.alm.client.github.security.UserAccessToken;
import org.sonar.api.server.ServerSide;

@ServerSide
public interface GithubApplicationClient {

  /**
   * Create a user access token for the enterprise app installation.
   *
   * See https://developer.github.com/enterprise/2.20/apps/building-github-apps/identifying-and-authorizing-users-for-github-apps/#identifying-users-on-your-site
   *
   * @throws IllegalStateException if an internal error occured: network issue, invalid response, etc
   * @throws IllegalArgumentException if the request failed due to one of the parameters being invalid.
   */
  UserAccessToken createUserAccessToken(String appUrl, String clientId, String clientSecret, String code);

  /**
   * Lists all the organizations accessible to the access token provided.
   */
  Organizations listOrganizations(String appUrl, AccessToken accessToken, int page, int pageSize);

  /**
   * Lists all the repositories of the provided organization accessible to the access token provided.
   */
  Repositories listRepositories(String appUrl, AccessToken accessToken, String organization, @Nullable String query, int page, int pageSize);

  void checkApiEndpoint(GithubAppConfiguration githubAppConfiguration);

  /**
   * Checks if an app has all the permissions required.
   */
  void checkAppPermissions(GithubAppConfiguration githubAppConfiguration);

  /**
   * Returns the repository identified by the repositoryKey owned by the provided organization.
   */
  Optional<Repository> getRepository(String appUrl, AccessToken accessToken, String organization, String repositoryKey);

  class Repositories {
    private int total;
    private List<Repository> repositories;

    public Repositories() {
      //nothing to do
    }

    public int getTotal() {
      return total;
    }

    public Repositories setTotal(int total) {
      this.total = total;
      return this;
    }

    @CheckForNull
    public List<Repository> getRepositories() {
      return repositories;
    }

    public Repositories setRepositories(List<Repository> repositories) {
      this.repositories = repositories;
      return this;
    }
  }

  @Immutable
  final class Repository {
    private final long id;
    private final String name;
    private final boolean isPrivate;
    private final String fullName;
    private final String url;
    private final String defaultBranch;

    public Repository(long id, String name, boolean isPrivate, String fullName, String url, String defaultBranch) {
      this.id = id;
      this.name = name;
      this.isPrivate = isPrivate;
      this.fullName = fullName;
      this.url = url;
      this.defaultBranch = defaultBranch;
    }

    public long getId() {
      return id;
    }

    public String getName() {
      return name;
    }

    public boolean isPrivate() {
      return isPrivate;
    }

    public String getFullName() {
      return fullName;
    }

    public String getUrl() {
      return url;
    }

    public String getDefaultBranch() {
      return defaultBranch;
    }

    @Override
    public String toString() {
      return "Repository{" +
        "id=" + id +
        ", name='" + name + '\'' +
        ", isPrivate='" + isPrivate + '\'' +
        ", fullName='" + fullName + '\'' +
        ", url='" + url + '\'' +
        '}';
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      Repository that = (Repository) o;
      return id == that.id;
    }

    @Override
    public int hashCode() {
      return Long.hashCode(id);
    }
  }

  @Immutable
  final class RepositoryDetails {
    private final Repository repository;
    private final String description;
    private final String mainBranchName;
    private final String url;

    public RepositoryDetails(Repository repository, String description, String mainBranchName, String url) {
      this.repository = repository;
      this.description = description;
      this.mainBranchName = mainBranchName;
      this.url = url;
    }

    public Repository getRepository() {
      return repository;
    }

    public String getDescription() {
      return description;
    }

    public String getMainBranchName() {
      return mainBranchName;
    }

    public String getUrl() {
      return url;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      RepositoryDetails that = (RepositoryDetails) o;
      return Objects.equals(repository, that.repository) &&
        Objects.equals(description, that.description) &&
        Objects.equals(mainBranchName, that.mainBranchName) &&
        Objects.equals(url, that.url);
    }

    @Override
    public int hashCode() {
      return Objects.hash(repository, description, mainBranchName, url);
    }

    @Override
    public String toString() {
      return "RepositoryDetails{" +
        "repository=" + repository +
        ", description='" + description + '\'' +
        ", mainBranchName='" + mainBranchName + '\'' +
        ", url='" + url + '\'' +
        '}';
    }
  }

  class Organizations {
    private int total;
    private List<Organization> organizations;

    public Organizations() {
      //nothing to do
    }

    public int getTotal() {
      return total;
    }

    public Organizations setTotal(int total) {
      this.total = total;
      return this;
    }

    @CheckForNull
    public List<Organization> getOrganizations() {
      return organizations;
    }

    public Organizations setOrganizations(List<Organization> organizations) {
      this.organizations = organizations;
      return this;
    }
  }

  class Organization {
    private final long id;
    private final String login;
    private final String name;
    private final String bio;
    private final String blog;
    @SerializedName("html_url")
    private final String htmlUrl;
    @SerializedName("avatar_url")
    private final String avatarUrl;
    private final String type;

    public Organization(long id, String login, @Nullable String name, @Nullable String bio, @Nullable String blog, @Nullable String htmlUrl, @Nullable String avatarUrl,
      String type) {
      this.id = id;
      this.login = login;
      this.name = name;
      this.bio = bio;
      this.blog = blog;
      this.htmlUrl = htmlUrl;
      this.avatarUrl = avatarUrl;
      this.type = type;
    }

    public long getId() {
      return id;
    }

    public String getLogin() {
      return login;
    }

    @CheckForNull
    public String getName() {
      return name;
    }

    @CheckForNull
    public String getBio() {
      return bio;
    }

    @CheckForNull
    public String getBlog() {
      return blog;
    }

    public String getHtmlUrl() {
      return htmlUrl;
    }

    @CheckForNull
    public String getAvatarUrl() {
      return avatarUrl;
    }

    public String getType() {
      return type;
    }
  }

}
