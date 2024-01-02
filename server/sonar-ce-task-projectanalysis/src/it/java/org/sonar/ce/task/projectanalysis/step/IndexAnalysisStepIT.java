/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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

import java.util.Set;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.ce.task.projectanalysis.batch.BatchReportReaderRule;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.FileStatuses;
import org.sonar.ce.task.projectanalysis.component.ReportComponent;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolderRule;
import org.sonar.ce.task.projectanalysis.component.ViewsComponent;
import org.sonar.ce.task.step.ComputationStep;
import org.sonar.ce.task.step.TestComputationStepContext;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchDao;
import org.sonar.server.es.AnalysisIndexer;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.PROJECT;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.VIEW;

public class IndexAnalysisStepIT extends BaseStepTest {

  private static final String PROJECT_KEY = "PROJECT_KEY";
  private static final String PROJECT_UUID = "PROJECT_UUID";

  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule();
  @Rule
  public BatchReportReaderRule reportReader = new BatchReportReaderRule();

  private final DbClient dbClient = mock(DbClient.class);
  private final FileStatuses fileStatuses = mock(FileStatuses.class);
  private final AnalysisIndexer analysisIndexer = mock(AnalysisIndexer.class);
  private final DbSession dbSession = mock(DbSession.class);
  private final BranchDao branchDao = mock(BranchDao.class);
  private final IndexAnalysisStep underTest = new IndexAnalysisStep(treeRootHolder, fileStatuses, dbClient, analysisIndexer);

  private TestComputationStepContext testComputationStepContext;

  @Before
  public void init() {
    testComputationStepContext = new TestComputationStepContext();

    when(dbClient.openSession(false)).thenReturn(dbSession);
    when(dbClient.branchDao()).thenReturn(branchDao);
  }

  @Test
  public void call_indexByProjectUuid_of_indexer_for_project() {
    Component project = ReportComponent.builder(PROJECT, 1).setUuid(PROJECT_UUID).setKey(PROJECT_KEY).build();
    treeRootHolder.setRoot(project);

    underTest.execute(testComputationStepContext);

    verify(analysisIndexer).indexOnAnalysis(PROJECT_UUID, Set.of());
  }

  @Test
  public void call_indexByProjectUuid_of_indexer_for_view() {
    Component view = ViewsComponent.builder(VIEW, PROJECT_KEY).setUuid(PROJECT_UUID).build();
    treeRootHolder.setRoot(view);

    underTest.execute(testComputationStepContext);

    verify(analysisIndexer).indexOnAnalysis(PROJECT_UUID, Set.of());
  }

  @Test
  public void execute_whenMarkAsUnchangedFlagActivated_shouldCallIndexOnAnalysisWithChangedComponents() {
    Component project = ReportComponent.builder(PROJECT, 1).setUuid(PROJECT_UUID).setKey(PROJECT_KEY).build();
    treeRootHolder.setRoot(project);
    Set<String> anyUuids = Set.of("any-uuid");
    when(fileStatuses.getFileUuidsMarkedAsUnchanged()).thenReturn(anyUuids);

    underTest.execute(testComputationStepContext);

    verify(analysisIndexer).indexOnAnalysis(PROJECT_UUID, anyUuids);
  }

  @Test
  public void execute_whenBranchIsNeedIssueSync_shouldReindexEverything() {
    Component project = ReportComponent.builder(PROJECT, 1).setUuid(PROJECT_UUID).setKey(PROJECT_KEY).build();
    treeRootHolder.setRoot(project);
    when(branchDao.isBranchNeedIssueSync(dbSession, PROJECT_UUID)).thenReturn(true);

    underTest.execute(testComputationStepContext);

    verify(analysisIndexer).indexOnAnalysis(PROJECT_UUID);
  }

  @Override
  protected ComputationStep step() {
    return underTest;
  }
}
