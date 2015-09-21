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
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.batch.mediumtest.BatchMediumTester;
import org.sonar.batch.mediumtest.Benchmark;
import org.sonar.batch.mediumtest.TaskResult;
import org.sonar.batch.protocol.output.BatchReport.Issue;
import org.sonar.xoo.XooPlugin;
import org.sonar.xoo.rule.XooRulesDefinition;

import static org.assertj.core.api.Assertions.assertThat;

public class RandomFsAccessMediumTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Rule
  public Benchmark bench = new Benchmark();

  public BatchMediumTester tester = BatchMediumTester.builder()
    .registerPlugin("xoo", new XooPlugin())
    .addDefaultQProfile("xoo", "Sonar Way")
    .addRules(new XooRulesDefinition())
    .addActiveRule("xoo", "RandomAccessIssue", null, "One issue per line", "MAJOR", null, "xoo")
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
  public void testRandomFsAccessByAbsolutePath() throws IOException {

    File baseDir = temp.getRoot();
    File srcDir = prepareBigProject(baseDir);

    File paths = new File(baseDir, "paths.txt");
    int ISSUE_COUNT = 10000;
    for (int i = 0; i < ISSUE_COUNT; i++) {
      File xooFile = new File(srcDir, "sample" + (i / 10 + 1) + ".xoo");
      FileUtils.write(paths, xooFile.getAbsolutePath() + "\n", StandardCharsets.UTF_8, true);
    }

    long start = System.currentTimeMillis();
    TaskResult result = tester.newTask()
      .properties(ImmutableMap.<String, String>builder()
        .put("sonar.task", "scan")
        .put("sonar.projectBaseDir", baseDir.getAbsolutePath())
        .put("sonar.projectKey", "com.foo.project")
        .put("sonar.projectName", "Foo Project")
        .put("sonar.projectVersion", "1.0-SNAPSHOT")
        .put("sonar.projectDescription", "Description of Foo Project")
        .put("sonar.sources", "src")
        .put("sonar.xoo.randomAccessIssue.paths", paths.getAbsolutePath())
        .build())
      .start();

    List<Issue> issues = result.issuesFor(result.inputFile("src/sample1.xoo"));
    assertThat(issues).hasSize(10);
    bench.expectLessThanOrEqualTo("Time to create " + ISSUE_COUNT + " issues on random files using FileSystem query", System.currentTimeMillis() - start, 2000);
  }

  @Test
  public void testRandomFsAccessByRelativePath() throws IOException {

    File baseDir = temp.getRoot();
    prepareBigProject(baseDir);

    File paths = new File(baseDir, "paths.txt");
    int ISSUE_COUNT = 10000;
    for (int i = 0; i < ISSUE_COUNT; i++) {
      FileUtils.write(paths, "src/sample" + (i / 10 + 1) + ".xoo\n", StandardCharsets.UTF_8, true);
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
        .put("sonar.xoo.randomAccessIssue.paths", paths.getAbsolutePath())
        .build())
      .start();

    List<Issue> issues = result.issuesFor(result.inputFile("src/sample1.xoo"));
    assertThat(issues).hasSize(10);

  }

  private File prepareBigProject(File baseDir) throws IOException {
    File srcDir = new File(baseDir, "src");
    srcDir.mkdir();

    for (int i = 1; i <= 1000; i++) {
      File xooFile = new File(srcDir, "sample" + i + ".xoo");
      FileUtils.write(xooFile, "foo");
    }
    return srcDir;
  }

}
