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

import javax.annotation.CheckForNull;
import java.util.HashSet;
import java.util.Set;

/**
 * @since 4.4
 */
public class Indexable {

  public static Set<IndexField> ALL_FIELDS = new HashSet<IndexField>();

  public static IndexField add(IndexField.Type type, String field){
    IndexField indexField = new IndexField(type, field);
    ALL_FIELDS.add(indexField);
    return indexField;
  }

  public static IndexField addSearchable(IndexField.Type type, String field){
    IndexField indexField = new IndexField(type, field)
      .searchable(true);
    ALL_FIELDS.add(indexField);
    return indexField;
  }


  public static IndexField addSortableAndSearchable(IndexField.Type type, String field) {
    IndexField indexField = new IndexField(type, field)
      .searchable(true)
      .sortable(true);
    ALL_FIELDS.add(indexField);
    return indexField;
  }

  public static IndexField addSortable(IndexField.Type type, String field){
    IndexField indexField = new IndexField(type, field)
      .sortable(true);
    ALL_FIELDS.add(indexField);
    return indexField;
  }

  @CheckForNull
  public static IndexField fromField(String field){
    for(IndexField indexField:ALL_FIELDS){
      if(indexField.field().equals(field)){
        return indexField;
      }
    }
    return null;
  }

  @Override
  public String toString() {
    return ReflectionToStringBuilder.toString(this, ToStringStyle.SHORT_PREFIX_STYLE);
  }
}

