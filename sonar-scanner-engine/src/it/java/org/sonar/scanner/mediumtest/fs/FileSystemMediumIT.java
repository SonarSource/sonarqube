/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.scanner.mediumtest.fs;

import com.google.common.collect.ImmutableMap;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.event.Level;
import org.sonar.api.CoreProperties;
import org.sonar.api.SonarEdition;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.testfixtures.log.LogTester;
import org.sonar.api.utils.MessageException;
import org.sonar.api.utils.PathUtils;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.scanner.mediumtest.AnalysisResult;
import org.sonar.scanner.mediumtest.ScannerMediumTester;
import org.sonar.scanner.mediumtest.ScannerMediumTester.AnalysisBuilder;
import org.sonar.xoo.XooPlugin;
import org.sonar.xoo.global.DeprecatedGlobalSensor;
import org.sonar.xoo.global.GlobalProjectSensor;
import org.sonar.xoo.rule.XooRulesDefinition;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.Assume.assumeFalse;
import static org.junit.Assume.assumeTrue;

public class FileSystemMediumIT {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Rule
  public LogTester logTester = new LogTester();

  @Rule
  public ScannerMediumTester tester = new ScannerMediumTester()
    .setEdition(SonarEdition.COMMUNITY)
    .registerPlugin("xoo", new XooPlugin())
    .addDefaultQProfile("xoo", "Sonar Way")
    .addDefaultQProfile("xoo2", "Sonar Way");

  private File baseDir;
  private ImmutableMap.Builder<String, String> builder;

  @Before
  public void prepare() throws IOException {
    logTester.setLevel(Level.DEBUG);
    baseDir = temp.newFolder().getCanonicalFile();

    builder = ImmutableMap.<String, String>builder()
      .put("sonar.projectBaseDir", baseDir.getAbsolutePath())
      .put("sonar.projectKey", "com.foo.project");
  }

  @Test
  public void scan_project_without_name() throws IOException {
    File srcDir = new File(baseDir, "src");
    srcDir.mkdir();

    writeFile(srcDir, "sample.xoo", "Sample xoo\ncontent");

    AnalysisResult result = tester.newAnalysis()
      .properties(builder
        .put("sonar.sources", "src")
        .build())
      .execute();

    int ref = result.getReportReader().readMetadata().getRootComponentRef();
    assertThat(result.getReportReader().readComponent(ref).getName()).isEmpty();
    assertThat(result.inputFiles()).hasSize(1);

    DefaultInputFile file = (DefaultInputFile) result.inputFile("src/sample.xoo");
    assertThat(file.type()).isEqualTo(InputFile.Type.MAIN);
    assertThat(file.relativePath()).isEqualTo("src/sample.xoo");
    assertThat(file.language()).isEqualTo("xoo");

    // file was published, since language matched xoo
    assertThat(file.isPublished()).isTrue();
    assertThat(result.getReportComponent(file.scannerId())).isNotNull();
  }

  @Test
  public void log_branch_name_and_type() {
    builder.put("sonar.branch.name", "my-branch");
    File srcDir = new File(baseDir, "src");
    assertThat(srcDir.mkdir()).isTrue();

    // Using sonar.branch.name when the branch plugin is not installed is an error.
    assertThatThrownBy(() -> tester.newAnalysis()
      .properties(builder
        .put("sonar.sources", "src")
        .build())
      .execute())
        .isInstanceOf(MessageException.class);
  }

  @Test
  public void only_generate_metadata_if_needed() throws IOException {
    File srcDir = new File(baseDir, "src");
    srcDir.mkdir();

    writeFile(srcDir, "sample.xoo", "Sample xoo\ncontent");
    writeFile(srcDir, "sample.java", "Sample xoo\ncontent");

    logTester.setLevel(LoggerLevel.DEBUG);

    tester.newAnalysis()
      .properties(builder
        .put("sonar.sources", "src")
        .build())
      .execute();

    assertThat(logTester.logs()).contains("2 files indexed");
    assertThat(logTester.logs()).contains("'src/sample.xoo' generated metadata with charset 'UTF-8'");
    assertThat(String.join("\n", logTester.logs())).doesNotContain("'src/sample.java' generated metadata");
  }

  @Test
  public void preload_file_metadata() throws IOException {
    builder.put("sonar.preloadFileMetadata", "true");

    File srcDir = new File(baseDir, "src");
    srcDir.mkdir();

    writeFile(srcDir, "sample.xoo", "Sample xoo\ncontent");
    writeFile(srcDir, "sample.java", "Sample xoo\ncontent");

    logTester.setLevel(LoggerLevel.DEBUG);

    tester.newAnalysis()
      .properties(builder
        .put("sonar.sources", "src")
        .build())
      .execute();

    assertThat(logTester.logs()).contains("2 files indexed");
    assertThat(logTester.logs()).contains("'src/sample.xoo' generated metadata with charset 'UTF-8'");
    assertThat(logTester.logs()).contains("'src/sample.java' generated metadata with charset 'UTF-8'");
  }

  @Test
  public void dont_publish_files_without_detected_language() throws IOException {
    Path mainDir = baseDir.toPath().resolve("src").resolve("main");
    Files.createDirectories(mainDir);

    Path testDir = baseDir.toPath().resolve("src").resolve("test");
    Files.createDirectories(testDir);

    writeFile(testDir.toFile(), "sample.java", "Sample xoo\ncontent");
    writeFile(mainDir.toFile(), "sample.xoo", "Sample xoo\ncontent");
    writeFile(mainDir.toFile(), "sample.java", "Sample xoo\ncontent");

    logTester.setLevel(LoggerLevel.DEBUG);

    AnalysisResult result = tester.newAnalysis()
      .properties(builder
        .put("sonar.sources", "src/main")
        .put("sonar.tests", "src/test")
        .build())
      .execute();

    assertThat(logTester.logs()).containsAnyOf("'src/main/sample.java' indexed with no language", "'src\\main\\sample.java' indexed with no language");
    assertThat(logTester.logs()).contains("3 files indexed");
    assertThat(logTester.logs()).contains("'src/main/sample.xoo' generated metadata with charset 'UTF-8'");
    assertThat(logTester.logs()).doesNotContain("'src/main/sample.java' generated metadata", "'src\\main\\sample.java' generated metadata");
    assertThat(logTester.logs()).doesNotContain("'src/test/sample.java' generated metadata", "'src\\test\\sample.java' generated metadata");
    DefaultInputFile javaInputFile = (DefaultInputFile) result.inputFile("src/main/sample.java");

    assertThatThrownBy(() -> result.getReportComponent(javaInputFile))
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining("Unable to find report for component");
  }

