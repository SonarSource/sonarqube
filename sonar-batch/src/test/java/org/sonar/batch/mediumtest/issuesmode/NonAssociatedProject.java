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
package org.sonar.batch.mediumtest.issuesmode;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.CoreProperties;
import org.sonar.api.utils.log.LogTester;
import org.sonar.batch.mediumtest.BatchMediumTester;
import org.sonar.batch.mediumtest.TaskResult;
import org.sonar.xoo.XooPlugin;
import org.sonar.xoo.rule.XooRulesDefinition;

import java.io.File;
import java.io.IOException;

public class NonAssociatedProject {
  @org.junit.Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @org.junit.Rule
  public LogTester logTester = new LogTester();

  public BatchMediumTester tester;

  @Before
  public void prepare() throws IOException {
    tester = BatchMediumTester.builder()
      .bootstrapProperties(ImmutableMap.of(
        CoreProperties.ANALYSIS_MODE, CoreProperties.ANALYSIS_MODE_ISSUES,
        CoreProperties.GLOBAL_WORKING_DIRECTORY, temp.newFolder().getAbsolutePath()))
      .registerPlugin("xoo", new XooPlugin())
      .addQProfile("xoo", "Sonar Way")
      .addRules(new XooRulesDefinition())
      .addRule("manual:MyManualIssue", "manual", "MyManualIssue", "My manual issue")
      .addRule("manual:MyManualIssueDup", "manual", "MyManualIssue", "My manual issue")
      .addActiveRule("xoo", "OneIssuePerLine", null, "One issue per line", "MAJOR", null, "xoo")
      .addActiveRule("xoo", "OneIssueOnDirPerFile", null, "OneIssueOnDirPerFile", "MAJOR", null, "xoo")
      .addActiveRule("xoo", "OneIssuePerModule", null, "OneIssuePerModule", "MAJOR", null, "xoo")
      .addActiveRule("manual", "MyManualIssue", null, "My manual issue", "MAJOR", null, null)
      .setAssociated(false)
      .build();
    tester.start();
  }

  @After
  public void stop() {
    tester.stop();
  }

  private File copyProject(String path) throws Exception {
    File projectDir = temp.newFolder();
    File originalProjectDir = new File(IssueModeAndReportsMediumTest.class.getResource(path).toURI());
    FileUtils.copyDirectory(originalProjectDir, projectDir, FileFilterUtils.notFileFilter(FileFilterUtils.nameFileFilter(".sonar")));
    return projectDir;
  }

  @Test
  public void testNonAssociated() throws Exception {
    File projectDir = copyProject("/mediumtest/xoo/multi-modules-sample-not-associated");

    TaskResult result = tester
      .newScanTask(new File(projectDir, "sonar-project.properties"))
      .start();

  }
}
