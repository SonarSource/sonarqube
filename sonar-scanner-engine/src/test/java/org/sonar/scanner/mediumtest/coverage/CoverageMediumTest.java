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
package org.sonar.scanner.mediumtest.coverage;

import com.google.common.collect.ImmutableMap;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.scanner.mediumtest.AnalysisResult;
import org.sonar.scanner.mediumtest.ScannerMediumTester;
import org.sonar.scanner.protocol.output.ScannerReport;
import org.sonar.xoo.XooPlugin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.tuple;

public class CoverageMediumTest {

  @Rule
  public LogTester logTester = new LogTester();

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Rule
  public ScannerMediumTester tester = new ScannerMediumTester()
    .registerPlugin("xoo", new XooPlugin())
    .addDefaultQProfile("xoo", "Sonar Way");

  @Test
  public void singleReport() throws IOException {

    File baseDir = temp.getRoot();
    File srcDir = new File(baseDir, "src");
    srcDir.mkdir();

    File xooFile = new File(srcDir, "sample.xoo");
    File xooUtCoverageFile = new File(srcDir, "sample.xoo.coverage");
    FileUtils.write(xooFile, "function foo() {\n  if (a && b) {\nalert('hello');\n}\n}", StandardCharsets.UTF_8);
    FileUtils.write(xooUtCoverageFile, "2:2:2:1\n3:1", StandardCharsets.UTF_8);

    AnalysisResult result = tester.newAnalysis()
      .properties(ImmutableMap.<String, String>builder()
        .put("sonar.projectBaseDir", baseDir.getAbsolutePath())
        .put("sonar.projectKey", "com.foo.project")
        .put("sonar.sources", "src")
        .build())
      .execute();

    InputFile file = result.inputFile("src/sample.xoo");
    assertThat(result.coverageFor(file, 2).getHits()).isTrue();
    assertThat(result.coverageFor(file, 2).getConditions()).isEqualTo(2);
    assertThat(result.coverageFor(file, 2).getCoveredConditions()).isEqualTo(1);
  }

  @Test
  public void twoReports() throws IOException {

    File baseDir = temp.getRoot();
    File srcDir = new File(baseDir, "src");
    srcDir.mkdir();

    File xooFile = new File(srcDir, "sample.xoo");
    FileUtils.write(xooFile, "function foo() {\n  if (a && b) {\nalert('hello');\n}\n}", StandardCharsets.UTF_8);
    File xooUtCoverageFile = new File(srcDir, "sample.xoo.coverage");
    FileUtils.write(xooUtCoverageFile, "2:2:2:2\n4:0", StandardCharsets.UTF_8);
    File xooItCoverageFile = new File(srcDir, "sample.xoo.itcoverage");
    FileUtils.write(xooItCoverageFile, "2:0:2:1\n3:1\n5:0", StandardCharsets.UTF_8);

    AnalysisResult result = tester.newAnalysis()
      .properties(ImmutableMap.<String, String>builder()
        .put("sonar.projectBaseDir", baseDir.getAbsolutePath())
        .put("sonar.projectKey", "com.foo.project")
        .put("sonar.sources", "src")
        .build())
      .execute();

    InputFile file = result.inputFile("src/sample.xoo");
    assertThat(result.coverageFor(file, 2).getHits()).isTrue();
    assertThat(result.coverageFor(file, 2).getConditions()).isEqualTo(2);
    assertThat(result.coverageFor(file, 2).getCoveredConditions()).isEqualTo(2);
    assertThat(result.coverageFor(file, 3).getHits()).isTrue();
  }

  @Test
  public void exclusionsForSimpleProject() throws IOException {

    File baseDir = temp.getRoot();
    File srcDir = new File(baseDir, "src");
    srcDir.mkdir();

    File xooFile = new File(srcDir, "sample.xoo");
    File xooUtCoverageFile = new File(srcDir, "sample.xoo.coverage");
    FileUtils.write(xooFile, "function foo() {\n  if (a && b) {\nalert('hello');\n}\n}", StandardCharsets.UTF_8);
    FileUtils.write(xooUtCoverageFile, "2:2:2:1\n3:1", StandardCharsets.UTF_8);

    AnalysisResult result = tester.newAnalysis()
      .properties(ImmutableMap.<String, String>builder()
        .put("sonar.projectBaseDir", baseDir.getAbsolutePath())
        .put("sonar.projectKey", "com.foo.project")
        .put("sonar.sources", "src")
        .put("sonar.coverage.exclusions", "**/sample.xoo")
        .build())
      .execute();

    InputFile file = result.inputFile("src/sample.xoo");
    assertThat(result.coverageFor(file, 2)).isNull();
  }

