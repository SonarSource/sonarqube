/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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

import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.lucene.search.TotalHits;

import static java.util.Optional.ofNullable;

public class SearchIdResult<ID> {

  private final List<ID> uuids;
  private final Facets facets;
  private final long total;

  public <T> SearchIdResult(SearchResponse<T> response, Function<String, ID> converter, ZoneId timeZone) {
    this.facets = new Facets(response, timeZone);
    this.total = getTotalHits(response).value;
    this.uuids = convertToIds(response.hits().hits(), converter);
  }

  private static <T> TotalHits getTotalHits(SearchResponse<T> response) {
    return ofNullable(response.hits().total())
      .map(total -> {
        TotalHits.Relation relation = switch (total.relation()) {
          case Eq -> TotalHits.Relation.EQUAL_TO;
          case Gte -> TotalHits.Relation.GREATER_THAN_OR_EQUAL_TO;
        };
        return new TotalHits(total.value(), relation);
      })
      .orElseThrow(() -> new IllegalStateException("Could not get total hits of search results"));
  }

  public List<ID> getUuids() {
    return uuids;
  }

  public long getTotal() {
    return total;
  }

  public Facets getFacets() {
    return this.facets;
  }

  @Override
  public String toString() {
    return ReflectionToStringBuilder.toString(this);
  }

  private static <I, T> List<I> convertToIds(List<Hit<T>> hits, Function<String, I> converter) {
    List<I> docs = new ArrayList<>();
    for (Hit<T> hit : hits) {
      docs.add(converter.apply(hit.id()));
    }
    return docs;
  }
}
