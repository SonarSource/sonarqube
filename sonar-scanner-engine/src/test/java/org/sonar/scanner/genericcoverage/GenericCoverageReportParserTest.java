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
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.utils.MessageException;

import static org.assertj.core.api.Assertions.assertThat;

public class GenericCoverageReportParserTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private DefaultInputFile fileWithBranches;
  private DefaultInputFile fileWithoutBranch;
  private DefaultInputFile emptyFile;
  private SensorContextTester context;

  @Before
  public void before() {
    context = SensorContextTester.create(new File(""));
    fileWithBranches = setupFile("src/main/java/com/example/ClassWithBranches.java");
    fileWithoutBranch = setupFile("src/main/java/com/example/ClassWithoutBranch.java");
    emptyFile = setupFile("src/main/java/com/example/EmptyClass.java");
  }

  @Test
  public void empty_file() throws Exception {
    addFileToFs(emptyFile);
    GenericCoverageReportParser parser = new GenericCoverageReportParser();
    parser.parse(new File(this.getClass().getResource("coverage.xml").toURI()), context);
    assertThat(parser.numberOfMatchedFiles()).isEqualTo(1);
    assertThat(parser.numberOfUnknownFiles()).isEqualTo(3);
    assertThat(parser.firstUnknownFiles()).hasSize(3);
  }

  @Test
  public void file_without_branch() throws Exception {
    addFileToFs(fileWithoutBranch);
    GenericCoverageReportParser parser = new GenericCoverageReportParser();
    parser.parse(new File(this.getClass().getResource("coverage.xml").toURI()), context);
    assertThat(parser.numberOfMatchedFiles()).isEqualTo(1);

    assertThat(context.lineHits(fileWithoutBranch.key(), 2)).isEqualTo(0);
    assertThat(context.lineHits(fileWithoutBranch.key(), 3)).isEqualTo(1);
    assertThat(context.lineHits(fileWithoutBranch.key(), 4)).isNull();
    assertThat(context.lineHits(fileWithoutBranch.key(), 5)).isEqualTo(1);
    assertThat(context.lineHits(fileWithoutBranch.key(), 6)).isEqualTo(0);
  }

  @Test
  public void file_with_branches() throws Exception {
    addFileToFs(fileWithBranches);
    GenericCoverageReportParser parser = new GenericCoverageReportParser();
    parser.parse(new File(this.getClass().getResource("coverage.xml").toURI()), context);
    assertThat(parser.numberOfMatchedFiles()).isEqualTo(1);

    assertThat(context.lineHits(fileWithBranches.key(), 3)).isEqualTo(1);
    assertThat(context.lineHits(fileWithBranches.key(), 4)).isEqualTo(1);

    assertThat(context.conditions(fileWithBranches.key(), 3)).isEqualTo(8);
    assertThat(context.conditions(fileWithBranches.key(), 4)).isEqualTo(2);

    assertThat(context.coveredConditions(fileWithBranches.key(), 3)).isEqualTo(5);
    assertThat(context.coveredConditions(fileWithBranches.key(), 4)).isEqualTo(0);
  }

  @Test(expected = MessageException.class)
  public void coverage_invalid_root_node_name() throws Exception {
    parseCoverageReport("<mycoverage version=\"1\"></mycoverage>");
  }

  @Test(expected = MessageException.class)
  public void coverage_invalid_report_version() throws Exception {
    parseCoverageReport("<coverage version=\"2\"></coverage>");
  }

  @Test(expected = MessageException.class)
  public void coverage_no_report_version() throws Exception {
    parseCoverageReport("<coverage></coverage>");
  }

  @Test(expected = MessageException.class)
  public void coverage_invalid_file_node_name() throws Exception {
    parseCoverageReport("<coverage version=\"1\"><xx></xx></coverage>");
  }

  @Test(expected = MessageException.class)
  public void unitTest_invalid_file_node_name() throws Exception {
    parseCoverageReport("<unitTest version=\"1\"><xx></xx></unitTest>");
  }

  @Test(expected = MessageException.class)
  public void coverage_missing_path_attribute() throws Exception {
    parseCoverageReport("<coverage version=\"1\"><file></file></coverage>");
  }

  @Test(expected = MessageException.class)
  public void unitTest_missing_path_attribute() throws Exception {
    parseCoverageReport("<unitTest version=\"1\"><file></file></unitTest>");
  }

  @Test(expected = MessageException.class)
  public void coverage_invalid_lineToCover_node_name() throws Exception {
    addFileToFs(setupFile("file1"));
    parseCoverageReport("<coverage version=\"1\"><file path=\"file1\"><xx/></file></coverage>");
  }

  @Test(expected = MessageException.class)
  public void coverage_missing_lineNumber_in_lineToCover() throws Exception {
    addFileToFs(setupFile("file1"));
    parseCoverageReport("<coverage version=\"1\"><file path=\"file1\"><lineToCover covered=\"true\"/></file></coverage>");
  }

  @Test(expected = MessageException.class)
  public void coverage_lineNumber_in_lineToCover_should_be_a_number() throws Exception {
    addFileToFs(setupFile("file1"));
    parseCoverageReport("<coverage version=\"1\"><file path=\"file1\"><lineToCover lineNumber=\"x\" covered=\"true\"/></file></coverage>");
  }

  @Test(expected = MessageException.class)
  public void coverage_lineNumber_in_lineToCover_should_be_positive() throws Exception {
    addFileToFs(setupFile("file1"));
    parseCoverageReport("<coverage version=\"1\"><file path=\"file1\"><lineToCover lineNumber=\"0\" covered=\"true\"/></file></coverage>");
  }

  @Test
  public void coverage_lineNumber_in_lineToCover_can_appear_several_times_for_same_file() throws Exception {
    addFileToFs(setupFile("file1"));
    parseCoverageReport("<coverage version=\"1\"><file path=\"file1\">"
      + "<lineToCover lineNumber=\"1\" covered=\"true\"/>"
      + "<lineToCover lineNumber=\"1\" covered=\"true\"/></file></coverage>");
  }

  @Test(expected = MessageException.class)
  public void coverage_missing_covered_in_lineToCover() throws Exception {
    addFileToFs(setupFile("file1"));
    parseCoverageReport("<coverage version=\"1\"><file path=\"file1\"><lineToCover lineNumber=\"3\"/></file></coverage>");
  }

  @Test(expected = MessageException.class)
  public void coverage_covered_in_lineToCover_should_be_a_boolean() throws Exception {
    addFileToFs(setupFile("file1"));
    parseCoverageReport("<coverage version=\"1\"><file path=\"file1\"><lineToCover lineNumber=\"3\" covered=\"x\"/></file></coverage>");
  }

  @Test(expected = MessageException.class)
  public void coverage_branchesToCover_in_lineToCover_should_be_a_number() throws Exception {
    addFileToFs(setupFile("file1"));
    parseCoverageReport("<coverage version=\"1\"><file path=\"file1\">"
      + "<lineToCover lineNumber=\"1\" covered=\"true\" branchesToCover=\"x\"/></file></coverage>");
  }

  @Test(expected = MessageException.class)
  public void coverage_branchesToCover_in_lineToCover_should_not_be_negative() throws Exception {
    addFileToFs(setupFile("file1"));
    parseCoverageReport("<coverage version=\"1\"><file path=\"file1\">"
      + "<lineToCover lineNumber=\"1\" covered=\"true\" branchesToCover=\"-1\"/></file></coverage>");
  }

  @Test(expected = MessageException.class)
  public void coverage_coveredBranches_in_lineToCover_should_be_a_number() throws Exception {
    addFileToFs(setupFile("file1"));
    parseCoverageReport("<coverage version=\"1\"><file path=\"file1\">"
      + "<lineToCover lineNumber=\"1\" covered=\"true\" branchesToCover=\"2\" coveredBranches=\"x\"/></file></coverage>");
  }

  @Test(expected = MessageException.class)
  public void coverage_coveredBranches_in_lineToCover_should_not_be_negative() throws Exception {
    addFileToFs(setupFile("file1"));
    parseCoverageReport("<coverage version=\"1\"><file path=\"file1\">"
      + "<lineToCover lineNumber=\"1\" covered=\"true\" branchesToCover=\"2\" coveredBranches=\"-1\"/></file></coverage>");
  }

  @Test(expected = MessageException.class)
  public void coverage_coveredBranches_should_not_be_greater_than_branchesToCover() throws Exception {
    addFileToFs(setupFile("file1"));
    parseCoverageReport("<coverage version=\"1\"><file path=\"file1\">"
      + "<lineToCover lineNumber=\"1\" covered=\"true\" branchesToCover=\"2\" coveredBranches=\"3\"/></file></coverage>");
  }

  @Test(expected = MessageException.class)
  public void testUnknownFile() throws Exception {
    parseCoverageReportFile("xxx.xml");
  }

  private void addFileToFs(DefaultInputFile inputFile) {
    context.fileSystem().add(inputFile);
  }

  private void parseCoverageReport(String string) throws Exception {
    File report = temp.newFile();
    FileUtils.write(report, string, StandardCharsets.UTF_8);
    new GenericCoverageReportParser().parse(report, context);
  }

  private void parseCoverageReportFile(String reportLocation) throws Exception {
    new GenericCoverageReportParser().parse(new File(reportLocation), context);
  }

  private DefaultInputFile setupFile(String path) {
    return new TestInputFileBuilder(context.module().key(), path)
      .setLanguage("bla")
      .setType(InputFile.Type.TEST)
      .initMetadata("1\n2\n3\n4\n5\n6")
      .build();
  }
}