  @Test
  public void warn_user_for_outdated_inherited_scanner_side_exclusions_for_multi_module_project() throws IOException {

    File baseDir = temp.getRoot();
    File baseDirModuleA = new File(baseDir, "moduleA");
    File baseDirModuleB = new File(baseDir, "moduleB");
    File srcDirA = new File(baseDirModuleA, "src");
    srcDirA.mkdirs();
    File srcDirB = new File(baseDirModuleB, "src");
    srcDirB.mkdirs();

    File xooFileA = new File(srcDirA, "sampleA.xoo");
    File xooUtCoverageFileA = new File(srcDirA, "sampleA.xoo.coverage");
    FileUtils.write(xooFileA, "function foo() {\n  if (a && b) {\nalert('hello');\n}\n}", StandardCharsets.UTF_8);
    FileUtils.write(xooUtCoverageFileA, "2:2:2:1\n3:1", StandardCharsets.UTF_8);

    File xooFileB = new File(srcDirB, "sampleB.xoo");
    File xooUtCoverageFileB = new File(srcDirB, "sampleB.xoo.coverage");
    FileUtils.write(xooFileB, "function foo() {\n  if (a && b) {\nalert('hello');\n}\n}", StandardCharsets.UTF_8);
    FileUtils.write(xooUtCoverageFileB, "2:2:2:1\n3:1", StandardCharsets.UTF_8);

    AnalysisResult result = tester.newAnalysis()
      .properties(ImmutableMap.<String, String>builder()
        .put("sonar.projectBaseDir", baseDir.getAbsolutePath())
        .put("sonar.projectKey", "com.foo.project")
        .put("sonar.sources", "src")
        .put("sonar.modules", "moduleA,moduleB")
        .put("sonar.coverage.exclusions", "src/sampleA.xoo")
        .build())
      .execute();

    InputFile fileA = result.inputFile("moduleA/src/sampleA.xoo");
    assertThat(result.coverageFor(fileA, 2)).isNull();

    InputFile fileB = result.inputFile("moduleB/src/sampleB.xoo");
    assertThat(result.coverageFor(fileB, 2)).isNotNull();

    assertThat(logTester.logs(LoggerLevel.WARN)).contains("Specifying module-relative paths at project level in the property 'sonar.coverage.exclusions' is deprecated. " +
      "To continue matching files like 'moduleA/src/sampleA.xoo', update this property so that patterns refer to project-relative paths.");
  }

  @Test
  public void module_level_exclusions_override_parent_for_multi_module_project() throws IOException {

    File baseDir = temp.getRoot();
    File baseDirModuleA = new File(baseDir, "moduleA");
    File baseDirModuleB = new File(baseDir, "moduleB");
    File srcDirA = new File(baseDirModuleA, "src");
    srcDirA.mkdirs();
    File srcDirB = new File(baseDirModuleB, "src");
    srcDirB.mkdirs();

    File xooFileA = new File(srcDirA, "sampleA.xoo");
    File xooUtCoverageFileA = new File(srcDirA, "sampleA.xoo.coverage");
    FileUtils.write(xooFileA, "function foo() {\n  if (a && b) {\nalert('hello');\n}\n}", StandardCharsets.UTF_8);
    FileUtils.write(xooUtCoverageFileA, "2:2:2:1\n3:1", StandardCharsets.UTF_8);

    File xooFileB = new File(srcDirB, "sampleB.xoo");
    File xooUtCoverageFileB = new File(srcDirB, "sampleB.xoo.coverage");
    FileUtils.write(xooFileB, "function foo() {\n  if (a && b) {\nalert('hello');\n}\n}", StandardCharsets.UTF_8);
    FileUtils.write(xooUtCoverageFileB, "2:2:2:1\n3:1", StandardCharsets.UTF_8);

    AnalysisResult result = tester.newAnalysis()
      .properties(ImmutableMap.<String, String>builder()
        .put("sonar.projectBaseDir", baseDir.getAbsolutePath())
        .put("sonar.projectKey", "com.foo.project")
        .put("sonar.sources", "src")
        .put("sonar.modules", "moduleA,moduleB")
        .put("sonar.coverage.exclusions", "**/*.xoo")
        .put("moduleA.sonar.coverage.exclusions", "**/*.nothing")
        .build())
      .execute();

    InputFile fileA = result.inputFile("moduleA/src/sampleA.xoo");
    assertThat(result.coverageFor(fileA, 2)).isNotNull();

    InputFile fileB = result.inputFile("moduleB/src/sampleB.xoo");
    assertThat(result.coverageFor(fileB, 2)).isNull();
  }

