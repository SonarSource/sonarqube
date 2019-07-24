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
package org.sonar.scanner.mediumtest.cpd;

import com.google.common.collect.ImmutableMap;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.scanner.mediumtest.AnalysisResult;
import org.sonar.scanner.mediumtest.ScannerMediumTester;
import org.sonar.scanner.protocol.output.ScannerReport;
import org.sonar.xoo.XooPlugin;
import org.sonar.xoo.rule.XooRulesDefinition;

import static org.assertj.core.api.Assertions.assertThat;

public class CpdMediumTest {
  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Rule
  public LogTester logTester = new LogTester();

  private File baseDir;

  @Rule
  public ScannerMediumTester tester = new ScannerMediumTester()
    .registerPlugin("xoo", new XooPlugin())
    .addDefaultQProfile("xoo", "Sonar Way")
    .addRules(new XooRulesDefinition())
    // active a rule just to be sure that xoo files are published
    .addActiveRule("xoo", "xoo:OneIssuePerFile", null, "One Issue Per File", null, null, null);

  private ImmutableMap.Builder<String, String> builder;

  @Before
  public void prepare() {
    baseDir = temp.getRoot();

    builder = ImmutableMap.<String, String>builder()
      .put("sonar.task", "scan")
      .put("sonar.projectBaseDir", baseDir.getAbsolutePath())
      .put("sonar.projectKey", "com.foo.project")
      .put("sonar.projectName", "Foo Project")
      .put("sonar.projectVersion", "1.0-SNAPSHOT")
      .put("sonar.projectDescription", "Description of Foo Project");
  }

  @Test
  public void testCrossModuleDuplications() throws IOException {
    builder.put("sonar.modules", "module1,module2")
      .put("sonar.cpd.xoo.minimumTokens", "10")
      .put("sonar.verbose", "true");

    // module 1
    builder.put("module1.sonar.projectKey", "module1");
    builder.put("module1.sonar.projectName", "Module 1");
    builder.put("module1.sonar.sources", ".");

    // module2
    builder.put("module2.sonar.projectKey", "module2");
    builder.put("module2.sonar.projectName", "Module 2");
    builder.put("module2.sonar.sources", ".");

    File module1Dir = new File(baseDir, "module1");
    File module2Dir = new File(baseDir, "module2");

    module1Dir.mkdir();
    module2Dir.mkdir();

    String duplicatedStuff = "Sample xoo\ncontent\n"
      + "foo\nbar\ntoto\ntiti\n"
      + "foo\nbar\ntoto\ntiti\n"
      + "bar\ntoto\ntiti\n"
      + "foo\nbar\ntoto\ntiti";

    // create duplicated file in both modules
    File xooFile1 = new File(module1Dir, "sample1.xoo");
    FileUtils.write(xooFile1, duplicatedStuff);

    File xooFile2 = new File(module2Dir, "sample2.xoo");
    FileUtils.write(xooFile2, duplicatedStuff);

    AnalysisResult result = tester.newAnalysis().properties(builder.build()).execute();

    assertThat(result.inputFiles()).hasSize(2);

    InputFile inputFile1 = result.inputFile("module1/sample1.xoo");
    InputFile inputFile2 = result.inputFile("module2/sample2.xoo");

    // One clone group on each file
    List<ScannerReport.Duplication> duplicationGroupsFile1 = result.duplicationsFor(inputFile1);
    assertThat(duplicationGroupsFile1).hasSize(1);

    ScannerReport.Duplication cloneGroupFile1 = duplicationGroupsFile1.get(0);
    assertThat(cloneGroupFile1.getOriginPosition().getStartLine()).isEqualTo(1);
    assertThat(cloneGroupFile1.getOriginPosition().getEndLine()).isEqualTo(17);
    assertThat(cloneGroupFile1.getDuplicateList()).hasSize(1);
    assertThat(cloneGroupFile1.getDuplicate(0).getOtherFileRef()).isEqualTo(result.getReportComponent(inputFile2).getRef());

    List<ScannerReport.Duplication> duplicationGroupsFile2 = result.duplicationsFor(inputFile2);
    assertThat(duplicationGroupsFile2).hasSize(1);

    ScannerReport.Duplication cloneGroupFile2 = duplicationGroupsFile2.get(0);
    assertThat(cloneGroupFile2.getOriginPosition().getStartLine()).isEqualTo(1);
    assertThat(cloneGroupFile2.getOriginPosition().getEndLine()).isEqualTo(17);
    assertThat(cloneGroupFile2.getDuplicateList()).hasSize(1);
    assertThat(cloneGroupFile2.getDuplicate(0).getOtherFileRef()).isEqualTo(result.getReportComponent(inputFile1).getRef());

    assertThat(result.duplicationBlocksFor(inputFile1)).isEmpty();
  }

