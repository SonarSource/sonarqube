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

import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.sonar.batch.protocol.Constants;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.batch.protocol.output.BatchReportWriter;

import java.io.File;
import java.io.IOException;

public class PersistSyntaxHighLightingStepTest extends BaseStepTest {

  private static final Integer FILE_REF = 3;

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  File reportDir;

  PersistSyntaxHighLightingStep step;

  @Before
  public void setup() throws Exception {
    reportDir = temp.newFolder();
    step = new PersistSyntaxHighLightingStep();
  }

  @Override
  protected ComputationStep step() throws IOException {
    return step;
  }

//  @Test
//  public void compute_no_symbol() throws Exception {
//    initReport();
//
//    step.execute(new ComputationContext(new BatchReportReader(reportDir),
//      ComponentTesting.newProjectDto("PROJECT_A")));
//
//    assertThat(step.getSyntaxHighlightingByLine()).isEmpty();
//  }
//
//  @Test
//  public void compute_syntax_highlighting() throws Exception {
//    BatchReportWriter writer = initReport();
//
//    writer.writeComponentSyntaxHighlighting(FILE_REF, newArrayList(
//      BatchReport.SyntaxHighlighting.HighlightingRule.newBuilder()
//        .setRange(BatchReport.Range.newBuilder()
//          .setStartLine(1)
//          .setStartOffset(3)
//          .setEndLine(1)
//          .setEndOffset(5)
//          .build())
//        .setType(Constants.HighlightingType.ANNOTATION)
//        .build(),
//      BatchReport.SyntaxHighlighting.HighlightingRule.newBuilder()
//        .setRange(BatchReport.Range.newBuilder()
//          .setStartLine(3)
//          .setStartOffset(6)
//          .setEndLine(3)
//          .setEndOffset(7)
//          .build())
//        .setType(Constants.HighlightingType.COMMENT)
//        .build())
//      );
//
//    step.execute(new ComputationContext(new BatchReportReader(reportDir), mock(ComponentDto.class)));
//
//    assertThat(step.getSyntaxHighlightingByLine()).hasSize(2);
//    assertThat(step.getSyntaxHighlightingByLine().get(1).toString()).isEqualTo("3,5,ANNOTATION");
//    assertThat(step.getSyntaxHighlightingByLine().get(3).toString()).isEqualTo("6,7,COMMENT");
//  }
//
//  @Test(expected = IllegalStateException.class)
//  public void fail_when_range_is_defined_on_different_line() throws Exception {
//    BatchReportWriter writer = initReport();
//
//    writer.writeComponentSyntaxHighlighting(FILE_REF, newArrayList(
//      BatchReport.SyntaxHighlighting.HighlightingRule.newBuilder()
//        .setRange(BatchReport.Range.newBuilder()
//          .setStartLine(1)
//          .setStartOffset(3)
//          .setEndLine(2)
//          .setEndOffset(2)
//          .build())
//        .setType(Constants.HighlightingType.ANNOTATION)
//        .build()));
//
//    step.execute(new ComputationContext(new BatchReportReader(reportDir), mock(ComponentDto.class)));
//  }

  private BatchReportWriter initReport() {
    BatchReportWriter writer = new BatchReportWriter(reportDir);
    writer.writeMetadata(BatchReport.Metadata.newBuilder()
      .setRootComponentRef(1)
      .setProjectKey("PROJECT_KEY")
      .setAnalysisDate(150000000L)
      .build());

    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(1)
      .setType(Constants.ComponentType.PROJECT)
      .setUuid("PROJECT_A")
      .addChildRef(2)
      .build());
    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(2)
      .setType(Constants.ComponentType.MODULE)
      .setUuid("BCDE")
      .addChildRef(FILE_REF)
      .build());
    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(FILE_REF)
      .setType(Constants.ComponentType.FILE)
      .setUuid("FILE_A")
      .build());
    return writer;
  }

}
