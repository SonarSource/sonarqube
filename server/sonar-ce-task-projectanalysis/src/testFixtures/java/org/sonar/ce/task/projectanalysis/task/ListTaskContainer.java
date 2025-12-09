/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.ce.task.projectanalysis.task;

import com.google.common.collect.Iterables;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.sonar.ce.task.container.TaskContainer;
import org.sonar.core.platform.Module;
import org.sonar.core.platform.SpringComponentContainer;

public class ListTaskContainer implements TaskContainer {
  private final List<Object> addedComponents = new ArrayList<>();

  @Override
  public SpringComponentContainer add(Object... objects) {
    for (Object o : objects) {
      if (o instanceof Module) {
        ((Module) o).configure(this);
      } else if (o instanceof Iterable) {
        add(Iterables.toArray((Iterable<?>) o, Object.class));
      } else {
        this.addedComponents.add(o);
      }
    }
    // not used anyway
    return null;
  }

  public List<Object> getAddedComponents() {
    return addedComponents;
  }

  @Override
  public SpringComponentContainer getParent() {
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
  public <T> T getComponentByType(Class<T> type) {
    throw new UnsupportedOperationException("getParent is not implemented");
  }

  @Override public <T> Optional<T> getOptionalComponentByType(Class<T> type) {
    throw new UnsupportedOperationException("getParent is not implemented");
  }

  @Override
  public <T> List<T> getComponentsByType(final Class<T> type) {
    throw new UnsupportedOperationException("getParent is not implemented");
  }
}
