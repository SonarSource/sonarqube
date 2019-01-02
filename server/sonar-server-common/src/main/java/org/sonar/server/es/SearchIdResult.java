/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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

import com.google.common.base.Function;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;

public class SearchIdResult<ID> {

  private final List<ID> ids;
  private final Facets facets;
  private final long total;

  public SearchIdResult(SearchResponse response, Function<String, ID> converter, TimeZone timeZone) {
    this.facets = new Facets(response, timeZone);
    this.total = response.getHits().getTotalHits();
    this.ids = convertToIds(response.getHits(), converter);
  }

  public List<ID> getIds() {
    return ids;
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

  private static <ID> List<ID> convertToIds(SearchHits hits, Function<String, ID> converter) {
    List<ID> docs = new ArrayList<>();
    for (SearchHit hit : hits.getHits()) {
      docs.add(converter.apply(hit.getId()));
    }
    return docs;
  }
}