  @Test
  public void create_issue_on_any_file() throws IOException {
    tester
      .addRules(new XooRulesDefinition())
      .addActiveRule("xoo", "OneIssuePerUnknownFile", null, "OneIssuePerUnknownFile", "MAJOR", null, "xoo");

    File srcDir = new File(baseDir, "src");
    srcDir.mkdir();

    writeFile(srcDir, "sample.unknown", "Sample xoo\ncontent");

    logTester.setLevel(LoggerLevel.DEBUG);

    AnalysisResult result = tester.newAnalysis()
      .properties(builder
        .put("sonar.sources", "src")
        .build())
      .execute();

    assertThat(logTester.logs()).contains("1 file indexed");
    assertThat(logTester.logs()).contains("'src" + File.separator + "sample.unknown' indexed with no language");
    assertThat(logTester.logs()).contains("'src/sample.unknown' generated metadata with charset 'UTF-8'");
    DefaultInputFile inputFile = (DefaultInputFile) result.inputFile("src/sample.unknown");
    assertThat(result.getReportComponent(inputFile)).isNotNull();
  }

  @Test
  public void lazyIssueExclusion() throws IOException {
    tester
      .addRules(new XooRulesDefinition())
      .addActiveRule("xoo", "OneIssuePerFile", null, "OneIssuePerFile", "MAJOR", null, "xoo");

    builder.put("sonar.issue.ignore.allfile", "1")
      .put("sonar.issue.ignore.allfile.1.fileRegexp", "pattern");
    File srcDir = new File(baseDir, "src");
    srcDir.mkdir();

    writeFile(srcDir, "sample.xoo", "Sample xoo\ncontent");

    File unknownFile = new File(srcDir, "myfile.binary");
    byte[] b = new byte[512];
    new Random().nextBytes(b);
    FileUtils.writeByteArrayToFile(unknownFile, b);

    logTester.setLevel(LoggerLevel.DEBUG);

    tester.newAnalysis()
      .properties(builder
        .put("sonar.sources", "src")
        .build())
      .execute();

    assertThat(logTester.logs()).containsOnlyOnce("'src" + File.separator + "myfile.binary' indexed with no language");
    assertThat(logTester.logs()).doesNotContain("Evaluate issue exclusions for 'src/myfile.binary'");
    assertThat(logTester.logs()).containsOnlyOnce("Evaluate issue exclusions for 'src/sample.xoo'");
  }

  @Test
  public void preloadIssueExclusions() throws IOException {
    builder.put("sonar.issue.ignore.allfile", "1")
      .put("sonar.issue.ignore.allfile.1.fileRegexp", "pattern")
      .put("sonar.preloadFileMetadata", "true");
    File srcDir = new File(baseDir, "src");
    srcDir.mkdir();

    writeFile(srcDir, "sample.xoo", "Sample xoo\npattern");
    writeFile(srcDir, "myfile.binary", "some text");

    logTester.setLevel(LoggerLevel.DEBUG);

    tester.newAnalysis()
      .properties(builder
        .put("sonar.sources", "src")
        .build())
      .execute();

    assertThat(logTester.logs()).containsSequence("Evaluate issue exclusions for 'src/sample.xoo'",
      "  - Exclusion pattern 'pattern': all issues in this file will be ignored.");
  }

  @Test
  public void publishFilesWithIssues() throws IOException {
    tester
      .addRules(new XooRulesDefinition())
      .addActiveRule("xoo", "OneIssueOnDirPerFile", null, "OneIssueOnDirPerFile", "MAJOR", null, "xoo");

    File srcDir = new File(baseDir, "src");
    srcDir.mkdir();

    writeFile(srcDir, "sample.xoo", "Sample xoo\ncontent");

    AnalysisResult result = tester.newAnalysis()
      .properties(builder
        .put("sonar.sources", "src")
        .build())
      .execute();

    DefaultInputFile file = (DefaultInputFile) result.inputFile("src/sample.xoo");

    assertThat(file.isPublished()).isTrue();
    assertThat(result.getReportComponent(file)).isNotNull();
  }

  @Test
  public void scanProjectWithSourceDir() throws IOException {
    File srcDir = new File(baseDir, "src");
    srcDir.mkdir();

    writeFile(srcDir, "sample.xoo", "Sample xoo\ncontent");

    AnalysisResult result = tester.newAnalysis()
      .properties(builder
        .put("sonar.sources", "src")
        .build())
      .execute();

    assertThat(result.inputFiles()).hasSize(1);
    assertThat(result.inputFile("src/sample.xoo").type()).isEqualTo(InputFile.Type.MAIN);
    assertThat(result.inputFile("src/sample.xoo").relativePath()).isEqualTo("src/sample.xoo");
  }

  @Test
  public void scanBigProject() throws IOException {
    File srcDir = new File(baseDir, "src");
    srcDir.mkdir();

    int nbFiles = 100;
    int ruleCount = 100000;
    for (int nb = 1; nb <= nbFiles; nb++) {
      File xooFile = new File(srcDir, "sample" + nb + ".xoo");
      FileUtils.write(xooFile, StringUtils.repeat(StringUtils.repeat("a", 100) + "\n", ruleCount / 1000));
    }

    AnalysisResult result = tester.newAnalysis()
      .properties(builder
        .put("sonar.sources", "src")
        .build())
      .execute();

    assertThat(result.inputFiles()).hasSize(100);
  }

  @Test
  public void scanProjectWithTestDir() throws IOException {
    File test = new File(baseDir, "test");
    test.mkdir();

    writeFile(test, "sampleTest.xoo", "Sample test xoo\ncontent");

    AnalysisResult result = tester.newAnalysis()
      .properties(builder
        .put("sonar.sources", "")
        .put("sonar.tests", "test")
        .build())
      .execute();

    assertThat(result.inputFiles()).hasSize(1);
    assertThat(result.inputFile("test/sampleTest.xoo").type()).isEqualTo(InputFile.Type.TEST);
  }

  /**
   * SONAR-5419
   */
  @Test
  public void scanProjectWithMixedSourcesAndTests() throws IOException {
    File srcDir = new File(baseDir, "src");
    srcDir.mkdir();

    writeFile(srcDir, "sample.xoo", "Sample xoo\ncontent");
    writeFile(baseDir, "another.xoo", "Sample xoo 2\ncontent");

    File testDir = new File(baseDir, "test");
    testDir.mkdir();

    writeFile(baseDir, "sampleTest2.xoo", "Sample test xoo\ncontent");
    writeFile(testDir, "sampleTest.xoo", "Sample test xoo 2\ncontent");

    AnalysisResult result = tester.newAnalysis()
      .properties(builder
        .put("sonar.sources", "src,another.xoo")
        .put("sonar.tests", "test,sampleTest2.xoo")
        .build())
      .execute();

    assertThat(result.inputFiles()).hasSize(4);
  }

