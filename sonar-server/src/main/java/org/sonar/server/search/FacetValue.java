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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

public class FacetValue implements Comparable<FacetValue> {

  public static enum Sort {
    BY_KEY, BY_VALUE
  }

  private String key;
  private Integer value;
  private Sort sort;
  private Multimap<String, FacetValue> subFacets;

  public FacetValue(String key, Integer value){
    this.key = key;
    this.value = value;
    this.subFacets = ArrayListMultimap.create();
    this.sort = Sort.BY_VALUE;
  }

  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  public Integer getValue() {
    return value;
  }

  public void setValue(Integer value) {
    this.value = value;
  }

  public Multimap<String, FacetValue> getSubFacets() {
    return subFacets;
  }

  public void setSubFacets(Multimap<String, FacetValue> subFacets) {
    this.subFacets = subFacets;
  }

  @Override
  public String toString() {
    return ReflectionToStringBuilder.toString(this, ToStringStyle.MULTI_LINE_STYLE);
  }

  @Override
  public int compareTo(FacetValue other) {
    if (this.sort.equals(Sort.BY_KEY)) {
      return this.getKey().compareTo(other.getKey());
    } else {
      return this.getValue().compareTo(other.getValue());
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof FacetValue)) return false;

    FacetValue that = (FacetValue) o;

    if (!key.equals(that.key)) return false;
    if (subFacets != null ? !subFacets.equals(that.subFacets) : that.subFacets != null) return false;
    if (!value.equals(that.value)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = key.hashCode();
    result = 31 * result + value.hashCode();
    result = 31 * result + (subFacets != null ? subFacets.hashCode() : 0);
    return result;
  }
}
