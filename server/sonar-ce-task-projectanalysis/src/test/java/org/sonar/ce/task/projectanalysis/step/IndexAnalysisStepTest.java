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

import org.junit.Rule;
import org.junit.Test;
import org.sonar.ce.task.projectanalysis.batch.BatchReportReaderRule;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.ReportComponent;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolderRule;
import org.sonar.ce.task.projectanalysis.component.ViewsComponent;
import org.sonar.ce.task.step.ComputationStep;
import org.sonar.ce.task.step.TestComputationStepContext;
import org.sonar.server.es.ProjectIndexer;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.PROJECT;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.VIEW;

public class IndexAnalysisStepTest extends BaseStepTest {

  private static final String PROJECT_KEY = "PROJECT_KEY";
  private static final String PROJECT_UUID = "PROJECT_UUID";

  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule();
  @Rule
  public BatchReportReaderRule reportReader = new BatchReportReaderRule();

  private ProjectIndexer componentIndexer = mock(ProjectIndexer.class);
  private IndexAnalysisStep underTest = new IndexAnalysisStep(treeRootHolder, componentIndexer);

  @Test
  public void call_indexByProjectUuid_of_indexer_for_project() {
    Component project = ReportComponent.builder(PROJECT, 1).setUuid(PROJECT_UUID).setKey(PROJECT_KEY).build();
    treeRootHolder.setRoot(project);

    underTest.execute(new TestComputationStepContext());

    verify(componentIndexer).indexOnAnalysis(PROJECT_UUID);
  }

  @Test
  public void call_indexByProjectUuid_of_indexer_for_view() {
    Component view = ViewsComponent.builder(VIEW, PROJECT_KEY).setUuid(PROJECT_UUID).build();
    treeRootHolder.setRoot(view);

    underTest.execute(new TestComputationStepContext());

    verify(componentIndexer).indexOnAnalysis(PROJECT_UUID);
  }

  @Override
  protected ComputationStep step() {
    return underTest;
  }
}
