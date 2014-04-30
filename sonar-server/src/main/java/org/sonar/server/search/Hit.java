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

import javax.annotation.CheckForNull;
import java.util.HashMap;
import java.util.Map;

public class Hit implements Comparable<Hit> {

  private Map<String, Object> fields;

  private Integer rank;
  private Float score;

  public Hit(Integer rank) {
    this.fields = new HashMap<String, Object>();
    this.rank = rank;
  }

  public Hit(Float score){
    this.fields = new HashMap<String, Object>();
    this.score = score;
  }

  public Map<String, Object> getFields() {
    return this.fields;
  }

  @CheckForNull
  public Object getField(String key) {
    return this.fields.get(key);
  }

  @CheckForNull
  public String getFieldAsString(String key) {
    Object val = this.fields.get(key);
    return val == null ? null : val.toString();
  }

  public Integer getRank() {
    return this.rank;
  }

  public Float getScore() {
    return this.score;
  }

  @Override
  public int compareTo(Hit hit) {
    if (this.getRank() != null) {
      return this.getRank().compareTo(hit.getRank());
    } else {
      return this.getScore().compareTo(hit.getScore());
    }
  }

  public static Hit fromMap(Integer rank, Map<String, Object> fieldMap) {
    Hit hit = new Hit(0);
    hit.fields = fieldMap;
    return hit;
  }

  public static Hit fromMap(float score, Map<String, Object> fieldMap) {
    Hit hit = new Hit(score);
    hit.fields = fieldMap;
    return hit;
  }
}
