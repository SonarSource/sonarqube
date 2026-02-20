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
package org.sonar.scanner.sca;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.SystemUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultInputModule;
import org.sonar.api.batch.scm.ScmProvider;
import org.sonar.api.platform.Server;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;
import org.sonar.api.utils.System2;
import org.sonar.core.util.ProcessWrapperFactory;
import org.sonar.scanner.config.DefaultConfiguration;
import org.sonar.scanner.repository.TelemetryCache;
import org.sonar.scanner.scan.filesystem.ProjectExclusionFilters;
import org.sonar.scanner.scm.ScmConfiguration;
import org.sonar.scm.git.GitScmProvider;
import org.sonar.scm.git.JGitUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.slf4j.event.Level.DEBUG;
import static org.slf4j.event.Level.INFO;

class CliServiceTest {
  private TelemetryCache telemetryCache;
  private DefaultInputModule rootInputModule;
  private final Server server = mock(Server.class);
  @RegisterExtension
  private final LogTesterJUnit5 logTester = new LogTesterJUnit5();
  @TempDir
  Path rootModuleDir;
  private final ScmConfiguration scmConfiguration = mock(ScmConfiguration.class);
  private final ScmProvider scmProvider = mock(GitScmProvider.class);
  ProcessWrapperFactory processWrapperFactory = mock(ProcessWrapperFactory.class, CALLS_REAL_METHODS);
  private MockedStatic<JGitUtils> jGitUtilsMock;
  DefaultConfiguration configuration = mock(DefaultConfiguration.class);
  ProjectExclusionFilters projectExclusionFilters = mock(ProjectExclusionFilters.class);

  private CliService underTest;

  @BeforeEach
  void setup() throws IOException {
    telemetryCache = new TelemetryCache();
    Path workDir = rootModuleDir.resolve(".scannerwork");
    Files.createDirectories(workDir);
    rootInputModule = new DefaultInputModule(
      ProjectDefinition.create().setBaseDir(rootModuleDir.toFile()).setWorkDir(workDir.toFile()));
    when(scmConfiguration.provider()).thenReturn(scmProvider);
    when(scmProvider.key()).thenReturn("git");
    when(scmConfiguration.isExclusionDisabled()).thenReturn(false);
    jGitUtilsMock = org.mockito.Mockito.mockStatic(JGitUtils.class);
    jGitUtilsMock.when(() -> JGitUtils.getAllIgnoredPaths(any(Path.class))).thenReturn(List.of("ignored.txt"));
    when(server.getVersion()).thenReturn("1.0.0");
    logTester.setLevel(INFO);
    when(projectExclusionFilters.getExclusionsConfig(InputFile.Type.MAIN)).thenReturn(new String[0]);
    when(configuration.getStringArray(CliService.SCA_EXCLUSIONS_KEY)).thenReturn(new String[0]);
    when(configuration.getStringArray(CliService.LEGACY_SCA_EXCLUSIONS_KEY)).thenReturn(new String[0]);

    underTest = new CliService(processWrapperFactory, telemetryCache, System2.INSTANCE, server, scmConfiguration, projectExclusionFilters);
  }

  @AfterEach
  void teardown() {
    if (jGitUtilsMock != null) {
      jGitUtilsMock.close();
    }
  }

  @Test
  void generateManifestsArchive_shouldCallProcessCorrectly_andRegisterTelemetry() throws IOException, URISyntaxException {
    assertThat(rootModuleDir.resolve("test_file").toFile().createNewFile()).isTrue();

    when(configuration.getProperties()).thenReturn(Map.of(CliService.SCA_EXCLUSIONS_KEY, "foo,bar,baz/**"));
    when(configuration.getStringArray(CliService.SCA_EXCLUSIONS_KEY)).thenReturn(new String[] {"foo", "bar", "baz/**"});

    File producedArchive = underTest.generateManifestsArchive(rootInputModule, scriptDir(), configuration);

    assertThat(producedArchive).exists();

    String capturedArgs = logTester.logs().stream().filter(log -> log.contains("Arguments Passed In:")).findFirst().get();
    var expectedArgs = List.of(
      "projects",
      "save-lockfiles",
      "--xz",
      "--xz-filename",
      rootInputModule.getWorkDir().resolve("dependency-files.tar.xz").toString(),
      "--directory",
      rootInputModule.getBaseDir().toString(),
      "--recursive",
      "--exclude-from-file",
      "sca-exclusions-",
      ".txt");
    assertThat(capturedArgs).containsSubsequence(expectedArgs);

    assertThat(logTester.logs(INFO))
      .contains("TIDELIFT_SKIP_UPDATE_CHECK=1")
      .contains("TIDELIFT_ALLOW_MANIFEST_FAILURES=1")
      .contains("Generated manifests archive file: " + producedArchive.getName());

    assertThat(telemetryCache.getAll()).containsKey("scanner.sca.execution.cli.duration").isNotNull();
    assertThat(telemetryCache.getAll()).containsEntry("scanner.sca.execution.cli.success", "true");
  }

