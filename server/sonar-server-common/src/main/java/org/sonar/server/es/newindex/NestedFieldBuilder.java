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
package org.sonar.server.es.newindex;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedMap;
import java.util.Map;
import java.util.TreeMap;

import static com.google.common.base.Preconditions.checkArgument;
import static org.sonar.server.es.newindex.DefaultIndexSettings.FIELD_TYPE_KEYWORD;
import static org.sonar.server.es.newindex.DefaultIndexSettings.INDEX;
import static org.sonar.server.es.newindex.DefaultIndexSettings.INDEX_SEARCHABLE;

public class NestedFieldBuilder<U extends FieldAware<U>> {
  private final U parent;
  private final String fieldName;
  private final Map<String, Object> properties = new TreeMap<>();

  protected NestedFieldBuilder(U parent, String fieldName) {
    this.parent = parent;
    this.fieldName = fieldName;
  }

  private NestedFieldBuilder setProperty(String fieldName, Object value) {
    properties.put(fieldName, value);

    return this;
  }

  public NestedFieldBuilder addKeywordField(String fieldName) {
    return setProperty(fieldName, ImmutableSortedMap.of(
      "type", FIELD_TYPE_KEYWORD,
      INDEX, INDEX_SEARCHABLE));
  }

  public NestedFieldBuilder addDoubleField(String fieldName) {
    return setProperty(fieldName, ImmutableMap.of("type", "double"));
  }

  public NestedFieldBuilder addIntegerField(String fieldName) {
    return setProperty(fieldName, ImmutableMap.of("type", "integer"));
  }

  public U build() {
    checkArgument(!properties.isEmpty(), "At least one sub-field must be declared in nested property '%s'", fieldName);

    return parent.setField(fieldName, ImmutableSortedMap.of(
      "type", "nested",
      "properties", properties));
  }
}
