/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.server.computation.step;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.scanner.protocol.output.ScannerReport;
import org.sonar.server.computation.analysis.AnalysisMetadataHolderRule;
import org.sonar.server.computation.batch.BatchReportReaderRule;
import org.sonar.server.computation.batch.TreeRootHolderRule;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.component.ReportComponent;
import org.sonar.server.computation.duplication.CrossProjectDuplicationStatusHolder;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PersistCrossProjectDuplicationIndexStepTest {

  private static final int FILE_REF = 2;
  private static final Component FILE = ReportComponent.builder(Component.Type.FILE, FILE_REF).build();

  private static final Component PROJECT = ReportComponent.builder(Component.Type.PROJECT, 1)
    .addChildren(FILE)
    .build();

  private static final ScannerReport.CpdTextBlock CPD_TEXT_BLOCK = ScannerReport.CpdTextBlock.newBuilder()
    .setHash("a8998353e96320ec")
    .setStartLine(30)
    .setEndLine(45)
    .build();
  private static final String ANALYSIS_UUID = "analysis uuid";

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);
  @Rule
  public BatchReportReaderRule reportReader = new BatchReportReaderRule();
  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule().setRoot(PROJECT);
  @Rule
  public AnalysisMetadataHolderRule analysisMetadataHolder = new AnalysisMetadataHolderRule();

  CrossProjectDuplicationStatusHolder crossProjectDuplicationStatusHolder = mock(CrossProjectDuplicationStatusHolder.class);

  DbClient dbClient = dbTester.getDbClient();

  ComputationStep underTest = new PersistCrossProjectDuplicationIndexStep(crossProjectDuplicationStatusHolder, dbClient, treeRootHolder, analysisMetadataHolder, reportReader);

  @Before
  public void setUp() throws Exception {
    analysisMetadataHolder.setUuid(ANALYSIS_UUID);
  }

  @Test
  public void persist_cpd_text_block() throws Exception {
    when(crossProjectDuplicationStatusHolder.isEnabled()).thenReturn(true);
    reportReader.putDuplicationBlocks(FILE_REF, singletonList(CPD_TEXT_BLOCK));

    underTest.execute();

    Map<String, Object> dto = dbTester.selectFirst("select HASH, START_LINE, END_LINE, INDEX_IN_FILE, COMPONENT_UUID, ANALYSIS_UUID from duplications_index");
    assertThat(dto.get("HASH")).isEqualTo(CPD_TEXT_BLOCK.getHash());
    assertThat(dto.get("START_LINE")).isEqualTo(30L);
    assertThat(dto.get("END_LINE")).isEqualTo(45L);
    assertThat(dto.get("INDEX_IN_FILE")).isEqualTo(0L);
    assertThat(dto.get("COMPONENT_UUID")).isEqualTo(FILE.getUuid());
    assertThat(dto.get("ANALYSIS_UUID")).isEqualTo(ANALYSIS_UUID);
  }

  @Test
  public void persist_many_cpd_text_blocks() throws Exception {
    when(crossProjectDuplicationStatusHolder.isEnabled()).thenReturn(true);
    reportReader.putDuplicationBlocks(FILE_REF, Arrays.asList(
      CPD_TEXT_BLOCK,
      ScannerReport.CpdTextBlock.newBuilder()
        .setHash("b1234353e96320ff")
        .setStartLine(20)
        .setEndLine(15)
        .build()));

    underTest.execute();

    List<Map<String, Object>> dtos = dbTester.select("select HASH, START_LINE, END_LINE, INDEX_IN_FILE, COMPONENT_UUID, ANALYSIS_UUID from duplications_index");
    assertThat(dtos).extracting("HASH").containsOnly(CPD_TEXT_BLOCK.getHash(), "b1234353e96320ff");
    assertThat(dtos).extracting("START_LINE").containsOnly(30L, 20L);
    assertThat(dtos).extracting("END_LINE").containsOnly(45L, 15L);
    assertThat(dtos).extracting("INDEX_IN_FILE").containsOnly(0L, 1L);
    assertThat(dtos).extracting("COMPONENT_UUID").containsOnly(FILE.getUuid());
    assertThat(dtos).extracting("ANALYSIS_UUID").containsOnly(ANALYSIS_UUID);
  }

  @Test
  public void nothing_to_persist_when_no_cpd_text_blocks_in_report() throws Exception {
    when(crossProjectDuplicationStatusHolder.isEnabled()).thenReturn(true);
    reportReader.putDuplicationBlocks(FILE_REF, Collections.<ScannerReport.CpdTextBlock>emptyList());

    underTest.execute();

    assertThat(dbTester.countRowsOfTable("duplications_index")).isEqualTo(0);
  }

  @Test
  public void nothing_to_do_when_cross_project_duplication_is_disabled() throws Exception {
    when(crossProjectDuplicationStatusHolder.isEnabled()).thenReturn(false);
    reportReader.putDuplicationBlocks(FILE_REF, singletonList(CPD_TEXT_BLOCK));

    underTest.execute();

    assertThat(dbTester.countRowsOfTable("duplications_index")).isEqualTo(0);
  }

}