  @Test
  void generateManifestsArchive_whenDebugLogLevelAndScaDebugNotEnabled_shouldWriteDebugLogsToDebugStream() throws IOException, URISyntaxException {
    logTester.setLevel(DEBUG);

    assertThat(rootModuleDir.resolve("test_file").toFile().createNewFile()).isTrue();

    underTest.generateManifestsArchive(rootInputModule, scriptDir(), configuration);

    String capturedArgs = logTester.logs().stream().filter(log -> log.contains("Arguments Passed In:")).findFirst().get();
    var expectedArgs = List.of(
      "projects",
      "save-lockfiles",
      "--xz",
      "--xz-filename",
      rootInputModule.getWorkDir().resolve("dependency-files.tar.xz").toString(),
      "--directory",
      rootInputModule.getBaseDir().toString(),
      "--recursive",
      "--exclude-from-file",
      "sca-exclusions-",
      ".txt",
      "--debug");
    assertThat(capturedArgs).containsSubsequence(expectedArgs);
  }

  @Test
  void generateManifestsArchive_whenScaDebugEnabled_shouldWriteDebugLogsToInfoStream() throws IOException, URISyntaxException {
    assertThat(rootModuleDir.resolve("test_file").toFile().createNewFile()).isTrue();

    underTest.generateManifestsArchive(rootInputModule, scriptDir(), configuration);

    String capturedArgs = logTester.logs().stream().filter(log -> log.contains("Arguments Passed In:")).findFirst().get();
    var expectedArgs = List.of(
      "projects",
      "save-lockfiles",
      "--xz",
      "--xz-filename",
      rootInputModule.getWorkDir().resolve("dependency-files.tar.xz").toString(),
      "--directory",
      rootInputModule.getBaseDir().toString(),
      "--recursive",
      "--exclude-from-file",
      "sca-exclusions-",
      ".txt");
    assertThat(capturedArgs).containsSubsequence(expectedArgs);
  }

  @Test
  void generateManifestsArchive_shouldSendSQEnvVars() throws IOException, URISyntaxException {
    underTest.generateManifestsArchive(rootInputModule, scriptDir(), configuration);

    assertThat(logTester.logs(INFO))
      .contains("TIDELIFT_CLI_INSIDE_SCANNER_ENGINE=1")
      .contains("TIDELIFT_CLI_SQ_SERVER_VERSION=1.0.0");
  }

  @Test
  void generateManifestsArchive_withNoExcludes_shouldNotCreateExcludesFile() throws IOException, URISyntaxException {
    // Setup: external work dir (no .scannerwork/** exclude) + no SCM ignores = no excludes at all
    Path externalWorkDir = Files.createTempDirectory("externalWorkDir");
    try {
      rootInputModule = new DefaultInputModule(ProjectDefinition.create().setBaseDir(rootModuleDir.toFile()).setWorkDir(externalWorkDir.toFile()));
      jGitUtilsMock.when(() -> JGitUtils.getAllIgnoredPaths(any(Path.class))).thenReturn(List.of());

      assertThat(rootModuleDir.resolve("test_file").toFile().createNewFile()).isTrue();

      File producedArchive = underTest.generateManifestsArchive(rootInputModule, scriptDir(), configuration);

      assertThat(producedArchive).exists();

      String capturedArgs = logTester.logs().stream().filter(log -> log.contains("Arguments Passed In:")).findFirst().get();
      var expectedArgs = List.of(
        "projects",
        "save-lockfiles",
        "--xz",
        "--xz-filename",
        externalWorkDir.resolve("dependency-files.tar.xz").toString(),
        "--directory",
        rootInputModule.getBaseDir().toString(),
        "--recursive");
      assertThat(capturedArgs).containsSubsequence(expectedArgs);
      assertThat(capturedArgs)
        .doesNotContain("--exclude-from-file")
        .doesNotContain("sca-exclusions-");
    } finally {
      externalWorkDir.toFile().delete();
    }
  }