  @Test
  public void fileInclusionsExclusions() throws IOException {
    File srcDir = new File(baseDir, "src");
    srcDir.mkdir();

    writeFile(srcDir, "sample.xoo", "Sample xoo\ncontent");
    writeFile(baseDir, "another.xoo", "Sample xoo 2\ncontent");

    File testDir = new File(baseDir, "test");
    testDir.mkdir();

    writeFile(baseDir, "sampleTest2.xoo", "Sample test xoo\ncontent");
    writeFile(testDir, "sampleTest.xoo", "Sample test xoo 2\ncontent");

    AnalysisResult result = tester.newAnalysis()
      .properties(builder
        .put("sonar.sources", "src,another.xoo")
        .put("sonar.tests", "test,sampleTest2.xoo")
        .put("sonar.inclusions", "src/**")
        .put("sonar.exclusions", "**/another.*")
        .put("sonar.test.inclusions", "**/sampleTest*.*")
        .put("sonar.test.exclusions", "**/sampleTest2.xoo")
        .build())
      .execute();

    assertThat(result.inputFiles()).hasSize(2);
  }

  @Test
  public void ignoreFilesWhenGreaterThanDefinedSize() throws IOException {
    File srcDir = new File(baseDir, "src");
    srcDir.mkdir();

    File fileGreaterThanLimit = writeFile(srcDir, "sample.xoo", 1024 * 1024 + 1);
    writeFile(srcDir, "another.xoo", "Sample xoo 2\ncontent");

    AnalysisResult result = tester.newAnalysis()
      .properties(builder
        // set limit to 1MB
        .put("sonar.filesize.limit", "1")
        .build())
      .execute();

    assertThat(result.inputFiles()).hasSize(1);
    assertThat(logTester.logs())
      .contains(format("File '%s' is bigger than 1MB and as consequence is removed from the analysis scope.", fileGreaterThanLimit.getAbsolutePath()));
  }

  @Test
  public void analysisFailsSymbolicLinkWithoutTargetIsInTheFolder() throws IOException {
    assumeFalse(SystemUtils.IS_OS_WINDOWS);
    File srcDir = new File(baseDir, "src");
    srcDir.mkdir();

    File target = writeFile(srcDir, "target.xoo", 1024 * 1024 + 1);
    Path link = Paths.get(srcDir.getPath(), "target_link.xoo");
    Files.createSymbolicLink(link, target.toPath());
    Files.delete(target.toPath());

    AnalysisBuilder analysis = tester.newAnalysis()
      .properties(builder.build());

    assertThatThrownBy(analysis::execute)
      .isExactlyInstanceOf(IllegalStateException.class)
      .hasMessageEndingWith(format("Unable to read file %s", link));
  }

  @Test
  public void test_inclusions_on_multi_modules() throws IOException {
    File baseDir = temp.getRoot();
    File baseDirModuleA = new File(baseDir, "moduleA");
    File baseDirModuleB = new File(baseDir, "moduleB");
    File srcDirA = new File(baseDirModuleA, "tests");
    srcDirA.mkdirs();
    File srcDirB = new File(baseDirModuleB, "tests");
    srcDirB.mkdirs();

    writeFile(srcDirA, "sampleTestA.xoo", "Sample xoo\ncontent");
    writeFile(srcDirB, "sampleTestB.xoo", "Sample xoo\ncontent");

    final ImmutableMap.Builder<String, String> builder = ImmutableMap.<String, String>builder()
      .put("sonar.projectBaseDir", baseDir.getAbsolutePath())
      .put("sonar.projectKey", "com.foo.project")
      .put("sonar.sources", "")
      .put("sonar.tests", "tests")
      .put("sonar.modules", "moduleA,moduleB");

    AnalysisResult result = tester.newAnalysis()
      .properties(builder.build())
      .execute();

    assertThat(result.inputFiles()).hasSize(2);

    InputFile fileA = result.inputFile("moduleA/tests/sampleTestA.xoo");
    assertThat(fileA).isNotNull();

    InputFile fileB = result.inputFile("moduleB/tests/sampleTestB.xoo");
    assertThat(fileB).isNotNull();

    result = tester.newAnalysis()
      .properties(builder
        .put("sonar.test.inclusions", "moduleA/tests/**")
        .build())
      .execute();

    assertThat(result.inputFiles()).hasSize(1);

    fileA = result.inputFile("moduleA/tests/sampleTestA.xoo");
    assertThat(fileA).isNotNull();

    fileB = result.inputFile("moduleB/tests/sampleTestB.xoo");
    assertThat(fileB).isNull();
  }

  @Test
  public void test_module_level_inclusions_override_parent_on_multi_modules() throws IOException {
    File baseDir = temp.getRoot();
    File baseDirModuleA = new File(baseDir, "moduleA");
    File baseDirModuleB = new File(baseDir, "moduleB");
    File srcDirA = new File(baseDirModuleA, "src");
    srcDirA.mkdirs();
    File srcDirB = new File(baseDirModuleB, "src");
    srcDirB.mkdirs();

    writeFile(srcDirA, "sampleA.xoo", "Sample xoo\ncontent");
    writeFile(srcDirB, "sampleB.xoo", "Sample xoo\ncontent");

    final ImmutableMap.Builder<String, String> builder = ImmutableMap.<String, String>builder()
      .put("sonar.projectBaseDir", baseDir.getAbsolutePath())
      .put("sonar.projectKey", "com.foo.project")
      .put("sonar.sources", "src")
      .put("sonar.modules", "moduleA,moduleB")
      .put("sonar.inclusions", "**/*.php");

    AnalysisResult result = tester.newAnalysis()
      .properties(builder.build())
      .execute();

    assertThat(result.inputFiles()).isEmpty();

    result = tester.newAnalysis()
      .properties(builder
        .put("moduleA.sonar.inclusions", "**/*.xoo")
        .build())
      .execute();

    assertThat(result.inputFiles()).hasSize(1);

    InputFile fileA = result.inputFile("moduleA/src/sampleA.xoo");
    assertThat(fileA).isNotNull();
  }

