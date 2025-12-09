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
package org.sonar.scanner.ci.vendors;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import javax.annotation.Nullable;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.event.Level;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;
import org.sonar.api.utils.System2;
import org.sonar.scanner.ci.CiConfiguration;
import org.sonar.scanner.ci.CiVendor;
import org.sonar.scanner.ci.DevOpsPlatformInfo;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.scanner.ci.vendors.GithubActions.GITHUB_ACTION;
import static org.sonar.scanner.ci.vendors.GithubActions.GITHUB_API_URL_ENV_VAR;
import static org.sonar.scanner.ci.vendors.GithubActions.GITHUB_EVENT_NAME;
import static org.sonar.scanner.ci.vendors.GithubActions.GITHUB_EVENT_PATH;
import static org.sonar.scanner.ci.vendors.GithubActions.GITHUB_REPOSITORY_ENV_VAR;
import static org.sonar.scanner.ci.vendors.GithubActions.GITHUB_SHA;

class GithubActionsTest {

  private static final String GITHUB_API_URL = "https://api.github.com/";
  private static final String REPOSITORY = "foo/bar";

  @RegisterExtension
  private final LogTesterJUnit5 logs = new LogTesterJUnit5();
  @TempDir
  public Path temp;

  private final System2 system = mock(System2.class);
  private final CiVendor underTest = new GithubActions(system);

  @Test
  void getName() {
    assertThat(underTest.getName()).isEqualTo("Github Actions");
  }

  @Test
  void isDetected() {
    setEnvVariable(GITHUB_ACTION, "build");
    assertThat(underTest.isDetected()).isTrue();

    setEnvVariable(GITHUB_ACTION, null);
    assertThat(underTest.isDetected()).isFalse();
  }

  @Test
  void loadConfiguration_whenIsAPullRequest_ThenGetRevisionFromPullRequest() throws IOException {
    prepareEvent("pull_request_event.json", "pull_request");
    setEnvVariable(GITHUB_SHA, "abd12fc");
    setEnvVariable(GITHUB_API_URL_ENV_VAR, GITHUB_API_URL);
    setEnvVariable(GITHUB_REPOSITORY_ENV_VAR, REPOSITORY);

    CiConfiguration configuration = underTest.loadConfiguration();
    assertThat(configuration.getScmRevision()).hasValue("4aeb9d5d52f1b39cc7d17e4daf8bb6da2e1c55eb");
    checkDevOpsPlatformInfo(configuration);
  }

  @Test
  void loadConfiguration_whenIsNotAPullRequest_ThenGetRevisionFromEnvVar() {
    setEnvVariable(GITHUB_SHA, "abd12fc");
    setEnvVariable(GITHUB_API_URL_ENV_VAR, GITHUB_API_URL);
    setEnvVariable(GITHUB_REPOSITORY_ENV_VAR, REPOSITORY);

    CiConfiguration configuration = underTest.loadConfiguration();
    assertThat(configuration.getScmRevision()).hasValue("abd12fc");
    checkDevOpsPlatformInfo(configuration);
  }

  @Test
  void log_warning_if_missing_GITHUB_SHA() {
    assertThat(underTest.loadConfiguration().getScmRevision()).isEmpty();
    assertThat(logs.logs(Level.WARN)).contains("Missing environment variable GITHUB_SHA");
  }

  @Test
  void loadConfiguration_whenMissingGitHubEnvironmentVariables_shouldLogWarn() {
    assertThat(underTest.loadConfiguration().getDevOpsPlatformInfo()).isEmpty();
    assertThat(logs.logs(Level.WARN)).contains("Missing or empty environment variables: GITHUB_API_URL, and/or GITHUB_REPOSITORY");
  }

  private void setEnvVariable(String key, @Nullable String value) {
    when(system.envVariable(key)).thenReturn(value);
  }

  private void checkDevOpsPlatformInfo(CiConfiguration configuration) {
    assertThat(configuration.getDevOpsPlatformInfo()).isNotEmpty();
    DevOpsPlatformInfo devOpsPlatformInfo = configuration.getDevOpsPlatformInfo().get();
    assertThat(devOpsPlatformInfo.getProjectIdentifier()).isEqualTo(REPOSITORY);
    assertThat(devOpsPlatformInfo.getUrl()).isEqualTo(GITHUB_API_URL);
  }

  private void prepareEvent(String jsonFilename, String eventName) throws IOException {
    File tempFile = Files.createFile(temp.resolve("file.json")).toFile();
    String json = readTestResource(jsonFilename);
    FileUtils.write(tempFile, json, UTF_8);

    setEnvVariable(GITHUB_EVENT_PATH, tempFile.getAbsolutePath());
    setEnvVariable(GITHUB_EVENT_NAME, eventName);
  }

  private static String readTestResource(String filename) throws IOException {
    return IOUtils.toString(Objects.requireNonNull(GithubActionsTest.class.getResource(filename)), UTF_8);
  }
}
