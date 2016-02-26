/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.server.search;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.annotation.CheckForNull;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.elasticsearch.action.search.SearchResponse;

public class Result<K> {

  private final List<K> hits;
  private final Facets facets;
  private final long total;

  public Result(SearchResponse response) {
    this.facets = new Facets(response);
    this.total = (int) response.getHits().totalHits();
    this.hits = new ArrayList<>();
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

  public Facets getFacetsObject() {
    return this.facets;
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