  @Test
  public void testCrossFileDuplications() throws IOException {
    File srcDir = new File(baseDir, "src");
    srcDir.mkdir();

    String duplicatedStuff = "Sample xoo\ncontent\n"
      + "foo\nbar\ntoto\ntiti\n"
      + "foo\nbar\ntoto\ntiti\n"
      + "bar\ntoto\ntiti\n"
      + "foo\nbar\ntoto\ntiti";

    File xooFile1 = new File(srcDir, "sample1.xoo");
    FileUtils.write(xooFile1, duplicatedStuff, StandardCharsets.UTF_8);

    File xooFile2 = new File(srcDir, "sample2.xoo");
    FileUtils.write(xooFile2, duplicatedStuff, StandardCharsets.UTF_8);

    AnalysisResult result = tester.newAnalysis()
      .properties(builder
        .put("sonar.sources", "src")
        .put("sonar.cpd.xoo.minimumTokens", "10")
        .put("sonar.verbose", "true")
        .build())
      .execute();

    assertThat(result.inputFiles()).hasSize(2);

    InputFile inputFile1 = result.inputFile("src/sample1.xoo");
    InputFile inputFile2 = result.inputFile("src/sample2.xoo");

    // One clone group on each file
    List<ScannerReport.Duplication> duplicationGroupsFile1 = result.duplicationsFor(inputFile1);
    assertThat(duplicationGroupsFile1).hasSize(1);

    ScannerReport.Duplication cloneGroupFile1 = duplicationGroupsFile1.get(0);
    assertThat(cloneGroupFile1.getOriginPosition().getStartLine()).isEqualTo(1);
    assertThat(cloneGroupFile1.getOriginPosition().getEndLine()).isEqualTo(17);
    assertThat(cloneGroupFile1.getDuplicateList()).hasSize(1);
    assertThat(cloneGroupFile1.getDuplicate(0).getOtherFileRef()).isEqualTo(result.getReportComponent(inputFile2).getRef());

    List<ScannerReport.Duplication> duplicationGroupsFile2 = result.duplicationsFor(inputFile2);
    assertThat(duplicationGroupsFile2).hasSize(1);

    ScannerReport.Duplication cloneGroupFile2 = duplicationGroupsFile2.get(0);
    assertThat(cloneGroupFile2.getOriginPosition().getStartLine()).isEqualTo(1);
    assertThat(cloneGroupFile2.getOriginPosition().getEndLine()).isEqualTo(17);
    assertThat(cloneGroupFile2.getDuplicateList()).hasSize(1);
    assertThat(cloneGroupFile2.getDuplicate(0).getOtherFileRef()).isEqualTo(result.getReportComponent(inputFile1).getRef());

    assertThat(result.duplicationBlocksFor(inputFile1)).isEmpty();
  }

  @Test
  public void testFilesWithoutBlocks() throws IOException {
    File srcDir = new File(baseDir, "src");
    srcDir.mkdir();

    String file1 = "Sample xoo\ncontent\n"
      + "foo\nbar\ntoto\ntiti\n"
      + "foo\nbar\ntoto\ntiti\n"
      + "bar\ntoto\ntiti\n"
      + "foo\nbar\ntoto\ntiti";

    String file2 = "string\n";

    File xooFile1 = new File(srcDir, "sample1.xoo");
    FileUtils.write(xooFile1, file1, StandardCharsets.UTF_8);

    File xooFile2 = new File(srcDir, "sample2.xoo");
    FileUtils.write(xooFile2, file2, StandardCharsets.UTF_8);

    AnalysisResult result = tester.newAnalysis()
      .properties(builder
        .put("sonar.sources", "src")
        .put("sonar.cpd.xoo.minimumTokens", "10")
        .put("sonar.verbose", "true")
        .build())
      .execute();

    assertThat(result.inputFiles()).hasSize(2);

    assertThat(logTester.logs()).contains("Not enough content in 'src/sample2.xoo' to have CPD blocks, it will not be part of the duplication detection");
    assertThat(logTester.logs()).contains("CPD Executor 1 file had no CPD blocks");

  }

