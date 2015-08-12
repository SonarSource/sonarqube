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
package org.sonar.server.computation.container;

import org.junit.Test;
import org.sonar.core.platform.ComponentContainer;
import org.sonar.core.platform.ContainerPopulator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class ComputeEngineContainerImplTest {

  @Test(expected = NullPointerException.class)
  public void constructor_fails_fast_on_null_container() {
    new ComputeEngineContainerImpl(null, mock(ContainerPopulator.class));
  }

  @Test(expected = NullPointerException.class)
  public void constructor_fails_fast_on_null_item() {
    new ComputeEngineContainerImpl(new ComponentContainer(), null);
  }

  @Test
  public void calls_method_populateContainer_of_passed_in_populator() {
    ComponentContainer parent = new ComponentContainer();
    ContainerPopulator populator = mock(ContainerPopulator.class);
    ComputeEngineContainerImpl ceContainer = new ComputeEngineContainerImpl(parent, populator);

    verify(populator).populateContainer(ceContainer);
  }

  @Test
  public void ce_container_is_not_child_of_specified_container() {
    ComponentContainer parent = new ComponentContainer();
    ContainerPopulator populator = mock(ContainerPopulator.class);
    ComputeEngineContainerImpl ceContainer = new ComputeEngineContainerImpl(parent, populator);

    assertThat(parent.getChild()).isNull();
    verify(populator).populateContainer(ceContainer);
  }

}
