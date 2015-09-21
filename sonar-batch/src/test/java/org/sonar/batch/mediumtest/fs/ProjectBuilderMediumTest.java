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
import java.util.Date;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.batch.mediumtest.BatchMediumTester;
import org.sonar.batch.mediumtest.TaskResult;
import org.sonar.batch.protocol.output.BatchReport.Issue;
import org.sonar.xoo.XooPlugin;
import org.sonar.xoo.rule.XooRulesDefinition;

import static org.assertj.core.api.Assertions.assertThat;

public class ProjectBuilderMediumTest {

  @org.junit.Rule
  public TemporaryFolder temp = new TemporaryFolder();

  public BatchMediumTester tester = BatchMediumTester.builder()
    .registerPlugin("xoo", new XooPlugin())
    .addRules(new XooRulesDefinition())
    .addDefaultQProfile("xoo", "Sonar Way")
    .setPreviousAnalysisDate(new Date())
    .addActiveRule("xoo", "OneIssuePerLine", null, "One issue per line", "MAJOR", "OneIssuePerLine.internal", "xoo")
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
  public void testProjectBuilder() throws IOException {

    File baseDir = temp.getRoot();
    File module1Dir = new File(baseDir, "module1");
    module1Dir.mkdir();

    File srcDir = new File(module1Dir, "src");
    srcDir.mkdir();

    File xooFile = new File(srcDir, "sample.xoo");
    FileUtils.write(xooFile, "1\n2\n3\n4\n5\n6\n7\n8\n9\n10");

    TaskResult result = tester.newTask()
      .properties(ImmutableMap.<String, String>builder()
        .put("sonar.task", "scan")
        .put("sonar.projectBaseDir", baseDir.getAbsolutePath())
        .put("sonar.projectKey", "com.foo.project")
        .put("sonar.projectName", "Foo Project")
        .put("sonar.projectVersion", "1.0-SNAPSHOT")
        .put("sonar.projectDescription", "Description of Foo Project")
        .put("sonar.sources", ".")
        .put("sonar.xoo.enableProjectBuilder", "true")
        .build())
      .start();
    List<Issue> issues = result.issuesFor(result.inputFile("src/sample.xoo"));
    assertThat(issues).hasSize(10);

    boolean foundIssueAtLine1 = false;
    for (Issue issue : issues) {
      if (issue.getLine() == 1) {
        foundIssueAtLine1 = true;
        assertThat(issue.getMsg()).isEqualTo("This issue is generated on each line");
        assertThat(issue.hasEffortToFix()).isFalse();
      }
    }
    assertThat(foundIssueAtLine1).isTrue();

  }

  @Test
  public void testProjectBuilderWithBranch() throws IOException {

    File baseDir = temp.getRoot();
    File module1Dir = new File(baseDir, "module1");
    module1Dir.mkdir();

    File srcDir = new File(module1Dir, "src");
    srcDir.mkdir();

    File xooFile = new File(srcDir, "sample.xoo");
    FileUtils.write(xooFile, "1\n2\n3\n4\n5\n6\n7\n8\n9\n10");

    TaskResult result = tester.newTask()
      .properties(ImmutableMap.<String, String>builder()
        .put("sonar.task", "scan")
        .put("sonar.projectBaseDir", baseDir.getAbsolutePath())
        .put("sonar.projectKey", "com.foo.project")
        .put("sonar.projectName", "Foo Project")
        .put("sonar.projectVersion", "1.0-SNAPSHOT")
        .put("sonar.projectDescription", "Description of Foo Project")
        .put("sonar.branch", "my-branch")
        .put("sonar.sources", ".")
        .put("sonar.xoo.enableProjectBuilder", "true")
        .build())
      .start();

    List<Issue> issues = result.issuesFor(result.inputFile("src/sample.xoo"));
    assertThat(issues).hasSize(10);

    boolean foundIssueAtLine1 = false;
    for (Issue issue : issues) {
      if (issue.getLine() == 1) {
        foundIssueAtLine1 = true;
        assertThat(issue.getMsg()).isEqualTo("This issue is generated on each line");
        assertThat(issue.hasEffortToFix()).isFalse();
      }
    }
    assertThat(foundIssueAtLine1).isTrue();
  }

}