  @Test
  public void testExclusions() throws IOException {
    File srcDir = new File(baseDir, "src");
    srcDir.mkdir();

    String duplicatedStuff = "Sample xoo\ncontent\n"
      + "foo\nbar\ntoto\ntiti\n"
      + "foo\nbar\ntoto\ntiti\n"
      + "bar\ntoto\ntiti\n"
      + "foo\nbar\ntoto\ntiti";

    File xooFile1 = new File(srcDir, "sample1.xoo");
    FileUtils.write(xooFile1, duplicatedStuff);

    File xooFile2 = new File(srcDir, "sample2.xoo");
    FileUtils.write(xooFile2, duplicatedStuff);

    AnalysisResult result = tester.newAnalysis()
      .properties(builder
        .put("sonar.sources", "src")
        .put("sonar.cpd.xoo.minimumTokens", "10")
        .put("sonar.cpd.exclusions", "src/sample1.xoo")
        .build())
      .execute();

    assertThat(result.inputFiles()).hasSize(2);

    InputFile inputFile1 = result.inputFile("src/sample1.xoo");
    InputFile inputFile2 = result.inputFile("src/sample2.xoo");

    List<ScannerReport.Duplication> duplicationGroupsFile1 = result.duplicationsFor(inputFile1);
    assertThat(duplicationGroupsFile1).isEmpty();

    List<ScannerReport.Duplication> duplicationGroupsFile2 = result.duplicationsFor(inputFile2);
    assertThat(duplicationGroupsFile2).isEmpty();
  }

  @Test
  public void cross_module_duplication() throws IOException {

    String duplicatedStuff = "Sample xoo\ncontent\n"
      + "foo\nbar\ntoto\ntiti\n"
      + "foo\nbar\ntoto\ntiti\n"
      + "bar\ntoto\ntiti\n"
      + "foo\nbar\ntoto\ntiti";

    File baseDir = temp.getRoot();
    File baseDirModuleA = new File(baseDir, "moduleA");
    File baseDirModuleB = new File(baseDir, "moduleB");
    File srcDirA = new File(baseDirModuleA, "src");
    srcDirA.mkdirs();
    File srcDirB = new File(baseDirModuleB, "src");
    srcDirB.mkdirs();

    File xooFileA = new File(srcDirA, "sampleA.xoo");
    FileUtils.write(xooFileA, duplicatedStuff, StandardCharsets.UTF_8);

    File xooFileB = new File(srcDirB, "sampleB.xoo");
    FileUtils.write(xooFileB, duplicatedStuff, StandardCharsets.UTF_8);

    AnalysisResult result = tester.newAnalysis()
      .properties(ImmutableMap.<String, String>builder()
        .put("sonar.projectBaseDir", baseDir.getAbsolutePath())
        .put("sonar.projectKey", "com.foo.project")
        .put("sonar.sources", "src")
        .put("sonar.modules", "moduleA,moduleB")
        .put("sonar.cpd.xoo.minimumTokens", "10")
        .build())
      .execute();

    InputFile inputFile1 = result.inputFile("moduleA/src/sampleA.xoo");
    InputFile inputFile2 = result.inputFile("moduleB/src/sampleB.xoo");

    List<ScannerReport.Duplication> duplicationGroupsFile1 = result.duplicationsFor(inputFile1);
    assertThat(duplicationGroupsFile1).isNotEmpty();

    List<ScannerReport.Duplication> duplicationGroupsFile2 = result.duplicationsFor(inputFile2);
    assertThat(duplicationGroupsFile2).isNotEmpty();
  }

