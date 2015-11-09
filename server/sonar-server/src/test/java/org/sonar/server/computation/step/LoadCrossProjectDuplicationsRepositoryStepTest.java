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
import java.util.Collections;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.batch.protocol.output.BatchReport;
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
import org.sonar.server.computation.analysis.AnalysisMetadataHolderRule;
import org.sonar.server.computation.batch.BatchReportReaderRule;
import org.sonar.server.computation.batch.TreeRootHolderRule;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.component.FileAttributes;
import org.sonar.server.computation.component.ReportComponent;
import org.sonar.server.computation.duplication.IntegrateCrossProjectDuplications;
import org.sonar.server.computation.snapshot.Snapshot;
import org.sonar.server.computation.snapshot.SnapshotImpl;

import static java.util.Arrays.asList;
import static org.mockito.Mockito.*;
import static org.sonar.server.computation.component.Component.Type.FILE;
import static org.sonar.server.computation.component.Component.Type.PROJECT;

public class LoadCrossProjectDuplicationsRepositoryStepTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  static final String XOO_LANGUAGE = "xoo";

  static final int PROJECT_REF = 1;
  static final int FILE_REF = 2;
  static final String CURRENT_FILE_KEY = "FILE_KEY";

  static final Component CURRENT_FILE = ReportComponent.builder(FILE, FILE_REF)
    .setKey(CURRENT_FILE_KEY)
    .setFileAttributes(new FileAttributes(false, XOO_LANGUAGE))
    .build();

  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule().setRoot(
    ReportComponent.builder(PROJECT, PROJECT_REF)
      .addChildren(CURRENT_FILE
      ).build());

  @Rule
  public BatchReportReaderRule batchReportReader = new BatchReportReaderRule();

  @Rule
  public AnalysisMetadataHolderRule analysisMetadataHolder = new AnalysisMetadataHolderRule();

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  DbClient dbClient = dbTester.getDbClient();

  DbSession dbSession = dbTester.getSession();

  IntegrateCrossProjectDuplications integrateCrossProjectDuplications = mock(IntegrateCrossProjectDuplications.class);

  Snapshot baseProjectSnapshot;

  ComputationStep underTest = new LoadCrossProjectDuplicationsRepositoryStep(treeRootHolder, batchReportReader, analysisMetadataHolder, integrateCrossProjectDuplications, dbClient);

  @Before
  public void setUp() throws Exception {
    ComponentDto project = ComponentTesting.newProjectDto();
    dbClient.componentDao().insert(dbSession, project);
    SnapshotDto projectSnapshot = SnapshotTesting.newSnapshotForProject(project);
    dbClient.snapshotDao().insert(dbSession, projectSnapshot);
    dbSession.commit();

    baseProjectSnapshot = new SnapshotImpl.Builder()
      .setId(projectSnapshot.getId())
      .setCreatedAt(projectSnapshot.getCreatedAt())
      .build();
  }

  @Test
  public void call_compute_cpd() throws Exception {
    analysisMetadataHolder
      .setCrossProjectDuplicationEnabled(true)
      .setBranch(null)
      .setBaseProjectSnapshot(baseProjectSnapshot);

    ComponentDto otherProject = createProject("OTHER_PROJECT_KEY");
    SnapshotDto otherProjectSnapshot = createProjectSnapshot(otherProject);

    ComponentDto otherFIle = createFile("OTHER_FILE_KEY", otherProject);
    SnapshotDto otherFileSnapshot = createFileSnapshot(otherFIle, otherProjectSnapshot);

    String hash = "a8998353e96320ec";
    DuplicationUnitDto duplicate = new DuplicationUnitDto()
      .setHash(hash)
      .setStartLine(40)
      .setEndLine(55)
      .setIndexInFile(0)
      .setProjectSnapshotId(otherProjectSnapshot.getId())
      .setSnapshotId(otherFileSnapshot.getId());
    dbClient.duplicationDao().insert(dbSession, duplicate);
    dbSession.commit();

    BatchReport.CpdTextBlock originBlock = BatchReport.CpdTextBlock.newBuilder()
      .setHash(hash)
      .setStartLine(30)
      .setEndLine(45)
      .setStartTokenIndex(0)
      .setEndTokenIndex(10)
      .build();
    batchReportReader.putDuplicationBlocks(FILE_REF, asList(originBlock));

    underTest.execute();

    verify(integrateCrossProjectDuplications).computeCpd(CURRENT_FILE,
      Arrays.asList(
        new Block.Builder()
          .setResourceId(CURRENT_FILE_KEY)
          .setBlockHash(new ByteArray(hash))
          .setIndexInFile(0)
          .setLines(originBlock.getStartLine(), originBlock.getEndLine())
          .setUnit(originBlock.getStartTokenIndex(), originBlock.getEndTokenIndex())
          .build()
        ),
      Arrays.asList(
        new Block.Builder()
          .setResourceId(otherFIle.getKey())
          .setBlockHash(new ByteArray(hash))
          .setIndexInFile(duplicate.getIndexInFile())
          .setLines(duplicate.getStartLine(), duplicate.getEndLine())
          .build()
        )
      );
  }

  @Test
  public void nothing_to_do_when_cross_project_duplication_is_disabled() throws Exception {
    analysisMetadataHolder
      .setCrossProjectDuplicationEnabled(false)
      .setBranch(null)
      .setBaseProjectSnapshot(baseProjectSnapshot);

    ComponentDto otherProject = createProject("OTHER_PROJECT_KEY");
    SnapshotDto otherProjectSnapshot = createProjectSnapshot(otherProject);

    ComponentDto otherFIle = createFile("OTHER_FILE_KEY", otherProject);
    SnapshotDto otherFileSnapshot = createFileSnapshot(otherFIle, otherProjectSnapshot);

    String hash = "a8998353e96320ec";
    DuplicationUnitDto duplicate = new DuplicationUnitDto()
      .setHash(hash)
      .setStartLine(40)
      .setEndLine(55)
      .setIndexInFile(0)
      .setProjectSnapshotId(otherProjectSnapshot.getId())
      .setSnapshotId(otherFileSnapshot.getId());
    dbClient.duplicationDao().insert(dbSession, duplicate);
    dbSession.commit();

    BatchReport.CpdTextBlock originBlock = BatchReport.CpdTextBlock.newBuilder()
      .setHash(hash)
      .setStartLine(30)
      .setEndLine(45)
      .setStartTokenIndex(0)
      .setEndTokenIndex(10)
      .build();
    batchReportReader.putDuplicationBlocks(FILE_REF, asList(originBlock));

    underTest.execute();

    verifyZeroInteractions(integrateCrossProjectDuplications);
  }

  @Test
  public void nothing_to_do_when_branch_is_used() throws Exception {
    analysisMetadataHolder
      .setCrossProjectDuplicationEnabled(true)
      .setBranch("origin/master")
      .setBaseProjectSnapshot(baseProjectSnapshot);

    ComponentDto otherProject = createProject("OTHER_PROJECT_KEY");
    SnapshotDto otherProjectSnapshot = createProjectSnapshot(otherProject);

    ComponentDto otherFIle = createFile("OTHER_FILE_KEY", otherProject);
    SnapshotDto otherFileSnapshot = createFileSnapshot(otherFIle, otherProjectSnapshot);

    String hash = "a8998353e96320ec";
    DuplicationUnitDto duplicate = new DuplicationUnitDto()
      .setHash(hash)
      .setStartLine(40)
      .setEndLine(55)
      .setIndexInFile(0)
      .setProjectSnapshotId(otherProjectSnapshot.getId())
      .setSnapshotId(otherFileSnapshot.getId());
    dbClient.duplicationDao().insert(dbSession, duplicate);
    dbSession.commit();

    BatchReport.CpdTextBlock originBlock = BatchReport.CpdTextBlock.newBuilder()
      .setHash(hash)
      .setStartLine(30)
      .setEndLine(45)
      .setStartTokenIndex(0)
      .setEndTokenIndex(10)
      .build();
    batchReportReader.putDuplicationBlocks(FILE_REF, asList(originBlock));

    underTest.execute();

    verifyZeroInteractions(integrateCrossProjectDuplications);
  }

  @Test
  public void nothing_to_do_when_no_cpd_text_blocks_found() throws Exception {
    analysisMetadataHolder
      .setCrossProjectDuplicationEnabled(true)
      .setBranch(null)
      .setBaseProjectSnapshot(baseProjectSnapshot);

    batchReportReader.putDuplicationBlocks(FILE_REF, Collections.<BatchReport.CpdTextBlock>emptyList());

    underTest.execute();

    verifyZeroInteractions(integrateCrossProjectDuplications);
  }

  private ComponentDto createProject(String projectKey) {
    ComponentDto project = ComponentTesting.newProjectDto().setKey(projectKey);
    dbClient.componentDao().insert(dbSession, project);
    dbSession.commit();
    return project;
  }

  private SnapshotDto createProjectSnapshot(ComponentDto project) {
    SnapshotDto projectSnapshot = SnapshotTesting.newSnapshotForProject(project);
    dbClient.snapshotDao().insert(dbSession, projectSnapshot);
    dbSession.commit();
    return projectSnapshot;
  }

  private ComponentDto createFile(String fileKey, ComponentDto project) {
    ComponentDto file = ComponentTesting.newFileDto(project)
      .setKey(fileKey)
      .setLanguage(XOO_LANGUAGE);
    dbClient.componentDao().insert(dbSession, file);
    dbSession.commit();
    return file;
  }

  private SnapshotDto createFileSnapshot(ComponentDto file, SnapshotDto projectSnapshot) {
    SnapshotDto fileSnapshot = SnapshotTesting.createForComponent(file, projectSnapshot);
    dbClient.snapshotDao().insert(dbSession, fileSnapshot);
    dbSession.commit();
    return fileSnapshot;
  }

}
