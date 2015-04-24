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

import java.util.Collection;
import java.util.Collections;

public class IndexField {

  public enum Type {
    STRING, TEXT, DATE, BOOLEAN, INTEGER, LONG, DOUBLE, OBJECT, UUID_PATH
  }

  public static final String SORT_SUFFIX = "sort";
  public static final String SEARCH_WORDS_SUFFIX = "words";
  public static final String SEARCH_PARTIAL_SUFFIX = "grams";

  private final Type type;
  private final String field;
  private final Collection<IndexField> nestedFields;

  private boolean sortable = false;
  private boolean searchable = false;

  IndexField(Type type, String field) {
    this(type, field, Collections.<IndexField>emptyList());
  }

  IndexField(Type type, String field, Collection<IndexField> nestedFields) {
    this.type = type;
    this.field = field;
    this.nestedFields = nestedFields;
  }

  public boolean isSortable() {
    return sortable;
  }

  public IndexField setSortable(boolean b) {
    this.sortable = b;
    return this;
  }

  public boolean isSearchable() {
    return searchable;
  }

  public IndexField setSearchable(boolean b) {
    this.searchable = b;
    return this;
  }

  public Type type() {
    return type;
  }

  public String field() {
    return field;
  }

  public Collection<IndexField> nestedFields() {
    return nestedFields;
  }

  public String sortField() {
    if (isSortable()) {
      return this.field + ((type == IndexField.Type.TEXT
        || type == IndexField.Type.STRING) ? "." + IndexField.SORT_SUFFIX : "");
    } else {
      throw new IllegalStateException("Field is not sortable: " + field);
    }
  }

  @Override
  public String toString() {
    return ReflectionToStringBuilder.toString(this, ToStringStyle.SHORT_PREFIX_STYLE);
  }
}
