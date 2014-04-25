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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class Hit implements Comparable<Hit> {

  private Map<String, Collection<Serializable>> fields;

  private Integer rank;

  public Hit(Integer rank){
    this.fields = new HashMap<String, Collection<Serializable>>();
    this.rank = rank;
  }

  public Collection<Serializable> getFieldValues(String field){
    return this.fields.get(field);
  }

  public Serializable getFieldValue(String field){
    if(this.hasField(field)){
      return fields.get(field).iterator().next();
    } else {
      return null;
    }
  }

  public Hit addFieldValue(String field, Serializable value){
    if(!this.hasField(field)){
      this.fields.put(field, new ArrayList<Serializable>());
    }
    return this;
  }

  public boolean hasField(String field){
    return this.fields.containsKey(field) &&
      !this.fields.get(field).isEmpty();
  }

  public Integer getRank(){
    return this.rank;
  }

  @Override
  public int compareTo(Hit hit) {
    return this.getRank().compareTo(hit.getRank());
  }
}
