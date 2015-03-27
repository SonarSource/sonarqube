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
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.CoreProperties;
import org.sonar.api.issue.Issue;
import org.sonar.batch.mediumtest.BatchMediumTester;
import org.sonar.batch.mediumtest.TaskResult;
import org.sonar.batch.protocol.Constants.Severity;
import org.sonar.batch.protocol.input.ActiveRule;
import org.sonar.batch.protocol.input.FileData;
import org.sonar.xoo.XooPlugin;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

public class IncrementalModeMediumTest {

  private static final String SAMPLE_CONTENT = "Sample content\nwith\n4\nlines";

  @org.junit.Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private static SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");

  private static Long date(String date) {
    try {
      return sdf.parse(date).getTime();
    } catch (ParseException e) {
      throw new IllegalStateException(e);
    }
  }

  public BatchMediumTester tester = BatchMediumTester
    .builder()
    .bootstrapProperties(ImmutableMap.of(CoreProperties.ANALYSIS_MODE, CoreProperties.ANALYSIS_MODE_INCREMENTAL))
    .registerPlugin("xoo", new XooPlugin())
    .addDefaultQProfile("xoo", "Sonar Way")
    .activateRule(new ActiveRule("xoo", "OneIssuePerLine", null, "One issue per line", "MAJOR", null, "xoo"))
    .activateRule(new ActiveRule("manual", "MyManualIssue", null, "My manual issue", "MAJOR", null, null))
    .setPreviousAnalysisDate(new Date())
    .addFileData("sample", "src/sample.xoo", new FileData(DigestUtils.md5Hex(SAMPLE_CONTENT), false))
    .mockLineHashes("sample:src/sample.xoo",
      new String[] {DigestUtils.md5Hex("Samplecontent"), DigestUtils.md5Hex("oldcode"), DigestUtils.md5Hex("4"), DigestUtils.md5Hex("lines")})
    // Remote open issue => will be tracked and not new
    .mockServerIssue(org.sonar.batch.protocol.input.BatchInput.ServerIssue.newBuilder().setKey("xyz")
      .setModuleKey("sample")
      .setPath("src/sample.xoo")
      .setRuleRepository("xoo")
      .setRuleKey("OneIssuePerLine")
      .setLine(1)
      .setSeverity(Severity.MAJOR)
      .setCreationDate(date("14/03/2004"))
      .setChecksum(DigestUtils.md5Hex("Samplecontent"))
      .setStatus("OPEN")
      .build())
    // Remote open issue that no more exists => will be closed
    .mockServerIssue(org.sonar.batch.protocol.input.BatchInput.ServerIssue.newBuilder().setKey("resolved")
      .setModuleKey("sample")
      .setPath("src/sample.xoo")
      .setRuleRepository("xoo")
      .setRuleKey("OneIssuePerFile")
      .setMsg("An issue that is no more detected")
      .setLine(2)
      .setSeverity(Severity.MAJOR)
      .setCreationDate(date("14/03/2004"))
      .setChecksum(DigestUtils.md5Hex("oldcode"))
      .setStatus("OPEN")
      .build())
    // Manual issue
    .mockServerIssue(org.sonar.batch.protocol.input.BatchInput.ServerIssue.newBuilder().setKey("manual")
      .setModuleKey("sample")
      .setPath("src/sample.xoo")
      .setRuleRepository("manual")
      .setRuleKey("MyManualIssue")
      .setLine(4)
      .setSeverity(Severity.MAJOR)
      .setCreationDate(date("14/03/2004"))
      .setChecksum(DigestUtils.md5Hex("lines"))
      .setStatus("OPEN")
      .build())
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
  public void testIssueTrackingIncrementalMode() throws Exception {
    File baseDir = temp.newFolder();
    File srcDir = new File(baseDir, "src");
    srcDir.mkdir();

    File xooFile = new File(srcDir, "sample.xoo");
    FileUtils.write(xooFile, SAMPLE_CONTENT + "\nmodification");

    TaskResult result = tester.newTask()
      .properties(ImmutableMap.<String, String>builder()
        .put("sonar.task", "scan")
        .put("sonar.projectBaseDir", baseDir.getAbsolutePath())
        .put("sonar.projectKey", "sample")
        .put("sonar.projectName", "Foo Project")
        .put("sonar.projectVersion", "1.0-SNAPSHOT")
        .put("sonar.projectDescription", "Description of Foo Project")
        .put("sonar.sources", "src")
        .build())
      .start();

    int newIssues = 0;
    int openIssues = 0;
    int resolvedIssue = 0;
    for (Issue issue : result.issues()) {
      System.out.println(issue.key() + " " + issue.line() + " " + issue.status());
      if (issue.isNew()) {
        newIssues++;
      } else if (issue.resolution() != null) {
        resolvedIssue++;
      } else {
        openIssues++;
      }
    }
    assertThat(newIssues).isEqualTo(4);
    assertThat(openIssues).isEqualTo(2);
    assertThat(resolvedIssue).isEqualTo(1);
  }

}
