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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import java.util.List;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.sonar.core.util.stream.MoreCollectors;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Construct sorting criteria of ES requests. Sortable fields must be previously
 * declared with methods prefixed by <code>add</code>.
 */
public class Sorting {

  private final ListMultimap<String, Field> fields = ArrayListMultimap.create();
  private final List<Field> defaultFields = Lists.newArrayList();

  public Field add(String name) {
    Field field = new Field(name);
    fields.put(name, field);
    return field;
  }

  public Field add(String name, String fieldName) {
    Field field = new Field(fieldName);
    fields.put(name, field);
    return field;
  }

  public Field addDefault(String fieldName) {
    Field field = new Field(fieldName);
    defaultFields.add(field);
    return field;
  }

  public List<Field> getFields(String name) {
    return fields.get(name);
  }

  public List<FieldSortBuilder> fill(String name, boolean asc) {
    List<Field> list = fields.get(name);
    checkArgument(!list.isEmpty(), "Bad sort field: %s", name);
    return doFill(list, asc);
  }

  public List<FieldSortBuilder> fillDefault() {
    return doFill(defaultFields, true);
  }

  private static List<FieldSortBuilder> doFill(List<Field> fields, boolean asc) {
    return fields.stream().map(field -> {
      FieldSortBuilder sortBuilder = SortBuilders.fieldSort(field.name);
      boolean effectiveAsc = asc != field.reverse;
      sortBuilder.order(effectiveAsc ? SortOrder.ASC : SortOrder.DESC);
      boolean effectiveMissingLast = asc == field.missingLast;
      sortBuilder.missing(effectiveMissingLast ? "_last" : "_first");
      return sortBuilder;
    }).collect(MoreCollectors.toList(fields.size()));
  }

  public static class Field {
    private final String name;
    private boolean reverse = false;
    private boolean missingLast = false;

    /**
     * Default is missing first, same order as requested
     */
    public Field(String name) {
      this.name = name;
    }

    /**
     * Mark missing value as moved to the end of sorted results if requested sort is ascending.
     */
    public Field missingLast() {
      missingLast = true;
      return this;
    }

    /**
     * Mark values as ordered in the opposite direction than the requested sort.
     */
    public Field reverse() {
      reverse = true;
      return this;
    }

    public String getName() {
      return name;
    }

    public boolean isReverse() {
      return reverse;
    }

    public boolean isMissingLast() {
      return missingLast;
    }
  }
}