  @Test
  void getAllExcludedPaths_includesIgnoredPathsFromGitProvider() {
    List<String> excludes = underTest.getAllExcludedPaths(rootInputModule, configuration);

    assertThat(excludes).containsExactlyInAnyOrder("ignored.txt", ".scannerwork/**");
  }

  @Test
  void getAllExcludedPaths_withNoScm_doesNotIncludeScmIgnoredPaths() {
    when(scmConfiguration.provider()).thenReturn(null);

    List<String> excludes = underTest.getAllExcludedPaths(rootInputModule, configuration);
    assertThat(excludes).containsExactlyInAnyOrder(".scannerwork/**");
  }

  @Test
  void getAllExcludedPaths_withNonGit_doesNotIncludeScmIgnoredPaths() {
    when(scmProvider.key()).thenReturn("notgit");

    List<String> excludes = underTest.getAllExcludedPaths(rootInputModule, configuration);
    assertThat(excludes).containsExactlyInAnyOrder(".scannerwork/**");
  }

  @Test
  void getAllExcludedPaths_withScmExclusionDisabled_doesNotIncludeScmIgnoredPaths() {
    when(scmConfiguration.isExclusionDisabled()).thenReturn(true);

    List<String> excludes = underTest.getAllExcludedPaths(rootInputModule, configuration);
    assertThat(excludes).containsExactlyInAnyOrder(".scannerwork/**");
  }

  @Test
  void getAllExcludedPaths_withNoScmIgnores_doesNotIncludeScmIgnoredPaths() {
    jGitUtilsMock.when(() -> JGitUtils.getAllIgnoredPaths(any(Path.class))).thenReturn(List.of());

    List<String> excludes = underTest.getAllExcludedPaths(rootInputModule, configuration);
    assertThat(excludes).containsExactlyInAnyOrder(".scannerwork/**");
  }

  @Test
  void getAllExcludedPaths_withExcludedManifests_appendsScmIgnoredPaths() {
    when(configuration.getStringArray(CliService.SCA_EXCLUSIONS_KEY)).thenReturn(new String[] {"**/test/**"});

    List<String> excludes = underTest.getAllExcludedPaths(rootInputModule, configuration);
    assertThat(excludes).containsExactlyInAnyOrder(".scannerwork/**", "**/test/**", "ignored.txt");
  }

  @Test
  void getAllExcludedPaths_withExcludedManifestsContainingBadCharacters_handlesTheBadCharacters() {
    when(configuration.getStringArray(CliService.SCA_EXCLUSIONS_KEY)).thenReturn(new String[] {
      "**/test/**", "**/path with spaces/**", "**/path'with'quotes/**", "**/path\"with\"double\"quotes/**"});

    List<String> excludes = underTest.getAllExcludedPaths(rootInputModule, configuration);
    assertThat(excludes).containsExactlyInAnyOrder("**/test/**",
      "**/path with spaces/**",
      "**/path'with'quotes/**",
      "**/path\"with\"double\"quotes/**",
       "ignored.txt",
       ".scannerwork/**");
  }

  @Test
  void getAllExcludedPaths_withExcludedManifestsContainingDupes_dedupes() {
    when(configuration.getStringArray(CliService.SCA_EXCLUSIONS_KEY)).thenReturn(new String[] {"**/test1/**", "**/test2/**", "**/test1/**"});
    when(configuration.getStringArray(CliService.LEGACY_SCA_EXCLUSIONS_KEY)).thenReturn(new String[] {"**/test1/**", "**/test3/**"});

    List<String> excludes = underTest.getAllExcludedPaths(rootInputModule, configuration);
    assertThat(excludes).containsExactlyInAnyOrder(".scannerwork/**", "**/test1/**", "**/test2/**", "**/test3/**", "ignored.txt");
  }