  @Test
  public void warn_user_for_outdated_inherited_scanner_side_exclusions_for_multi_module_project() throws IOException {

    String duplicatedStuff = "Sample xoo\ncontent\n"
      + "foo\nbar\ntoto\ntiti\n"
      + "foo\nbar\ntoto\ntiti\n"
      + "bar\ntoto\ntiti\n"
      + "foo\nbar\ntoto\ntiti";

    File baseDir = temp.getRoot();
    File baseDirModuleA = new File(baseDir, "moduleA");
    File baseDirModuleB = new File(baseDir, "moduleB");
    File srcDirA = new File(baseDirModuleA, "src");
    srcDirA.mkdirs();
    File srcDirB = new File(baseDirModuleB, "src");
    srcDirB.mkdirs();

    File xooFileA = new File(srcDirA, "sampleA.xoo");
    FileUtils.write(xooFileA, duplicatedStuff, StandardCharsets.UTF_8);

    File xooFileB = new File(srcDirB, "sampleB.xoo");
    FileUtils.write(xooFileB, duplicatedStuff, StandardCharsets.UTF_8);

    AnalysisResult result = tester.newAnalysis()
      .properties(ImmutableMap.<String, String>builder()
        .put("sonar.projectBaseDir", baseDir.getAbsolutePath())
        .put("sonar.projectKey", "com.foo.project")
        .put("sonar.sources", "src")
        .put("sonar.modules", "moduleA,moduleB")
        .put("sonar.cpd.xoo.minimumTokens", "10")
        .put("sonar.cpd.exclusions", "src/sampleA.xoo")
        .build())
      .execute();

    InputFile inputFile1 = result.inputFile("moduleA/src/sampleA.xoo");
    InputFile inputFile2 = result.inputFile("moduleB/src/sampleB.xoo");

    List<ScannerReport.Duplication> duplicationGroupsFile1 = result.duplicationsFor(inputFile1);
    assertThat(duplicationGroupsFile1).isEmpty();

    List<ScannerReport.Duplication> duplicationGroupsFile2 = result.duplicationsFor(inputFile2);
    assertThat(duplicationGroupsFile2).isEmpty();

    assertThat(logTester.logs(LoggerLevel.WARN)).contains("Specifying module-relative paths at project level in the property 'sonar.cpd.exclusions' is deprecated. " +
      "To continue matching files like 'moduleA/src/sampleA.xoo', update this property so that patterns refer to project-relative paths.");
  }

  @Test
  public void module_level_exclusions_override_parent_for_multi_module_project() throws IOException {

    String duplicatedStuff = "Sample xoo\ncontent\n"
      + "foo\nbar\ntoto\ntiti\n"
      + "foo\nbar\ntoto\ntiti\n"
      + "bar\ntoto\ntiti\n"
      + "foo\nbar\ntoto\ntiti";

    File baseDir = temp.getRoot();
    File baseDirModuleA = new File(baseDir, "moduleA");
    File baseDirModuleB = new File(baseDir, "moduleB");
    File srcDirA = new File(baseDirModuleA, "src");
    srcDirA.mkdirs();
    File srcDirB = new File(baseDirModuleB, "src");
    srcDirB.mkdirs();

    File xooFileA = new File(srcDirA, "sampleA.xoo");
    FileUtils.write(xooFileA, duplicatedStuff, StandardCharsets.UTF_8);

    File xooFileB = new File(srcDirB, "sampleB.xoo");
    FileUtils.write(xooFileB, duplicatedStuff, StandardCharsets.UTF_8);

    AnalysisResult result = tester.newAnalysis()
      .properties(ImmutableMap.<String, String>builder()
        .put("sonar.projectBaseDir", baseDir.getAbsolutePath())
        .put("sonar.projectKey", "com.foo.project")
        .put("sonar.sources", "src")
        .put("sonar.modules", "moduleA,moduleB")
        .put("sonar.cpd.xoo.minimumTokens", "10")
        .put("sonar.cpd.exclusions", "**/*")
        .put("moduleA.sonar.cpd.exclusions", "**/*.nothing")
        .put("moduleB.sonar.cpd.exclusions", "**/*.nothing")
        .build())
      .execute();

    InputFile inputFile1 = result.inputFile("moduleA/src/sampleA.xoo");
    InputFile inputFile2 = result.inputFile("moduleB/src/sampleB.xoo");

    List<ScannerReport.Duplication> duplicationGroupsFile1 = result.duplicationsFor(inputFile1);
    assertThat(duplicationGroupsFile1).isNotEmpty();

    List<ScannerReport.Duplication> duplicationGroupsFile2 = result.duplicationsFor(inputFile2);
    assertThat(duplicationGroupsFile2).isNotEmpty();
  }

