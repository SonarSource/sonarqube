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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import org.apache.commons.lang3.StringUtils;

import static org.sonar.server.exceptions.BadRequestException.throwBadRequestException;

/**
 * Generic parser for Git repository URLs.
 * 
 * <p>This parser extracts organization, project, and repository information from repository URLs
 * without trying to identify the specific DevOps platform. It focuses on parsing the path
 * components after the domain to work with any Git hosting platform including enterprise instances.</p>
 * 
 * <p>Supported URL patterns:</p>
 * <ul>
 *   <li><strong>Standard format:</strong> http(s)://domain.com/org/repo</li>
 *   <li><strong>With project:</strong> http(s)://domain.com/org/project/_git/repo (Azure DevOps style)</li>
 *   <li><strong>Nested groups:</strong> http(s)://domain.com/group/subgroup/repo (GitLab style)</li>
 *   <li><strong>SSH format:</strong> git@domain.com:org/repo</li>
 *   <li><strong>SSH with project:</strong> git@domain.com:v3/org/project/repo (Azure DevOps style)</li>
 * </ul>
 */
public final class GitUrlParser {

  // SSH-only patterns (HTTP/HTTPS handled via URI parsing)
  private static final Pattern SSH_STANDARD_PATTERN = Pattern.compile("^git@([^:]+):([^/]+)/([^/]+?)(?:\\.git)?/*$");
  private static final Pattern SSH_AZURE_PATTERN = Pattern.compile("^git@([^:]+):v3/([^/]+)/([^/]+)/([^/]+?)(?:\\.git)?/*$");
  private static final Pattern SSH_NESTED_PATTERN = Pattern.compile("^git@([^:]+):([^/]+/.+)/([^/]+?)(?:\\.git)?/*$");

  // Path-only patterns for HTTP/HTTPS URLs
  private static final Pattern PATH_STANDARD_PATTERN = Pattern.compile("^/([^/]+)/([^/]+?)(?:\\.git)?/*$");
  private static final Pattern PATH_AZURE_PATTERN = Pattern.compile("^/([^/]+)/([^/]+)/_git/([^/]+?)(?:\\.git)?/*$");
  private static final Pattern PATH_NESTED_PATTERN = Pattern.compile("^/([^/]+/.+)/([^/]+?)(?:\\.git)?/*$");

  private static final String GIT_SUFFIX = ".git";
  private static final String HTTP_PREFIX = "http://";
  private static final String HTTPS_PREFIX = "https://";
  private static final String SSH_PREFIX = "git@";

  private GitUrlParser() {
    // Utility class, no instantiation
  }

  /**
   * Parses a Git repository URL to extract path components.
   * 
   * @param url The Git repository URL to parse
   * @return RepositoryInfo if the URL matches a supported format
   * @throws org.sonar.server.exceptions.BadRequestException if the URL is null or empty
   */
  public static Optional<RepositoryInfo> parseRepositoryUrl(@Nullable String url) {
    validateInput(url);
    String trimmedUrl = url.trim();

    // Protocol detection upfront
    if (trimmedUrl.startsWith(SSH_PREFIX)) {
      return parseSshUrl(trimmedUrl);
    }

    if (trimmedUrl.startsWith(HTTPS_PREFIX) || trimmedUrl.startsWith(HTTP_PREFIX)) {
      return parseHttpUrl(trimmedUrl);
    }

    // Fallback for other formats
    return Optional.empty();
  }

  /**
   * Validates the input URL.
   */
  private static void validateInput(@Nullable String url) {
    if (StringUtils.isBlank(url)) {
      throwBadRequestException("URL cannot be empty");
    }
  }

  /**
   * Parses SSH URLs using regex patterns.
   */
  private static Optional<RepositoryInfo> parseSshUrl(String url) {
    return tryPattern(SSH_AZURE_PATTERN, url, GitUrlParser::parseAzureMatch)
      .or(() -> tryPattern(SSH_NESTED_PATTERN, url, GitUrlParser::parseNestedMatch))
      .or(() -> tryPattern(SSH_STANDARD_PATTERN, url, GitUrlParser::parseStandardMatch));
  }

