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
package org.sonar.batch.mediumtest.dependency;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestName;
import org.sonar.batch.mediumtest.BatchMediumTester;
import org.sonar.batch.mediumtest.TaskResult;
import org.sonar.xoo.XooPlugin;

import java.io.File;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class DependencyMediumTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Rule
  public TestName testName = new TestName();

  public BatchMediumTester tester = BatchMediumTester.builder()
    .registerPlugin("xoo", new XooPlugin())
    .addDefaultQProfile("xoo", "Sonar Way")
    .build();

  @Before
  public void prepare() {
    tester.start();
  }

  @After
  public void stop() {
    tester.stop();
  }

  @Test
  public void populateDependenciesOnTempProject() throws IOException {

    File baseDir = temp.newFolder();
    File srcDir = new File(baseDir, "src");
    srcDir.mkdir();

    File xooFile = new File(srcDir, "sample.xoo");
    File xooFile2 = new File(srcDir, "sample2.xoo");
    File xooFile3 = new File(srcDir, "foo/sample3.xoo");
    File xooDepsFile = new File(srcDir, "sample.xoo.deps");
    FileUtils.write(xooFile, "Sample xoo\ncontent");
    FileUtils.write(xooFile2, "Sample xoo\ncontent");
    FileUtils.write(xooFile3, "Sample xoo\ncontent");
    FileUtils.write(xooDepsFile, "src/sample2.xoo:3\nsrc/foo/sample3.xoo:6");

    TaskResult result = tester.newTask()
      .properties(ImmutableMap.<String, String>builder()
        .put("sonar.task", "scan")
        .put("sonar.projectBaseDir", baseDir.getAbsolutePath())
        .put("sonar.projectKey", "com.foo.project")
        .put("sonar.projectName", "Foo Project")
        .put("sonar.projectVersion", "1.0-SNAPSHOT")
        .put("sonar.projectDescription", "Description of Foo Project")
        .put("sonar.sources", "src")
        .build())
      .start();

    assertThat(result.fileDependencyFor(result.inputFile("src/sample.xoo"), result.inputFile("src/sample2.xoo")).getWeight()).isEqualTo(3);
    assertThat(result.fileDependencyFor(result.inputFile("src/sample.xoo"), result.inputFile("src/foo/sample3.xoo")).getWeight()).isEqualTo(6);
  }

  @Test
  public void manyDependenciesNoCycle() throws IOException {

    File baseDir = temp.newFolder();
    File srcDir = new File(baseDir, "src");
    srcDir.mkdir();

    int nbFiles = 100;
    for (int nb = 1; nb <= nbFiles; nb++) {
      File xooFile = new File(srcDir, "dir1/sample" + nb + ".xoo");
      FileUtils.write(xooFile, "foo");
      File xooFile2 = new File(srcDir, "dir2/sample" + nb + ".xoo");
      FileUtils.write(xooFile2, "foo");
      File xooDepFile = new File(srcDir, "dir1/sample" + nb + ".xoo.deps");
      for (int otherId = 1; otherId <= nbFiles; otherId++) {
        FileUtils.write(xooDepFile, "src/dir2/sample" + otherId + ".xoo:1\n", Charsets.UTF_8, true);
      }
    }

    TaskResult result = tester.newTask()
      .properties(ImmutableMap.<String, String>builder()
        .put("sonar.task", "scan")
        .put("sonar.projectBaseDir", baseDir.getAbsolutePath())
        .put("sonar.projectKey", "com.foo.project")
        .put("sonar.projectName", "Foo Project")
        .put("sonar.projectVersion", "1.0-SNAPSHOT")
        .put("sonar.projectDescription", "Description of Foo Project")
        .put("sonar.sources", "src")
        .build())
      .start();

    assertThat(result.fileDependencyFor(result.inputFile("src/dir1/sample1.xoo"), result.inputFile("src/dir2/sample1.xoo")).getWeight()).isEqualTo(1);

  }
}
