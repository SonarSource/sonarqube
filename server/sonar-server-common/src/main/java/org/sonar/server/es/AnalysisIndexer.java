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
package org.sonar.server.es;

import java.util.Set;

/**
 * Indexers that should be called when a project branch is analyzed
 */
public interface AnalysisIndexer {
  /**
   * This method is called when an analysis must be indexed.
   *
   * @param branchUuid UUID of a project or application branch
   */
  void indexOnAnalysis(String branchUuid);

  /**
   * This method is called when an analysis must be indexed.
   *
   * @param branchUuid UUID of a project or application branch
   * @param unchangedComponentUuids UUIDs of components that didn't change in this analysis.
   *                                Indexers can be optimized by not re-indexing data related to these components.
   */
  void indexOnAnalysis(String branchUuid, Set<String> unchangedComponentUuids);
}
