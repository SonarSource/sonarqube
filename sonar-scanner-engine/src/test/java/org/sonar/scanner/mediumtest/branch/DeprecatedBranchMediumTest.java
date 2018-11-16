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
package org.sonar.scanner.mediumtest.branch;

import com.google.common.collect.ImmutableMap;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.scanner.mediumtest.ScannerMediumTester;
import org.sonar.scanner.mediumtest.AnalysisResult;
import org.sonar.xoo.XooPlugin;
import org.sonar.xoo.rule.XooRulesDefinition;

import static org.assertj.core.api.Assertions.assertThat;

public class DeprecatedBranchMediumTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Rule
  public ScannerMediumTester tester = new ScannerMediumTester()
    .registerPlugin("xoo", new XooPlugin())
    .addRules(new XooRulesDefinition())
    // active a rule just to be sure that xoo files are published
    .addActiveRule("xoo", "xoo:OneIssuePerFile", null, "One Issue Per File", null, null, null)
    .addDefaultQProfile("xoo", "Sonar Way");

  private File baseDir;

  private Map<String, String> commonProps;

  @Before
  public void prepare() throws IOException {
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

  @Test
  public void scanProjectWithBranch() throws IOException {
    File srcDir = new File(baseDir, "src");
    srcDir.mkdir();

    File xooFile = new File(srcDir, "sample.xoo");
    FileUtils.write(xooFile, "Sample xoo\ncontent");

    AnalysisResult result = tester.newAnalysis()
      .properties(ImmutableMap.<String, String>builder()
        .putAll(commonProps)
        .put("sonar.branch", "branch")
        .build())
      .execute();

    assertThat(result.inputFiles()).hasSize(1);
    assertThat(result.inputFile("src/sample.xoo").key()).isEqualTo("com.foo.project:src/sample.xoo");

    DefaultInputFile inputfile = (DefaultInputFile) result.inputFile("src/sample.xoo");
    assertThat(result.getReportReader().readComponent(inputfile.scannerId()).getProjectRelativePath()).isEqualTo("src/sample.xoo");

    assertThat(result.getReportReader().readMetadata().getDeprecatedBranch()).isEqualTo("branch");

    result = tester.newAnalysis()
      .properties(ImmutableMap.<String, String>builder()
        .putAll(commonProps)
        .put("sonar.branch", "")
        .build())
      .execute();

    assertThat(result.inputFiles()).hasSize(1);
    assertThat(result.inputFile("src/sample.xoo").key()).isEqualTo("com.foo.project:src/sample.xoo");
  }

  @Test
  public void scanMultiModuleWithBranch() throws IOException {
    Path srcDir = baseDir.toPath().resolve("moduleA").resolve("src");
    Files.createDirectories(srcDir);

    File xooFile = new File(srcDir.toFile(), "sample.xoo");
    FileUtils.write(xooFile, "Sample xoo\ncontent");

    AnalysisResult result = tester.newAnalysis()
      .properties(ImmutableMap.<String, String>builder()
        .putAll(commonProps)
        .put("sonar.branch", "branch")
        .put("sonar.modules", "moduleA")
        .build())
      .execute();

    assertThat(result.inputFiles()).hasSize(1);
    assertThat(result.inputFile("moduleA/src/sample.xoo").key()).isEqualTo("com.foo.project:moduleA/src/sample.xoo");

    // no branch in the report
    DefaultInputFile inputfile = (DefaultInputFile) result.inputFile("moduleA/src/sample.xoo");
    assertThat(result.getReportReader().readComponent(inputfile.scannerId()).getProjectRelativePath()).isEqualTo("moduleA/src/sample.xoo");

    assertThat(result.getReportReader().readMetadata().getDeprecatedBranch()).isEqualTo("branch");

    result = tester.newAnalysis()
      .properties(ImmutableMap.<String, String>builder()
        .putAll(commonProps)
        .put("sonar.branch", "")
        .put("sonar.modules", "moduleA")
        .build())
      .execute();

    assertThat(result.inputFiles()).hasSize(1);
    assertThat(result.inputFile("moduleA/src/sample.xoo").key()).isEqualTo("com.foo.project:moduleA/src/sample.xoo");
  }

}
