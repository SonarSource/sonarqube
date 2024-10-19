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

import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.lucene.search.TotalHits;
import org.elasticsearch.action.search.SearchResponse;

import static java.util.Optional.ofNullable;

public class SearchResult<DOC extends BaseDoc> {

  private final List<DOC> docs;
  private final Facets facets;
  private final long total;

  public SearchResult(SearchResponse response, Function<Map<String, Object>, DOC> converter, ZoneId timeZone) {
    this.facets = new Facets(response, timeZone);
    this.total = getTotalHits(response).value;
    this.docs = EsUtils.convertToDocs(response.getHits(), converter);
  }

  private static TotalHits getTotalHits(SearchResponse response) {
    return ofNullable(response.getHits().getTotalHits()).orElseThrow(() -> new IllegalStateException("Could not get total hits of search results"));
  }

  public List<DOC> getDocs() {
    return docs;
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
}
