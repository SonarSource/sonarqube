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
package org.sonar.batch.mediumtest.issues;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.rule.Severity;
import org.sonar.batch.mediumtest.BatchMediumTester;
import org.sonar.batch.mediumtest.TaskResult;
import org.sonar.batch.protocol.input.ActiveRule;
import org.sonar.xoo.XooPlugin;

import java.io.File;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class IssuesMediumTest {

  @org.junit.Rule
  public TemporaryFolder temp = new TemporaryFolder();

  public BatchMediumTester tester = BatchMediumTester.builder()
    .registerPlugin("xoo", new XooPlugin())
    .addDefaultQProfile("xoo", "Sonar Way")
    .activateRule(new ActiveRule("xoo", "OneIssuePerLine", null, "One issue per line", "MAJOR", "OneIssuePerLine.internal", "xoo"))
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
  public void testOneIssuePerLine() throws Exception {
    File projectDir = new File(IssuesMediumTest.class.getResource("/mediumtest/xoo/sample").toURI());

    TaskResult result = tester
      .newScanTask(new File(projectDir, "sonar-project.properties"))
      .start();

    assertThat(result.issues()).hasSize(14);
  }

  @Test
  public void findActiveRuleByInternalKey() throws Exception {
    File projectDir = new File(IssuesMediumTest.class.getResource("/mediumtest/xoo/sample").toURI());

    TaskResult result = tester
      .newScanTask(new File(projectDir, "sonar-project.properties"))
      .property("sonar.xoo.internalKey", "OneIssuePerLine.internal")
      .start();

    assertThat(result.issues()).hasSize(14 /* 8 + 6 lines */+ 2 /* 2 files */);
  }

  @Test
  public void testOverrideQProfileSeverity() throws Exception {
    File projectDir = new File(IssuesMediumTest.class.getResource("/mediumtest/xoo/sample").toURI());

    TaskResult result = tester
      .newScanTask(new File(projectDir, "sonar-project.properties"))
      .property("sonar.oneIssuePerLine.forceSeverity", "CRITICAL")
      .start();

    assertThat(result.issues().iterator().next().severity()).isEqualTo(Severity.CRITICAL.name());
  }

  @Test
  public void testIssueExclusion() throws Exception {
    File projectDir = new File(IssuesMediumTest.class.getResource("/mediumtest/xoo/sample").toURI());

    TaskResult result = tester
      .newScanTask(new File(projectDir, "sonar-project.properties"))
      .property("sonar.issue.ignore.allfile", "1")
      .property("sonar.issue.ignore.allfile.1.fileRegexp", "object")
      .start();

    assertThat(result.issues()).hasSize(8);
  }

  @Test
  public void testIssueDetails() throws IOException {

    File baseDir = temp.newFolder();
    File srcDir = new File(baseDir, "src");
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
        .put("sonar.sources", "src")
        .build())
      .start();

    assertThat(result.issues()).hasSize(10);

    boolean foundIssueAtLine1 = false;
    for (org.sonar.api.issue.Issue issue : result.issues()) {
      if (issue.line() == 1) {
        foundIssueAtLine1 = true;
        assertThat(issue.componentKey()).isEqualTo("com.foo.project:src/sample.xoo");
        assertThat(issue.message()).isEqualTo("This issue is generated on each line");
        assertThat(issue.effortToFix()).isNull();
      }
    }
    assertThat(foundIssueAtLine1).isTrue();
  }

}
