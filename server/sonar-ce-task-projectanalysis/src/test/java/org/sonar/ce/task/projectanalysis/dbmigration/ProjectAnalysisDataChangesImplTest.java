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
package org.sonar.ce.task.projectanalysis.dbmigration;

import java.util.List;
import java.util.stream.Collectors;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.ce.task.CeTask;
import org.sonar.db.Database;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class ProjectAnalysisDataChangesImplTest {
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void constructor_throws_IAE_if_argument_is_empty() {
    ProjectAnalysisDataChange[] empty = new ProjectAnalysisDataChange[0];
    int expectedArraySize = ProjectAnalysisDataChangesImpl.getDataChangeClasses().size();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Number of ProjectAnalysisDataChange instance available (0) is inconsistent with " +
      "the number of declared ProjectAnalysisDataChange types (" + expectedArraySize + ")");

    new ProjectAnalysisDataChangesImpl(empty);
  }

  @Test
  public void constructor_throws_ISE_if_an_instance_of_declared_class_is_missing() {
    ProjectAnalysisDataChange[] wrongInstance = new ProjectAnalysisDataChange[] {
      mock(ProjectAnalysisDataChange.class)
    };

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Some of the ProjectAnalysisDataChange type declared have no instance in the container");

    new ProjectAnalysisDataChangesImpl(wrongInstance);

  }

  @Test
  public void getDataChanges_returns_instances_of_classes_in_order_defined_by_getDataChangeClasses() {
    Database database = mock(Database.class);
    CeTask ceTask = mock(CeTask.class);
    ProjectAnalysisDataChangesImpl underTest = new ProjectAnalysisDataChangesImpl(new ProjectAnalysisDataChange[] {
      new PopulateFileSourceLineCount(database, ceTask)
    });

    List<ProjectAnalysisDataChange> dataChanges = underTest.getDataChanges();

    List<? extends Class<?>> dataChangeClasses = dataChanges
      .stream()
      .map(ProjectAnalysisDataChange::getClass)
      .collect(Collectors.toList());
    assertThat(dataChangeClasses).isEqualTo(ProjectAnalysisDataChangesImpl.getDataChangeClasses());
  }
}
