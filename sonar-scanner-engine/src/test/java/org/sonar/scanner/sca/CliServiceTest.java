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
package org.sonar.scanner.sca;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.lang3.SystemUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.fs.internal.DefaultInputModule;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;
import org.sonar.api.utils.System2;
import org.sonar.core.util.ProcessWrapperFactory;
import org.sonar.scanner.config.DefaultConfiguration;
import org.sonar.scanner.repository.TelemetryCache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.slf4j.event.Level.DEBUG;
import static org.slf4j.event.Level.INFO;

class CliServiceTest {
  private TelemetryCache telemetryCache;
  @RegisterExtension
  private final LogTesterJUnit5 logTester = new LogTesterJUnit5();

  private CliService underTest;

  @BeforeEach
  void setup() {
    telemetryCache = new TelemetryCache();
    underTest = new CliService(new ProcessWrapperFactory(), telemetryCache, System2.INSTANCE);
  }

  @Test
  void generateZip_shouldCallProcessCorrectly_andRegisterTelemetry(@TempDir Path rootModuleDir) throws IOException, URISyntaxException {
    DefaultInputModule root = new DefaultInputModule(
      ProjectDefinition.create().setBaseDir(rootModuleDir.toFile()).setWorkDir(rootModuleDir.toFile()));

    // There is a custom test Bash script available in src/test/resources/org/sonar/scanner/sca that
    // will serve as our "CLI". This script will output some messages about what arguments were passed
    // to it and will try to generate a zip file in the location the process specifies. This allows us
    // to simulate a real CLI call without needing an OS specific CLI executable to run on a real project.
    URL scriptUrl = CliServiceTest.class.getResource(SystemUtils.IS_OS_WINDOWS ? "echo_args.bat" : "echo_args.sh");
    assertThat(scriptUrl).isNotNull();
    File scriptDir = new File(scriptUrl.toURI());
    assertThat(rootModuleDir.resolve("test_file").toFile().createNewFile()).isTrue();

    // We need to set the logging level to debug in order to be able to view the shell script's output
    logTester.setLevel(DEBUG);

    List<String> args = List.of(
      "projects",
      "save-lockfiles",
      "--zip",
      "--zip-filename",
      root.getWorkDir().resolve("dependency-files.zip").toString(),
      "--directory",
      root.getBaseDir().toString(),
      "--debug");

    String argumentOutput = "Arguments Passed In: " + String.join(" ", args);
    DefaultConfiguration configuration = mock(DefaultConfiguration.class);
    when(configuration.getProperties()).thenReturn(Map.of("sonar.sca.recursiveManifestSearch", "true"));
    when(configuration.get("sonar.sca.recursiveManifestSearch")).thenReturn(Optional.of("true"));

    File producedZip = underTest.generateManifestsZip(root, scriptDir, configuration);
    assertThat(producedZip).exists();

    assertThat(logTester.logs(DEBUG))
      .contains(argumentOutput)
      .contains("TIDELIFT_SKIP_UPDATE_CHECK=1")
      .contains("TIDELIFT_RECURSIVE_MANIFEST_SEARCH=true");
    assertThat(logTester.logs(INFO)).contains("Generated manifests zip file: " + producedZip.getName());

    assertThat(telemetryCache.getAll()).containsKey("scanner.sca.execution.cli.duration").isNotNull();
    assertThat(telemetryCache.getAll()).containsEntry("scanner.sca.execution.cli.success", "true");
  }

  @Test
  void generateZip_whenDebugLogLevel_shouldCallProcessCorrectly(@TempDir Path rootModuleDir) throws IOException, URISyntaxException {
    DefaultInputModule root = new DefaultInputModule(
      ProjectDefinition.create().setBaseDir(rootModuleDir.toFile()).setWorkDir(rootModuleDir.toFile()));

    // There is a custom test Bash script available in src/test/resources/org/sonar/scanner/sca that
    // will serve as our "CLI". This script will output some messages about what arguments were passed
    // to it and will try to generate a zip file in the location the process specifies. This allows us
    // to simulate a real CLI call without needing an OS specific CLI executable to run on a real project.
    URL scriptUrl = CliServiceTest.class.getResource(SystemUtils.IS_OS_WINDOWS ? "echo_args.bat" : "echo_args.sh");
    assertThat(scriptUrl).isNotNull();
    File scriptDir = new File(scriptUrl.toURI());
    assertThat(rootModuleDir.resolve("test_file").toFile().createNewFile()).isTrue();

    // We need to set the logging level to debug in order to be able to view the shell script's output
    logTester.setLevel(DEBUG);

    List<String> args = List.of(
      "projects",
      "save-lockfiles",
      "--zip",
      "--zip-filename",
      root.getWorkDir().resolve("dependency-files.zip").toString(),
      "--directory",
      root.getBaseDir().toString(),
      "--debug");

    String argumentOutput = "Arguments Passed In: " + String.join(" ", args);
    DefaultConfiguration configuration = mock(DefaultConfiguration.class);
    when(configuration.getProperties()).thenReturn(Map.of("sonar.sca.recursiveManifestSearch", "true"));
    when(configuration.get("sonar.sca.recursiveManifestSearch")).thenReturn(Optional.of("true"));

    underTest.generateManifestsZip(root, scriptDir, configuration);

    assertThat(logTester.logs(DEBUG))
      .contains(argumentOutput);
  }

  @Test
  void generateZip_whenScaDebugEnabled_shouldCallProcessCorrectly(@TempDir Path rootModuleDir) throws IOException, URISyntaxException {
    DefaultInputModule root = new DefaultInputModule(
      ProjectDefinition.create().setBaseDir(rootModuleDir.toFile()).setWorkDir(rootModuleDir.toFile()));

    // There is a custom test Bash script available in src/test/resources/org/sonar/scanner/sca that
    // will serve as our "CLI". This script will output some messages about what arguments were passed
    // to it and will try to generate a zip file in the location the process specifies. This allows us
    // to simulate a real CLI call without needing an OS specific CLI executable to run on a real project.
    URL scriptUrl = CliServiceTest.class.getResource(SystemUtils.IS_OS_WINDOWS ? "echo_args.bat" : "echo_args.sh");
    assertThat(scriptUrl).isNotNull();
    File scriptDir = new File(scriptUrl.toURI());
    assertThat(rootModuleDir.resolve("test_file").toFile().createNewFile()).isTrue();

    // Set the logging level to info so that we don't automatically set --debug flag
    logTester.setLevel(INFO);

    List<String> args = List.of(
      "projects",
      "save-lockfiles",
      "--zip",
      "--zip-filename",
      root.getWorkDir().resolve("dependency-files.zip").toString(),
      "--directory",
      root.getBaseDir().toString(),
      "--debug");

    String argumentOutput = "Arguments Passed In: " + String.join(" ", args);
    DefaultConfiguration configuration = mock(DefaultConfiguration.class);
    when(configuration.getProperties()).thenReturn(Map.of("sonar.sca.recursiveManifestSearch", "true"));
    when(configuration.get("sonar.sca.recursiveManifestSearch")).thenReturn(Optional.of("true"));
    when(configuration.getBoolean("sonar.sca.debug")).thenReturn(Optional.of(true));

    underTest.generateManifestsZip(root, scriptDir, configuration);

    assertThat(logTester.logs(INFO))
      .contains(argumentOutput);
  }
}
