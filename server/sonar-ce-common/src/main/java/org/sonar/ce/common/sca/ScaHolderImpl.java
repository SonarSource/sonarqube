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
package org.sonar.ce.common.sca;

import org.sonar.db.sca.ScaDependencyDto;

import java.util.List;
import java.util.Optional;

public class ScaHolderImpl implements ScaHolder {
  private List<ScaDependencyDto> dependencies = null;

  @Override
  public void setDependencies(List<ScaDependencyDto> dependencies) {
    this.dependencies = List.copyOf(dependencies);
  }

  @Override
  public List<ScaDependencyDto> getDependencies() {
    return Optional.ofNullable(this.dependencies).orElseThrow(() -> new IllegalStateException("SCA dependency analysis was not performed"));
  }

  @Override
  public boolean dependencyAnalysisPresent() {
    // for the time being, we just go by whether dependencies were set.
    // When we add more data that can be set by ScaStep, we might store this differently.
    return this.dependencies != null;
  }
}
