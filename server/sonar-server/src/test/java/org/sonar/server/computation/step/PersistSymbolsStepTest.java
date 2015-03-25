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
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.TemporaryFolder;
import org.sonar.batch.protocol.Constants;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.batch.protocol.output.BatchReportReader;
import org.sonar.batch.protocol.output.BatchReportWriter;
import org.sonar.core.component.ComponentDto;
import org.sonar.server.computation.ComputationContext;
import org.sonar.test.DbTests;

import java.io.File;
import java.io.IOException;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@Category(DbTests.class)
public class PersistSymbolsStepTest extends BaseStepTest {

  private static final Integer FILE_REF = 3;

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  File reportDir;

  PersistSymbolsStep step;

  @Before
  public void setup() throws Exception {
    reportDir = temp.newFolder();
    step = new PersistSymbolsStep();
  }

  @Override
  protected ComputationStep step() throws IOException {
    return step;
  }

  @Test
  public void compute_no_symbol() throws Exception {
    initReport();

    step.execute(new ComputationContext(new BatchReportReader(reportDir), mock(ComponentDto.class)));

    assertThat(step.getSymbolsByLine()).isEmpty();
  }

  @Test
  public void compute_one_symbol() throws Exception {
    BatchReportWriter writer = initReport();

    writer.writeComponentSymbols(FILE_REF, newArrayList(BatchReport.Symbols.Symbol.newBuilder()
      .setDeclaration(BatchReport.Range.newBuilder()
        .setStartLine(1)
        .setStartOffset(3)
        .setEndLine(1)
        .setEndOffset(5)
        .build())
      .addReference(BatchReport.Range.newBuilder()
        .setStartLine(10)
        .setStartOffset(15)
        .setEndLine(10)
        .setEndOffset(17)
        .build())
      .addReference(BatchReport.Range.newBuilder()
        .setStartLine(11)
        .setStartOffset(7)
        .setEndLine(11)
        .setEndOffset(9)
        .build())
      .build()));

    step.execute(new ComputationContext(new BatchReportReader(reportDir), mock(ComponentDto.class)));

    assertThat(step.getSymbolsByLine()).hasSize(3);
    assertThat(step.getSymbolsByLine().get(1).toString()).isEqualTo("3,5,1");
    assertThat(step.getSymbolsByLine().get(10).toString()).isEqualTo("15,17,1");
    assertThat(step.getSymbolsByLine().get(11).toString()).isEqualTo("7,9,1");
  }

  @Test
  public void compute_two_symbols() throws Exception {
    BatchReportWriter writer = initReport();

    writer.writeComponentSymbols(FILE_REF, newArrayList(
        BatchReport.Symbols.Symbol.newBuilder()
          .setDeclaration(BatchReport.Range.newBuilder()
            .setStartLine(1)
            .setEndLine(1)
            .setStartOffset(3)
            .setEndOffset(5)
            .build())
          .addReference(BatchReport.Range.newBuilder()
            .setStartLine(10)
            .setStartOffset(15)
            .setEndLine(10)
            .setEndOffset(16)
            .build())
          .build(),
        BatchReport.Symbols.Symbol.newBuilder()
          .setDeclaration(BatchReport.Range.newBuilder()
            .setStartLine(1)
            .setStartOffset(5)
            .setEndLine(1)
            .setEndOffset(6)
            .build())
          .addReference(BatchReport.Range.newBuilder()
            .setStartLine(10)
            .setStartOffset(9)
            .setEndLine(10)
            .setEndOffset(10)
            .build())
          .build())
    );

    step.execute(new ComputationContext(new BatchReportReader(reportDir), mock(ComponentDto.class)));

    assertThat(step.getSymbolsByLine()).hasSize(2);
    assertThat(step.getSymbolsByLine().get(1).toString()).isEqualTo("3,5,1;5,6,2");
    assertThat(step.getSymbolsByLine().get(10).toString()).isEqualTo("15,16,1;9,10,2");
  }

  @Test(expected = IllegalStateException.class)
  public void fail_when_symbol_is_defined_on_different_line() throws Exception {
    BatchReportWriter writer = initReport();

    writer.writeComponentSymbols(FILE_REF, newArrayList(BatchReport.Symbols.Symbol.newBuilder()
      .setDeclaration(BatchReport.Range.newBuilder()
        .setStartLine(1)
        .setStartOffset(3)
        .setEndLine(2)
        .setEndOffset(1)
        .build())
      .addReference(BatchReport.Range.newBuilder()
        .setStartLine(10)
        .setStartOffset(15)
        .setEndLine(10)
        .setEndOffset(17)
        .build())
      .build()));

    step.execute(new ComputationContext(new BatchReportReader(reportDir), mock(ComponentDto.class)));
  }

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
