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
package org.sonar.scanner.mediumtest.tests;

import com.google.common.collect.ImmutableMap;
import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.scanner.mediumtest.ScannerMediumTester;
import org.sonar.scanner.mediumtest.TaskResult;
import org.sonar.scanner.protocol.output.ScannerReport.Test.TestStatus;
import org.sonar.xoo.XooPlugin;

import static org.assertj.core.api.Assertions.assertThat;

public class TestExecutionMediumTest {

  @org.junit.Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Rule
  public ScannerMediumTester tester = new ScannerMediumTester()
    .registerPlugin("xoo", new XooPlugin())
    .addDefaultQProfile("xoo", "Sonar Way");

  @Test
  public void unitTests() throws IOException {

    File baseDir = temp.getRoot();
    File srcDir = new File(baseDir, "src");
    srcDir.mkdir();
    File testDir = new File(baseDir, "test");
    testDir.mkdir();

    File xooFile = new File(srcDir, "sample.xoo");
    FileUtils.write(xooFile, "foo");

    File xooTestFile = new File(testDir, "sampleTest.xoo");
    FileUtils.write(xooTestFile, "failure\nerror\nok\nskipped");

    File xooTestExecutionFile = new File(testDir, "sampleTest.xoo.test");
    FileUtils.write(xooTestExecutionFile, "skipped::::SKIPPED:UNIT\n" +
      "failure:2:Failure::FAILURE:UNIT\n" +
      "error:2:Error:The stack:ERROR:UNIT\n" +
      "success:4:::OK:INTEGRATION");

    TaskResult result = tester.newTask()
      .properties(ImmutableMap.<String, String>builder()
        .put("sonar.task", "scan")
        .put("sonar.projectBaseDir", baseDir.getAbsolutePath())
        .put("sonar.projectKey", "com.foo.project")
        .put("sonar.projectName", "Foo Project")
        .put("sonar.projectVersion", "1.0-SNAPSHOT")
        .put("sonar.projectDescription", "Description of Foo Project")
        .put("sonar.sources", "src")
        .put("sonar.tests", "test")
        .build())
      .execute();

    InputFile file = result.inputFile("test/sampleTest.xoo");
    org.sonar.scanner.protocol.output.ScannerReport.Test success = result.firstTestExecutionForName(file, "success");
    assertThat(success.getDurationInMs()).isEqualTo(4);
    assertThat(success.getStatus()).isEqualTo(TestStatus.OK);

    org.sonar.scanner.protocol.output.ScannerReport.Test error = result.firstTestExecutionForName(file, "error");
    assertThat(error.getDurationInMs()).isEqualTo(2);
    assertThat(error.getStatus()).isEqualTo(TestStatus.ERROR);
    assertThat(error.getMsg()).isEqualTo("Error");
    assertThat(error.getStacktrace()).isEqualTo("The stack");
  }

}
