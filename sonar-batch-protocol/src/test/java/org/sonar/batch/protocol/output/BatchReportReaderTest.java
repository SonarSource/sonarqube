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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.batch.protocol.output.BatchReport.Issues;
import org.sonar.batch.protocol.output.BatchReport.Metadata;

import java.io.File;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

public class BatchReportReaderTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  BatchReportReader sut;

  @Before
  public void setUp() throws Exception {
    sut = new BatchReportReader(temp.newFolder());
  }

  @Test
  public void create_dir_if_does_not_exist() throws Exception {
    File dir = temp.newFolder();

    initFiles(dir);

    sut = new BatchReportReader(dir);
    Metadata readMetadata = sut.readMetadata();
    assertThat(readMetadata.getAnalysisDate()).isEqualTo(15000000L);
    assertThat(readMetadata.getDeletedComponentsCount()).isEqualTo(1);
    assertThat(sut.readComponentIssues(1)).hasSize(1);
    assertThat(sut.readComponentIssues(200)).isEmpty();
    assertThat(sut.readComponent(1).getUuid()).isEqualTo("UUID_A");
    Issues deletedComponentIssues = sut.readDeletedComponentIssues(1);
    assertThat(deletedComponentIssues.getComponentUuid()).isEqualTo("compUuid");
    assertThat(deletedComponentIssues.getIssueList()).hasSize(1);
    assertThat(sut.readComponentMeasures(1)).hasSize(1);
    assertThat(sut.readComponentMeasures(1).get(0).getStringValue()).isEqualTo("value_a");
  }

  @Test(expected = IllegalStateException.class)
  public void fail_if_missing_metadata_file() throws Exception {
    sut.readMetadata();
  }

  @Test(expected = IllegalStateException.class)
   public void fail_if_missing_file_on_deleted_component() throws Exception {
    sut.readDeletedComponentIssues(666);
  }

  @Test(expected = IllegalStateException.class)
  public void fail_if_missing_file_on_component() throws Exception {
    sut.readComponent(666);
  }

  @Test
  public void empty_list_if_no_measure_found() throws Exception {
    assertThat(sut.readComponentMeasures(666)).isEmpty();
  }

  /**
   * no file if no issues
   */
  @Test
  public void empty_list_if_no_issue_found() throws Exception {
    assertThat(sut.readComponentIssues(666)).isEmpty();
  }

  private void initFiles(File dir) {
    BatchReportWriter writer = new BatchReportWriter(dir);

    BatchReport.Metadata.Builder metadata = BatchReport.Metadata.newBuilder()
      .setAnalysisDate(15000000L)
      .setProjectKey("PROJECT_A")
      .setRootComponentRef(1)
      .setDeletedComponentsCount(1);
    writer.writeMetadata(metadata.build());

    BatchReport.Component.Builder component = BatchReport.Component.newBuilder()
      .setRef(1)
      .setUuid("UUID_A");
    writer.writeComponent(component.build());

    BatchReport.Issue issue = BatchReport.Issue.newBuilder()
      .setUuid("ISSUE_A")
      .setLine(50)
      .build();

    writer.writeComponentIssues(1, Arrays.asList(issue));

    writer.writeDeletedComponentIssues(1, "compUuid", Arrays.asList(issue));

    BatchReport.Measure.Builder measure = BatchReport.Measure.newBuilder()
      .setStringValue("value_a");

    writer.writeComponentMeasures(1, Arrays.asList(measure.build()));
  }
}
