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
package org.sonar.server.es;

import java.util.Set;
import org.junit.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

public class AnalysisIndexerTest {

  @Test
  public void indexOnAnalysis_whenDiffIndexingNotSupported_shouldThrowISE() {
    AnalysisIndexer analysisIndexer = new IndexerNotSupportingDiffs();

    assertThatThrownBy(() -> analysisIndexer.indexOnAnalysis("branchUuid", Set.of("diffToIndex")))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Diff indexing is not supported by this indexer org.sonar.server.es.AnalysisIndexerTest$IndexerNotSupportingDiffs");

  }

  @Test
  public void indexOnAnalysis_whenDiffIndexingSupported_shouldNotThrowISE() {
    AnalysisIndexer analysisIndexer = new IndexerSupportingDiffs();

    assertThatCode(() -> analysisIndexer.indexOnAnalysis("branchUuid", Set.of("diffToIndex")))
      .doesNotThrowAnyException();
  }

  private static class IndexerNotSupportingDiffs implements AnalysisIndexer {
    @Override
    public void indexOnAnalysis(String branchUuid) {
      // no-op
    }

  }

  private static class IndexerSupportingDiffs implements AnalysisIndexer {
    @Override
    public void indexOnAnalysis(String branchUuid) {
      // no-op
    }

    @Override
    public boolean supportDiffIndexing() {
      return true;
    }
  }

}
