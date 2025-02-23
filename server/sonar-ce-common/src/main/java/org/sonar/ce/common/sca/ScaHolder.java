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
import org.sonar.db.sca.ScaDependencyDto;
import org.sonar.db.sca.ScaReleaseDto;

public interface ScaHolder {
  /**
   * Get the dependencies of this ScaHolder. This is an error
   * to call if dependencyAnalysisPresent() returns false.
   *
   * @return the dependencies found by the analysis
   */
  List<ScaDependencyDto> getDependencies();

  void setDependencies(Collection<ScaDependencyDto> dependencies);

  /**
   * Get the releases of this ScaHolder. This is an error
   * to call if dependencyAnalysisPresent() returns false.
   *
   * @return the releases found by the analysis
   */
  List<ScaReleaseDto> getReleases();

  void setReleases(Collection<ScaReleaseDto> releases);

  /**
   * Returns true if we were able to analyze dependencies.
   * If we were not able, then the other getters can't return
   * sensible results.
   *
   * @return true if we have dependencies
   */
  boolean dependencyAnalysisPresent();
}
