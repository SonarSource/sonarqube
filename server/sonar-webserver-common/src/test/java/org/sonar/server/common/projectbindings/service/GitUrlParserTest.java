/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.server.common.projectbindings.service;

import java.util.Optional;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.sonar.server.common.projectbindings.service.GitUrlParser.RepositoryInfo;
import org.sonar.server.exceptions.BadRequestException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class GitUrlParserTest {

  @ParameterizedTest
  @MethodSource("successfulParsingCases")
  void parseRepositoryUrl_shouldParseValidUrls(String url, String expectedOrg, String expectedRepo, @Nullable String expectedProject) {
    Optional<RepositoryInfo> result = GitUrlParser.parseRepositoryUrl(url);

    assertRepositoryInfo(result, expectedOrg, expectedRepo, expectedProject);
  }

  @ParameterizedTest
  @NullAndEmptySource
  void parseRepositoryUrl_shouldThrowExceptionForNullOrEmptyUrls(@Nullable String url) {
    assertThatThrownBy(() -> GitUrlParser.parseRepositoryUrl(url))
      .isInstanceOf(BadRequestException.class)
      .hasMessage("URL cannot be empty");
  }

  @ParameterizedTest
  @MethodSource("invalidUrls")
  void parseRepositoryUrl_shouldReturnEmptyForInvalidUrls(@Nullable String url) {
    assertThat(GitUrlParser.parseRepositoryUrl(url)).isEmpty();
  }

  private static void assertRepositoryInfo(Optional<RepositoryInfo> result, String expectedOrg, String expectedRepo, @Nullable String expectedProject) {
    assertThat(result).isPresent();
    RepositoryInfo info = result.get();

    assertThat(info.organization()).isEqualTo(expectedOrg);
    assertThat(info.repository()).isEqualTo(expectedRepo);
    assertThat(info.project()).isEqualTo(expectedProject);
    assertThat(info.slug()).isEqualTo(expectedOrg + "/" + expectedRepo);

    if (expectedProject != null) {
      assertThat(info.projectName()).isEqualTo(expectedProject);
    } else {
      assertThat(info.projectName()).isEmpty();
    }
  }

  private static Stream<Arguments> successfulParsingCases() {
    return Stream.of(
      // ===== STANDARD FORMAT (org/repo) =====
      
      // HTTP/HTTPS Standard - Basic cases
      arguments("https://github.com/org/repo", "org", "repo", null),
      arguments("http://github.com/org/repo", "org", "repo", null),
      arguments("https://bitbucket.org/myorg/myrepo", "myorg", "myrepo", null),
      arguments("https://gitlab.com/company/project", "company", "project", null),
      arguments("http://git.company.com/team/service", "team", "service", null),
      
      // HTTP/HTTPS Standard - With .git suffix
      arguments("https://github.com/org/repo.git", "org", "repo", null),
      arguments("http://gitlab.com/user/project.git", "user", "project", null),
      
      // HTTP/HTTPS Standard - With trailing slashes
      arguments("https://github.com/org/repo/", "org", "repo", null),
      arguments("https://github.com/org/repo//", "org", "repo", null),
      arguments("https://github.com/org/repo///", "org", "repo", null),
      arguments("https://github.com/org/repo.git/", "org", "repo", null),
      arguments("https://github.com/org/repo.git///", "org", "repo", null),
      
      // HTTP/HTTPS Standard - With ports
      arguments("https://git.company.com:8080/org/repo", "org", "repo", null),
      arguments("http://gitlab.local:3000/org/repo.git", "org", "repo", null),
      arguments("https://bitbucket.example.com:9443/team/service/", "team", "service", null),
      
      // HTTP/HTTPS Standard - With authentication
      arguments("https://user@github.com/org/repo", "org", "repo", null),
      arguments("https://user:pass@gitlab.com/org/repo.git", "org", "repo", null),
      
      // HTTP/HTTPS Standard - With query parameters
      arguments("https://github.com/org/repo?branch=main", "org", "repo", null),
      arguments("https://github.com/org/repo.git?ref=develop", "org", "repo", null),
      
      // SSH Standard - Basic cases
      arguments("git@github.com:org/repo", "org", "repo", null),
      arguments("git@gitlab.com:user/project", "user", "project", null),
      arguments("git@bitbucket.org:company/service", "company", "service", null),
      
      // SSH Standard - With .git suffix
      arguments("git@github.com:org/repo.git", "org", "repo", null),
      arguments("git@gitlab.com:user/project.git", "user", "project", null),
      
      // SSH Standard - With trailing slashes
      arguments("git@github.com:org/repo/", "org", "repo", null),
      arguments("git@github.com:org/repo//", "org", "repo", null),
      arguments("git@github.com:org/repo.git/", "org", "repo", null),
      
      // SSH Standard - Custom hosts
      arguments("git@git.company.com:team/project", "team", "project", null),
      arguments("git@gitlab.example.org:dept/app.git", "dept", "app", null),
      
      // ===== AZURE DEVOPS FORMAT (org/project/_git/repo) =====
      
      // HTTP/HTTPS Azure - Basic cases
      arguments("https://dev.azure.com/myorg/myproject/_git/myrepo", "myorg", "myrepo", "myproject"),
      arguments("http://dev.azure.com/company/frontend/_git/webapp", "company", "webapp", "frontend"),
      arguments("https://devops.company.com/team/backend/_git/api", "team", "api", "backend"),
      
      // HTTP/HTTPS Azure - With .git suffix
      arguments("https://dev.azure.com/myorg/myproject/_git/myrepo.git", "myorg", "myrepo", "myproject"),
      arguments("http://azure.example.com/org/proj/_git/service.git", "org", "service", "proj"),
      
      // HTTP/HTTPS Azure - With trailing slashes
      arguments("https://dev.azure.com/myorg/myproject/_git/myrepo/", "myorg", "myrepo", "myproject"),
      arguments("https://dev.azure.com/myorg/myproject/_git/myrepo//", "myorg", "myrepo", "myproject"),
      arguments("https://dev.azure.com/myorg/myproject/_git/myrepo.git/", "myorg", "myrepo", "myproject"),
      arguments("https://dev.azure.com/myorg/myproject/_git/myrepo.git///", "myorg", "myrepo", "myproject"),
      
      // HTTP/HTTPS Azure - With ports
      arguments("https://devops.company.com:8443/myorg/myproject/_git/myrepo", "myorg", "myrepo", "myproject"),
      arguments("http://azure.local:8080/team/proj/_git/app.git", "team", "app", "proj"),
      
      // SSH Azure - Basic cases
      arguments("git@ssh.dev.azure.com:v3/myorg/myproject/myrepo", "myorg", "myrepo", "myproject"),
      arguments("git@azure.company.com:v3/team/frontend/webapp", "team", "webapp", "frontend"),
      
      // SSH Azure - With .git suffix
      arguments("git@ssh.dev.azure.com:v3/myorg/myproject/myrepo.git", "myorg", "myrepo", "myproject"),
      arguments("git@devops.example.com:v3/org/backend/api.git", "org", "api", "backend"),
      
      // SSH Azure - With trailing slashes
      arguments("git@ssh.dev.azure.com:v3/myorg/myproject/myrepo/", "myorg", "myrepo", "myproject"),
      arguments("git@ssh.dev.azure.com:v3/myorg/myproject/myrepo//", "myorg", "myrepo", "myproject"),
      arguments("git@ssh.dev.azure.com:v3/myorg/myproject/myrepo.git/", "myorg", "myrepo", "myproject"),
      
      // ===== NESTED FORMAT (group/subgroup/repo) =====
      
      // HTTP/HTTPS Nested - Basic cases
      arguments("https://gitlab.com/group/subgroup/project", "group/subgroup", "project", null),
      arguments("http://gitlab.com/company/team/service", "company/team", "service", null),
      arguments("https://git.company.com/dept/division/app", "dept/division", "app", null),
      
      // HTTP/HTTPS Nested - With .git suffix
      arguments("https://gitlab.com/group/subgroup/project.git", "group/subgroup", "project", null),
      arguments("http://gitlab.example.com/org/team/repo.git", "org/team", "repo", null),
      
      // HTTP/HTTPS Nested - With trailing slashes
      arguments("https://gitlab.com/group/subgroup/project/", "group/subgroup", "project", null),
      arguments("https://gitlab.com/group/subgroup/project//", "group/subgroup", "project", null),
      arguments("https://gitlab.com/group/subgroup/project.git/", "group/subgroup", "project", null),
      arguments("https://gitlab.com/group/subgroup/project.git///", "group/subgroup", "project", null),
      
      // HTTP/HTTPS Nested - With ports
      arguments("https://gitlab.company.com:9443/group/subgroup/project", "group/subgroup", "project", null),
      arguments("http://git.local:3000/team/division/app.git", "team/division", "app", null),
      
      // HTTP/HTTPS Nested - Deep nesting
      arguments("https://gitlab.com/org/team/dept/division/project", "org/team/dept/division", "project", null),
      arguments("https://gitlab.com/a/b/c/d/e/f/repo.git", "a/b/c/d/e/f", "repo", null),
      
      // SSH Nested - Basic cases
      arguments("git@gitlab.com:group/subgroup/project", "group/subgroup", "project", null),
      arguments("git@git.company.com:team/division/service", "team/division", "service", null),
      
      // SSH Nested - With .git suffix
      arguments("git@gitlab.com:group/subgroup/project.git", "group/subgroup", "project", null),
      arguments("git@gitlab.example.com:org/team/app.git", "org/team", "app", null),
      
      // SSH Nested - With trailing slashes
      arguments("git@gitlab.com:group/subgroup/project/", "group/subgroup", "project", null),
      arguments("git@gitlab.com:group/subgroup/project//", "group/subgroup", "project", null),
      arguments("git@gitlab.com:group/subgroup/project.git/", "group/subgroup", "project", null),
      
      // SSH Nested - Deep nesting
      arguments("git@gitlab.com:org/team/dept/division/project.git", "org/team/dept/division", "project", null),
      
      // ===== EDGE CASES =====
      
      // URLs with special characters in names
      arguments("https://github.com/my-org/my-repo", "my-org", "my-repo", null),
      arguments("git@gitlab.com:user.name/project_name.git", "user.name", "project_name", null),
      arguments("https://dev.azure.com/my-org/my-project/_git/my-repo", "my-org", "my-repo", "my-project"),
      
      // Very long paths (potential exponential backtracking)
      arguments(
        "https://gitlab.com/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/NOTFOUND",
        "a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a",
        "NOTFOUND",
        null),
      
      // Mixed case scenarios
      arguments("https://GitHub.COM/ORG/REPO.GIT", "ORG", "REPO.GIT", null), // .GIT (uppercase) is not removed
      
      // Numeric organization/project names
      arguments("https://github.com/123org/456repo", "123org", "456repo", null),
      arguments("git@gitlab.com:999/project123.git", "999", "project123", null),
      
      // URLs that might be mistaken as invalid but are parsed as nested format
      arguments("https://dev.azure.com/org/_git/repo", "org/_git", "repo", null), // parsed as nested, not Azure
      arguments("https://dev.azure.com/org/project/git/repo", "org/project/git", "repo", null), // parsed as nested
      arguments("git@ssh.dev.azure.com:v3/org/repo", "v3/org", "repo", null), // parsed as nested, not Azure SSH
      arguments("git@ssh.dev.azure.com:v2/org/project/repo", "v2/org/project", "repo", null), // parsed as nested
      arguments("https://dev.azure.com/org/project/_git/", "org/project", "_git", null) // trailing slash parsed as nested
    );
  }

  private static Stream<String> invalidUrls() {
    return Stream.of(
      // Unsupported protocols
      "ftp://github.com/user/repo",
      "svn://server.com/repo",
      "file:///local/path/repo",
      
      // Incomplete URLs
      "https://github.com/", // missing org and repo
      "https://github.com/org", // missing repo
      "http://gitlab.com", // no path
      
      // Invalid SSH formats
      "git@", // incomplete SSH
      "git@github.com", // missing colon and path
      "git@github.com:", // missing path
      "git@github.com:/", // invalid path
      
      // Not URLs at all
      "not-a-url-at-all",
      "github.com/org/repo", // missing protocol
      "www.github.com/org/repo", // missing protocol
      
      // Invalid characters or malformed
      "https://github.com/org repo/test", // space in path
      "https://", // protocol only
      "http://", // protocol only
      
      // Just domain without path
      "https://github.com",
      "http://dev.azure.com",
      "git@gitlab.com:",
      
      // URLs with only root path
      "https://github.com/",
      "http://gitlab.com/",
      
      // URLs that result in empty repo names
      "https://github.com/org/", // missing repo (empty after slash removal)
      
      // URLs that are too short (after reconsidering the actual parser behavior)
      "https://a.com/b",
      "git@a.com:b"
    );
  }

}
