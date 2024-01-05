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

import java.util.Collection;

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
   * This method is called when {@link #supportDiffIndexing()} is true.
   *
   * @param diffToIndex Diff of uuids of indexed entities (issue keys, project uuids, etc.)
   */
  default void indexOnAnalysis(String branchUuid, Collection<String> diffToIndex) {
    if (!supportDiffIndexing()) {
      throw new IllegalStateException("Diff indexing is not supported by this indexer " + getClass().getName());
    }
  }

  /**
   * This method indicates if the indexer supports diff indexing during analysis.
   *
   * @return true if it is supported, false otherwise
   */
  default boolean supportDiffIndexing() {
    return false;
  }
}
