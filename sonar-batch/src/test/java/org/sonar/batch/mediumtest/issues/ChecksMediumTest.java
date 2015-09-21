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

import org.sonarqube.ws.Rules.Rule.Param;

import org.sonarqube.ws.Rules.Rule.Params;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableMap;

import java.io.File;
import java.io.IOException;
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

public class ChecksMediumTest {

  @org.junit.Rule
  public TemporaryFolder temp = new TemporaryFolder();

  public BatchMediumTester tester = BatchMediumTester.builder()
    .registerPlugin("xoo", new XooPlugin())
    .addRules(new XooRulesDefinition())
    .addDefaultQProfile("xoo", "Sonar Way")
    .addRule("TemplateRule_1234", "xoo", "TemplateRule_1234", "A template rule")
    .addRule("TemplateRule_1235", "xoo", "TemplateRule_1235", "Another template rule")
    .activateRule(createActiveRuleWithParam("xoo", "TemplateRule_1234", "TemplateRule", "A template rule", "MAJOR", null, "xoo", "line", "1"))
    .activateRule(createActiveRuleWithParam("xoo", "TemplateRule_1235", "TemplateRule", "Another template rule", "MAJOR", null, "xoo", "line", "2"))
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
  public void testCheckWithTemplate() throws IOException {

    File baseDir = temp.getRoot();
    File srcDir = new File(baseDir, "src");
    srcDir.mkdir();

    File xooFile = new File(srcDir, "sample.xoo");
    FileUtils.write(xooFile, "foo\nbar");

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

    List<Issue> issues = result.issuesFor(result.inputFile("src/sample.xoo"));
    assertThat(issues).hasSize(2);

    boolean foundIssueAtLine1 = false;
    boolean foundIssueAtLine2 = false;
    for (Issue issue : issues) {
      if (issue.getLine() == 1) {
        foundIssueAtLine1 = true;
        assertThat(issue.getMsg()).isEqualTo("A template rule");
      }
      if (issue.getLine() == 2) {
        foundIssueAtLine2 = true;
        assertThat(issue.getMsg()).isEqualTo("Another template rule");
      }
    }
    assertThat(foundIssueAtLine1).isTrue();
    assertThat(foundIssueAtLine2).isTrue();
  }

  private org.sonarqube.ws.Rules.Rule createActiveRuleWithParam(String repositoryKey, String ruleKey, @Nullable String templateRuleKey, String name, @Nullable String severity,
    @Nullable String internalKey, @Nullable String languag, String paramKey, String paramValue) {
    org.sonarqube.ws.Rules.Rule.Builder builder = org.sonarqube.ws.Rules.Rule.newBuilder();
    builder.setRepo(repositoryKey);
    builder.setKey(ruleKey);
    if (templateRuleKey != null) {
      builder.setTemplateKey(templateRuleKey);
    }
    if (languag != null) {
      builder.setLang(languag);
    }
    if (internalKey != null) {
      builder.setInternalKey(internalKey);
    }
    if (severity != null) {
      builder.setSeverity(severity);
    }
    builder.setName(name);

    Param param = Param.newBuilder().setKey(paramKey).setDefaultValue(paramValue).build();
    Params params = Params.newBuilder().addParams(param).build();
    builder.setParams(params);
    return builder.build();
  }

}
