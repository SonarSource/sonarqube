/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
package org.sonar.ce.task.projectexport;

import com.google.common.base.Predicate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.junit.Test;
import org.picocontainer.PicoContainer;
import org.sonar.ce.task.container.TaskContainer;
import org.sonar.ce.task.projectexport.taskprocessor.ProjectDescriptor;
import org.sonar.ce.task.setting.SettingsLoader;
import org.sonar.ce.task.step.ComputationStep;
import org.sonar.core.platform.ComponentContainer;

import static org.assertj.core.api.Assertions.assertThat;

public class ProjectExportContainerPopulatorTest {

  private static final int COMPONENTS_BY_DEFAULT_IN_CONTAINER = 2;

  private final ProjectDescriptor descriptor = new ProjectDescriptor("project_uuid", "project_key", "Project Name");
  private final ProjectExportContainerPopulator underTest = new ProjectExportContainerPopulator(descriptor);

  @Test
  public void test_populateContainer() {
    RecorderTaskContainer container = new RecorderTaskContainer();
    underTest.populateContainer(container);
    assertThat(container.addedComponents)
      .hasSize(COMPONENTS_BY_DEFAULT_IN_CONTAINER + 8)
      .contains(descriptor, SettingsLoader.class);
  }

  private static class RecorderTaskContainer implements TaskContainer {
    private final List<Object> addedComponents = new ArrayList<>();

    @Override
    public ComponentContainer add(Object... objects) {
      addedComponents.addAll(Arrays.asList(objects));
      // not used anyway
      return null;
    }

    @Override
    public ComponentContainer addSingletons(Iterable<?> components) {
      List<Object> filteredComponents = StreamSupport.stream(components.spliterator(), false)
        .filter((Predicate<Object>) input -> !(input instanceof Class) || !ComputationStep.class.isAssignableFrom((Class<?>) input))
        .collect(Collectors.toList());

      addedComponents.addAll(filteredComponents);
      // not used anyway
      return null;
    }

    @Override
    public ComponentContainer getParent() {
      throw new UnsupportedOperationException("getParent is not implemented");
    }

    @Override
    public void bootup() {
      throw new UnsupportedOperationException("bootup is not implemented");
    }

    @Override
    public void close() {
      throw new UnsupportedOperationException("close is not implemented");
    }

    @Override
    public PicoContainer getPicoContainer() {
      throw new UnsupportedOperationException("getParent is not implemented");
    }

    @Override
    public <T> T getComponentByType(Class<T> type) {
      throw new UnsupportedOperationException("getParent is not implemented");
    }

    @Override
    public <T> List<T> getComponentsByType(final Class<T> type) {
      throw new UnsupportedOperationException("getParent is not implemented");
    }
  }
}
