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
package org.sonar.server.search;

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;

public class Result<K> {

  private final List<K> hits;
  private final Facets facets;
  private final long total;
  private final String scrollId;
  private final BaseIndex<K, ?, ?> index;

  public Result(SearchResponse response) {
    this(null, response);
  }

  public Result(@Nullable BaseIndex<K, ?, ?> index, SearchResponse response) {
    this.index = index;
    this.scrollId = response.getScrollId();
    this.facets = new Facets(response);
    this.total = (int) response.getHits().totalHits();
    this.hits = new ArrayList<>();
    if (index != null) {
      for (SearchHit hit : response.getHits()) {
        this.hits.add(index.toDoc(hit.getSource()));
      }
    }
  }

  public Iterator<K> scroll() {
    Preconditions.checkState(scrollId != null, "Result is not scrollable. Please use QueryOptions.setScroll()");
    return index.scroll(scrollId);
  }

  public List<K> getHits() {
    return hits;
  }

  public long getTotal() {
    return total;
  }

  public Map<String, Collection<FacetValue>> getFacets() {
    return this.facets.getFacets();
  }

  @CheckForNull
  public Collection<FacetValue> getFacetValues(String facetName) {
    return this.facets.getFacetValues(facetName);
  }

  @CheckForNull
  public List<String> getFacetKeys(String facetName) {
    return this.facets.getFacetKeys(facetName);
  }

  @Override
  public String toString() {
    return ReflectionToStringBuilder.toString(this);
  }
}
