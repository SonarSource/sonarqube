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
package org.sonar.server.component.index;

import co.elastic.clients.elasticsearch._types.query_dsl.MatchAllQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mockito;
import org.sonar.api.utils.System2;
import org.sonar.server.es.EsClient;
import org.sonar.server.es.EsTester;
import org.sonar.server.es.SearchOptions;
import org.sonar.server.permission.index.WebAuthorizationTypeSupport;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ComponentIndexSearchWindowExceededTest {
  @RegisterExtension
  public EsTester es = EsTester.create();

  private final WebAuthorizationTypeSupport authorizationTypeSupport = mock(WebAuthorizationTypeSupport.class);
  private final EsClient esClient = Mockito.spy(es.client());
  private final ComponentIndex underTest = new ComponentIndex(esClient, authorizationTypeSupport, System2.INSTANCE);

  @Test
  void search_shouldUseAccurateCountTrackParameterAndNotLimitCountTo10000() {
    // bypassing the permission check
    when(authorizationTypeSupport.createQueryFilterV2()).thenReturn(Query.of(q -> q.matchAll(new MatchAllQuery.Builder().build())));

    underTest.searchV2(ComponentQuery.builder().build(), new SearchOptions().setPage(2, 3));

    // Verify searchV2 was called (the track total hits is set to enabled in the searchV2 implementation)
    verify(esClient).searchV2(any(), any());
  }
}
