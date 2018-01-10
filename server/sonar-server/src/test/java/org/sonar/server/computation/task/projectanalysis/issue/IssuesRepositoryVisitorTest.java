/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.computation.task.projectanalysis.issue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.sonar.server.computation.task.projectanalysis.component.ReportComponent.builder;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.server.computation.task.projectanalysis.component.Component;
import org.sonar.server.computation.task.projectanalysis.component.TreeRootHolderRule;

public class IssuesRepositoryVisitorTest {
  static final String FILE_UUID = "FILE_UUID";
  static final String FILE_KEY = "FILE_KEY";
  static final int FILE_REF = 2;
  static final Component FILE = builder(Component.Type.FILE, FILE_REF)
    .setKey(FILE_KEY)
    .setUuid(FILE_UUID)
    .build();

  static final String PROJECT_KEY = "PROJECT_KEY";
  static final String PROJECT_UUID = "PROJECT_UUID";
  static final int PROJECT_REF = 1;
  static final Component PROJECT = builder(Component.Type.PROJECT, PROJECT_REF)
    .setKey(PROJECT_KEY)
    .setUuid(PROJECT_UUID)
    .addChildren(FILE)
    .build();

  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule();

  @Rule
  public ComponentIssuesRepositoryRule componentIssuesRepository = new ComponentIssuesRepositoryRule(treeRootHolder);

  IssuesRepositoryVisitor underTest = new IssuesRepositoryVisitor(componentIssuesRepository);

  @Before
  public void setUp() throws Exception {
    treeRootHolder.setRoot(PROJECT);
  }

  @Test
  public void feed_component_issues_repo() {
    DefaultIssue i1 = mock(DefaultIssue.class);
    DefaultIssue i2 = mock(DefaultIssue.class);

    underTest.beforeComponent(FILE);
    underTest.onIssue(FILE, i1);
    underTest.onIssue(FILE, i2);
    underTest.afterComponent(FILE);

    assertThat(componentIssuesRepository.getIssues(FILE_REF)).hasSize(2);
  }

  @Test
  public void empty_component_issues_repo_when_no_issue() {
    DefaultIssue i1 = mock(DefaultIssue.class);
    DefaultIssue i2 = mock(DefaultIssue.class);

    underTest.beforeComponent(FILE);
    underTest.onIssue(FILE, i1);
    underTest.onIssue(FILE, i2);
    underTest.afterComponent(FILE);
    assertThat(componentIssuesRepository.getIssues(FILE)).hasSize(2);

    underTest.beforeComponent(PROJECT);
    underTest.afterComponent(PROJECT);
    assertThat(componentIssuesRepository.getIssues(PROJECT)).isEmpty();
  }
}
