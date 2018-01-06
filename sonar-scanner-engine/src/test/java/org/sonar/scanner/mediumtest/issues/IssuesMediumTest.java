/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.scanner.mediumtest.issues;

import com.google.common.collect.ImmutableMap;
import java.io.File;
import java.io.IOException;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.scanner.mediumtest.ScannerMediumTester;
import org.sonar.scanner.mediumtest.TaskResult;
import org.sonar.scanner.protocol.output.ScannerReport.Issue;
import org.sonar.xoo.XooPlugin;
import org.sonar.xoo.rule.XooRulesDefinition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class IssuesMediumTest {

  @org.junit.Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Rule
  public ScannerMediumTester tester = new ScannerMediumTester()
    .registerPlugin("xoo", new XooPlugin())
    .addDefaultQProfile("xoo", "Sonar Way")
    .addRules(new XooRulesDefinition())
    .addActiveRule("xoo", "OneIssuePerLine", null, "One issue per line", "MAJOR", "OneIssuePerLine.internal", "xoo");

  @Test
  public void testOneIssuePerLine() throws Exception {
    File projectDir = new File(IssuesMediumTest.class.getResource("/mediumtest/xoo/sample").toURI());
    File tmpDir = temp.newFolder();
    FileUtils.copyDirectory(projectDir, tmpDir);

    TaskResult result = tester
      .newScanTask(new File(tmpDir, "sonar-project.properties"))
      .execute();

    List<Issue> issues = result.issuesFor(result.inputFile("xources/hello/HelloJava.xoo"));
    assertThat(issues).hasSize(8 /* lines */);

    Issue issue = issues.get(0);
    assertThat(issue.getTextRange().getStartLine()).isEqualTo(issue.getTextRange().getStartLine());
  }

  @Test
  public void findActiveRuleByInternalKey() throws Exception {
    File projectDir = new File(IssuesMediumTest.class.getResource("/mediumtest/xoo/sample").toURI());
    File tmpDir = temp.newFolder();
    FileUtils.copyDirectory(projectDir, tmpDir);

    TaskResult result = tester
      .newScanTask(new File(tmpDir, "sonar-project.properties"))
      .property("sonar.xoo.internalKey", "OneIssuePerLine.internal")
      .execute();

    List<Issue> issues = result.issuesFor(result.inputFile("xources/hello/HelloJava.xoo"));
    assertThat(issues).hasSize(8 /* lines */ + 1 /* file */);
  }

  @Test
  public void testOverrideQProfileSeverity() throws Exception {
    File projectDir = new File(IssuesMediumTest.class.getResource("/mediumtest/xoo/sample").toURI());
    File tmpDir = temp.newFolder();
    FileUtils.copyDirectory(projectDir, tmpDir);

    TaskResult result = tester
      .newScanTask(new File(tmpDir, "sonar-project.properties"))
      .property("sonar.oneIssuePerLine.forceSeverity", "CRITICAL")
      .execute();

    List<Issue> issues = result.issuesFor(result.inputFile("xources/hello/HelloJava.xoo"));
    assertThat(issues.get(0).getSeverity()).isEqualTo(org.sonar.scanner.protocol.Constants.Severity.CRITICAL);
  }

  @Test
  public void testIssueExclusion() throws Exception {
    File projectDir = new File(IssuesMediumTest.class.getResource("/mediumtest/xoo/sample").toURI());
    File tmpDir = temp.newFolder();
    FileUtils.copyDirectory(projectDir, tmpDir);

    TaskResult result = tester
      .newScanTask(new File(tmpDir, "sonar-project.properties"))
      .property("sonar.issue.ignore.allfile", "1")
      .property("sonar.issue.ignore.allfile.1.fileRegexp", "object")
      .execute();

    assertThat(result.issuesFor(result.inputFile("xources/hello/HelloJava.xoo"))).hasSize(8 /* lines */);
    assertThat(result.issuesFor(result.inputFile("xources/hello/helloscala.xoo"))).isEmpty();
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
      .execute();

    List<Issue> issues = result.issuesFor(result.inputFile("src/sample.xoo"));
    assertThat(issues).hasSize(10);
    assertThat(issues)
      .extracting("msg", "textRange.startLine", "gap")
      .contains(tuple("This issue is generated on each line", 1, 0.0));
  }

}
