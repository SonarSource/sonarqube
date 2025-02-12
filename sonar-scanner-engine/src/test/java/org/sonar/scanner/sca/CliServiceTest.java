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
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.event.Level;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.fs.internal.DefaultInputModule;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;
import org.sonar.core.util.ProcessWrapperFactory;

import static org.assertj.core.api.Assertions.assertThat;

class CliServiceTest {
  private final ProcessWrapperFactory processWrapperFactory = new ProcessWrapperFactory();
  private final CliService underTest = new CliService(processWrapperFactory);

  @RegisterExtension
  private final LogTesterJUnit5 logTester = new LogTesterJUnit5();

  @Test
  void generateZip_shouldCallProcessCorrectly(@TempDir Path rootModuleDir) throws IOException, URISyntaxException {
    DefaultInputModule root = new DefaultInputModule(
      ProjectDefinition.create().setBaseDir(rootModuleDir.toFile()).setWorkDir(rootModuleDir.toFile()));

    // There is a custom test Bash script available in src/test/resources/org/sonar/scanner/sca that
    // will serve as our "CLI". This script will output some messages about what arguments were passed
    // to it and will try to generate a zip file in the location the process specifies. This allows us
    // to simulate a real CLI call without needing an OS specific CLI executable to run on a real project.
    URL scriptUrl = CliServiceTest.class.getResource("echo_args.sh");
    assertThat(scriptUrl).isNotNull();
    File scriptDir = new File(scriptUrl.toURI());
    assertThat(rootModuleDir.resolve("test_file").toFile().createNewFile()).isTrue();

    // We need to set the logging level to debug in order to be able to view the shell script's output
    logTester.setLevel(Level.DEBUG);

    List<String> args = new ArrayList<>();
    args.add("projects");
    args.add("save-lockfiles");
    args.add("--zip");
    args.add("--zip-filename");
    args.add(root.getWorkDir().resolve("dependency-files.zip").toString());
    args.add("--directory");
    args.add(root.getBaseDir().toString());
    args.add("--debug");

    String argumentOutput = "Arguments Passed In: " + String.join(" ", args);


    File producedZip = underTest.generateManifestsZip(root, scriptDir);
    assertThat(producedZip).exists();
    // The simulated CLI output will only be available at the debug level
    assertThat(logTester.logs(Level.DEBUG)).contains(argumentOutput);
    assertThat(logTester.logs(Level.DEBUG)).contains("TIDELIFT_SKIP_UPDATE_CHECK=1");
    assertThat(logTester.logs(Level.INFO)).contains("Generated manifests zip file: " + producedZip.getName());
  }
}
