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
package org.sonar.server.computation.step;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.batch.protocol.Constants;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.batch.protocol.output.BatchReportReader;
import org.sonar.batch.protocol.output.BatchReportWriter;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.persistence.DbTester;
import org.sonar.server.computation.ComputationContext;
import org.sonar.server.computation.issue.IssueComputation;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class ParseReportStepTest extends BaseStepTest {

  private static final List<BatchReport.Issue> ISSUES_ON_DELETED_COMPONENT = Arrays.asList(BatchReport.Issue.newBuilder()
    .setUuid("DELETED_ISSUE_UUID")
    .build());

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @ClassRule
  public static DbTester dbTester = new DbTester();

  IssueComputation issueComputation = mock(IssueComputation.class);
  ParseReportStep sut = new ParseReportStep(issueComputation);

  @Test
  public void extract_report_from_db_and_browse_components() throws Exception {
    File reportDir = generateReport();

    ComputationContext context = new ComputationContext(new BatchReportReader(reportDir), mock(ComponentDto.class));
    sut.execute(context);

    assertThat(context.getReportMetadata().getRootComponentRef()).isEqualTo(1);
    assertThat(context.getReportMetadata().getDeletedComponentsCount()).isEqualTo(1);

    // verify that all components are processed (currently only for issues)
    verify(issueComputation).processComponentIssues(context, Collections.<BatchReport.Issue>emptyList(), "PROJECT_UUID", 1);
    verify(issueComputation).processComponentIssues(context, Collections.<BatchReport.Issue>emptyList(), "FILE1_UUID", 2);
    verify(issueComputation).processComponentIssues(context, Collections.<BatchReport.Issue>emptyList(), "FILE2_UUID", 3);
    verify(issueComputation).processComponentIssues(context, ISSUES_ON_DELETED_COMPONENT, "DELETED_UUID", null);
    verify(issueComputation).afterReportProcessing();
  }

  private File generateReport() throws IOException {
    File dir = temp.newFolder();
    // project and 2 files
    BatchReportWriter writer = new BatchReportWriter(dir);
    writer.writeMetadata(BatchReport.Metadata.newBuilder()
      .setRootComponentRef(1)
      .setProjectKey("PROJECT_KEY")
      .setAnalysisDate(150000000L)
      .setDeletedComponentsCount(1)
      .build());

    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(1)
      .setType(Constants.ComponentType.PROJECT)
      .setUuid("PROJECT_UUID")
      .addChildRef(2)
      .addChildRef(3)
      .build());
    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(2)
      .setType(Constants.ComponentType.FILE)
      .setUuid("FILE1_UUID")
      .build());
    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(3)
      .setType(Constants.ComponentType.FILE)
      .setUuid("FILE2_UUID")
      .build());

    // deleted components
    writer.writeDeletedComponentIssues(1, "DELETED_UUID", ISSUES_ON_DELETED_COMPONENT);
    return dir;
  }

  @Override
  protected ComputationStep step() {
    return sut;
  }
}
