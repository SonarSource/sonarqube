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
package org.sonar.server.component.index;

import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.internal.SearchContext;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.sonar.api.utils.System2;
import org.sonar.server.es.EsClient;
import org.sonar.server.es.EsTester;
import org.sonar.server.es.SearchOptions;
import org.sonar.server.permission.index.WebAuthorizationTypeSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ComponentIndexSearchWindowExceededTest {
  @Rule
  public EsTester es = EsTester.create();

  private final WebAuthorizationTypeSupport authorizationTypeSupport = mock(WebAuthorizationTypeSupport.class);
  private final EsClient esClient = Mockito.spy(es.client());
  private final ComponentIndex underTest = new ComponentIndex(esClient, authorizationTypeSupport, System2.INSTANCE);

  @Test
  public void search_shouldUseAccurateCountTrackParameterAndNotLimitCountTo10000() {
    // bypassing the permission check
    when(authorizationTypeSupport.createQueryFilter()).thenReturn(QueryBuilders.matchAllQuery());

    underTest.search(ComponentQuery.builder().build(), new SearchOptions().setPage(2, 3));

    ArgumentCaptor<SearchRequest> searchRequestArgumentCaptor = ArgumentCaptor.forClass(SearchRequest.class);
    verify(esClient).search(searchRequestArgumentCaptor.capture());
    SearchRequest searchRequest = searchRequestArgumentCaptor.getValue();
    assertThat(searchRequest.source().trackTotalHitsUpTo()).isEqualTo(SearchContext.TRACK_TOTAL_HITS_ACCURATE);
  }
}
