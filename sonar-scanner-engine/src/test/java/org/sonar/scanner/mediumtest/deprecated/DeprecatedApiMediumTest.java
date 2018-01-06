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
package org.sonar.scanner.mediumtest.deprecated;

import com.google.common.collect.ImmutableMap;
import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.scanner.mediumtest.ScannerMediumTester;
import org.sonar.scanner.mediumtest.TaskResult;
import org.sonar.xoo.XooPlugin;
import org.sonar.xoo.rule.XooRulesDefinition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

public class DeprecatedApiMediumTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Rule
  public ScannerMediumTester tester = new ScannerMediumTester()
    .registerPlugin("xoo", new XooPlugin())
    .addRules(new XooRulesDefinition())
    .addDefaultQProfile("xoo", "Sonar Way")
    .addActiveRule("xoo", "DeprecatedResourceApi", null, "One issue per line", "MAJOR", null, "xoo");

  @Test
  public void testIssueDetails() throws IOException {

    File baseDir = temp.getRoot();
    File srcDir = new File(baseDir, "src");
    srcDir.mkdir();

    File xooFileInRootDir = new File(srcDir, "sample.xoo");
    FileUtils.write(xooFileInRootDir, "1\n2\n3\n4\n5\n6\n7\n8\n9\n10");

    File xooFileInAnotherDir = new File(srcDir, "package/sample.xoo");
    FileUtils.write(xooFileInAnotherDir, "1\n2\n3\n4\n5\n6\n7\n8\n9\n10");

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

    assertThat(result.issuesFor(result.inputFile("src/sample.xoo"))).extracting("msg", "textRange.startLine").containsOnly(
      tuple("Issue created using deprecated API", 0),
      tuple("Issue created using deprecated API", 1));
    assertThat(result.issuesFor(result.inputFile("src/package/sample.xoo"))).extracting("msg", "textRange.startLine").containsOnly(
      tuple("Issue created using deprecated API", 0),
      tuple("Issue created using deprecated API", 1));
    assertThat(result.issuesFor(result.inputDir("src"))).extracting("msg", "textRange.startLine").containsOnly(
      tuple("Issue created using deprecated API", 0));
    assertThat(result.issuesFor(result.inputDir("src/package"))).extracting("msg", "textRange.startLine").containsOnly(
      tuple("Issue created using deprecated API", 0));

  }

  @Test
  public void createIssueOnRootDir() throws IOException {

    File baseDir = temp.getRoot();

    File xooFileInRootDir = new File(baseDir, "sample.xoo");
    FileUtils.write(xooFileInRootDir, "1\n2\n3\n4\n5\n6\n7\n8\n9\n10");

    File xooFileInAnotherDir = new File(baseDir, "package/sample.xoo");
    FileUtils.write(xooFileInAnotherDir, "1\n2\n3\n4\n5\n6\n7\n8\n9\n10");

    TaskResult result = tester.newTask()
      .properties(ImmutableMap.<String, String>builder()
        .put("sonar.task", "scan")
        .put("sonar.projectBaseDir", baseDir.getAbsolutePath())
        .put("sonar.projectKey", "com.foo.project")
        .put("sonar.projectName", "Foo Project")
        .put("sonar.projectVersion", "1.0-SNAPSHOT")
        .put("sonar.projectDescription", "Description of Foo Project")
        .put("sonar.sources", ".")
        .build())
      .execute();

    assertThat(result.issuesFor(result.inputFile("sample.xoo"))).extracting("msg", "textRange.startLine").containsOnly(
      tuple("Issue created using deprecated API", 0),
      tuple("Issue created using deprecated API", 1));
    assertThat(result.issuesFor(result.inputFile("package/sample.xoo"))).extracting("msg", "textRange.startLine").containsOnly(
      tuple("Issue created using deprecated API", 0),
      tuple("Issue created using deprecated API", 1));
    assertThat(result.issuesFor(result.inputDir(""))).extracting("msg", "textRange.startLine").containsOnly(
      tuple("Issue created using deprecated API", 0));
    assertThat(result.issuesFor(result.inputDir("package"))).extracting("msg", "textRange.startLine").containsOnly(
      tuple("Issue created using deprecated API", 0));

  }

}
