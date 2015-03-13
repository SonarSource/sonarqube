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
package org.sonar.batch.protocol.output;

import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.batch.protocol.Constants;
import org.sonar.batch.protocol.ProtobufUtil;

import java.io.File;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

public class BatchReportWriterTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void create_dir_if_does_not_exist() throws Exception {
    File dir = temp.newFolder();
    FileUtils.deleteQuietly(dir);

    new BatchReportWriter(dir);

    assertThat(dir).isDirectory().exists();
  }

  @Test
  public void write_metadata() throws Exception {
    File dir = temp.newFolder();
    BatchReportWriter writer = new BatchReportWriter(dir);
    BatchReport.Metadata.Builder metadata = BatchReport.Metadata.newBuilder()
      .setAnalysisDate(15000000L)
      .setProjectKey("PROJECT_A")
      .setRootComponentRef(1);
    writer.writeMetadata(metadata.build());

    BatchReport.Metadata read = ProtobufUtil.readFile(writer.getFileStructure().metadataFile(), BatchReport.Metadata.PARSER);
    assertThat(read.getAnalysisDate()).isEqualTo(15000000L);
    assertThat(read.getProjectKey()).isEqualTo("PROJECT_A");
    assertThat(read.getRootComponentRef()).isEqualTo(1);
  }

  @Test
  public void write_component() throws Exception {
    File dir = temp.newFolder();
    BatchReportWriter writer = new BatchReportWriter(dir);

    // no data yet
    assertThat(writer.hasComponentData(FileStructure.Domain.COMPONENT, 1)).isFalse();

    // write data
    BatchReport.Component.Builder component = BatchReport.Component.newBuilder()
      .setRef(1)
      .setLanguage("java")
      .setPath("src/Foo.java")
      .setUuid("UUID_A")
      .setType(Constants.ComponentType.FILE)
      .setIsTest(false)
      .addChildRef(5)
      .addChildRef(42);
    writer.writeComponent(component.build());

    assertThat(writer.hasComponentData(FileStructure.Domain.COMPONENT, 1)).isTrue();
    File file = writer.getFileStructure().fileFor(FileStructure.Domain.COMPONENT, 1);
    assertThat(file).exists().isFile();
    BatchReport.Component read = ProtobufUtil.readFile(file, BatchReport.Component.PARSER);
    assertThat(read.getRef()).isEqualTo(1);
    assertThat(read.getChildRefList()).containsOnly(5, 42);
    assertThat(read.hasName()).isFalse();
    assertThat(read.getIsTest()).isFalse();
    assertThat(read.getUuid()).isEqualTo("UUID_A");
  }

  @Test
  public void write_issues() throws Exception {
    File dir = temp.newFolder();
    BatchReportWriter writer = new BatchReportWriter(dir);

    // no data yet
    assertThat(writer.hasComponentData(FileStructure.Domain.ISSUES, 1)).isFalse();

    // write data
    BatchReport.Issue issue = BatchReport.Issue.newBuilder()
      .setUuid("ISSUE_A")
      .setLine(50)
      .setMsg("the message")
      .build();

    writer.writeComponentIssues(1, Arrays.asList(issue));

    assertThat(writer.hasComponentData(FileStructure.Domain.ISSUES, 1)).isTrue();
    File file = writer.getFileStructure().fileFor(FileStructure.Domain.ISSUES, 1);
    assertThat(file).exists().isFile();
    BatchReport.Issues read = ProtobufUtil.readFile(file, BatchReport.Issues.PARSER);
    assertThat(read.getComponentRef()).isEqualTo(1);
    assertThat(read.hasComponentUuid()).isFalse();
    assertThat(read.getIssueCount()).isEqualTo(1);
  }

  @Test
  public void write_measures() throws Exception {
    File dir = temp.newFolder();
    BatchReportWriter writer = new BatchReportWriter(dir);

    assertThat(writer.hasComponentData(FileStructure.Domain.MEASURES, 1)).isFalse();

    BatchReport.Measure measure = BatchReport.Measure.newBuilder()
      .setStringValue("text-value")
      .setDoubleValue(2.5d)
      .setValueType(Constants.MeasureValueType.DOUBLE)
      .setDescription("description")
      .build();

    writer.writeComponentMeasures(1, Arrays.asList(measure));

    assertThat(writer.hasComponentData(FileStructure.Domain.MEASURES, 1)).isTrue();
    File file = writer.getFileStructure().fileFor(FileStructure.Domain.MEASURES, 1);
    assertThat(file).exists().isFile();
    BatchReport.Measures measures = ProtobufUtil.readFile(file, BatchReport.Measures.PARSER);
    assertThat(measures.getComponentRef()).isEqualTo(1);
    assertThat(measures.getMeasureCount()).isEqualTo(1);
  }

  @Test
  public void write_issues_of_deleted_component() throws Exception {
    File dir = temp.newFolder();
    BatchReportWriter writer = new BatchReportWriter(dir);

    // no data yet
    assertThat(writer.hasComponentData(FileStructure.Domain.ISSUES_ON_DELETED, 1)).isFalse();

    // write data
    BatchReport.Issue issue = BatchReport.Issue.newBuilder()
      .setUuid("ISSUE_A")
      .setLine(50)
      .setMsg("the message")
      .build();

    writer.writeDeletedComponentIssues(1, "componentUuid", Arrays.asList(issue));

    assertThat(writer.hasComponentData(FileStructure.Domain.ISSUES_ON_DELETED, 1)).isTrue();
    File file = writer.getFileStructure().fileFor(FileStructure.Domain.ISSUES_ON_DELETED, 1);
    assertThat(file).exists().isFile();
    BatchReport.Issues read = ProtobufUtil.readFile(file, BatchReport.Issues.PARSER);
    assertThat(read.getComponentRef()).isEqualTo(1);
    assertThat(read.getComponentUuid()).isEqualTo("componentUuid");
    assertThat(read.getIssueCount()).isEqualTo(1);
  }
}
