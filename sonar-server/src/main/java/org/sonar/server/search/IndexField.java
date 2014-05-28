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

import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

public class IndexField {

  public static enum Type {
    STRING, TEXT, DATE, BOOLEAN, NUMERIC, OBJECT
  }

  public static final String SORT_SUFFIX = "sort";
  public static final String SEARCH_WORDS_SUFFIX = "words";
  public static final String SEARCH_PARTIAL_SUFFIX = "grams";

  private final Type type;
  private final String field;

  private boolean sortable = false;
  private boolean searchable = false;

  IndexField(Type type, String field) {
    this.type = type;
    this.field = field;
  }

  public boolean sortable() {
    return sortable;
  }

  public IndexField sortable(Boolean sortable) {
    this.sortable = sortable;
    return this;
  }

  public boolean searchable() {
    return searchable;
  }

  public IndexField searchable(Boolean searchable) {
    this.searchable = searchable;
    return this;
  }

  public Type type() {
    return type;
  }

  public String field() {
    return field;
  }

  public String sortField() {
    if(this.sortable()){
      return this.field + ((type == IndexField.Type.TEXT
        || type == IndexField.Type.STRING)?"."+IndexField.SORT_SUFFIX:"");
    } else {
      throw new IllegalStateException("Cannot sort on field (Field is not sortable)");
    }
  }

  @Override
  public String toString() {
    return ReflectionToStringBuilder.toString(this, ToStringStyle.SHORT_PREFIX_STYLE);
  }
}
