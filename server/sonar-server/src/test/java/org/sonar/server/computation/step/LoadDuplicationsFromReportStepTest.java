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

import java.util.Arrays;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.server.computation.batch.BatchReportReaderRule;
import org.sonar.server.computation.batch.TreeRootHolderRule;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.duplication.Duplicate;
import org.sonar.server.computation.duplication.Duplication;
import org.sonar.server.computation.duplication.DuplicationRepositoryRule;
import org.sonar.server.computation.duplication.InProjectDuplicate;
import org.sonar.server.computation.duplication.InnerDuplicate;
import org.sonar.server.computation.duplication.TextBlock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.server.computation.component.Component.Type.FILE;
import static org.sonar.server.computation.component.Component.Type.PROJECT;
import static org.sonar.server.computation.component.ReportComponent.builder;

public class LoadDuplicationsFromReportStepTest {
  private static final int LINE = 2;
  private static final int OTHER_LINE = 300;
  private static final int ROOT_REF = 1;
  private static final int FILE_1_REF = 11;
  private static final int FILE_2_REF = 12;

  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule().setRoot(
    builder(PROJECT, ROOT_REF)
      .addChildren(
        builder(FILE, FILE_1_REF).build(),
        builder(FILE, FILE_2_REF).build()
      )
      .build()
    );
  @Rule
  public BatchReportReaderRule reportReader = new BatchReportReaderRule();
  @Rule
  public DuplicationRepositoryRule duplicationRepository = DuplicationRepositoryRule.create(treeRootHolder);
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private LoadDuplicationsFromReportStep underTest = new LoadDuplicationsFromReportStep(treeRootHolder, reportReader, duplicationRepository);

  @Test
  public void verify_description() {
    assertThat(underTest.getDescription()).isEqualTo("Load inner file and in project duplications");
  }

  @Test
  public void loads_no_duplications_if_reader_has_no_duplication() {
    underTest.execute();

    assertNoDuplication(FILE_1_REF);
  }

  @Test
  public void loads_duplication_without_otherFileRef_as_inner_duplication() {
    reportReader.putDuplications(FILE_2_REF, createDuplication(singleLineTextRange(LINE), createInnerDuplicate(LINE + 1)));

    underTest.execute();

    assertNoDuplication(FILE_1_REF);
    assertDuplications(FILE_2_REF, singleLineTextBlock(LINE), new InnerDuplicate(singleLineTextBlock(LINE + 1)));
  }

  @Test
  public void loads_duplication_with_otherFileRef_as_inProject_duplication() {
    reportReader.putDuplications(FILE_1_REF, createDuplication(singleLineTextRange(LINE), createInProjectDuplicate(FILE_2_REF, LINE + 1)));

    underTest.execute();

    assertDuplications(FILE_1_REF, singleLineTextBlock(LINE), new InProjectDuplicate(treeRootHolder.getComponentByRef(FILE_2_REF), singleLineTextBlock(LINE + 1)));
    assertNoDuplication(FILE_2_REF);
  }

  @Test
  public void loads_multiple_duplications_with_multiple_duplicates() {
    reportReader.putDuplications(
      FILE_2_REF,
      createDuplication(
        singleLineTextRange(LINE),
        createInnerDuplicate(LINE + 1), createInnerDuplicate(LINE + 2), createInProjectDuplicate(FILE_1_REF, LINE), createInProjectDuplicate(FILE_1_REF, LINE + 10)),
      createDuplication(
        singleLineTextRange(OTHER_LINE),
        createInProjectDuplicate(FILE_1_REF, OTHER_LINE)),
      createDuplication(
        singleLineTextRange(OTHER_LINE + 80),
        createInnerDuplicate(LINE), createInnerDuplicate(LINE + 10))
      );

    underTest.execute();

    Component file1Component = treeRootHolder.getComponentByRef(FILE_1_REF);
    assertThat(duplicationRepository.getDuplications(FILE_2_REF)).containsOnly(
      duplication(
        singleLineTextBlock(LINE),
        new InnerDuplicate(singleLineTextBlock(LINE + 1)), new InnerDuplicate(singleLineTextBlock(LINE + 2)), new InProjectDuplicate(file1Component, singleLineTextBlock(LINE)),
        new InProjectDuplicate(file1Component, singleLineTextBlock(LINE + 10))),
      duplication(
        singleLineTextBlock(OTHER_LINE),
        new InProjectDuplicate(file1Component, singleLineTextBlock(OTHER_LINE))
      ),
      duplication(
        singleLineTextBlock(OTHER_LINE + 80),
        new InnerDuplicate(singleLineTextBlock(LINE)), new InnerDuplicate(singleLineTextBlock(LINE + 10))
      )
      );
  }

  @Test
  public void loads_duplication_with_otherFileRef_throws_IAE_if_component_does_not_exist() {
    int line = 2;
    reportReader.putDuplications(FILE_1_REF, createDuplication(singleLineTextRange(line), createInProjectDuplicate(666, line + 1)));

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Component with ref '666' hasn't been found");

    underTest.execute();
  }

  @Test
  public void loads_duplication_with_otherFileRef_throws_IAE_if_references_itself() {
    int line = 2;
    reportReader.putDuplications(FILE_1_REF, createDuplication(singleLineTextRange(line), createInProjectDuplicate(FILE_1_REF, line + 1)));

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("file and otherFile Components can not be the same");

    underTest.execute();
  }

  private void assertDuplications(int fileRef, TextBlock original, Duplicate... duplicates) {
    assertThat(duplicationRepository.getDuplications(fileRef)).containsExactly(duplication(original, duplicates));
  }

  private static Duplication duplication(TextBlock original, Duplicate... duplicates) {
    return new Duplication(original, Arrays.asList(duplicates));
  }

  private TextBlock singleLineTextBlock(int line) {
    return new TextBlock(line, line);
  }

  private static BatchReport.Duplication createDuplication(BatchReport.TextRange original, BatchReport.Duplicate... duplicates) {
    BatchReport.Duplication.Builder builder = BatchReport.Duplication.newBuilder()
      .setOriginPosition(original);
    for (BatchReport.Duplicate duplicate : duplicates) {
      builder.addDuplicate(duplicate);
    }
    return builder.build();
  }

  private static BatchReport.Duplicate createInnerDuplicate(int line) {
    return BatchReport.Duplicate.newBuilder()
      .setRange(singleLineTextRange(line))
      .build();
  }

  private static BatchReport.Duplicate createInProjectDuplicate(int componentRef, int line) {
    return BatchReport.Duplicate.newBuilder()
      .setOtherFileRef(componentRef)
      .setRange(singleLineTextRange(line))
      .build();
  }

  private static BatchReport.TextRange singleLineTextRange(int line) {
    return BatchReport.TextRange.newBuilder()
      .setStartLine(line)
      .setEndLine(line)
      .build();
  }

  private void assertNoDuplication(int fileRef) {
    assertThat(duplicationRepository.getDuplications(fileRef)).isEmpty();
  }
}
