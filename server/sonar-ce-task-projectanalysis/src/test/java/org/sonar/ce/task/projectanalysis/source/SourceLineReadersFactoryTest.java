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
package org.sonar.ce.task.projectanalysis.source;

import org.junit.Rule;
import org.junit.Test;
import org.sonar.ce.task.projectanalysis.batch.BatchReportReaderRule;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.FileAttributes;
import org.sonar.ce.task.projectanalysis.component.ReportComponent;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolderRule;
import org.sonar.ce.task.projectanalysis.duplication.DuplicationRepositoryRule;
import org.sonar.ce.task.projectanalysis.scm.ScmInfoRepositoryRule;
import org.sonar.ce.task.projectanalysis.source.SourceLineReadersFactory.LineReadersImpl;
import org.sonar.scanner.protocol.output.ScannerReport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class SourceLineReadersFactoryTest {
  private static final int FILE1_REF = 3;
  private static final String PROJECT_UUID = "PROJECT";
  private static final String PROJECT_KEY = "PROJECT_KEY";
  private static final String FILE1_UUID = "FILE1";

  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule();
  @Rule
  public BatchReportReaderRule reportReader = new BatchReportReaderRule();
  @Rule
  public ScmInfoRepositoryRule scmInfoRepository = new ScmInfoRepositoryRule();
  @Rule
  public DuplicationRepositoryRule duplicationRepository = DuplicationRepositoryRule.create(treeRootHolder);

  private NewLinesRepository newLinesRepository = mock(NewLinesRepository.class);

  private SourceLineReadersFactory underTest = new SourceLineReadersFactory(reportReader, scmInfoRepository, duplicationRepository, newLinesRepository);

  @Test
  public void should_create_readers() {
    initBasicReport(10);
    LineReadersImpl lineReaders = (LineReadersImpl) underTest.getLineReaders(fileComponent());

    assertThat(lineReaders).isNotNull();
    assertThat(lineReaders.closeables).hasSize(3);
    assertThat(lineReaders.readers).hasSize(5);
  }

  private Component fileComponent() {
    return ReportComponent.builder(Component.Type.FILE, FILE1_REF).build();
  }

  private void initBasicReport(int numberOfLines) {
    treeRootHolder.setRoot(ReportComponent.builder(Component.Type.PROJECT, 1).setUuid(PROJECT_UUID).setKey(PROJECT_KEY).addChildren(
      ReportComponent.builder(Component.Type.DIRECTORY, 2).setUuid("MODULE").setKey("MODULE_KEY").addChildren(
        ReportComponent.builder(Component.Type.FILE, FILE1_REF).setUuid(FILE1_UUID).setKey("MODULE_KEY:src/Foo.java")
          .setFileAttributes(new FileAttributes(false, null, numberOfLines)).build())
        .build())
      .build());

    reportReader.putComponent(ScannerReport.Component.newBuilder()
      .setRef(1)
      .setType(ScannerReport.Component.ComponentType.PROJECT)
      .addChildRef(2)
      .build());
    reportReader.putComponent(ScannerReport.Component.newBuilder()
      .setRef(2)
      .setType(ScannerReport.Component.ComponentType.MODULE)
      .addChildRef(FILE1_REF)
      .build());
    reportReader.putComponent(ScannerReport.Component.newBuilder()
      .setRef(FILE1_REF)
      .setType(ScannerReport.Component.ComponentType.FILE)
      .setLines(numberOfLines)
      .build());
  }

}
