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
package org.sonar.auth.gitlab;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.Test;
import org.sonar.core.platform.Container;

import static java.util.Collections.unmodifiableList;
import static org.assertj.core.api.Assertions.assertThat;

public class GitLabModuleTest {
  @Test
  public void verify_count_of_added_components() {
    ListContainer container = new ListContainer();
    new GitLabModule().configure(container);
    assertThat(container.getAddedObjects()).hasSize(10);
  }

  private static class ListContainer implements Container {
    private final List<Object> objects = new ArrayList<>();

    @Override
    public Container add(Object... objects) {
      this.objects.add(objects);
      return this;
    }

    public List<Object> getAddedObjects() {
      return unmodifiableList(new ArrayList<>(objects));
    }

    @Override
    public <T> T getComponentByType(Class<T> type) {
      throw new UnsupportedOperationException();
    }

    @Override
    public <T> Optional<T> getOptionalComponentByType(Class<T> type) {
      throw new UnsupportedOperationException();
    }

    @Override
    public <T> List<T> getComponentsByType(Class<T> type) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Container getParent() {
      throw new UnsupportedOperationException();
    }
  }
}
