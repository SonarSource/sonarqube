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
package org.sonar.ce.task.projectanalysis.step;

import java.util.Arrays;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolderRule;
import org.sonar.ce.task.projectanalysis.analysis.Branch;
import org.sonar.ce.task.projectanalysis.batch.BatchReportReaderRule;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolderRule;
import org.sonar.ce.task.projectanalysis.component.VisitException;
import org.sonar.ce.task.projectanalysis.duplication.DetailedTextBlock;
import org.sonar.ce.task.projectanalysis.duplication.Duplicate;
import org.sonar.ce.task.projectanalysis.duplication.Duplication;
import org.sonar.ce.task.projectanalysis.duplication.DuplicationRepositoryRule;
import org.sonar.ce.task.projectanalysis.duplication.InExtendedProjectDuplicate;
import org.sonar.ce.task.projectanalysis.duplication.InProjectDuplicate;
import org.sonar.ce.task.projectanalysis.duplication.InnerDuplicate;
import org.sonar.ce.task.projectanalysis.duplication.TextBlock;
import org.sonar.ce.task.step.TestComputationStepContext;
import org.sonar.db.component.BranchType;
import org.sonar.scanner.protocol.output.ScannerReport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.FILE;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.PROJECT;
import static org.sonar.ce.task.projectanalysis.component.ReportComponent.builder;
import static org.sonar.test.ExceptionCauseMatcher.hasType;

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
        // status has no effect except if it's a SLB or PR
        builder(FILE, FILE_2_REF).setStatus(Component.Status.SAME).build())
      .build());
  @Rule
  public BatchReportReaderRule reportReader = new BatchReportReaderRule();
  @Rule
  public DuplicationRepositoryRule duplicationRepository = DuplicationRepositoryRule.create(treeRootHolder);
  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public AnalysisMetadataHolderRule analysisMetadataHolder = new AnalysisMetadataHolderRule();

  private LoadDuplicationsFromReportStep underTest = new LoadDuplicationsFromReportStep(treeRootHolder, analysisMetadataHolder,
    reportReader, duplicationRepository);

  @Test
  public void verify_description() {
    assertThat(underTest.getDescription()).isEqualTo("Load duplications");
  }

  @Test
  public void loads_duplication_without_otherFileRef_as_inner_duplication() {
    reportReader.putDuplications(FILE_2_REF, createDuplication(singleLineTextRange(LINE), createInnerDuplicate(LINE + 1)));

    TestComputationStepContext context = new TestComputationStepContext();
    underTest.execute(context);

    assertNoDuplication(FILE_1_REF);
    assertDuplications(FILE_2_REF, singleLineDetailedTextBlock(1, LINE), new InnerDuplicate(singleLineTextBlock(LINE + 1)));
    assertNbOfDuplications(context, 1);
  }

  @Test
  public void loads_duplication_with_otherFileRef_as_inProject_duplication() {
    reportReader.putDuplications(FILE_1_REF, createDuplication(singleLineTextRange(LINE), createInProjectDuplicate(FILE_2_REF, LINE + 1)));

    TestComputationStepContext context = new TestComputationStepContext();
    underTest.execute(context);

    assertDuplications(FILE_1_REF, singleLineDetailedTextBlock(1, LINE), new InProjectDuplicate(treeRootHolder.getComponentByRef(FILE_2_REF), singleLineTextBlock(LINE + 1)));
    assertNoDuplication(FILE_2_REF);
    assertNbOfDuplications(context, 1);
  }

  @Test
  public void loads_duplication_with_otherFileRef_as_InExtendedProject_duplication() {
    Branch branch = mock(Branch.class);
    when(branch.getType()).thenReturn(BranchType.PULL_REQUEST);
    analysisMetadataHolder.setBranch(branch);

    reportReader.putDuplications(FILE_1_REF, createDuplication(singleLineTextRange(LINE), createInProjectDuplicate(FILE_2_REF, LINE + 1)));

    TestComputationStepContext context = new TestComputationStepContext();
    underTest.execute(context);

    assertDuplications(FILE_1_REF, singleLineDetailedTextBlock(1, LINE),
      new InExtendedProjectDuplicate(treeRootHolder.getComponentByRef(FILE_2_REF), singleLineTextBlock(LINE + 1)));
    assertNoDuplication(FILE_2_REF);
    assertNbOfDuplications(context, 1);
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
        createInnerDuplicate(LINE), createInnerDuplicate(LINE + 10)));

    TestComputationStepContext context = new TestComputationStepContext();
    underTest.execute(context);

    Component file1Component = treeRootHolder.getComponentByRef(FILE_1_REF);
    assertThat(duplicationRepository.getDuplications(FILE_2_REF)).containsOnly(
      duplication(
        singleLineDetailedTextBlock(1, LINE),
        new InnerDuplicate(singleLineTextBlock(LINE + 1)), new InnerDuplicate(singleLineTextBlock(LINE + 2)), new InProjectDuplicate(file1Component, singleLineTextBlock(LINE)),
        new InProjectDuplicate(file1Component, singleLineTextBlock(LINE + 10))),
      duplication(
        singleLineDetailedTextBlock(2, OTHER_LINE),
        new InProjectDuplicate(file1Component, singleLineTextBlock(OTHER_LINE))),
      duplication(
        singleLineDetailedTextBlock(3, OTHER_LINE + 80),
        new InnerDuplicate(singleLineTextBlock(LINE)), new InnerDuplicate(singleLineTextBlock(LINE + 10))));
    assertNbOfDuplications(context, 3);
  }

  @Test
  public void loads_never_consider_originals_from_batch_on_same_lines_as_the_equals() {
    reportReader.putDuplications(
      FILE_2_REF,
      createDuplication(
        singleLineTextRange(LINE),
        createInnerDuplicate(LINE + 1), createInnerDuplicate(LINE + 2), createInProjectDuplicate(FILE_1_REF, LINE + 2)),
      createDuplication(
        singleLineTextRange(LINE),
        createInnerDuplicate(LINE + 2), createInnerDuplicate(LINE + 3), createInProjectDuplicate(FILE_1_REF, LINE + 2)));

    TestComputationStepContext context = new TestComputationStepContext();
    underTest.execute(context);

    Component file1Component = treeRootHolder.getComponentByRef(FILE_1_REF);
    assertThat(duplicationRepository.getDuplications(FILE_2_REF)).containsOnly(
      duplication(
        singleLineDetailedTextBlock(1, LINE),
        new InnerDuplicate(singleLineTextBlock(LINE + 1)), new InnerDuplicate(singleLineTextBlock(LINE + 2)),
        new InProjectDuplicate(file1Component, singleLineTextBlock(LINE + 2))),
      duplication(
        singleLineDetailedTextBlock(2, LINE),
        new InnerDuplicate(singleLineTextBlock(LINE + 2)), new InnerDuplicate(singleLineTextBlock(LINE + 3)),
        new InProjectDuplicate(file1Component, singleLineTextBlock(LINE + 2))));

    assertNbOfDuplications(context, 2);
  }

  @Test
  public void loads_duplication_with_otherFileRef_throws_IAE_if_component_does_not_exist() {
    int line = 2;
    reportReader.putDuplications(FILE_1_REF, createDuplication(singleLineTextRange(line), createInProjectDuplicate(666, line + 1)));

    expectedException.expect(VisitException.class);
    expectedException.expectCause(hasType(IllegalArgumentException.class).andMessage("Component with ref '666' can't be found"));

    underTest.execute(new TestComputationStepContext());
  }

  @Test
  public void loads_duplication_with_otherFileRef_throws_IAE_if_references_itself() {
    int line = 2;
    reportReader.putDuplications(FILE_1_REF, createDuplication(singleLineTextRange(line), createInProjectDuplicate(FILE_1_REF, line + 1)));

    expectedException.expect(VisitException.class);
    expectedException.expectCause(hasType(IllegalArgumentException.class).andMessage("file and otherFile references can not be the same"));

    underTest.execute(new TestComputationStepContext());
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

  private DetailedTextBlock singleLineDetailedTextBlock(int id, int line) {
    return new DetailedTextBlock(id, line, line);
  }

  private static ScannerReport.Duplication createDuplication(ScannerReport.TextRange original, ScannerReport.Duplicate... duplicates) {
    ScannerReport.Duplication.Builder builder = ScannerReport.Duplication.newBuilder()
      .setOriginPosition(original);
    for (ScannerReport.Duplicate duplicate : duplicates) {
      builder.addDuplicate(duplicate);
    }
    return builder.build();
  }

  private static ScannerReport.Duplicate createInnerDuplicate(int line) {
    return ScannerReport.Duplicate.newBuilder()
      .setRange(singleLineTextRange(line))
      .build();
  }

  private static ScannerReport.Duplicate createInProjectDuplicate(int componentRef, int line) {
    return ScannerReport.Duplicate.newBuilder()
      .setOtherFileRef(componentRef)
      .setRange(singleLineTextRange(line))
      .build();
  }

  private static ScannerReport.TextRange singleLineTextRange(int line) {
    return ScannerReport.TextRange.newBuilder()
      .setStartLine(line)
      .setEndLine(line)
      .build();
  }

  private void assertNoDuplication(int fileRef) {
    assertThat(duplicationRepository.getDuplications(fileRef)).isEmpty();
  }

  private static void assertNbOfDuplications(TestComputationStepContext context, int expected) {
    context.getStatistics().assertValue("duplications", expected);
  }

}