  @Test
  public void warn_user_for_outdated_scanner_side_inherited_exclusions_for_multi_module_project() throws IOException {
    File baseDir = temp.getRoot();
    File baseDirModuleA = new File(baseDir, "moduleA");
    File baseDirModuleB = new File(baseDir, "moduleB");
    File srcDirA = new File(baseDirModuleA, "src");
    srcDirA.mkdirs();
    File srcDirB = new File(baseDirModuleB, "src");
    srcDirB.mkdirs();

    writeFile(srcDirA, "sample.xoo", "Sample xoo\ncontent");
    writeFile(srcDirB, "sample.xoo", "Sample xoo\ncontent");

    AnalysisResult result = tester.newAnalysis()
      .properties(ImmutableMap.<String, String>builder()
        .put("sonar.projectBaseDir", baseDir.getAbsolutePath())
        .put("sonar.projectKey", "com.foo.project")
        .put("sonar.sources", "src")
        .put("sonar.modules", "moduleA,moduleB")
        .put("sonar.exclusions", "src/sample.xoo")
        .build())
      .execute();

    InputFile fileA = result.inputFile("moduleA/src/sample.xoo");
    assertThat(fileA).isNull();

    InputFile fileB = result.inputFile("moduleB/src/sample.xoo");
    assertThat(fileB).isNull();

    assertThat(logTester.logs(Level.WARN))
      .contains("Specifying module-relative paths at project level in the property 'sonar.exclusions' is deprecated. " +
        "To continue matching files like 'moduleA/src/sample.xoo', update this property so that patterns refer to project-relative paths.");
  }

  @Test
  public void support_global_server_side_exclusions_for_multi_module_project() throws IOException {
    File baseDir = temp.getRoot();
    File baseDirModuleA = new File(baseDir, "moduleA");
    File baseDirModuleB = new File(baseDir, "moduleB");
    File srcDirA = new File(baseDirModuleA, "src");
    srcDirA.mkdirs();
    File srcDirB = new File(baseDirModuleB, "src");
    srcDirB.mkdirs();

    writeFile(srcDirA, "sample.xoo", "Sample xoo\ncontent");
    writeFile(srcDirB, "sample.xoo", "Sample xoo\ncontent");

    tester.addGlobalServerSettings(CoreProperties.PROJECT_EXCLUSIONS_PROPERTY, "**/*.xoo");

    AnalysisResult result = tester.newAnalysis()
      .properties(ImmutableMap.<String, String>builder()
        .put("sonar.projectBaseDir", baseDir.getAbsolutePath())
        .put("sonar.projectKey", "com.foo.project")
        .put("sonar.sources", "src")
        .put("sonar.modules", "moduleA,moduleB")
        .build())
      .execute();

    InputFile fileA = result.inputFile("moduleA/src/sample.xoo");
    assertThat(fileA).isNull();

    InputFile fileB = result.inputFile("moduleB/src/sample.xoo");
    assertThat(fileB).isNull();
  }

  @Test
  public void support_global_server_side_global_exclusions_for_multi_module_project() throws IOException {
    File baseDir = temp.getRoot();
    File baseDirModuleA = new File(baseDir, "moduleA");
    File baseDirModuleB = new File(baseDir, "moduleB");
    File srcDirA = new File(baseDirModuleA, "src");
    srcDirA.mkdirs();
    File srcDirB = new File(baseDirModuleB, "src");
    srcDirB.mkdirs();

    writeFile(srcDirA, "sample.xoo", "Sample xoo\ncontent");
    writeFile(srcDirB, "sample.xoo", "Sample xoo\ncontent");

    tester.addGlobalServerSettings(CoreProperties.GLOBAL_EXCLUSIONS_PROPERTY, "**/*.xoo");

    AnalysisResult result = tester.newAnalysis()
      .properties(ImmutableMap.<String, String>builder()
        .put("sonar.projectBaseDir", baseDir.getAbsolutePath())
        .put("sonar.projectKey", "com.foo.project")
        .put("sonar.sources", "src")
        .put("sonar.modules", "moduleA,moduleB")
        .build())
      .execute();

    InputFile fileA = result.inputFile("moduleA/src/sample.xoo");
    assertThat(fileA).isNull();

    InputFile fileB = result.inputFile("moduleB/src/sample.xoo");
    assertThat(fileB).isNull();
  }

  @Test
  public void warn_user_for_outdated_server_side_inherited_exclusions_for_multi_module_project() throws IOException {
    File baseDir = temp.getRoot();
    File baseDirModuleA = new File(baseDir, "moduleA");
    File baseDirModuleB = new File(baseDir, "moduleB");
    File srcDirA = new File(baseDirModuleA, "src");
    srcDirA.mkdirs();
    File srcDirB = new File(baseDirModuleB, "src");
    srcDirB.mkdirs();

    writeFile(srcDirA, "sample.xoo", "Sample xoo\ncontent");
    writeFile(srcDirB, "sample.xoo", "Sample xoo\ncontent");

    tester.addProjectServerSettings("sonar.exclusions", "src/sample.xoo");

    AnalysisResult result = tester.newAnalysis()
      .properties(ImmutableMap.<String, String>builder()
        .put("sonar.projectBaseDir", baseDir.getAbsolutePath())
        .put("sonar.projectKey", "com.foo.project")
        .put("sonar.sources", "src")
        .put("sonar.modules", "moduleA,moduleB")
        .build())
      .execute();

    InputFile fileA = result.inputFile("moduleA/src/sample.xoo");
    assertThat(fileA).isNull();

    InputFile fileB = result.inputFile("moduleB/src/sample.xoo");
    assertThat(fileB).isNull();

    assertThat(logTester.logs(Level.WARN))
      .contains("Specifying module-relative paths at project level in the property 'sonar.exclusions' is deprecated. " +
        "To continue matching files like 'moduleA/src/sample.xoo', update this property so that patterns refer to project-relative paths.");
  }

  @Test
  public void failForDuplicateInputFile() throws IOException {
    File srcDir = new File(baseDir, "src");
    srcDir.mkdir();

    writeFile(srcDir, "sample.xoo", "Sample xoo\ncontent");

    assertThatThrownBy(() -> tester.newAnalysis()
      .properties(builder
        .put("sonar.sources", "src,src/sample.xoo")
        .build())
      .execute())
        .isInstanceOf(MessageException.class)
        .hasMessage("File src/sample.xoo can't be indexed twice. Please check that inclusion/exclusion patterns produce disjoint sets for main and test files");
  }

  // SONAR-9574
  @Test
  public void failForDuplicateInputFileInDifferentModules() throws IOException {
    File srcDir = new File(baseDir, "module1/src");
    srcDir.mkdir();

    writeFile(srcDir, "sample.xoo", "Sample xoo\ncontent");

    assertThatThrownBy(() -> tester.newAnalysis()
      .properties(builder
        .put("sonar.sources", "module1/src")
        .put("sonar.modules", "module1")
        .put("module1.sonar.sources", "src")
        .build())
      .execute())
        .isInstanceOf(MessageException.class)
        .hasMessage("File module1/src/sample.xoo can't be indexed twice. Please check that inclusion/exclusion patterns produce disjoint sets for main and test files");
  }

