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
package org.sonar.batch.mediumtest.fs;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.rule.RuleKey;
import org.sonar.batch.mediumtest.AnalyzerMediumTester;
import org.sonar.batch.mediumtest.AnalyzerMediumTester.TaskResult;
import org.sonar.batch.mediumtest.xoo.plugin.XooPlugin;
import org.sonar.batch.mediumtest.xoo.plugin.base.Xoo;

import java.io.File;
import java.io.IOException;

import static org.fest.assertions.Assertions.assertThat;

public class FileSystemMediumTest {

  @org.junit.Rule
  public TemporaryFolder temp = new TemporaryFolder();

  public AnalyzerMediumTester tester = AnalyzerMediumTester.builder()
    .registerPlugin("xoo", new XooPlugin())
    .registerLanguage(new Xoo())
    .addDefaultQProfile("xoo", "Sonar Way")
    .activateRule(RuleKey.of("xoo", "OneIssuePerLine"))
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
  public void scanProjectWithSourceDir() throws IOException {
    File srcDir = new File(baseDir, "src");
    srcDir.mkdir();

    File xooFile = new File(srcDir, "sample.xoo");
    FileUtils.write(xooFile, "Sample xoo\ncontent");

    TaskResult result = tester.newTask()
      .properties(builder
        .put("sonar.sources", "src")
        .build())
      .start();

    assertThat(result.inputFiles()).hasSize(1);
    assertThat(result.inputFiles().get(0).type()).isEqualTo(InputFile.Type.MAIN);
  }

  @Test
  public void scanProjectWithTestDir() throws IOException {
    File test = new File(baseDir, "test");
    test.mkdir();

    File xooFile = new File(test, "sampleTest.xoo");
    FileUtils.write(xooFile, "Sample test xoo\ncontent");

    TaskResult result = tester.newTask()
      .properties(builder
        .put("sonar.sources", "")
        .put("sonar.tests", "test")
        .build())
      .start();

    assertThat(result.inputFiles()).hasSize(1);
    assertThat(result.inputFiles().get(0).type()).isEqualTo(InputFile.Type.TEST);
  }

  @Test
  public void scanProjectWithMixedSourcesAndTests() throws IOException {
    File srcDir = new File(baseDir, "src");
    srcDir.mkdir();

    File xooFile = new File(srcDir, "sample.xoo");
    FileUtils.write(xooFile, "Sample xoo\ncontent");

    File xooFile2 = new File(baseDir, "another.xoo");
    FileUtils.write(xooFile2, "Sample xoo 2\ncontent");

    File testDir = new File(baseDir, "test");
    testDir.mkdir();

    File xooTestFile = new File(baseDir, "sampleTest2.xoo");
    FileUtils.write(xooTestFile, "Sample test xoo\ncontent");

    File xooTestFile2 = new File(testDir, "sampleTest.xoo");
    FileUtils.write(xooTestFile2, "Sample test xoo 2\ncontent");

    TaskResult result = tester.newTask()
      .properties(builder
        .put("sonar.sources", "src,another.xoo")
        .put("sonar.tests", "test,sampleTest2.xoo")
        .build())
      .start();

    assertThat(result.inputFiles()).hasSize(4);
  }

}