  @Test
  public void enableCrossProjectDuplication() throws IOException {
    File srcDir = new File(baseDir, "src");
    srcDir.mkdir();

    String duplicatedStuff = "Sample xoo\ncontent\nfoo\nbar\ntoto\ntiti\nfoo";

    File xooFile1 = new File(srcDir, "sample1.xoo");
    FileUtils.write(xooFile1, duplicatedStuff);

    AnalysisResult result = tester.newAnalysis()
      .properties(builder
        .put("sonar.sources", "src")
        .put("sonar.cpd.xoo.minimumTokens", "1")
        .put("sonar.cpd.xoo.minimumLines", "5")
        .put("sonar.verbose", "true")
        .put("sonar.cpd.cross_project", "true")
        .build())
      .execute();

    InputFile inputFile1 = result.inputFile("src/sample1.xoo");

    List<ScannerReport.CpdTextBlock> duplicationBlocks = result.duplicationBlocksFor(inputFile1);
    assertThat(duplicationBlocks).hasSize(3);
    assertThat(duplicationBlocks.get(0).getStartLine()).isEqualTo(1);
    assertThat(duplicationBlocks.get(0).getEndLine()).isEqualTo(5);
    assertThat(duplicationBlocks.get(0).getStartTokenIndex()).isEqualTo(1);
    assertThat(duplicationBlocks.get(0).getEndTokenIndex()).isEqualTo(6);
    assertThat(duplicationBlocks.get(0).getHash()).isNotEmpty();

    assertThat(duplicationBlocks.get(1).getStartLine()).isEqualTo(2);
    assertThat(duplicationBlocks.get(1).getEndLine()).isEqualTo(6);
    assertThat(duplicationBlocks.get(1).getStartTokenIndex()).isEqualTo(3);
    assertThat(duplicationBlocks.get(1).getEndTokenIndex()).isEqualTo(7);
    assertThat(duplicationBlocks.get(0).getHash()).isNotEmpty();

    assertThat(duplicationBlocks.get(2).getStartLine()).isEqualTo(3);
    assertThat(duplicationBlocks.get(2).getEndLine()).isEqualTo(7);
    assertThat(duplicationBlocks.get(2).getStartTokenIndex()).isEqualTo(4);
    assertThat(duplicationBlocks.get(2).getEndTokenIndex()).isEqualTo(8);
    assertThat(duplicationBlocks.get(0).getHash()).isNotEmpty();
  }

  @Test
  public void testIntraFileDuplications() throws IOException {
    File srcDir = new File(baseDir, "src");
    srcDir.mkdir();

    String content = "Sample xoo\ncontent\nfoo\nbar\nSample xoo\ncontent\n";

    File xooFile = new File(srcDir, "sample.xoo");
    FileUtils.write(xooFile, content);

    AnalysisResult result = tester.newAnalysis()
      .properties(builder
        .put("sonar.sources", "src")
        .put("sonar.cpd.xoo.minimumTokens", "2")
        .put("sonar.cpd.xoo.minimumLines", "2")
        .put("sonar.verbose", "true")
        .build())
      .execute();

    InputFile inputFile = result.inputFile("src/sample.xoo");
    // One clone group
    List<ScannerReport.Duplication> duplicationGroups = result.duplicationsFor(inputFile);
    assertThat(duplicationGroups).hasSize(1);

    ScannerReport.Duplication cloneGroup = duplicationGroups.get(0);
    assertThat(cloneGroup.getOriginPosition().getStartLine()).isEqualTo(1);
    assertThat(cloneGroup.getOriginPosition().getEndLine()).isEqualTo(2);
    assertThat(cloneGroup.getDuplicateList()).hasSize(1);
    assertThat(cloneGroup.getDuplicate(0).getRange().getStartLine()).isEqualTo(5);
    assertThat(cloneGroup.getDuplicate(0).getRange().getEndLine()).isEqualTo(6);
  }

}
