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
package org.sonar.server.es.textsearch;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import java.util.stream.Stream;
import org.sonar.server.es.textsearch.ComponentTextSearchQueryFactory.ComponentTextSearchQuery;

public interface ComponentTextSearchFeature {

  enum UseCase {
    GENERATE_RESULTS, CHANGE_ORDER_OF_RESULTS
  }

  default UseCase getUseCase() {
    return UseCase.GENERATE_RESULTS;
  }

  /**
   * Get queries using the new Elasticsearch Java API Client (8.x).
   */
  default Stream<Query> getQueriesV2(ComponentTextSearchQuery query) {
    return Stream.of(getQueryV2(query));
  }

  /**
   * Get a single query using the new Elasticsearch Java API Client (8.x).
   */
  Query getQueryV2(ComponentTextSearchQuery query);
}
