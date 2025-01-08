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
package org.sonar.scanner.ci.vendors;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import java.io.File;
import java.io.IOException;
import java.util.Optional;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.utils.System2;
import org.sonar.scanner.ci.CiConfiguration;
import org.sonar.scanner.ci.CiConfigurationImpl;
import org.sonar.scanner.ci.CiVendor;
import org.sonar.scanner.ci.DevOpsPlatformInfo;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.isEmpty;

/**
 * Support of https://github.com/features/actions
 * <p>
 * Environment variables: https://developer.github.com/actions/creating-github-actions/accessing-the-runtime-environment/#environment-variables
 */
public class GithubActions implements CiVendor {

  private static final Logger LOG = LoggerFactory.getLogger(GithubActions.class);

  public static final String GITHUB_SHA = "GITHUB_SHA";
  public static final String GITHUB_REPOSITORY_ENV_VAR = "GITHUB_REPOSITORY";
  public static final String GITHUB_API_URL_ENV_VAR = "GITHUB_API_URL";
  public static final String GITHUB_EVENT_PATH = "GITHUB_EVENT_PATH";
  public static final String GITHUB_EVENT_NAME = "GITHUB_EVENT_NAME";
  public static final String GITHUB_ACTION = "GITHUB_ACTION";
  public static final String NAME = "Github Actions";

  private final System2 system;

  public GithubActions(System2 system) {
    this.system = system;
  }

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public boolean isDetected() {
    return StringUtils.isNotBlank(system.envVariable(GITHUB_ACTION));
  }

  @Override
  public CiConfiguration loadConfiguration() {
    String revision = getScmRevision().orElse(null);
    return getDevOpsPlatformInfo().map(devOpsPlatformInfo -> new CiConfigurationImpl(revision, getName(), devOpsPlatformInfo))
      .orElseGet(() -> new CiConfigurationImpl(revision, getName()));
  }

  private Optional<DevOpsPlatformInfo> getDevOpsPlatformInfo() {
    String githubRepository = system.envVariable(GITHUB_REPOSITORY_ENV_VAR);
    String githubApiUrl = system.envVariable(GITHUB_API_URL_ENV_VAR);
    if (isEmpty(githubRepository) || isEmpty(githubApiUrl)) {
      LOG.warn("Missing or empty environment variables: {}, and/or {}", GITHUB_API_URL_ENV_VAR, GITHUB_REPOSITORY_ENV_VAR);
      return Optional.empty();
    }
    return Optional.of(new DevOpsPlatformInfo(githubApiUrl, githubRepository));
  }

  private Optional<String> getScmRevision() {
    Optional<String> revisionOpt = getScmRevisionIfPullRequest();
    if (revisionOpt.isPresent()) {
      return revisionOpt;
    }
    return getScmRevisionFromEnvVar();
  }

  private Optional<String> getScmRevisionIfPullRequest() {
    String path = system.envVariable(GITHUB_EVENT_PATH);
    String eventName = system.envVariable(GITHUB_EVENT_NAME);
    boolean detected = isNotBlank(eventName) && isNotBlank(path);
    if (!detected) {
      return Optional.empty();
    }

    if ("pull_request".equals(eventName)) {
      String json = readFile(path);
      PrEvent prEvent = new Gson().fromJson(json, PrEvent.class);
      return Optional.of(prEvent.pullRequest.head.sha);
    }
    return Optional.empty();
  }

  private Optional<String> getScmRevisionFromEnvVar() {
    String revision = system.envVariable(GITHUB_SHA);
    if (isEmpty(revision)) {
      LOG.warn("Missing environment variable " + GITHUB_SHA);
      return Optional.empty();
    }
    return Optional.of(revision);
  }

  private static String readFile(String path) {
    File file = new File(path);
    if (!file.exists() || !file.isFile()) {
      throw new IllegalStateException("Event file does not exist: " + file);
    }
    try {
      return FileUtils.readFileToString(file, UTF_8);
    } catch (IOException e) {
      throw new IllegalStateException("Fail to read event file: " + file, e);
    }
  }

  @SuppressWarnings("ALL")
  private static class PrEvent {
    @SerializedName("ref")
    private String ref;

    @SerializedName("pull_request")
    private Pr pullRequest;
  }

  @SuppressWarnings("ALL")
  private static class Pr {
    @SerializedName("number")
    private int number;

    @SerializedName("base")
    private PrCommit base;

    @SerializedName("head")
    private PrCommit head;
  }

  @SuppressWarnings("ALL")
  private static class PrCommit {
    @SerializedName("ref")
    private String ref;

    @SerializedName("sha")
    private String sha;

    @SerializedName("repo")
    private Repository repository;
  }

  @SuppressWarnings("ALL")
  private static class Repository {
    /**
     * Example: "SonarSource/sonarqube"
     */
    @SerializedName("full_name")
    private String fullName;

    @SerializedName("owner")
    private Owner owner;
  }

  @SuppressWarnings("ALL")
  private static class Owner {
    /**
     * The organization or user owning the repository.
     * Example: "SonarSource"
     */
    @SerializedName("name")
    private String name;

    /**
     * Example: "https://api.github.com/repos/SonarSource/sonarqube"
     */
    @SerializedName("url")
    private String url;
  }
}
