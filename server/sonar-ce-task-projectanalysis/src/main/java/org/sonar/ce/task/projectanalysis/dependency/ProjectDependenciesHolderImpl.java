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
package org.sonar.ce.task.projectanalysis.dependency;

import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Map;
import javax.annotation.CheckForNull;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;

/**
 * Holds the reference to the project dependencies for the current CE run.
 */
public class ProjectDependenciesHolderImpl implements MutableProjectDependenciesHolder {
  @CheckForNull
  private Map<String, ProjectDependency> dependenciesByUuid = null;

  @Override
  public boolean isEmpty() {
    return this.dependenciesByUuid == null;
  }

  @Override
  public MutableProjectDependenciesHolder setDependencies(List<ProjectDependency> dependencies) {
    checkState(this.dependenciesByUuid == null, "dependencies can not be set twice in holder");
    this.dependenciesByUuid = requireNonNull(dependencies, "dependencies can not be null").stream().collect(ImmutableMap.toImmutableMap(ProjectDependency::getUuid, d -> d));
    return this;
  }

  @Override
  public List<ProjectDependency> getDependencies() {
    checkInitialized();
    return List.copyOf(dependenciesByUuid.values());
  }

  @Override
  public int getSize() {
    checkInitialized();
    return dependenciesByUuid.size();
  }

  private void checkInitialized() {
    checkState(this.dependenciesByUuid != null, "Holder has not been initialized yet");
  }
}