  @Test
  void getAllExcludedPaths_withExcludedManifestsAndSonarExcludesContainingDupes_mergesAndDedupes() {
    when(projectExclusionFilters.getExclusionsConfig(InputFile.Type.MAIN)).thenReturn(new String[] {"**/test1/**", "**/test4/**"});
    when(configuration.getStringArray(CliService.SCA_EXCLUSIONS_KEY)).thenReturn(new String[] {"**/test1/**", "**/test2/**", "**/test1/**"});
    when(configuration.getStringArray(CliService.LEGACY_SCA_EXCLUSIONS_KEY)).thenReturn(new String[] {"**/test1/**", "**/test3/**"});

    List<String> excludes = underTest.getAllExcludedPaths(rootInputModule, configuration);
    assertThat(excludes).containsExactlyInAnyOrder(".scannerwork/**", "**/test1/**", "**/test2/**", "**/test3/**", "**/test4/**", "ignored.txt");
  }

  @Test
  void getAllExcludedPaths_withScmIgnoresContainingBadCharacters_handlesTheBadCharacters() {
    jGitUtilsMock.when(() -> JGitUtils.getAllIgnoredPaths(any(Path.class)))
      .thenReturn(List.of("**/test/**", "**/path with spaces/**", "**/path'with'quotes/**", "**/path\"with\"double\"quotes/**"));

    List<String> excludes = underTest.getAllExcludedPaths(rootInputModule, configuration);
    assertThat(excludes).containsExactlyInAnyOrder("**/test/**",
      "**/path with spaces/**",
      "**/path'with'quotes/**",
      "**/path\"with\"double\"quotes/**",
       ".scannerwork/**");
  }

  @Test
  void getAllExcludedPaths_withIgnoredDirectories_GlobifiesDirectories() throws IOException {
    String ignoredDirectory = "directory1";
    Files.createDirectories(rootModuleDir.resolve(ignoredDirectory));
    String ignoredFile = "directory2/file.txt";
    Path ignoredFilePath = rootModuleDir.resolve(ignoredFile);
    Files.createDirectories(ignoredFilePath.getParent());
    Files.createFile(ignoredFilePath);

    jGitUtilsMock.when(() -> JGitUtils.getAllIgnoredPaths(any(Path.class))).thenReturn(List.of(ignoredDirectory, ignoredFile));
    List<String> excludes = underTest.getAllExcludedPaths(rootInputModule, configuration);
    assertThat(excludes).containsExactlyInAnyOrder(".scannerwork/**", "directory1/**", "directory2/file.txt");
  }

  @Test
  void getAllExcludedPaths_withExternalWorkDir_DoesNotExcludeWorkingDir() throws IOException {
    Path externalWorkDir = Files.createTempDirectory("externalWorkDir");
    try {
      rootInputModule = new DefaultInputModule(ProjectDefinition.create().setBaseDir(rootModuleDir.toFile()).setWorkDir(externalWorkDir.toFile()));
      List<String> excludes = underTest.getAllExcludedPaths(rootInputModule, configuration);
      assertThat(excludes).containsExactlyInAnyOrder("ignored.txt");
    } finally {
      externalWorkDir.toFile().delete();
    }
  }

  private URL scriptUrl() {
    // There is a custom test Bash script available in src/test/resources/org/sonar/scanner/sca that
    // will serve as our "CLI". This script will output some messages about what arguments were passed
    // to it and will try to generate an archive file in the location the process specifies. This allows us
    // to simulate a real CLI call without needing an OS specific CLI executable to run on a real project.
    URL scriptUrl = CliServiceTest.class.getResource(SystemUtils.IS_OS_WINDOWS ? "echo_args.bat" : "echo_args.sh");
    assertThat(scriptUrl).isNotNull();
    return scriptUrl;
  }

  private File scriptDir() throws URISyntaxException {
    return new File(scriptUrl().toURI());
  }
}
