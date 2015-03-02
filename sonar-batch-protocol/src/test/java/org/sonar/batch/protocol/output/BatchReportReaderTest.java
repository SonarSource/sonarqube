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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

public class BatchReportReaderTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void create_dir_if_does_not_exist() throws Exception {
    File dir = temp.newFolder();

    initFiles(dir);

    BatchReportReader reader = new BatchReportReader(dir);
    assertThat(reader.readMetadata().getAnalysisDate()).isEqualTo(15000000L);
    assertThat(reader.readComponentIssues(1)).hasSize(1);
    assertThat(reader.readComponentIssues(200)).isEmpty();
    assertThat(reader.readComponent(1).getUuid()).isEqualTo("UUID_A");
    assertThat(reader.readComponent(200)).isNull();

  }

  private void initFiles(File dir) {
    BatchOutputWriter writer = new BatchOutputWriter(dir);

    BatchReport.Metadata.Builder metadata = BatchReport.Metadata.newBuilder()
      .setAnalysisDate(15000000L)
      .setProjectKey("PROJECT_A")
      .setRootComponentRef(1);
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
  }

  @Test
  public void readMetadata() throws Exception {

  }
}
