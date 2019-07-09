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
package org.sonar.scanner.genericcoverage;

import java.io.File;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.test.MutableTestCase;
import org.sonar.api.test.MutableTestPlan;
import org.sonar.api.utils.MessageException;
import org.sonar.scanner.deprecated.test.TestPlanBuilder;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.sensor.internal.SensorContextTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GenericTestExecutionReportParserTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private TestPlanBuilder testPlanBuilder;
  private DefaultInputFile fileWithBranches;
  private DefaultInputFile emptyFile;
  private SensorContextTester context;
  private MutableTestPlan testPlan;

  @Before
  public void before() {
    context = SensorContextTester.create(new File(""));
    fileWithBranches = setupFile("src/main/java/com/example/ClassWithBranches.java");
    emptyFile = setupFile("src/main/java/com/example/EmptyClass.java");
    testPlanBuilder = mock(TestPlanBuilder.class);

    MutableTestCase testCase = mockMutableTestCase();
    testPlan = mockMutableTestPlan(testCase);

    when(testPlanBuilder.loadPerspective(eq(MutableTestPlan.class), any(InputFile.class))).thenReturn(testPlan);
  }

  @Test
  public void ut_empty_file() throws Exception {
    addFileToFs(emptyFile);
    GenericTestExecutionReportParser parser = parseReportFile("unittest.xml");
    assertThat(parser.numberOfMatchedFiles()).isEqualTo(1);
    assertThat(parser.numberOfUnknownFiles()).isEqualTo(1);
    assertThat(parser.firstUnknownFiles()).hasSize(1);
  }

  @Test
  public void file_with_unittests() throws Exception {
    addFileToFs(fileWithBranches);
    GenericTestExecutionReportParser parser = parseReportFile("unittest2.xml");
    assertThat(parser.numberOfMatchedFiles()).isEqualTo(1);

    verify(testPlan).addTestCase("test1");
    verify(testPlan).addTestCase("test2");
    verify(testPlan).addTestCase("test3");
  }

  @Test(expected = MessageException.class)
  public void unittest_invalid_root_node_name() throws Exception {
    parseUnitTestReport("<mycoverage version=\"1\"></mycoverage>");
  }

  @Test(expected = MessageException.class)
  public void unittest_invalid_report_version() throws Exception {
    parseUnitTestReport("<unitTest version=\"2\"></unitTest>");
  }

  @Test(expected = MessageException.class)
  public void unittest_duration_in_testCase_should_be_a_number() throws Exception {
    addFileToFs(setupFile("file1"));
    parseUnitTestReport("<unitTest version=\"1\"><file path=\"file1\">"
      + "<testCase name=\"test1\" duration=\"aaa\"/></file></unitTest>");
  }

  @Test(expected = MessageException.class)
  public void unittest_failure_should_have_a_message() throws Exception {
    addFileToFs(setupFile("file1"));
    parseUnitTestReport("<unitTest version=\"1\"><file path=\"file1\">"
      + "<testCase name=\"test1\" duration=\"2\"><failure /></testCase></file></unitTest>");
  }

  @Test(expected = MessageException.class)
  public void unittest_error_should_have_a_message() throws Exception {
    addFileToFs(setupFile("file1"));
    parseUnitTestReport("<unitTest version=\"1\"><file path=\"file1\">"
      + "<testCase name=\"test1\" duration=\"2\"><error /></testCase></file></unitTest>");
  }

  @Test(expected = MessageException.class)
  public void unittest_skipped_should_have_a_message() throws Exception {
    addFileToFs(setupFile("file1"));
    parseUnitTestReport("<unitTest version=\"1\"><file path=\"file1\">"
      + "<testCase name=\"test1\" duration=\"2\"><skipped notmessage=\"\"/></testCase></file></unitTest>");
  }

  @Test(expected = MessageException.class)
  public void unittest_duration_in_testCase_should_not_be_negative() throws Exception {
    addFileToFs(setupFile("file1"));
    parseUnitTestReport("<unitTest version=\"1\"><file path=\"file1\">"
      + "<testCase name=\"test1\" duration=\"-5\"/></file></unitTest>");
  }

  private void addFileToFs(DefaultInputFile inputFile) {
    context.fileSystem().add(inputFile);
  }

  private GenericTestExecutionReportParser parseUnitTestReport(String string) throws Exception {
    GenericTestExecutionReportParser parser = new GenericTestExecutionReportParser(testPlanBuilder);
    File report = temp.newFile();
    FileUtils.write(report, string, StandardCharsets.UTF_8);
    parser.parse(report, context);
    return parser;
  }

  private GenericTestExecutionReportParser parseReportFile(String reportLocation) throws Exception {
    GenericTestExecutionReportParser parser = new GenericTestExecutionReportParser(testPlanBuilder);
    parser.parse(new File(this.getClass().getResource(reportLocation).toURI()), context);
    return parser;
  }

  private DefaultInputFile setupFile(String path) {
    return new TestInputFileBuilder(context.module().key(), path)
      .setLanguage("bla")
      .setType(InputFile.Type.TEST)
      .initMetadata("1\n2\n3\n4\n5\n6")
      .build();
  }

  private MutableTestPlan mockMutableTestPlan(MutableTestCase testCase) {
    MutableTestPlan testPlan = mock(MutableTestPlan.class);
    when(testPlan.addTestCase(anyString())).thenReturn(testCase);
    return testPlan;
  }

  private MutableTestCase mockMutableTestCase() {
    MutableTestCase testCase = mock(MutableTestCase.class);
    when(testCase.setDurationInMs(anyLong())).thenReturn(testCase);
    when(testCase.setStatus(any(org.sonar.api.test.TestCase.Status.class))).thenReturn(testCase);
    when(testCase.setMessage(anyString())).thenReturn(testCase);
    when(testCase.setStackTrace(anyString())).thenReturn(testCase);
    when(testCase.setType(anyString())).thenReturn(testCase);
    return testCase;
  }

}
