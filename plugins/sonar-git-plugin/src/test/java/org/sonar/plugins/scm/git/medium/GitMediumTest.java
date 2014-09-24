/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.plugins.scm.git.medium;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.duplication.DuplicationGroup;
import org.sonar.batch.mediumtest.BatchMediumTester;
import org.sonar.batch.mediumtest.BatchMediumTester.TaskResult;
import org.sonar.plugins.scm.git.GitPlugin;
import org.sonar.xoo.XooPlugin;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;

public class GitMediumTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  public BatchMediumTester tester = BatchMediumTester.builder()
    .registerPlugin("xoo", new XooPlugin())
    .registerPlugin("git", new GitPlugin())
    .addDefaultQProfile("xoo", "Sonar Way")
    .bootstrapProperties(ImmutableMap.of("sonar.analysis.mode", "sensor"))
    .build();

  private File baseDir;

  private ImmutableMap.Builder<String, String> builder;

  @Before
  public void prepare() throws IOException {
    tester.start();

    baseDir = temp.newFolder();

    builder = ImmutableMap.<String, String>builder()
      .put("sonar.task", "scan")
      .put("sonar.projectBaseDir", baseDir.getAbsolutePath())
      .put("sonar.projectKey", "com.foo.project")
      .put("sonar.projectName", "Foo Project")
      .put("sonar.projectVersion", "1.0-SNAPSHOT")
      .put("sonar.projectDescription", "Description of Foo Project");
  }

  @After
  public void stop() {
    tester.stop();
  }

  @Test
  public void testDuplications() throws IOException {
    File srcDir = new File(baseDir, "src");
    srcDir.mkdir();

    String duplicatedStuff = "Sample xoo\ncontent\nfoo\nbar\ntoto\ntiti\nfoo\nbar\ntoto\ntiti\nbar\ntoto\ntiti\nfoo\nbar\ntoto\ntiti";

    File xooFile1 = new File(srcDir, "sample1.xoo");
    FileUtils.write(xooFile1, duplicatedStuff);

    File xooFile2 = new File(srcDir, "sample2.xoo");
    FileUtils.write(xooFile2, duplicatedStuff);

    TaskResult result = tester.newTask()
      .properties(builder
        .put("sonar.sources", "src")
        .put("sonar.cpd.xoo.minimumTokens", "10")
        .put("sonar.verbose", "true")
        .build())
      .start();

    assertThat(result.inputFiles()).hasSize(2);

    // 4 measures per file
    assertThat(result.measures()).hasSize(8);

    InputFile inputFile = result.inputFiles().get(0);
    // One clone group
    List<DuplicationGroup> duplicationGroups = result.duplicationsFor(inputFile);
    assertThat(duplicationGroups).hasSize(1);

    DuplicationGroup cloneGroup = duplicationGroups.get(0);
    assertThat(cloneGroup.duplicates()).hasSize(1);
    assertThat(cloneGroup.originBlock().startLine()).isEqualTo(1);
    assertThat(cloneGroup.originBlock().length()).isEqualTo(17);
  }

}
