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
package org.sonar.batch.mediumtest.test;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestName;
import org.sonar.batch.mediumtest.BatchMediumTester;
import org.sonar.batch.mediumtest.TaskResult;
import org.sonar.xoo.XooPlugin;

import java.io.File;
import java.io.IOException;

public class TestMediumTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Rule
  public TestName testName = new TestName();

  public BatchMediumTester tester = BatchMediumTester.builder()
    .registerPlugin("xoo", new XooPlugin())
    .addDefaultQProfile("xoo", "Sonar Way")
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
  public void populateTestCaseOnTempProject() throws IOException {

    File baseDir = temp.newFolder();
    File srcDir = new File(baseDir, "src");
    srcDir.mkdir();
    File testDir = new File(baseDir, "test");
    testDir.mkdir();

    File xooTestFile = new File(testDir, "sampleTest.xoo");
    File xooTestPlanFile = new File(testDir, "sampleTest.xoo.testplan");
    FileUtils.write(xooTestFile, "Sample test xoo\ncontent");
    FileUtils.write(xooTestPlanFile, "test1:UNIT:OK:::\ntest2:INTEGRATION:ERROR:Assertion failure:A very long stack:12");

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
      .start();

    // assertThat(result.testCasesFor(new DefaultInputFile("com.foo.project", "test/sampleTest.xoo"))).hasSize(2);
  }

  @Test
  public void populateTestCaseAndCoveragePerTestOnTempProject() throws IOException {

    File baseDir = temp.newFolder();
    File srcDir = new File(baseDir, "src");
    srcDir.mkdir();
    File testDir = new File(baseDir, "test");
    testDir.mkdir();

    File xooMainFile = new File(srcDir, "sample.xoo");
    File xooTestFile = new File(testDir, "sampleTest.xoo");
    File xooTestPlanFile = new File(testDir, "sampleTest.xoo.testplan");
    File xooTestCoverageFile = new File(testDir, "sampleTest.xoo.coveragePerTest");
    FileUtils.write(xooMainFile, "Sample xoo\ncontent");
    FileUtils.write(xooTestFile, "Sample test xoo\ncontent");
    FileUtils.write(xooTestPlanFile, "test1:UNIT:OK:::3\ntest2:INTEGRATION:ERROR:Assertion failure:A very long stack:12");
    FileUtils.write(xooTestCoverageFile, "test1:src/sample.xoo:1,2,3,8,9,10\ntest2:src/sample.xoo:3,4");

    // TaskResult result = tester.newTask()
    // .properties(ImmutableMap.<String, String>builder()
    // .put("sonar.task", "scan")
    // .put("sonar.projectBaseDir", baseDir.getAbsolutePath())
    // .put("sonar.projectKey", "com.foo.project")
    // .put("sonar.projectName", "Foo Project")
    // .put("sonar.projectVersion", "1.0-SNAPSHOT")
    // .put("sonar.projectDescription", "Description of Foo Project")
    // .put("sonar.sources", "src")
    // .put("sonar.tests", "test")
    // .build())
    // .start();
    //
    // assertThat(result.coveragePerTest(new DefaultInputFile("com.foo.project", "test/sampleTest.xoo"), "test1", new
    // DefaultInputFile("com.foo.project", "src/sample.xoo")))
    // .containsExactly(1, 2, 3, 8, 9, 10);
    // assertThat(result.coveragePerTest(new DefaultInputFile("com.foo.project", "test/sampleTest.xoo"), "test2", new
    // DefaultInputFile("com.foo.project", "src/sample.xoo")))
    // .containsExactly(3, 4);
  }
}
