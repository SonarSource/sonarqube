/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.scanner.mediumtest.branch;

import com.google.common.collect.ImmutableMap;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.sonar.scanner.mediumtest.BatchMediumTester;
import org.sonar.scanner.mediumtest.TaskResult;
import org.sonar.xoo.XooPlugin;

import static org.assertj.core.api.Assertions.assertThat;

public class BranchMediumTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  public BatchMediumTester tester = BatchMediumTester.builder()
    .registerPlugin("xoo", new XooPlugin())
    .addDefaultQProfile("xoo", "Sonar Way")
    .build();

  private File baseDir;

  private Map<String, String> commonProps;

  @Before
  public void prepare() throws IOException {
    tester.start();

    baseDir = temp.getRoot();

    commonProps = ImmutableMap.<String, String>builder()
      .put("sonar.task", "scan")
      .put("sonar.projectBaseDir", baseDir.getAbsolutePath())
      .put("sonar.projectKey", "com.foo.project")
      .put("sonar.projectName", "Foo Project")
      .put("sonar.projectVersion", "1.0-SNAPSHOT")
      .put("sonar.projectDescription", "Description of Foo Project")
      .put("sonar.sources", "src")
      .build();
  }

  @After
  public void stop() {
    tester.stop();
  }

  @Test
  public void scanProjectWithBranch() throws IOException {
    File srcDir = new File(baseDir, "src");
    srcDir.mkdir();

    File xooFile = new File(srcDir, "sample.xoo");
    FileUtils.write(xooFile, "Sample xoo\ncontent");

    TaskResult result = tester.newTask()
      .properties(ImmutableMap.<String, String>builder()
        .putAll(commonProps)
        .put("sonar.branch", "branch")
        .build())
      .start();

    assertThat(result.inputFiles()).hasSize(1);
    assertThat(result.inputFile("src/sample.xoo").key()).isEqualTo("com.foo.project:branch:src/sample.xoo");

    result = tester.newTask()
      .properties(ImmutableMap.<String, String>builder()
        .putAll(commonProps)
        .put("sonar.branch", "")
        .build())
      .start();

    assertThat(result.inputFiles()).hasSize(1);
    assertThat(result.inputFile("src/sample.xoo").key()).isEqualTo("com.foo.project:src/sample.xoo");
  }

  @Test
  public void scanMultiModuleWithBranch() throws IOException {
    Path srcDir = baseDir.toPath().resolve("moduleA").resolve("src");
    Files.createDirectories(srcDir);

    File xooFile = new File(srcDir.toFile(), "sample.xoo");
    FileUtils.write(xooFile, "Sample xoo\ncontent");

    TaskResult result = tester.newTask()
      .properties(ImmutableMap.<String, String>builder()
        .putAll(commonProps)
        .put("sonar.branch", "branch")
        .put("sonar.modules", "moduleA")
        .build())
      .start();

    assertThat(result.inputFiles()).hasSize(1);
    assertThat(result.inputFile("src/sample.xoo").key()).isEqualTo("com.foo.project:moduleA:branch:src/sample.xoo");

    result = tester.newTask()
      .properties(ImmutableMap.<String, String>builder()
        .putAll(commonProps)
        .put("sonar.branch", "")
        .put("sonar.modules", "moduleA")
        .build())
      .start();

    assertThat(result.inputFiles()).hasSize(1);
    assertThat(result.inputFile("src/sample.xoo").key()).isEqualTo("com.foo.project:moduleA:src/sample.xoo");
  }

}
