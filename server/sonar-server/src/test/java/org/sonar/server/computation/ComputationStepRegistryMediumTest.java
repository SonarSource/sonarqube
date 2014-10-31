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

package org.sonar.server.computation;

import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.api.platform.ComponentContainer;
import org.sonar.server.tester.ServerTester;

import java.util.List;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class ComputationStepRegistryMediumTest {

  @ClassRule
  public static ServerTester tester = new ServerTester();

  private ComputationStepRegistry sut;

  @Before
  public void before() {
    ComponentContainer pico = new ComponentContainer();

    pico.addSingleton(mock(SynchronizeProjectPermissionsStep.class));
    pico.addSingleton(mock(IndexProjectIssuesStep.class));
    pico.addSingleton(mock(SwitchSnapshotStep.class));
    pico.addSingleton(mock(DataCleanerStep.class));
    pico.addSingleton(mock(InvalidatePreviewCacheStep.class));
    pico.addSingleton(mock(ProjectDatabaseIndexationStep.class));

    sut = new ComputationStepRegistry(pico);
  }

  @Test
  public void steps_returned_in_the_right_order() throws Exception {
    List<Class<? extends ComputationStep>> wishStepsClasses = Lists.newArrayList(
      SynchronizeProjectPermissionsStep.class,
      SwitchSnapshotStep.class,
      InvalidatePreviewCacheStep.class,
      DataCleanerStep.class,
      ProjectDatabaseIndexationStep.class,
      IndexProjectIssuesStep.class
      );
    List<ComputationStep> steps = sut.steps();

    assertThat(steps).hasSize(wishStepsClasses.size());
    for (int i = 0; i < steps.size(); i++) {
      assertThat(steps.get(i)).isInstanceOf(wishStepsClasses.get(i));
    }
  }

  @Test
  public void steps_have_a_non_empty_description() {
    ComputationStepRegistry sut = tester.get(ComputationStepRegistry.class);

    List<ComputationStep> steps = sut.steps();

    assertThat(steps).onProperty("description").excludes(null, "");
  }
}
