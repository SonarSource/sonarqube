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
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.scanner.mediumtest.ScannerMediumTester;
import org.sonar.scanner.mediumtest.TaskResult;
import org.sonar.xoo.XooPlugin;

import static org.assertj.core.api.Assertions.assertThat;

public class CoveragePerTestMediumTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Rule
  public ExpectedException exception = ExpectedException.none();

  @Rule
  public ScannerMediumTester tester = new ScannerMediumTester()
    .registerPlugin("xoo", new XooPlugin())
    .addDefaultQProfile("xoo", "Sonar Way");

  @Test
  // SONAR-6183
  public void invalidCoverage() throws IOException {
    File baseDir = createTestFiles();
    File srcDir = new File(baseDir, "src");

    File coverageFile = new File(srcDir, "sample.xoo.coverage");
    FileUtils.write(coverageFile, "0:2\n");

    exception.expect(IllegalStateException.class);
    exception.expectMessage("Error processing line 1 of file");
    exception.expectCause(new TypeSafeMatcher<Throwable>() {

      @Override
      public void describeTo(Description description) {
        // nothing to do
      }

      @Override
      protected boolean matchesSafely(Throwable item) {
        return item.getMessage().contains("Line number must be strictly positive");
      }
    });
    runTask(baseDir);

  }

  @Test
  public void coveragePerTestInReport() throws IOException {
    File baseDir = createTestFiles();
    File testDir = new File(baseDir, "test");

    File xooTestExecutionFile = new File(testDir, "sampleTest.xoo.test");
    FileUtils.write(xooTestExecutionFile, "some test:4:::OK:UNIT\n" +
      "another test:10:::OK:UNIT\n" +
      "test without coverage:10:::OK:UNIT\n");

    File xooCoveragePerTestFile = new File(testDir, "sampleTest.xoo.testcoverage");
    FileUtils.write(xooCoveragePerTestFile, "some test;src/sample.xoo,10,11;src/sample2.xoo,1,2\n" +
      "another test;src/sample.xoo,10,20\n");

    TaskResult result = runTask(baseDir);

    InputFile file = result.inputFile("test/sampleTest.xoo");
    org.sonar.scanner.protocol.output.ScannerReport.CoverageDetail someTest = result.coveragePerTestFor(file, "some test");
    assertThat(someTest.getCoveredFileList()).hasSize(2);
    assertThat(someTest.getCoveredFile(0).getFileRef()).isGreaterThan(0);
    assertThat(someTest.getCoveredFile(0).getCoveredLineList()).containsExactly(10, 11);
    assertThat(someTest.getCoveredFile(1).getFileRef()).isGreaterThan(0);
    assertThat(someTest.getCoveredFile(1).getCoveredLineList()).containsExactly(1, 2);

    org.sonar.scanner.protocol.output.ScannerReport.CoverageDetail anotherTest = result.coveragePerTestFor(file, "another test");
    assertThat(anotherTest.getCoveredFileList()).hasSize(1);
    assertThat(anotherTest.getCoveredFile(0).getFileRef()).isGreaterThan(0);
    assertThat(anotherTest.getCoveredFile(0).getCoveredLineList()).containsExactly(10, 20);
  }

  private TaskResult runTask(File baseDir) {
    return tester.newTask()
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
  }

  private File createTestFiles() throws IOException {
    File baseDir = temp.getRoot();
    File srcDir = new File(baseDir, "src");
    srcDir.mkdir();
    File testDir = new File(baseDir, "test");
    testDir.mkdir();

    File xooFile = new File(srcDir, "sample.xoo");
    FileUtils.write(xooFile, "foo");

    File xooFile2 = new File(srcDir, "sample2.xoo");
    FileUtils.write(xooFile2, "foo");

    File xooTestFile = new File(testDir, "sampleTest.xoo");
    FileUtils.write(xooTestFile, "failure\nerror\nok\nskipped");

    File xooTestFile2 = new File(testDir, "sample2Test.xoo");
    FileUtils.write(xooTestFile2, "test file tests");

    return baseDir;
  }

}
