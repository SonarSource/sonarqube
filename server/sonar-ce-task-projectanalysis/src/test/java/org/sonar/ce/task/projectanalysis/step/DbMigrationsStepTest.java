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

import com.google.common.collect.ImmutableList;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Random;
import java.util.stream.IntStream;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.sonar.ce.task.projectanalysis.dbmigration.ProjectAnalysisDataChange;
import org.sonar.ce.task.projectanalysis.dbmigration.ProjectAnalysisDataChanges;
import org.sonar.ce.task.step.TestComputationStepContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DbMigrationsStepTest {
  private ProjectAnalysisDataChanges projectAnalysisDataChanges = mock(ProjectAnalysisDataChanges.class);

  private DbMigrationsStep underTest = new DbMigrationsStep(projectAnalysisDataChanges);

  @Test
  public void execute_has_no_effect_if_there_is_no_DataChange() {
    underTest.execute(new TestComputationStepContext());
  }

  @Test
  public void execute_calls_execute_on_DataChange_instances_in_order_provided_by_ProjectAnalysisDataChanges() {
    ProjectAnalysisDataChange[] dataChanges = IntStream.range(0, 5 + new Random().nextInt(5))
      .mapToObj(i -> mock(ProjectAnalysisDataChange.class))
      .toArray(ProjectAnalysisDataChange[]::new);
    InOrder inOrder = Mockito.inOrder((Object[]) dataChanges);
    when(projectAnalysisDataChanges.getDataChanges()).thenReturn(Arrays.asList(dataChanges));

    underTest.execute(new TestComputationStepContext());

    Arrays.stream(dataChanges).forEach(t -> {
      try {
        inOrder.verify(t).execute();
      } catch (SQLException e) {
        throw new RuntimeException("mock execute method throw an exception??!!??", e);
      }
    });
  }

  @Test
  public void execute_stops_executing_and_throws_ISE_at_first_failing_DataChange() throws SQLException {
    ProjectAnalysisDataChange okMock1 = mock(ProjectAnalysisDataChange.class);
    ProjectAnalysisDataChange okMock2 = mock(ProjectAnalysisDataChange.class);
    ProjectAnalysisDataChange failingMock1 = mock(ProjectAnalysisDataChange.class);
    SQLException expected = new SQLException("Faiking DataChange throwing a SQLException");
    doThrow(expected).when(failingMock1).execute();
    ProjectAnalysisDataChange okMock3 = mock(ProjectAnalysisDataChange.class);
    ProjectAnalysisDataChange failingMock2 = mock(ProjectAnalysisDataChange.class);
    doThrow(new SQLException("Faiking another failing DataChange throwing a SQLException but which should never be thrown"))
      .when(failingMock2)
      .execute();
    ProjectAnalysisDataChange okMock4 = mock(ProjectAnalysisDataChange.class);
    InOrder inOrder = Mockito.inOrder(okMock1, okMock2, failingMock1, okMock3, failingMock2, okMock4);
    when(projectAnalysisDataChanges.getDataChanges()).thenReturn(ImmutableList.of(
      okMock1, okMock2, failingMock1, okMock3, failingMock2, okMock4));

    try {
      underTest.execute(new TestComputationStepContext());
      fail("A IllegalStateException should have been thrown");
    } catch (IllegalStateException e) {
      assertThat(e)
        .hasCause(expected);
      inOrder.verify(okMock1).execute();
      inOrder.verify(okMock2).execute();
      inOrder.verify(failingMock1).execute();
      inOrder.verifyNoMoreInteractions();
    }
  }

  @Test
  public void verify_description() {
    assertThat(underTest.getDescription()).isNotEmpty();
  }
}