  @Test
  public void warn_user_for_outdated_server_side_exclusions_for_multi_module_project() throws IOException {

    File baseDir = temp.getRoot();
    File baseDirModuleA = new File(baseDir, "moduleA");
    File baseDirModuleB = new File(baseDir, "moduleB");
    File srcDirA = new File(baseDirModuleA, "src");
    srcDirA.mkdirs();
    File srcDirB = new File(baseDirModuleB, "src");
    srcDirB.mkdirs();

    File xooFileA = new File(srcDirA, "sample.xoo");
    File xooUtCoverageFileA = new File(srcDirA, "sample.xoo.coverage");
    FileUtils.write(xooFileA, "function foo() {\n  if (a && b) {\nalert('hello');\n}\n}", StandardCharsets.UTF_8);
    FileUtils.write(xooUtCoverageFileA, "2:2:2:1\n3:1", StandardCharsets.UTF_8);

    File xooFileB = new File(srcDirB, "sample.xoo");
    File xooUtCoverageFileB = new File(srcDirB, "sample.xoo.coverage");
    FileUtils.write(xooFileB, "function foo() {\n  if (a && b) {\nalert('hello');\n}\n}", StandardCharsets.UTF_8);
    FileUtils.write(xooUtCoverageFileB, "2:2:2:1\n3:1", StandardCharsets.UTF_8);

    tester.addProjectServerSettings("sonar.coverage.exclusions", "src/sample.xoo");

    AnalysisResult result = tester.newAnalysis()
      .properties(ImmutableMap.<String, String>builder()
        .put("sonar.projectBaseDir", baseDir.getAbsolutePath())
        .put("sonar.projectKey", "com.foo.project")
        .put("sonar.sources", "src")
        .put("sonar.modules", "moduleA,moduleB")
        .build())
      .execute();

    InputFile fileA = result.inputFile("moduleA/src/sample.xoo");
    assertThat(result.coverageFor(fileA, 2)).isNull();

    InputFile fileB = result.inputFile("moduleB/src/sample.xoo");
    assertThat(result.coverageFor(fileB, 2)).isNull();

    assertThat(logTester.logs(LoggerLevel.WARN)).contains("Specifying module-relative paths at project level in the property 'sonar.coverage.exclusions' is deprecated. " +
      "To continue matching files like 'moduleA/src/sample.xoo', update this property so that patterns refer to project-relative paths.");
  }

  @Test
  public void fallbackOnExecutableLines() throws IOException {

    File baseDir = temp.getRoot();
    File srcDir = new File(baseDir, "src");
    srcDir.mkdir();

    File xooFile = new File(srcDir, "sample.xoo");
    File measuresFile = new File(srcDir, "sample.xoo.measures");
    FileUtils.write(xooFile, "function foo() {\n  if (a && b) {\nalert('hello');\n}\n}", StandardCharsets.UTF_8);
    FileUtils.write(measuresFile, "executable_lines_data:2=1;3=1;4=0", StandardCharsets.UTF_8);

    AnalysisResult result = tester.newAnalysis()
      .properties(ImmutableMap.<String, String>builder()
        .put("sonar.projectBaseDir", baseDir.getAbsolutePath())
        .put("sonar.projectKey", "com.foo.project")
        .put("sonar.sources", "src")
        .build())
      .execute();

    InputFile file = result.inputFile("src/sample.xoo");
    assertThat(result.coverageFor(file, 1)).isNull();

    assertThat(result.coverageFor(file, 2).getHits()).isFalse();
    assertThat(result.coverageFor(file, 2).getConditions()).isEqualTo(0);
    assertThat(result.coverageFor(file, 2).getCoveredConditions()).isEqualTo(0);

    assertThat(result.coverageFor(file, 3).getHits()).isFalse();
    assertThat(result.coverageFor(file, 4)).isNull();

    assertThat(result.allMeasures().get(file.key()))
      .extracting(ScannerReport.Measure::getMetricKey, m -> m.getStringValue().getValue())
      .contains(tuple("executable_lines_data", "2=1;3=1;4=0"));
  }