  /**
   * Parses HTTP/HTTPS URLs using URI parsing.
   */
  private static Optional<RepositoryInfo> parseHttpUrl(String url) {
    try {
      URI uri = new URI(url);
      String path = uri.getPath();

      if (StringUtils.isBlank(path) || path.equals("/")) {
        return Optional.empty();
      }

      return parsePathComponents(path);
    } catch (URISyntaxException e) {
      return Optional.empty();
    }
  }

  /**
   * Parses path components extracted from URI (much simpler than full URL regex).
   */
  private static Optional<RepositoryInfo> parsePathComponents(String path) {
    return tryPattern(PATH_AZURE_PATTERN, path, GitUrlParser::parseAzureMatch)
      .or(() -> tryPattern(PATH_NESTED_PATTERN, path, GitUrlParser::parseNestedMatch))
      .or(() -> tryPattern(PATH_STANDARD_PATTERN, path, GitUrlParser::parseStandardMatch));
  }

  /**
   * Tries to match a pattern against input and applies a function to the matcher if successful.
   */
  private static Optional<RepositoryInfo> tryPattern(Pattern pattern, String input, Function<Matcher, RepositoryInfo> matchFunction) {
    Matcher matcher = pattern.matcher(input);
    return matcher.matches() ? Optional.of(matchFunction.apply(matcher)) : Optional.empty();
  }

  /**
   * Parses Azure DevOps format matches (org/project/_git/repo or org/project/repo).
   */
  private static RepositoryInfo parseAzureMatch(Matcher matcher) {
    if (matcher.groupCount() == 4) {
      // SSH: git@host:v3/org/project/repo
      return new RepositoryInfo(matcher.group(2), removeGitSuffix(matcher.group(4)), matcher.group(3));
    } else {
      // HTTP: /org/project/_git/repo
      return new RepositoryInfo(matcher.group(1), removeGitSuffix(matcher.group(3)), matcher.group(2));
    }
  }

  /**
   * Parses nested format matches (org/suborg/repo).
   */
  private static RepositoryInfo parseNestedMatch(Matcher matcher) {
    if (matcher.groupCount() == 3) {
      // SSH: git@host:org/suborg/repo
      return new RepositoryInfo(matcher.group(2), removeGitSuffix(matcher.group(3)));
    } else {
      // HTTP: /org/suborg/repo
      return new RepositoryInfo(matcher.group(1), removeGitSuffix(matcher.group(2)));
    }
  }

  /**
   * Parses standard format matches (org/repo).
   */
  private static RepositoryInfo parseStandardMatch(Matcher matcher) {
    if (matcher.groupCount() == 3) {
      // SSH: git@host:org/repo
      return new RepositoryInfo(matcher.group(2), removeGitSuffix(matcher.group(3)));
    } else {
      // HTTP: /org/repo
      return new RepositoryInfo(matcher.group(1), removeGitSuffix(matcher.group(2)));
    }
  }

  /**
   * Removes .git suffix and trailing slashes from URLs.
   */
  private static String removeGitSuffix(String url) {
    if (url.endsWith(GIT_SUFFIX)) {
      url = url.substring(0, url.length() - GIT_SUFFIX.length());
    }
    // Remove trailing slashes
    while (url.endsWith("/")) {
      url = url.substring(0, url.length() - 1);
    }
    return url;
  }

  /**
   * Information extracted from a parsed Git repository URL.
   * 
   * @param organization The organization, workspace, group, or namespace name
   * @param repository The repository name
   * @param project The project name (used by Azure DevOps, null for other platforms)
   */
  public record RepositoryInfo(String organization, String repository, @Nullable String project) {

    RepositoryInfo(String organization, String repository) {
      this(organization, repository, null);
    }

    public String slug() {
      return organization + "/" + repository;
    }

    public String projectName() {
      return StringUtils.defaultString(project);
    }
  }

}
