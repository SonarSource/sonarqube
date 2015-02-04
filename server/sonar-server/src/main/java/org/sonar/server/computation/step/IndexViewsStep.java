/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.server.computation.step;

import org.elasticsearch.action.admin.indices.cache.clear.ClearIndicesCacheRequest;
import org.sonar.api.resources.Qualifiers;
import org.sonar.server.computation.ComputationContext;
import org.sonar.server.search.SearchClient;
import org.sonar.server.view.index.ViewIndexer;

public class IndexViewsStep implements ComputationStep {

  private final ViewIndexer indexer;
  private final SearchClient searchClient;

  public IndexViewsStep(ViewIndexer indexer, SearchClient searchClient) {
    this.indexer = indexer;
    this.searchClient = searchClient;
  }

  @Override
  public void execute(ComputationContext context) {
    if (context.getProject().qualifier().equals(Qualifiers.VIEW)) {
      String viewUuid = context.getProject().uuid();
      indexer.index(context.getProject().uuid());

      try {
        ClearIndicesCacheRequest clearIndicesCacheRequest = new ClearIndicesCacheRequest();
        // TODO why does it not work ?
        // clearIndicesCacheRequest.filterKeys(IssueIndex.cacheKey(viewUuid));
        searchClient.admin().indices()
          .clearCache(clearIndicesCacheRequest)
          .get();

      } catch (Exception e) {
        throw new IllegalStateException(String.format("Unable to clear cache of view '%s'", viewUuid), e);
      }
    }
  }

  @Override
  public String getDescription() {
    return "Index views";
  }
}