  // SONAR-11641
  @Test
  public void dontFallbackOnExecutableLinesIfNoCoverageSaved() throws IOException {

    File baseDir = temp.getRoot();
    File srcDir = new File(baseDir, "src");
    srcDir.mkdir();

    File xooFile = new File(srcDir, "sample.xoo");
    File measuresFile = new File(srcDir, "sample.xoo.measures");
    File coverageFile = new File(srcDir, "sample.xoo.coverage");
    FileUtils.write(xooFile, "function foo() {\n  if (a && b) {\nalert('hello');\n}\n}", StandardCharsets.UTF_8);
    FileUtils.write(measuresFile, "# The code analyzer disagree with the coverage tool and consider some lines to be executable\nexecutable_lines_data:2=1;3=1;4=0",
      StandardCharsets.UTF_8);
    FileUtils.write(coverageFile, "# No lines to cover in this file according to the coverage tool", StandardCharsets.UTF_8);

    AnalysisResult result = tester.newAnalysis()
      .properties(ImmutableMap.<String, String>builder()
        .put("sonar.projectBaseDir", baseDir.getAbsolutePath())
        .put("sonar.projectKey", "com.foo.project")
        .put("sonar.sources", "src")
        .build())
      .execute();

    InputFile file = result.inputFile("src/sample.xoo");
    assertThat(result.coverageFor(file, 1)).isNull();
    assertThat(result.coverageFor(file, 2)).isNull();
    assertThat(result.coverageFor(file, 3)).isNull();
    assertThat(result.coverageFor(file, 4)).isNull();
  }

  // SONAR-9557
  @Test
  public void exclusionsAndForceToZeroOnModules() throws IOException {

    File baseDir = temp.getRoot();
    File srcDir = new File(baseDir, "module1/src");
    srcDir.mkdir();

    File xooFile1 = new File(srcDir, "sample1.xoo");
    File measuresFile1 = new File(srcDir, "sample1.xoo.measures");
    FileUtils.write(xooFile1, "function foo() {\n  if (a && b) {\nalert('hello');\n}\n}", StandardCharsets.UTF_8);
    FileUtils.write(measuresFile1, "executable_lines_data:2=1;3=1;4=0", StandardCharsets.UTF_8);

    File xooFile2 = new File(srcDir, "sample2.xoo");
    File measuresFile2 = new File(srcDir, "sample2.xoo.measures");
    FileUtils.write(xooFile2, "function foo() {\n  if (a && b) {\nalert('hello');\n}\n}", StandardCharsets.UTF_8);
    FileUtils.write(measuresFile2, "executable_lines_data:2=1;3=1;4=0", StandardCharsets.UTF_8);

    AnalysisResult result = tester.newAnalysis()
      .properties(ImmutableMap.<String, String>builder()
        .put("sonar.projectBaseDir", baseDir.getAbsolutePath())
        .put("sonar.projectKey", "com.foo.project")
        .put("sonar.modules", "module1")
        .put("sonar.sources", "src")
        .put("sonar.coverage.exclusions", "**/sample2.xoo")
        .build())
      .execute();

    InputFile file1 = result.inputFile("module1/src/sample1.xoo");
    assertThat(result.coverageFor(file1, 1)).isNull();

    assertThat(result.coverageFor(file1, 2).getHits()).isFalse();
    assertThat(result.coverageFor(file1, 2).getConditions()).isEqualTo(0);
    assertThat(result.coverageFor(file1, 2).getCoveredConditions()).isEqualTo(0);

    assertThat(result.coverageFor(file1, 3).getHits()).isFalse();
    assertThat(result.coverageFor(file1, 4)).isNull();

    InputFile file2 = result.inputFile("module1/src/sample2.xoo");
    assertThat(result.coverageFor(file2, 1)).isNull();
    assertThat(result.coverageFor(file2, 2)).isNull();
    assertThat(result.coverageFor(file2, 3)).isNull();
    assertThat(result.coverageFor(file2, 4)).isNull();

  }

}