  // SONAR-5330
  @Test
  public void scanProjectWithSourceSymlink() {
    assumeTrue(!System2.INSTANCE.isOsWindows());
    File projectDir = new File("test-resources/mediumtest/xoo/sample-with-symlink");
    AnalysisResult result = tester
      .newAnalysis(new File(projectDir, "sonar-project.properties"))
      .property("sonar.exclusions", "**/*.xoo.measures,**/*.xoo.scm")
      .property("sonar.test.exclusions", "**/*.xoo.measures,**/*.xoo.scm")
      .property("sonar.scm.exclusions.disabled", "true")
      .execute();

    assertThat(result.inputFiles()).hasSize(3);
    // check that symlink was not resolved to target
    assertThat(result.inputFiles()).extractingResultOf("path").toString().startsWith(projectDir.toString());
  }

  // SONAR-6719
  @Test
  public void scanProjectWithWrongCase() {
    // To please the quality gate, don't use assumeTrue, or the test will be reported as skipped
    File projectDir = new File("test-resources/mediumtest/xoo/sample");
    AnalysisBuilder analysis = tester
      .newAnalysis(new File(projectDir, "sonar-project.properties"))
      .property("sonar.sources", "XOURCES")
      .property("sonar.tests", "TESTX")
      .property("sonar.scm.exclusions.disabled", "true");

    if (System2.INSTANCE.isOsWindows()) { // Windows is file path case-insensitive
      AnalysisResult result = analysis.execute();

      assertThat(result.inputFiles()).hasSize(8);
      assertThat(result.inputFiles()).extractingResultOf("relativePath").containsOnly(
        "testx/ClassOneTest.xoo.measures",
        "xources/hello/helloscala.xoo.measures",
        "xources/hello/HelloJava.xoo.measures",
        "testx/ClassOneTest.xoo",
        "xources/hello/HelloJava.xoo.scm",
        "xources/hello/helloscala.xoo",
        "testx/ClassOneTest.xoo.scm",
        "xources/hello/HelloJava.xoo");
    } else if (System2.INSTANCE.isOsMac()) {
      AnalysisResult result = analysis.execute();

      assertThat(result.inputFiles()).hasSize(8);
      assertThat(result.inputFiles()).extractingResultOf("relativePath").containsOnly(
        "TESTX/ClassOneTest.xoo.measures",
        "XOURCES/hello/helloscala.xoo.measures",
        "XOURCES/hello/HelloJava.xoo.measures",
        "TESTX/ClassOneTest.xoo",
        "XOURCES/hello/HelloJava.xoo.scm",
        "XOURCES/hello/helloscala.xoo",
        "TESTX/ClassOneTest.xoo.scm",
        "XOURCES/hello/HelloJava.xoo");
    } else {
      assertThatThrownBy(() -> analysis.execute())
        .isInstanceOf(MessageException.class)
        .hasMessageContaining("The folder 'TESTX' does not exist for 'sample'");
    }
  }

  @Test
  public void indexAnyFile() throws IOException {
    File srcDir = new File(baseDir, "src");
    srcDir.mkdir();

    writeFile(srcDir, "sample.xoo", "Sample xoo\ncontent");
    writeFile(srcDir, "sample.other", "Sample other\ncontent");

    AnalysisResult result = tester.newAnalysis()
      .properties(builder
        .put("sonar.sources", "src")
        .build())
      .execute();

    assertThat(result.inputFiles()).hasSize(2);
    assertThat(result.inputFile("src/sample.other").type()).isEqualTo(InputFile.Type.MAIN);
    assertThat(result.inputFile("src/sample.other").relativePath()).isEqualTo("src/sample.other");
    assertThat(result.inputFile("src/sample.other").language()).isNull();
  }

  @Test
  public void scanMultiModuleProject() {
    File projectDir = new File("test-resources/mediumtest/xoo/multi-modules-sample");
    AnalysisResult result = tester
      .newAnalysis(new File(projectDir, "sonar-project.properties"))
      .execute();

    assertThat(result.inputFiles()).hasSize(4);
  }

  @Test
  public void deprecated_global_sensor_should_see_project_relative_paths() {
    File projectDir = new File("test-resources/mediumtest/xoo/multi-modules-sample");
    AnalysisResult result = tester
      .newAnalysis(new File(projectDir, "sonar-project.properties"))
      .property(DeprecatedGlobalSensor.ENABLE_PROP, "true")
      .execute();

    assertThat(result.inputFiles()).hasSize(4);
    assertThat(logTester.logs(Level.INFO)).contains(
      "Deprecated Global Sensor: module_a/module_a1/src/main/xoo/com/sonar/it/samples/modules/a1/HelloA1.xoo",
      "Deprecated Global Sensor: module_a/module_a2/src/main/xoo/com/sonar/it/samples/modules/a2/HelloA2.xoo",
      "Deprecated Global Sensor: module_b/module_b1/src/main/xoo/com/sonar/it/samples/modules/b1/HelloB1.xoo",
      "Deprecated Global Sensor: module_b/module_b2/src/main/xoo/com/sonar/it/samples/modules/b2/HelloB2.xoo");
  }

  @Test
  public void global_sensor_should_see_project_relative_paths() {
    File projectDir = new File("test-resources/mediumtest/xoo/multi-modules-sample");
    AnalysisResult result = tester
      .newAnalysis(new File(projectDir, "sonar-project.properties"))
      .property(GlobalProjectSensor.ENABLE_PROP, "true")
      .execute();

    assertThat(result.inputFiles()).hasSize(4);
    assertThat(logTester.logs(Level.INFO)).contains(
      "Global Sensor: module_a/module_a1/src/main/xoo/com/sonar/it/samples/modules/a1/HelloA1.xoo",
      "Global Sensor: module_a/module_a2/src/main/xoo/com/sonar/it/samples/modules/a2/HelloA2.xoo",
      "Global Sensor: module_b/module_b1/src/main/xoo/com/sonar/it/samples/modules/b1/HelloB1.xoo",
      "Global Sensor: module_b/module_b2/src/main/xoo/com/sonar/it/samples/modules/b2/HelloB2.xoo");
  }

