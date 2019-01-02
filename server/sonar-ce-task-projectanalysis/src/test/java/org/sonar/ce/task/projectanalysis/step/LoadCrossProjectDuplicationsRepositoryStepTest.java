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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.sonar.api.utils.System2;
import org.sonar.ce.task.projectanalysis.analysis.Analysis;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolderRule;
import org.sonar.ce.task.projectanalysis.batch.BatchReportReaderRule;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.FileAttributes;
import org.sonar.ce.task.projectanalysis.component.ReportComponent;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolderRule;
import org.sonar.ce.task.projectanalysis.duplication.CrossProjectDuplicationStatusHolder;
import org.sonar.ce.task.projectanalysis.duplication.IntegrateCrossProjectDuplications;
import org.sonar.ce.task.step.ComputationStep;
import org.sonar.ce.task.step.TestComputationStepContext;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.component.SnapshotTesting;
import org.sonar.db.duplication.DuplicationUnitDto;
import org.sonar.duplications.block.Block;
import org.sonar.duplications.block.ByteArray;
import org.sonar.scanner.protocol.output.ScannerReport;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.FILE;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.PROJECT;

public class LoadCrossProjectDuplicationsRepositoryStepTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private static final String XOO_LANGUAGE = "xoo";
  private static final int PROJECT_REF = 1;
  private static final int FILE_REF = 2;
  private static final String CURRENT_FILE_KEY = "FILE_KEY";

  private static final Component CURRENT_FILE = ReportComponent.builder(FILE, FILE_REF)
    .setKey(CURRENT_FILE_KEY)
    .setFileAttributes(new FileAttributes(false, XOO_LANGUAGE, 1))
    .build();

  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule().setRoot(
    ReportComponent.builder(PROJECT, PROJECT_REF)
      .addChildren(CURRENT_FILE).build());

  @Rule
  public BatchReportReaderRule batchReportReader = new BatchReportReaderRule();

  @Rule
  public AnalysisMetadataHolderRule analysisMetadataHolder = new AnalysisMetadataHolderRule();

  private CrossProjectDuplicationStatusHolder crossProjectDuplicationStatusHolder = mock(CrossProjectDuplicationStatusHolder.class);

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  private DbClient dbClient = dbTester.getDbClient();
  private DbSession dbSession = dbTester.getSession();
  private IntegrateCrossProjectDuplications integrateCrossProjectDuplications = mock(IntegrateCrossProjectDuplications.class);
  private Analysis baseProjectAnalysis;

  private ComputationStep underTest = new LoadCrossProjectDuplicationsRepositoryStep(treeRootHolder, batchReportReader, analysisMetadataHolder, crossProjectDuplicationStatusHolder,
    integrateCrossProjectDuplications, dbClient);

  @Before
  public void setUp() throws Exception {
    ComponentDto project = ComponentTesting.newPrivateProjectDto(dbTester.organizations().insert());
    dbClient.componentDao().insert(dbSession, project);
    SnapshotDto projectSnapshot = SnapshotTesting.newAnalysis(project);
    dbClient.snapshotDao().insert(dbSession, projectSnapshot);
    dbSession.commit();

    baseProjectAnalysis = new Analysis.Builder()
      .setId(projectSnapshot.getId())
      .setUuid(projectSnapshot.getUuid())
      .setCreatedAt(projectSnapshot.getCreatedAt())
      .build();
  }

  @Test
  public void call_compute_cpd_on_one_duplication() {
    when(crossProjectDuplicationStatusHolder.isEnabled()).thenReturn(true);
    analysisMetadataHolder.setBaseAnalysis(baseProjectAnalysis);

    ComponentDto otherProject = createProject("OTHER_PROJECT_KEY");
    SnapshotDto otherProjectSnapshot = createProjectSnapshot(otherProject);

    ComponentDto otherFile = createFile("OTHER_FILE_KEY", otherProject);

    String hash = "a8998353e96320ec";
    DuplicationUnitDto duplicate = new DuplicationUnitDto()
      .setHash(hash)
      .setStartLine(40)
      .setEndLine(55)
      .setIndexInFile(0)
      .setAnalysisUuid(otherProjectSnapshot.getUuid())
      .setComponentUuid(otherFile.uuid());
    dbClient.duplicationDao().insert(dbSession, duplicate);
    dbSession.commit();

    ScannerReport.CpdTextBlock originBlock = ScannerReport.CpdTextBlock.newBuilder()
      .setHash(hash)
      .setStartLine(30)
      .setEndLine(45)
      .setStartTokenIndex(0)
      .setEndTokenIndex(10)
      .build();
    batchReportReader.putDuplicationBlocks(FILE_REF, asList(originBlock));

    underTest.execute(new TestComputationStepContext());

    verify(integrateCrossProjectDuplications).computeCpd(CURRENT_FILE,
      asList(
        new Block.Builder()
          .setResourceId(CURRENT_FILE_KEY)
          .setBlockHash(new ByteArray(hash))
          .setIndexInFile(0)
          .setLines(originBlock.getStartLine(), originBlock.getEndLine())
          .setUnit(originBlock.getStartTokenIndex(), originBlock.getEndTokenIndex())
          .build()),
      asList(
        new Block.Builder()
          .setResourceId(otherFile.getDbKey())
          .setBlockHash(new ByteArray(hash))
          .setIndexInFile(duplicate.getIndexInFile())
          .setLines(duplicate.getStartLine(), duplicate.getEndLine())
          .build()));
  }

  @Test
  public void call_compute_cpd_on_many_duplication() {
    when(crossProjectDuplicationStatusHolder.isEnabled()).thenReturn(true);
    analysisMetadataHolder.setBaseAnalysis(baseProjectAnalysis);

    ComponentDto otherProject = createProject("OTHER_PROJECT_KEY");
    SnapshotDto otherProjectSnapshot = createProjectSnapshot(otherProject);

    ComponentDto otherFile = createFile("OTHER_FILE_KEY", otherProject);

    ScannerReport.CpdTextBlock originBlock1 = ScannerReport.CpdTextBlock.newBuilder()
      .setHash("a8998353e96320ec")
      .setStartLine(30)
      .setEndLine(45)
      .setStartTokenIndex(0)
      .setEndTokenIndex(10)
      .build();
    ScannerReport.CpdTextBlock originBlock2 = ScannerReport.CpdTextBlock.newBuilder()
      .setHash("b1234353e96320ff")
      .setStartLine(10)
      .setEndLine(25)
      .setStartTokenIndex(5)
      .setEndTokenIndex(15)
      .build();
    batchReportReader.putDuplicationBlocks(FILE_REF, asList(originBlock1, originBlock2));

    DuplicationUnitDto duplicate1 = new DuplicationUnitDto()
      .setHash(originBlock1.getHash())
      .setStartLine(40)
      .setEndLine(55)
      .setIndexInFile(0)
      .setAnalysisUuid(otherProjectSnapshot.getUuid())
      .setComponentUuid(otherFile.uuid());

    DuplicationUnitDto duplicate2 = new DuplicationUnitDto()
      .setHash(originBlock2.getHash())
      .setStartLine(20)
      .setEndLine(35)
      .setIndexInFile(1)
      .setAnalysisUuid(otherProjectSnapshot.getUuid())
      .setComponentUuid(otherFile.uuid());
    dbClient.duplicationDao().insert(dbSession, duplicate1);
    dbClient.duplicationDao().insert(dbSession, duplicate2);
    dbSession.commit();

    underTest.execute(new TestComputationStepContext());

    Class<ArrayList<Block>> listClass = (Class<ArrayList<Block>>) (Class) ArrayList.class;
    ArgumentCaptor<ArrayList<Block>> originBlocks = ArgumentCaptor.forClass(listClass);
    ArgumentCaptor<ArrayList<Block>> duplicationBlocks = ArgumentCaptor.forClass(listClass);

    verify(integrateCrossProjectDuplications).computeCpd(eq(CURRENT_FILE), originBlocks.capture(), duplicationBlocks.capture());

    Map<Integer, Block> originBlocksByIndex = blocksByIndexInFile(originBlocks.getValue());
    assertThat(originBlocksByIndex.get(0)).isEqualTo(
      new Block.Builder()
        .setResourceId(CURRENT_FILE_KEY)
        .setBlockHash(new ByteArray(originBlock1.getHash()))
        .setIndexInFile(0)
        .setLines(originBlock1.getStartLine(), originBlock1.getEndLine())
        .setUnit(originBlock1.getStartTokenIndex(), originBlock1.getEndTokenIndex())
        .build());
    assertThat(originBlocksByIndex.get(1)).isEqualTo(
      new Block.Builder()
        .setResourceId(CURRENT_FILE_KEY)
        .setBlockHash(new ByteArray(originBlock2.getHash()))
        .setIndexInFile(1)
        .setLines(originBlock2.getStartLine(), originBlock2.getEndLine())
        .setUnit(originBlock2.getStartTokenIndex(), originBlock2.getEndTokenIndex())
        .build());

    Map<Integer, Block> duplicationBlocksByIndex = blocksByIndexInFile(duplicationBlocks.getValue());
    assertThat(duplicationBlocksByIndex.get(0)).isEqualTo(
      new Block.Builder()
        .setResourceId(otherFile.getDbKey())
        .setBlockHash(new ByteArray(originBlock1.getHash()))
        .setIndexInFile(duplicate1.getIndexInFile())
        .setLines(duplicate1.getStartLine(), duplicate1.getEndLine())
        .build());
    assertThat(duplicationBlocksByIndex.get(1)).isEqualTo(
      new Block.Builder()
        .setResourceId(otherFile.getDbKey())
        .setBlockHash(new ByteArray(originBlock2.getHash()))
        .setIndexInFile(duplicate2.getIndexInFile())
        .setLines(duplicate2.getStartLine(), duplicate2.getEndLine())
        .build());
  }

  @Test
  public void nothing_to_do_when_cross_project_duplication_is_disabled() {
    when(crossProjectDuplicationStatusHolder.isEnabled()).thenReturn(false);
    analysisMetadataHolder.setBaseAnalysis(baseProjectAnalysis);

    ComponentDto otherProject = createProject("OTHER_PROJECT_KEY");
    SnapshotDto otherProjectSnapshot = createProjectSnapshot(otherProject);

    ComponentDto otherFIle = createFile("OTHER_FILE_KEY", otherProject);

    String hash = "a8998353e96320ec";
    DuplicationUnitDto duplicate = new DuplicationUnitDto()
      .setHash(hash)
      .setStartLine(40)
      .setEndLine(55)
      .setIndexInFile(0)
      .setAnalysisUuid(otherProjectSnapshot.getUuid())
      .setComponentUuid(otherFIle.uuid());
    dbClient.duplicationDao().insert(dbSession, duplicate);
    dbSession.commit();

    ScannerReport.CpdTextBlock originBlock = ScannerReport.CpdTextBlock.newBuilder()
      .setHash(hash)
      .setStartLine(30)
      .setEndLine(45)
      .setStartTokenIndex(0)
      .setEndTokenIndex(10)
      .build();
    batchReportReader.putDuplicationBlocks(FILE_REF, asList(originBlock));

    underTest.execute(new TestComputationStepContext());

    verifyZeroInteractions(integrateCrossProjectDuplications);
  }

  @Test
  public void nothing_to_do_when_no_cpd_text_blocks_found() {
    when(crossProjectDuplicationStatusHolder.isEnabled()).thenReturn(true);
    analysisMetadataHolder.setBaseAnalysis(baseProjectAnalysis);

    batchReportReader.putDuplicationBlocks(FILE_REF, Collections.emptyList());

    underTest.execute(new TestComputationStepContext());

    verifyZeroInteractions(integrateCrossProjectDuplications);
  }

  @Test
  public void nothing_to_do_when_cpd_text_blocks_exists_but_no_duplicated_found() {
    when(crossProjectDuplicationStatusHolder.isEnabled()).thenReturn(true);
    analysisMetadataHolder.setBaseAnalysis(baseProjectAnalysis);

    ScannerReport.CpdTextBlock originBlock = ScannerReport.CpdTextBlock.newBuilder()
      .setHash("a8998353e96320ec")
      .setStartLine(30)
      .setEndLine(45)
      .setStartTokenIndex(0)
      .setEndTokenIndex(10)
      .build();
    batchReportReader.putDuplicationBlocks(FILE_REF, asList(originBlock));

    underTest.execute(new TestComputationStepContext());

    verifyZeroInteractions(integrateCrossProjectDuplications);
  }

  private ComponentDto createProject(String projectKey) {
    ComponentDto project = ComponentTesting.newPrivateProjectDto(dbTester.organizations().insert()).setDbKey(projectKey);
    dbClient.componentDao().insert(dbSession, project);
    dbSession.commit();
    return project;
  }

  private SnapshotDto createProjectSnapshot(ComponentDto project) {
    SnapshotDto projectSnapshot = SnapshotTesting.newAnalysis(project);
    dbClient.snapshotDao().insert(dbSession, projectSnapshot);
    dbSession.commit();
    return projectSnapshot;
  }

  private ComponentDto createFile(String fileKey, ComponentDto project) {
    ComponentDto file = ComponentTesting.newFileDto(project, null)
      .setDbKey(fileKey)
      .setLanguage(XOO_LANGUAGE);
    dbClient.componentDao().insert(dbSession, file);
    dbSession.commit();
    return file;
  }

  private static Map<Integer, Block> blocksByIndexInFile(List<Block> blocks) {
    Map<Integer, Block> blocksByIndexInFile = new HashMap<>();
    for (Block block : blocks) {
      blocksByIndexInFile.put(block.getIndexInFile(), block);
    }
    return blocksByIndexInFile;
  }

}
