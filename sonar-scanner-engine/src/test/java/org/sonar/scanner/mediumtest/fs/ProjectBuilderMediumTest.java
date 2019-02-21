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
package org.sonar.scanner.mediumtest.fs;

import com.google.common.collect.ImmutableMap;
import java.io.File;
import java.io.IOException;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.bootstrap.ProjectBuilder;
import org.sonar.api.utils.MessageException;
import org.sonar.scanner.mediumtest.AnalysisResult;
import org.sonar.scanner.mediumtest.ScannerMediumTester;
import org.sonar.scanner.protocol.output.ScannerReport.Issue;
import org.sonar.xoo.XooPlugin;
import org.sonar.xoo.rule.XooRulesDefinition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

public class ProjectBuilderMediumTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Rule
  public ExpectedException exception = ExpectedException.none();

  private ProjectBuilder projectBuilder = mock(ProjectBuilder.class);

  @Rule
  public ScannerMediumTester tester = new ScannerMediumTester()
    .registerPlugin("xoo", new XooPluginWithBuilder(projectBuilder))
    .addRules(new XooRulesDefinition())
    .addDefaultQProfile("xoo", "Sonar Way")
    .addActiveRule("xoo", "OneIssuePerLine", null, "One issue per line", "MAJOR", "OneIssuePerLine.internal", "xoo");

  private class XooPluginWithBuilder extends XooPlugin {
    private ProjectBuilder builder;

    XooPluginWithBuilder(ProjectBuilder builder) {
      this.builder = builder;
    }

    @Override
    public void define(Context context) {
      super.define(context);
      context.addExtension(builder);
    }
  }

  @Test
  public void testProjectReactorValidation() throws IOException {
    File baseDir = prepareProject();

    doThrow(new IllegalStateException("My error message")).when(projectBuilder).build(any(ProjectBuilder.Context.class));
    exception.expectMessage("Failed to execute project builder");
    exception.expect(MessageException.class);
    exception.expectCause(new BaseMatcher<Throwable>() {

      @Override
      public boolean matches(Object item) {
        if (!(item instanceof IllegalStateException)) {
          return false;
        }
        IllegalStateException e = (IllegalStateException) item;
        return "My error message".equals(e.getMessage());
      }

      @Override
      public void describeTo(Description description) {
      }
    });

    tester.newAnalysis()
      .properties(ImmutableMap.<String, String>builder()
        .put("sonar.projectBaseDir", baseDir.getAbsolutePath())
        .put("sonar.projectKey", "com.foo.project")
        .put("sonar.sources", ".")
        .put("sonar.xoo.enableProjectBuilder", "true")
        .build())
      .execute();

  }

  @Test
  public void testProjectBuilder() throws IOException {
    File baseDir = prepareProject();

    AnalysisResult result = tester.newAnalysis()
      .properties(ImmutableMap.<String, String>builder()
        .put("sonar.projectBaseDir", baseDir.getAbsolutePath())
        .put("sonar.projectKey", "com.foo.project")
        .put("sonar.sources", ".")
        .put("sonar.verbose", "true")
        .put("sonar.xoo.enableProjectBuilder", "true")
        .build())
      .execute();
    List<Issue> issues = result.issuesFor(result.inputFile("module1/src/sample.xoo"));
    assertThat(issues).hasSize(10);

    assertThat(issues)
      .extracting("msg", "textRange.startLine", "gap")
      .contains(tuple("This issue is generated on each line", 1, 0.0));

  }

  @Test
  // SONAR-6976
  public void testProjectBuilderWithNewLine() throws IOException {
    File baseDir = prepareProject();

    exception.expect(MessageException.class);
    exception.expectMessage("is not a valid branch name");
    tester.newAnalysis()
      .properties(ImmutableMap.<String, String>builder()
        .put("sonar.projectBaseDir", baseDir.getAbsolutePath())
        .put("sonar.projectKey", "com.foo.project")
        .put("sonar.branch", "branch\n")
        .put("sonar.sources", ".")
        .put("sonar.xoo.enableProjectBuilder", "true")
        .build())
      .execute();
  }

  @Test
  public void testProjectBuilderWithBranch() throws IOException {
    File baseDir = prepareProject();

    AnalysisResult result = tester.newAnalysis()
      .properties(ImmutableMap.<String, String>builder()
        .put("sonar.projectBaseDir", baseDir.getAbsolutePath())
        .put("sonar.projectKey", "com.foo.project")
        .put("sonar.branch", "my-branch")
        .put("sonar.sources", ".")
        .put("sonar.xoo.enableProjectBuilder", "true")
        .build())
      .execute();

    List<Issue> issues = result.issuesFor(result.inputFile("module1/src/sample.xoo"));
    assertThat(issues).hasSize(10);

    assertThat(issues)
      .extracting("msg", "textRange.startLine", "gap")
      .contains(tuple("This issue is generated on each line", 1, 0.0));
  }

  private File prepareProject() throws IOException {
    File baseDir = temp.newFolder();
    File module1Dir = new File(baseDir, "module1");
    module1Dir.mkdir();

    File srcDir = new File(module1Dir, "src");
    srcDir.mkdir();

    File xooFile = new File(srcDir, "sample.xoo");
    FileUtils.write(xooFile, "1\n2\n3\n4\n5\n6\n7\n8\n9\n10");

    return baseDir;
  }

}