  @Test
  public void scan_project_with_comma_in_source_path() throws IOException {
    File srcDir = new File(baseDir, "src");
    srcDir.mkdir();

    writeFile(srcDir, "sample,1.xoo", "Sample xoo\ncontent");
    writeFile(baseDir, "another,2.xoo", "Sample xoo 2\ncontent");

    File testDir = new File(baseDir, "test");
    testDir.mkdir();

    writeFile(testDir, "sampleTest,1.xoo", "Sample test xoo\ncontent");
    writeFile(baseDir, "sampleTest,2.xoo", "Sample test xoo 2\ncontent");

    AnalysisResult result = tester.newAnalysis()
      .properties(builder
        .put("sonar.sources", "src,\"another,2.xoo\"")
        .put("sonar.tests", "\"test\",\"sampleTest,2.xoo\"")
        .build())
      .execute();

    assertThat(result.inputFiles()).hasSize(4);
  }

  @Test
  public void language_without_publishAllFiles_should_not_auto_publish_files() throws IOException {
    File srcDir = new File(baseDir, "src");
    srcDir.mkdir();

    writeFile(srcDir, "sample.xoo3", "Sample xoo\ncontent");
    writeFile(srcDir, "sample2.xoo3", "Sample xoo 2\ncontent");

    AnalysisResult result = tester.newAnalysis()
      .properties(builder
        .put("sonar.sources", "src")
        .build())
      .execute();

    assertThat(result.inputFiles())
      .extracting(InputFile::filename, InputFile::language, f -> ((DefaultInputFile) f).isPublished())
      .containsOnly(tuple("sample.xoo3", "xoo3", false), tuple("sample2.xoo3", "xoo3", false));
    assertThat(result.getReportReader().readComponent(result.getReportReader().readMetadata().getRootComponentRef()).getChildRefCount()).isZero();
  }

  @Test
  public void two_languages_with_same_extension() throws IOException {
    File srcDir = new File(baseDir, "src");
    srcDir.mkdir();

    writeFile(srcDir, "sample.xoo", "Sample xoo\ncontent");
    writeFile(srcDir, "sample.xoo2", "Sample xoo 2\ncontent");

    AnalysisResult result = tester.newAnalysis()
      .properties(builder
        .put("sonar.sources", "src")
        .build())
      .execute();

    assertThat(result.inputFiles()).hasSize(2);

    AnalysisBuilder analysisBuilder = tester.newAnalysis()
      .properties(builder
        .put("sonar.lang.patterns.xoo2", "**/*.xoo")
        .build());

    assertThatThrownBy(analysisBuilder::execute)
      .isInstanceOf(MessageException.class)
      .hasMessage(
        "Language of file 'src" + File.separator
          + "sample.xoo' can not be decided as the file matches patterns of both sonar.lang.patterns.xoo : **/*.xoo and sonar.lang.patterns.xoo2 : **/*.xoo");

    // SONAR-9561
    result = tester.newAnalysis()
      .properties(builder
        .put("sonar.exclusions", "**/sample.xoo")
        .build())
      .execute();

    assertThat(result.inputFiles()).hasSize(1);
  }

  @Test
  public void log_all_exclusions_properties_per_modules() throws IOException {
    File baseDir = temp.getRoot();
    File baseDirModuleA = new File(baseDir, "moduleA");
    File baseDirModuleB = new File(baseDir, "moduleB");
    File srcDirA = new File(baseDirModuleA, "src");
    srcDirA.mkdirs();
    File srcDirB = new File(baseDirModuleB, "src");
    srcDirB.mkdirs();

    writeFile(srcDirA, "sample.xoo", "Sample xoo\ncontent");
    writeFile(srcDirB, "sample.xoo", "Sample xoo\ncontent");

    AnalysisResult result = tester.newAnalysis()
      .properties(ImmutableMap.<String, String>builder()
        .put("sonar.projectBaseDir", baseDir.getAbsolutePath())
        .put("sonar.projectKey", "com.foo.project")
        .put("sonar.sources", "src")
        .put("sonar.modules", "moduleA,moduleB")
        .put("sonar.inclusions", "**/global.inclusions")
        .put("sonar.test.inclusions", "**/global.test.inclusions")
        .put("sonar.exclusions", "**/global.exclusions")
        .put("sonar.test.exclusions", "**/global.test.exclusions")
        .put("sonar.coverage.exclusions", "**/coverage.exclusions")
        .put("sonar.cpd.exclusions", "**/cpd.exclusions")
        .build())
      .execute();

    assertThat(logTester.logs(Level.INFO))
      .containsSequence("Project configuration:",
        "  Included sources: **/global.inclusions",
        "  Excluded sources: **/global.exclusions, **/global.test.inclusions",
        "  Included tests: **/global.test.inclusions",
        "  Excluded tests: **/global.test.exclusions",
        "  Excluded sources for coverage: **/coverage.exclusions",
        "  Excluded sources for duplication: **/cpd.exclusions",
        "Indexing files of module 'moduleA'",
        "  Base dir: " + baseDirModuleA.toPath().toRealPath(LinkOption.NOFOLLOW_LINKS),
        "  Included sources: **/global.inclusions",
        "  Excluded sources: **/global.exclusions, **/global.test.inclusions",
        "  Included tests: **/global.test.inclusions",
        "  Excluded tests: **/global.test.exclusions",
        "  Excluded sources for coverage: **/coverage.exclusions",
        "  Excluded sources for duplication: **/cpd.exclusions",
        "Indexing files of module 'moduleB'",
        "  Base dir: " + baseDirModuleB.toPath().toRealPath(LinkOption.NOFOLLOW_LINKS),
        "  Included sources: **/global.inclusions",
        "  Excluded sources: **/global.exclusions, **/global.test.inclusions",
        "  Included tests: **/global.test.inclusions",
        "  Excluded tests: **/global.test.exclusions",
        "  Excluded sources for coverage: **/coverage.exclusions",
        "  Excluded sources for duplication: **/cpd.exclusions",
        "Indexing files of module 'com.foo.project'",
        "  Base dir: " + baseDir.toPath().toRealPath(LinkOption.NOFOLLOW_LINKS),
        "  Included sources: **/global.inclusions",
        "  Excluded sources: **/global.exclusions, **/global.test.inclusions",
        "  Included tests: **/global.test.inclusions",
        "  Excluded tests: **/global.test.exclusions",
        "  Excluded sources for coverage: **/coverage.exclusions",
        "  Excluded sources for duplication: **/cpd.exclusions");
  }

