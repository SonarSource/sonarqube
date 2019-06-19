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
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.scanner.mediumtest.ScannerMediumTester;
import org.sonar.scanner.mediumtest.AnalysisResult;
import org.sonar.scanner.protocol.output.ScannerReport.ExternalIssue;
import org.sonar.scanner.protocol.output.ScannerReport.Issue;
import org.sonar.api.batch.rule.LoadedActiveRule;
import org.sonar.xoo.XooPlugin;
import org.sonar.xoo.rule.HasTagSensor;
import org.sonar.xoo.rule.OneExternalIssuePerLineSensor;
import org.sonar.xoo.rule.XooRulesDefinition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class IssuesMediumTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Rule
  public LogTester logTester = new LogTester();

  @Rule
  public ScannerMediumTester tester = new ScannerMediumTester()
    .registerPlugin("xoo", new XooPlugin())
    .addDefaultQProfile("xoo", "Sonar Way")
    .addRules(new XooRulesDefinition())
    .addActiveRule("xoo", "OneIssuePerLine", null, "One issue per line", "MAJOR", "OneIssuePerLine.internal", "xoo");

  @Test
  public void testOneIssuePerLine() throws Exception {
    File projectDir = new File("test-resources/mediumtest/xoo/sample");
    File tmpDir = temp.newFolder();
    FileUtils.copyDirectory(projectDir, tmpDir);

    AnalysisResult result = tester
      .newAnalysis(new File(tmpDir, "sonar-project.properties"))
      .execute();

    List<Issue> issues = result.issuesFor(result.inputFile("xources/hello/HelloJava.xoo"));
    assertThat(issues).hasSize(8 /* lines */);
    
    List<ExternalIssue> externalIssues = result.externalIssuesFor(result.inputFile("xources/hello/HelloJava.xoo"));
    assertThat(externalIssues).isEmpty();
  }
  
  @Test
  public void testOneExternalIssuePerLine() throws Exception {
    File projectDir = new File("test-resources/mediumtest/xoo/sample");
    File tmpDir = temp.newFolder();
    FileUtils.copyDirectory(projectDir, tmpDir);

    AnalysisResult result = tester
      .newAnalysis(new File(tmpDir, "sonar-project.properties"))
      .property(OneExternalIssuePerLineSensor.ACTIVATE, "true")
      .execute();

    List<ExternalIssue> externalIssues = result.externalIssuesFor(result.inputFile("xources/hello/HelloJava.xoo"));
    assertThat(externalIssues).hasSize(8 /* lines */);
  }

  @Test
  public void findActiveRuleByInternalKey() throws Exception {
    File projectDir = new File("test-resources/mediumtest/xoo/sample");
    File tmpDir = temp.newFolder();
    FileUtils.copyDirectory(projectDir, tmpDir);

    AnalysisResult result = tester
      .newAnalysis(new File(tmpDir, "sonar-project.properties"))
      .property("sonar.xoo.internalKey", "OneIssuePerLine.internal")
      .execute();

    List<Issue> issues = result.issuesFor(result.inputFile("xources/hello/HelloJava.xoo"));
    assertThat(issues).hasSize(8 /* lines */ + 1 /* file */);
  }

  @Test
  public void testOverrideQProfileSeverity() throws Exception {
    File projectDir = new File("test-resources/mediumtest/xoo/sample");
    File tmpDir = temp.newFolder();
    FileUtils.copyDirectory(projectDir, tmpDir);

    AnalysisResult result = tester
      .newAnalysis(new File(tmpDir, "sonar-project.properties"))
      .property("sonar.oneIssuePerLine.forceSeverity", "CRITICAL")
      .execute();

    List<Issue> issues = result.issuesFor(result.inputFile("xources/hello/HelloJava.xoo"));
    assertThat(issues.get(0).getSeverity()).isEqualTo(org.sonar.scanner.protocol.Constants.Severity.CRITICAL);
  }

  @Test
  public void testIssueExclusionByRegexp() throws Exception {
    File projectDir = new File("test-resources/mediumtest/xoo/sample");
    File tmpDir = temp.newFolder();
    FileUtils.copyDirectory(projectDir, tmpDir);

    AnalysisResult result = tester
      .newAnalysis(new File(tmpDir, "sonar-project.properties"))
      .property("sonar.issue.ignore.allfile", "1")
      .property("sonar.issue.ignore.allfile.1.fileRegexp", "object")
      .execute();

    assertThat(result.issuesFor(result.inputFile("xources/hello/HelloJava.xoo"))).hasSize(8 /* lines */);
    assertThat(result.issuesFor(result.inputFile("xources/hello/helloscala.xoo"))).isEmpty();
  }

  @Test
  public void testIssueExclusionByBlock() throws Exception {
    File baseDir = temp.newFolder();
    File srcDir = new File(baseDir, "src");
    srcDir.mkdir();

    File xooFile = new File(srcDir, "sample.xoo");
    FileUtils.write(xooFile, "1\nSONAR-OFF 2\n3\n4\n5\nSONAR-ON 6\n7\n8\n9\n10", StandardCharsets.UTF_8);

    AnalysisResult result = tester.newAnalysis()
      .properties(ImmutableMap.<String, String>builder()
        .put("sonar.projectBaseDir", baseDir.getAbsolutePath())
        .put("sonar.projectKey", "com.foo.project")
        .put("sonar.sources", "src")
        .build())
      .property("sonar.issue.ignore.block", "1")
      .property("sonar.issue.ignore.block.1.beginBlockRegexp", "SON.*-OFF")
      .property("sonar.issue.ignore.block.1.endBlockRegexp", "SON.*-ON")
      .execute();

    List<Issue> issues = result.issuesFor(result.inputFile("src/sample.xoo"));
    assertThat(issues).hasSize(5);
    assertThat(issues)
      .extracting("textRange.startLine")
      .containsExactlyInAnyOrder(1, 7, 8, 9, 10);
  }

  @Test
  public void testIssueExclusionByIgnoreMultiCriteria() throws Exception {
    File baseDir = temp.newFolder();
    File srcDir = new File(baseDir, "src");
    srcDir.mkdir();

    activateTODORule();

    File xooFile1 = new File(srcDir, "sample1.xoo");
    FileUtils.write(xooFile1, "1\n2\n3 TODO\n4\n5\n6 TODO\n7\n8\n9\n10", StandardCharsets.UTF_8);
    File xooFile11 = new File(srcDir, "sample11.xoo");
    FileUtils.write(xooFile11, "1\n2\n3 TODO\n4\n5\n6 TODO\n7\n8\n9\n10", StandardCharsets.UTF_8);

    AnalysisResult result = tester.newAnalysis()
      .properties(ImmutableMap.<String, String>builder()
        .put("sonar.projectBaseDir", baseDir.getAbsolutePath())
        .put("sonar.projectKey", "com.foo.project")
        .put("sonar.sources", "src")
        .build())
      .property("sonar.issue.ignore.multicriteria", "1,2")
      .property("sonar.issue.ignore.multicriteria.1.ruleKey", "xoo:HasTag")
      .property("sonar.issue.ignore.multicriteria.1.resourceKey", "src/sample11.xoo")
      .property("sonar.issue.ignore.multicriteria.2.ruleKey", "xoo:One*")
      .property("sonar.issue.ignore.multicriteria.2.resourceKey", "src/sample?.xoo")
      .execute();

    List<Issue> issues = result.issuesFor(result.inputFile("src/sample1.xoo"));
    assertThat(issues).hasSize(2);

    issues = result.issuesFor(result.inputFile("src/sample11.xoo"));
    assertThat(issues).hasSize(10);
  }

  @Test
  public void warn_user_for_outdated_IssueExclusionByIgnoreMultiCriteria() throws Exception {
    File baseDir = temp.getRoot();
    File baseDirModuleA = new File(baseDir, "moduleA");
    File baseDirModuleB = new File(baseDir, "moduleB");
    File srcDirA = new File(baseDirModuleA, "src");
    srcDirA.mkdirs();
    File srcDirB = new File(baseDirModuleB, "src");
    srcDirB.mkdirs();

    File xooFileA = new File(srcDirA, "sampleA.xoo");
    FileUtils.write(xooFileA, "1\n2\n3\n4\n5\n6 TODO\n7\n8\n9\n10", StandardCharsets.UTF_8);
    File xooFileB = new File(srcDirB, "sampleB.xoo");
    FileUtils.write(xooFileB, "1\n2\n3\n4\n5\n6 TODO\n7\n8\n9\n10", StandardCharsets.UTF_8);

    tester
      .addProjectServerSettings("sonar.issue.ignore.multicriteria", "1")
      .addProjectServerSettings("sonar.issue.ignore.multicriteria.1.ruleKey", "*")
      .addProjectServerSettings("sonar.issue.ignore.multicriteria.1.resourceKey", "src/sampleA.xoo");

    AnalysisResult result = tester.newAnalysis()
      .properties(ImmutableMap.<String, String>builder()
        .put("sonar.projectBaseDir", baseDir.getAbsolutePath())
        .put("sonar.projectKey", "com.foo.project")
        .put("sonar.modules", "moduleA,moduleB")
        .put("sonar.sources", "src")
        .build())
      .execute();

    assertThat(logTester.logs(LoggerLevel.WARN)).contains("Specifying module-relative paths at project level in property 'sonar.issue.ignore.multicriteria' is deprecated. To continue matching files like 'moduleA/src/sampleA.xoo', update this property so that patterns refer to project-relative paths.");

    List<Issue> issues = result.issuesFor(result.inputFile("moduleA/src/sampleA.xoo"));
    assertThat(issues).hasSize(0);

    issues = result.issuesFor(result.inputFile("moduleB/src/sampleB.xoo"));
    assertThat(issues).hasSize(10);
  }

  @Test
  public void warn_user_for_unsupported_module_level_IssueExclusion() throws Exception {
    File baseDir = temp.getRoot();
    File baseDirModuleA = new File(baseDir, "moduleA");
    File baseDirModuleB = new File(baseDir, "moduleB");
    File srcDirA = new File(baseDirModuleA, "src");
    srcDirA.mkdirs();
    File srcDirB = new File(baseDirModuleB, "src");
    srcDirB.mkdirs();

    File xooFileA = new File(srcDirA, "sampleA.xoo");
    FileUtils.write(xooFileA, "1\n2\n3\n4\n5\n6 TODO\n7\n8\n9\n10", StandardCharsets.UTF_8);
    File xooFileB = new File(srcDirB, "sampleB.xoo");
    FileUtils.write(xooFileB, "1\n2\n3\n4\n5\n6 TODO\n7\n8\n9\n10", StandardCharsets.UTF_8);

    AnalysisResult result = tester.newAnalysis()
      .properties(ImmutableMap.<String, String>builder()
        .put("sonar.projectBaseDir", baseDir.getAbsolutePath())
        .put("sonar.projectKey", "com.foo.project")
        .put("sonar.modules", "moduleA,moduleB")
        .put("sonar.sources", "src")
        .put("sonar.scm.disabled", "true")
        .put("sonar.issue.ignore.multicriteria", "1")
        .put("sonar.issue.ignore.multicriteria.1.ruleKey", "*")
        .put("sonar.issue.ignore.multicriteria.1.resourceKey", "*")
        .build())
      .execute();

    assertThat(logTester.logs(LoggerLevel.WARN)).isEmpty();

    result = tester.newAnalysis()
      .properties(ImmutableMap.<String, String>builder()
        .put("sonar.projectBaseDir", baseDir.getAbsolutePath())
        .put("sonar.projectKey", "com.foo.project")
        .put("sonar.modules", "moduleA,moduleB")
        .put("sonar.sources", "src")
        .put("sonar.scm.disabled", "true")
        .put("moduleA.sonar.issue.ignore.multicriteria", "1")
        .put("moduleA.sonar.issue.ignore.multicriteria.1.ruleKey", "*")
        .put("moduleA.sonar.issue.ignore.multicriteria.1.resourceKey", "*")
        .build())
      .execute();

    assertThat(logTester.logs(LoggerLevel.WARN)).containsOnly("Specifying issue exclusions at module level is not supported anymore. Configure the property 'sonar.issue.ignore.multicriteria' and any other issue exclusions at project level.");

    List<Issue> issues = result.issuesFor(result.inputFile("moduleA/src/sampleA.xoo"));
    assertThat(issues).hasSize(10);

    issues = result.issuesFor(result.inputFile("moduleB/src/sampleB.xoo"));
    assertThat(issues).hasSize(10);


    // SONAR-11850 The Maven scanner replicates properties defined on the root module to all modules
    logTester.clear();
    result = tester.newAnalysis()
      .properties(ImmutableMap.<String, String>builder()
        .put("sonar.projectBaseDir", baseDir.getAbsolutePath())
        .put("sonar.projectKey", "com.foo.project")
        .put("sonar.modules", "moduleA,moduleB")
        .put("sonar.sources", "src")
        .put("sonar.scm.disabled", "true")
        .put("sonar.issue.ignore.multicriteria", "1")
        .put("sonar.issue.ignore.multicriteria.1.ruleKey", "*")
        .put("sonar.issue.ignore.multicriteria.1.resourceKey", "*")
        .put("moduleA.sonar.issue.ignore.multicriteria", "1")
        .put("moduleA.sonar.issue.ignore.multicriteria.1.ruleKey", "*")
        .put("moduleA.sonar.issue.ignore.multicriteria.1.resourceKey", "*")
        .build())
      .execute();

    assertThat(logTester.logs(LoggerLevel.WARN)).isEmpty();
  }

  @Test
  public void testIssueExclusionByEnforceMultiCriteria() throws Exception {
    File baseDir = temp.newFolder();
    File srcDir = new File(baseDir, "src");
    srcDir.mkdir();

    activateTODORule();

    File xooFile1 = new File(srcDir, "sample1.xoo");
    FileUtils.write(xooFile1, "1\n2\n3 TODO\n4\n5\n6 TODO\n7\n8\n9\n10", StandardCharsets.UTF_8);
    File xooFile11 = new File(srcDir, "sample11.xoo");
    FileUtils.write(xooFile11, "1\n2\n3 TODO\n4\n5\n6 TODO\n7\n8\n9\n10", StandardCharsets.UTF_8);

    AnalysisResult result = tester.newAnalysis()
      .properties(ImmutableMap.<String, String>builder()
        .put("sonar.projectBaseDir", baseDir.getAbsolutePath())
        .put("sonar.projectKey", "com.foo.project")
        .put("sonar.sources", "src")
        .build())
      .property("sonar.issue.enforce.multicriteria", "1,2")
      .property("sonar.issue.enforce.multicriteria.1.ruleKey", "xoo:HasTag")
      .property("sonar.issue.enforce.multicriteria.1.resourceKey", "src/sample11.xoo")
      .property("sonar.issue.enforce.multicriteria.2.ruleKey", "xoo:One*")
      .property("sonar.issue.enforce.multicriteria.2.resourceKey", "src/sample?.xoo")
      .execute();

    List<Issue> issues = result.issuesFor(result.inputFile("src/sample1.xoo"));
    assertThat(issues).hasSize(10);

    issues = result.issuesFor(result.inputFile("src/sample11.xoo"));
    assertThat(issues).hasSize(2);
  }

  @Test
  public void warn_user_for_outdated_IssueExclusionByEnforceMultiCriteria() throws Exception {
    File baseDir = temp.getRoot();
    File baseDirModuleA = new File(baseDir, "moduleA");
    File baseDirModuleB = new File(baseDir, "moduleB");
    File srcDirA = new File(baseDirModuleA, "src");
    srcDirA.mkdirs();
    File srcDirB = new File(baseDirModuleB, "src");
    srcDirB.mkdirs();

    File xooFileA = new File(srcDirA, "sampleA.xoo");
    FileUtils.write(xooFileA, "1\n2\n3\n4\n5\n6 TODO\n7\n8\n9\n10", StandardCharsets.UTF_8);
    File xooFileB = new File(srcDirB, "sampleB.xoo");
    FileUtils.write(xooFileB, "1\n2\n3\n4\n5\n6 TODO\n7\n8\n9\n10", StandardCharsets.UTF_8);

    tester
      .addProjectServerSettings("sonar.issue.enforce.multicriteria", "1")
      .addProjectServerSettings("sonar.issue.enforce.multicriteria.1.ruleKey", "*")
      .addProjectServerSettings("sonar.issue.enforce.multicriteria.1.resourceKey", "src/sampleA.xoo");

    AnalysisResult result = tester.newAnalysis()
      .properties(ImmutableMap.<String, String>builder()
        .put("sonar.projectBaseDir", baseDir.getAbsolutePath())
        .put("sonar.projectKey", "com.foo.project")
        .put("sonar.modules", "moduleA,moduleB")
        .put("sonar.sources", "src")
        .build())
      .execute();

    assertThat(logTester.logs(LoggerLevel.WARN)).contains("Specifying module-relative paths at project level in property 'sonar.issue.enforce.multicriteria' is deprecated. To continue matching files like 'moduleA/src/sampleA.xoo', update this property so that patterns refer to project-relative paths.");

    List<Issue> issues = result.issuesFor(result.inputFile("moduleA/src/sampleA.xoo"));
    assertThat(issues).hasSize(10);

    issues = result.issuesFor(result.inputFile("moduleB/src/sampleB.xoo"));
    assertThat(issues).hasSize(0);
  }

  private void activateTODORule() {
    LoadedActiveRule r = new LoadedActiveRule();
    r.setRuleKey(RuleKey.of("xoo", HasTagSensor.RULE_KEY));
    r.setName("TODO");
    r.setLanguage("xoo");
    r.setSeverity("MAJOR");
    r.setParams(ImmutableMap.of("tag", "TODO"));
    tester.activateRule(r);
  }

  @Test
  public void testIssueDetails() throws IOException {
    File baseDir = temp.newFolder();
    File srcDir = new File(baseDir, "src");
    srcDir.mkdir();

    File xooFile = new File(srcDir, "sample.xoo");
    FileUtils.write(xooFile, "1\n2\n3\n4\n5\n6\n7\n8\n9\n10", StandardCharsets.UTF_8);

    AnalysisResult result = tester.newAnalysis()
      .properties(ImmutableMap.<String, String>builder()
        .put("sonar.projectBaseDir", baseDir.getAbsolutePath())
        .put("sonar.projectKey", "com.foo.project")
        .put("sonar.sources", "src")
        .build())
      .execute();

    List<Issue> issues = result.issuesFor(result.inputFile("src/sample.xoo"));
    assertThat(issues).hasSize(10);
    assertThat(issues)
      .extracting("msg", "textRange.startLine", "gap")
      .contains(tuple("This issue is generated on each line", 1, 0.0));
  }

  @Test
  public void testIssueFilter() throws Exception {
    File projectDir = new File("test-resources/mediumtest/xoo/sample");
    File tmpDir = temp.newFolder();
    FileUtils.copyDirectory(projectDir, tmpDir);

    AnalysisResult result = tester
      .newAnalysis(new File(tmpDir, "sonar-project.properties"))
      .property("sonar.xoo.excludeAllIssuesOnOddLines", "true")
      .execute();

    List<Issue> issues = result.issuesFor(result.inputFile("xources/hello/HelloJava.xoo"));
    assertThat(issues).hasSize(4 /* even lines */);
  }

}
