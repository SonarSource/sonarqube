/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.ce.task.projectanalysis.component;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.impl.utils.TestSystem2;
import org.sonar.api.utils.System2;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolderRule;
import org.sonar.db.DbTester;
import org.sonar.db.project.ProjectDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.PROJECT;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.VIEW;
import static org.sonar.ce.task.projectanalysis.component.ReportComponent.builder;

public class ProjectPersisterIT {
  private final static Component ROOT = builder(PROJECT, 1)
    .setUuid("PROJECT_UUID")
    .setKey("PROJECT_KEY")
    .setDescription("PROJECT_DESC")
    .setName("PROJECT_NAME")
    .build();

  @Rule
  public AnalysisMetadataHolderRule analysisMetadataHolder = new AnalysisMetadataHolderRule();
  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);
  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule();
  public TestSystem2 system2 = new TestSystem2();

  private ProjectPersister underTest = new ProjectPersister(dbTester.getDbClient(), treeRootHolder, system2);

  @Before
  public void prepare() {
    treeRootHolder.setRoot(ROOT);
    system2.setNow(1000L);
  }

  @Test
  public void skip_portfolios() {
    Component root = ViewsComponent.builder(VIEW, 1).build();
    TreeRootHolder treeRootHolder = mock(TreeRootHolder.class);
    when(treeRootHolder.getRoot()).thenReturn(root);
    new ProjectPersister(dbTester.getDbClient(), treeRootHolder, system2).persist(dbTester.getSession());
    verify(treeRootHolder).getRoot();
    verifyNoMoreInteractions(treeRootHolder);

  }

  @Test
  public void update_description() {
    ProjectDto p1 = dbTester.components().insertPublicProject("PROJECT_UUID",
      p -> p.setKey(ROOT.getKey()).setName(ROOT.getName()).setDescription("OLD_DESC")).getProjectDto();

    assertProject(p1.getUuid(), "OLD_DESC", ROOT.getName(), p1.getUpdatedAt());
    underTest.persist(dbTester.getSession());
    assertProject(ROOT.getUuid(), ROOT.getDescription(), ROOT.getName(), 1000L);
  }

  @Test
  public void update_name() {
    ProjectDto p1 = dbTester.components().insertPublicProject("PROJECT_UUID",
      p -> p.setKey(ROOT.getKey()).setName("OLD_NAME").setDescription(ROOT.getDescription())).getProjectDto();

    assertProject(p1.getUuid(), ROOT.getDescription(), "OLD_NAME", p1.getUpdatedAt());
    underTest.persist(dbTester.getSession());
    assertProject(ROOT.getUuid(), ROOT.getDescription(), ROOT.getName(), 1000L);
  }

  @Test
  public void dont_update() {
    ProjectDto p1 = dbTester.components().insertPublicProject(
      c -> c.setUuid("PROJECT_UUID").setKey(ROOT.getKey()).setName(ROOT.getName()).setDescription(ROOT.getDescription())).getProjectDto();

    assertProject(p1.getUuid(), ROOT.getDescription(), ROOT.getName(), p1.getUpdatedAt());
    underTest.persist(dbTester.getSession());
    assertProject(p1.getUuid(), ROOT.getDescription(), ROOT.getName(), p1.getUpdatedAt());
  }

  private void assertProject(String uuid, String description, String name, long updated) {
    assertThat(dbTester.getDbClient().projectDao().selectProjectByKey(dbTester.getSession(), ROOT.getKey()).get())
      .extracting(ProjectDto::getUuid, ProjectDto::getName, ProjectDto::getDescription, ProjectDto::getUpdatedAt)
      .containsExactly(uuid, name, description, updated);

  }
}