  @Test
  public void ignore_files_outside_project_basedir() throws IOException {
    File srcDir = new File(baseDir, "src");
    srcDir.mkdir();

    writeFile(srcDir, "sample1.xoo", "Sample xoo\ncontent");

    File outsideBaseDir = temp.newFolder().getCanonicalFile();
    File xooFile2 = writeFile(outsideBaseDir, "another.xoo", "Sample xoo 2\ncontent");

    AnalysisResult result = tester.newAnalysis()
      .properties(builder
        .put("sonar.sources", "src," + PathUtils.canonicalPath(xooFile2))
        .build())
      .execute();

    assertThat(result.inputFiles()).hasSize(1);
    assertThat(logTester.logs(Level.WARN)).contains("File '" + xooFile2.getAbsolutePath() + "' is ignored. It is not located in project basedir '" + baseDir + "'.");
  }

  @Test
  public void dont_log_warn_about_files_out_of_basedir_if_they_arent_included() throws IOException {
    File srcDir = new File(baseDir, "src");
    srcDir.mkdir();

    writeFile(srcDir, "sample1.xoo", "Sample xoo\ncontent");

    File outsideBaseDir = temp.newFolder().getCanonicalFile();
    File xooFile2 = new File(outsideBaseDir, "another.xoo");
    FileUtils.write(xooFile2, "Sample xoo 2\ncontent", StandardCharsets.UTF_8);

    AnalysisResult result = tester.newAnalysis()
      .properties(builder
        .put("sonar.sources", "src," + PathUtils.canonicalPath(xooFile2))
        .put("sonar.inclusions", "**/sample1.xoo")
        .build())
      .execute();

    assertThat(result.inputFiles()).hasSize(1);
    assertThat(logTester.logs(Level.WARN)).doesNotContain("File '" + xooFile2.getAbsolutePath() + "' is ignored. It is not located in project basedir '" + baseDir + "'.");
  }

  @Test
  public void ignore_files_outside_module_basedir() throws IOException {
    File moduleA = new File(baseDir, "moduleA");
    moduleA.mkdir();

    writeFile(moduleA, "src/sampleA.xoo", "Sample xoo\ncontent");
    File xooFile2 = writeFile(baseDir, "another.xoo", "Sample xoo 2\ncontent");

    AnalysisResult result = tester.newAnalysis()
      .properties(builder
        .put("sonar.modules", "moduleA")
        .put("moduleA.sonar.sources", "src," + PathUtils.canonicalPath(xooFile2))
        .build())
      .execute();

    assertThat(result.inputFiles()).hasSize(1);
    assertThat(logTester.logs(Level.WARN))
      .contains("File '" + xooFile2.getAbsolutePath() + "' is ignored. It is not located in module basedir '" + new File(baseDir, "moduleA") + "'.");
  }

  @Test
  public void exclusion_based_on_scm_info() {
    File projectDir = new File("test-resources/mediumtest/xoo/sample-with-ignored-file");
    AnalysisResult result = tester
      .newAnalysis(new File(projectDir, "sonar-project.properties"))
      .property("sonar.exclusions", "**/*.xoo.ignore")
      .property("sonar.test.exclusions", "**/*.xoo.ignore")
      .execute();

    assertThat(result.inputFile("xources/hello/ClassTwo.xoo")).isNull();
    assertThat(result.inputFile("testx/ClassTwoTest.xoo")).isNull();

    assertThat(result.inputFile("xources/hello/ClassOne.xoo")).isNotNull();
    assertThat(result.inputFile("testx/ClassOneTest.xoo")).isNotNull();
  }

  @Test
  public void no_exclusion_when_scm_exclusions_is_disabled() {
    File projectDir = new File("test-resources/mediumtest/xoo/sample-with-ignored-file");
    AnalysisResult result = tester
      .newAnalysis(new File(projectDir, "sonar-project.properties"))
      .property("sonar.scm.exclusions.disabled", "true")
      .property("sonar.exclusions", "**/*.xoo.ignore")
      .property("sonar.test.exclusions", "**/*.xoo.ignore")
      .execute();

    assertThat(result.inputFiles()).hasSize(4);

    assertThat(result.inputFile("xources/hello/ClassTwo.xoo")).isNotNull();
    assertThat(result.inputFile("testx/ClassTwoTest.xoo")).isNotNull();
    assertThat(result.inputFile("xources/hello/ClassOne.xoo")).isNotNull();
    assertThat(result.inputFile("testx/ClassOneTest.xoo")).isNotNull();
  }

  @Test
  public void index_basedir_by_default() throws IOException {
    writeFile(baseDir, "sample.xoo", "Sample xoo\ncontent");
    AnalysisResult result = tester.newAnalysis()
      .properties(builder
        .build())
      .execute();

    assertThat(logTester.logs()).contains("1 file indexed");
    assertThat(result.inputFile("sample.xoo")).isNotNull();
  }

  @Test
  public void givenExclusionEndingWithOneWildcardWhenAnalysedThenOnlyDirectChildrenFilesShouldBeExcluded() throws IOException {
    // src/src.xoo
    File srcDir = createDir(baseDir, "src", true);
    writeFile(srcDir, "src.xoo", "Sample xoo 2\ncontent");

    // src/srcSubDir/srcSub.xoo
    File srcSubDir = createDir(srcDir, "srcSubDir", true);
    writeFile(srcSubDir, "srcSub.xoo", "Sample xoo\ncontent");

    // src/srcSubDir/srcSubSubDir/subSubSrc.xoo
    File srcSubSubDir = createDir(srcSubDir, "srcSubSubDir", true);
    writeFile(srcSubSubDir, "subSubSrc.xoo", "Sample xoo\ncontent");

    AnalysisResult result = tester.newAnalysis()
      .properties(builder
        .put("sonar.sources", "src")
        .put("sonar.exclusions", "src/srcSubDir/*")
        .build())
      .execute();

    assertAnalysedFiles(result, "src/src.xoo", "src/srcSubDir/srcSubSubDir/subSubSrc.xoo");
  }

  @Test
  public void givenPathsWithoutReadPermissionWhenAllChildrenAreExcludedThenScannerShouldSkipIt() throws IOException {
    // src/src.xoo
    File srcDir = createDir(baseDir, "src", true);
    writeFile(srcDir, "src.xoo", "Sample xoo 2\ncontent");

    // src/srcSubDir/srcSub.xoo
    File srcSubDir = createDir(srcDir, "srcSubDir", false);
    writeFile(srcSubDir, "srcSub.xoo", "Sample xoo\ncontent");

    // src/srcSubDir2/srcSub2.xoo
    File srcSubDir2 = createDir(srcDir, "srcSubDir2", true);
    boolean fileNotReadable = writeFile(srcSubDir2, "srcSub2.xoo", "Sample 2 xoo\ncontent").setReadable(false);
    assumeTrue(fileNotReadable);

    // src/srcSubDir2/srcSubSubDir2/srcSubSub2.xoo
    File srcSubSubDir2 = createDir(srcSubDir2, "srcSubSubDir2", false);
    writeFile(srcSubSubDir2, "srcSubSub2.xoo", "Sample xoo\ncontent");

    AnalysisResult result = tester.newAnalysis()
      .properties(builder
        .put("sonar.sources", "src")
        .put("sonar.exclusions", "src/srcSubDir/**/*,src/srcSubDir2/**/*")
        .build())
      .execute();

    assertAnalysedFiles(result, "src/src.xoo");
    assertThat(logTester.logs()).contains("1 file ignored because of inclusion/exclusion patterns");
  }

