/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.scanner.mediumtest.ScannerMediumTester;
import org.sonar.scanner.mediumtest.AnalysisResult;
import org.sonar.xoo.XooPlugin;
import org.sonar.xoo.rule.XooRulesDefinition;

import static org.assertj.core.api.Assertions.assertThat;

public class IssuesOnDirMediumTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Rule
  public ScannerMediumTester tester = new ScannerMediumTester()
    .registerPlugin("xoo", new XooPlugin())
    .addDefaultQProfile("xoo", "Sonar Way")
    .addRules(new XooRulesDefinition())
    .addActiveRule("xoo", "OneIssueOnDirPerFile", null, "One issue per line", "MINOR", "xoo", "xoo");

  @Test
  public void scanTempProject() throws IOException {

    File baseDir = temp.getRoot();
    File srcDir = new File(baseDir, "src");
    srcDir.mkdir();

    File xooFile1 = new File(srcDir, "sample1.xoo");
    FileUtils.write(xooFile1, "Sample1 xoo\ncontent");

    File xooFile2 = new File(srcDir, "sample2.xoo");
    FileUtils.write(xooFile2, "Sample2 xoo\ncontent");

    AnalysisResult result = tester.newAnalysis()
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

    assertThat(result.issuesFor(result.project())).hasSize(2);
  }

  @Test
  public void issueOnRootFolder() throws IOException {

    File baseDir = temp.getRoot();

    File xooFile1 = new File(baseDir, "sample1.xoo");
    FileUtils.write(xooFile1, "Sample1 xoo\ncontent");

    File xooFile2 = new File(baseDir, "sample2.xoo");
    FileUtils.write(xooFile2, "Sample2 xoo\ncontent");

    AnalysisResult result = tester.newAnalysis()
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

    assertThat(result.issuesFor(result.project())).hasSize(2);
  }

}
