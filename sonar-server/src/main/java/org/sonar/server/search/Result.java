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

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.facet.Facet;
import org.elasticsearch.search.facet.terms.TermsFacet;

import javax.annotation.CheckForNull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Result<K> {

  private Collection<K> hits;
  private Map<String, Collection<FacetValue>> facets;
  private int total;
  private int offset;
  private long time;

  private Result(){}

  public Result(SearchResponse response){
    hits = new ArrayList<K>();
    if(response.getFacets() != null &&
      !response.getFacets().facets().isEmpty()){
      this.facets = new HashMap<String, Collection<FacetValue>>();
      for(Facet facet:response.getFacets().facets()){
        TermsFacet termFacet = (TermsFacet)facet;
        List<FacetValue> facetValues = new ArrayList<FacetValue>();
        for(TermsFacet.Entry facetValue:termFacet.getEntries()){
          facetValues.add(new FacetValue<Integer>(facetValue.getTerm().string(),
            facetValue.getCount()));
        }
        this.facets.put(facet.getName(), facetValues);
      }
    } else {
      this.facets = Collections.emptyMap();
    }
  }

  public Collection<K> getHits() {
    return hits;
  }

  public Result setHits(Collection<K> hits) {
    this.hits = hits;
    return this;
  }

  public int getTotal() {
    return total;
  }

  public int getOffset() {
    return offset;
  }

  public Result setTotal(int total) {
    this.total = total;
    return this;
  }

  public Result setOffset(int offset) {
    this.offset = offset;
    return this;
  }

  public long getTime() {
    return time;
  }

  public Result setTime(long time) {
    this.time = time;
    return this;
  }

  public Map<String, Collection<FacetValue>> getFacets(){
    return this.facets;
  }

  @CheckForNull
  public Collection<FacetValue> getFacet(String facetName){
    return this.facets.get(facetName);
  }

  @CheckForNull
  public Collection<String> getFacetKeys(String facetName){
    if(this.facets.containsKey(facetName)){
      List<String> keys = new ArrayList<String>();
      for (FacetValue facetValue : facets.get(facetName)) {
        keys.add(facetValue.getKey());
      }
      return keys;
    }
    return null;
  }

  @CheckForNull
  public Object getFacetTermValue(String facetName, String key){
    if(this.facets.containsKey(facetName)) {
      for (FacetValue facetValue : facets.get(facetName)) {
        if (facetValue.getKey().equals(key)) {
          return facetValue.getValue();
        }
      }
    }
    return null;
  }
}
