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

package org.sonar.server.computation.task.projectanalysis.step;

import org.junit.Rule;
import org.junit.Test;
import org.sonar.server.computation.task.projectanalysis.component.TreeRootHolderRule;
import org.sonar.server.component.es.ProjectMeasuresIndexer;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.sonar.server.computation.task.projectanalysis.component.Component.Type.PROJECT;
import static org.sonar.server.computation.task.projectanalysis.component.ReportComponent.builder;

public class IndexProjectMeasuresStepTest {

  static String PROJECT_UUID = "PROJECT_UUID";

  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule()
    .setRoot(builder(PROJECT, 1).setUuid(PROJECT_UUID).setKey("PROJECT_KEY").build());

  @Test
  public void call_indexer() {
    ProjectMeasuresIndexer indexer = mock(ProjectMeasuresIndexer.class);
    IndexProjectMeasuresStep underTest = new IndexProjectMeasuresStep(indexer, treeRootHolder);

    underTest.execute();

    verify(indexer).index(PROJECT_UUID);
  }

}