  @Test
  public void givenFileWithoutAccessWhenChildrenAreExcludedThenThenScanShouldFail() throws IOException {
    // src/src.xoo
    File srcDir = createDir(baseDir, "src", true);
    boolean fileNotReadable = writeFile(srcDir, "src.xoo", "Sample xoo\ncontent").setReadable(false);
    assumeTrue(fileNotReadable);

    AnalysisBuilder result = tester.newAnalysis()
      .properties(builder
        .put("sonar.sources", "src")
        .put("sonar.exclusions", "src/src.xoo/**/*") // incorrect pattern, but still the scan should fail if src.xoo is not accessible
        .build());

    assertThatThrownBy(result::execute)
      .isExactlyInstanceOf(IllegalStateException.class)
      .hasMessageStartingWith(format("java.lang.IllegalStateException: Unable to read file"));
  }

  @Test
  public void givenDirectoryWithoutReadPermissionWhenIncludedThenScanShouldFail() throws IOException {
    // src/src.xoo
    File srcDir = createDir(baseDir, "src", true);
    writeFile(srcDir, "src.xoo", "Sample xoo 2\ncontent");

    // src/srcSubDir/srcSub.xoo
    File srcSubDir = createDir(srcDir, "srcSubDir", false);
    writeFile(srcSubDir, "srcSub.xoo", "Sample xoo\ncontent");

    AnalysisBuilder result = tester.newAnalysis()
      .properties(builder
        .put("sonar.sources", "src")
        .put("sonar.exclusions", "src/srcSubDir/*") // srcSubDir should not be excluded unless all children are excluded (src/srcSubDir/**/*)
        .build());

    assertThatThrownBy(result::execute)
      .isExactlyInstanceOf(IllegalStateException.class)
      .hasMessageEndingWith(format("Failed to index files"));
  }

  @Test
  public void givenDirectoryWhenAllChildrenAreExcludedThenSkippedFilesShouldBeReported() throws IOException {
    // src/src.xoo
    File srcDir = createDir(baseDir, "src", true);
    writeFile(srcDir, "src.xoo", "Sample xoo 2\ncontent");

    // src/srcSubDir/srcSub.xoo
    File srcSubDir = createDir(srcDir, "srcSubDir", true);
    writeFile(srcSubDir, "srcSub.xoo", "Sample xoo\ncontent");

    // src/srcSubDir2/srcSub2.xoo
    File srcSubDir2 = createDir(srcDir, "srcSubDir2", true);
    boolean fileNotReadable = writeFile(srcSubDir2, "srcSub2.xoo", "Sample 2 xoo\ncontent").setReadable(false);
    assumeTrue(fileNotReadable);

    AnalysisResult result = tester.newAnalysis()
      .properties(builder
        .put("sonar.sources", "src")
        .put("sonar.exclusions", "src/srcSubDir/**/*,src/srcSubDir2/*")
        .build())
      .execute();

    assertAnalysedFiles(result, "src/src.xoo");
    assertThat(logTester.logs()).contains("2 files ignored because of inclusion/exclusion patterns");
  }

  @Ignore("Fails until we can pattern match inclusions to directories, not only files.")
  @Test
  public void givenDirectoryWithoutReadPermissionWhenNotIncludedThenScanShouldSkipIt() throws IOException {
    // src/src.xoo
    File srcDir = createDir(baseDir, "src", true);
    writeFile(srcDir, "src.xoo", "Sample xoo 2\ncontent");

    // src/srcSubDir/srcSub.xoo
    File srcSubDir = createDir(srcDir, "srcSubDir", true);
    writeFile(srcSubDir, "srcSub.xoo", "Sample xoo\ncontent");

    // src/srcSubDir2/srcSub2.xoo
    File srcSubDir2 = createDir(srcDir, "srcSubDir2", false);
    writeFile(srcSubDir2, "srcSub2.xoo", "Sample xoo\ncontent");

    AnalysisResult result = tester.newAnalysis()
      .properties(builder
        .put("sonar.sources", "src")
        .put("sonar.inclusions", "src/srcSubDir/**/*")
        .build())
      .execute();

    assertAnalysedFiles(result,  "src/srcSubDir/srcSub.xoo");
  }

  @Test
  public void givenDirectoryWithoutReadPermissionUnderSourcesWhenAnalysedThenShouldFail() throws IOException {
    // src/src.xoo
    File srcDir = createDir(baseDir, "src", true);
    writeFile(srcDir, "src.xoo", "Sample xoo 2\ncontent");

    // src/srcSubDir/srcSub.xoo
    File srcSubDir = createDir(srcDir, "srcSubDir", false);
    writeFile(srcSubDir, "srcSub.xoo", "Sample xoo\ncontent");

    AnalysisBuilder result = tester.newAnalysis()
      .properties(builder
        .put("sonar.sources", "src")
        .build());

    assertThatThrownBy(result::execute)
      .isExactlyInstanceOf(IllegalStateException.class)
      .hasMessageEndingWith(format("Failed to index files"));
  }

  private static void assertAnalysedFiles(AnalysisResult result, String... files) {
    assertThat(result.inputFiles().stream().map(InputFile::toString).collect(Collectors.toList())).contains(files);
  }

  private File createDir(File parentDir, String name, boolean isReadable) {
    File dir = new File(parentDir, name);
    dir.mkdir();
    boolean fileSystemOperationSucceded = dir.setReadable(isReadable);
    assumeTrue(fileSystemOperationSucceded); //On windows + java there is no reliable way to play with readable/not readable flag
    return dir;
  }

  private File writeFile(File parent, String name, String content) throws IOException {
    File file = new File(parent, name);
    FileUtils.write(file, content, StandardCharsets.UTF_8);
    return file;
  }

  private File writeFile(File parent, String name, long size) throws IOException {
    File file = new File(parent, name);
    RandomAccessFile raf = new RandomAccessFile(file, "rw");
    raf.setLength(size);
    raf.close();
    return file;
  }

}
