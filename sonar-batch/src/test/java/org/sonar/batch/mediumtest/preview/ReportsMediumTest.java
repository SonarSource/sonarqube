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
package org.sonar.batch.mediumtest.preview;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.CoreProperties;
import org.sonar.batch.mediumtest.BatchMediumTester;
import org.sonar.batch.mediumtest.TaskResult;
import org.sonar.batch.protocol.input.ActiveRule;
import org.sonar.batch.protocol.input.issues.PreviousIssue;
import org.sonar.xoo.XooPlugin;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ReportsMediumTest {

  @org.junit.Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private static SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");

  private static Date date(String date) {
    try {
      return sdf.parse(date);
    } catch (ParseException e) {
      throw new IllegalStateException(e);
    }
  }

  public BatchMediumTester tester = BatchMediumTester.builder()
    .bootstrapProperties(ImmutableMap.of(CoreProperties.ANALYSIS_MODE, CoreProperties.ANALYSIS_MODE_PREVIEW))
    .registerPlugin("xoo", new XooPlugin())
    .addDefaultQProfile("xoo", "Sonar Way")
    .activateRule(new ActiveRule("xoo", "OneIssuePerLine", null, "One issue per line", "MAJOR", "OneIssuePerLine.internal", "xoo"))
    // Existing issue
    .addPreviousIssue(new PreviousIssue().setKey("xyz")
      .setComponentKey("sample:xources/hello/HelloJava.xoo")
      .setRuleKey("xoo", "OneIssuePerLine")
      .setLine(1)
      .setSeverity("MAJOR")
      .setCreationDate(date("14/03/2004"))
      .setChecksum(DigestUtils.md5Hex("packagehello;"))
      .setStatus("OPEN"))
    // Resolved issue
    .addPreviousIssue(new PreviousIssue().setKey("resolved")
      .setComponentKey("sample:xources/hello/HelloJava.xoo")
      .setRuleKey("xoo", "OneIssuePerLine")
      .setLine(1)
      .setSeverity("MAJOR")
      .setCreationDate(date("14/03/2004"))
      .setChecksum(DigestUtils.md5Hex("dontexist"))
      .setStatus("OPEN"))
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
  public void testConsoleReport() throws Exception {
    File projectDir = new File(ReportsMediumTest.class.getResource("/mediumtest/xoo/sample").toURI());

    TaskResult result = tester
      .newScanTask(new File(projectDir, "sonar-project.properties"))
      .property("sonar.issuesReport.console.enable", "true")
      .start();

  }

  @Test
  public void testHtmlReport() throws Exception {
    File projectDir = new File(ReportsMediumTest.class.getResource("/mediumtest/xoo/sample").toURI());

    TaskResult result = tester
      .newScanTask(new File(projectDir, "sonar-project.properties"))
      .property("sonar.issuesReport.html.enable", "true")
      .start();

  }

}
