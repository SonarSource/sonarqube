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
package org.sonar.scanner.mediumtest.issuesmode;

import com.google.common.collect.ImmutableMap;
import java.io.File;
import java.util.Date;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.CoreProperties;
import org.sonar.api.utils.log.LogTester;
import org.sonar.scanner.issue.tracking.TrackedIssue;
import org.sonar.scanner.mediumtest.ScannerMediumTester;
import org.sonar.scanner.mediumtest.TaskResult;
import org.sonar.xoo.XooPlugin;
import org.sonar.xoo.rule.XooRulesDefinition;

import static org.assertj.core.api.Assertions.assertThat;

public class EmptyFileTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Rule
  public LogTester logTester = new LogTester();

  @Rule
  public ScannerMediumTester tester = new ScannerMediumTester()
    .bootstrapProperties(ImmutableMap.of(CoreProperties.ANALYSIS_MODE, CoreProperties.ANALYSIS_MODE_ISSUES))
    .registerPlugin("xoo", new XooPlugin())
    .addRules(new XooRulesDefinition())
    .addDefaultQProfile("xoo", "Sonar Way")
    .addActiveRule("xoo", "OneIssuePerLine", null, "One issue per line", "MAJOR", "my/internal/key", "xoo")
    .setPreviousAnalysisDate(new Date());

  @Test
  public void testIssueTrackingWithIssueOnEmptyFile() throws Exception {
    File projectDir = copyProject("/mediumtest/xoo/sample-with-empty-file");

    TaskResult result = tester
      .newScanTask(new File(projectDir, "sonar-project.properties"))
      .property("sonar.xoo.internalKey", "my/internal/key")
      .execute();

    for (TrackedIssue i : result.trackedIssues()) {
      System.out.println(i.startLine() + " " + i.getMessage());
    }

    assertThat(result.trackedIssues()).hasSize(11);
  }

  private File copyProject(String path) throws Exception {
    File projectDir = temp.newFolder();
    File originalProjectDir = new File(EmptyFileTest.class.getResource(path).toURI());
    FileUtils.copyDirectory(originalProjectDir, projectDir, FileFilterUtils.notFileFilter(FileFilterUtils.nameFileFilter(".sonar")));
    return projectDir;
  }

}
