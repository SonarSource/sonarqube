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

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.sonar.db.sca.ScaDependencyDto;
import org.sonar.db.sca.ScaReleaseDto;

public class ScaHolderImpl implements ScaHolder {
  private List<ScaDependencyDto> dependencies = null;
  private List<ScaReleaseDto> releases = null;

  @Override
  public List<ScaDependencyDto> getDependencies() {
    return Optional.ofNullable(this.dependencies).orElseThrow(() -> new IllegalStateException("SCA dependency analysis was not performed"));
  }

  @Override
  public void setDependencies(Collection<ScaDependencyDto> dependencies) {
    this.dependencies = List.copyOf(dependencies);
  }

  @Override
  public List<ScaReleaseDto> getReleases() {
    return Optional.ofNullable(this.releases).orElseThrow(() -> new IllegalStateException("SCA dependency analysis was not performed"));
  }

  @Override
  public void setReleases(Collection<ScaReleaseDto> releases) {
    this.releases = List.copyOf(releases);
  }

  @Override
  public boolean dependencyAnalysisPresent() {
    return this.dependencies != null && this.releases != null;
  }
}
