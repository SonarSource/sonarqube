/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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

import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Lists;
import org.apache.commons.codec.digest.DigestUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

/**
 * Hash of index definition is stored in the index itself in order to detect changes of mappings
 * between SonarQube versions. In this case, contrary to database tables, indices are dropped
 * and re-populated from scratch. There's no attempt to migrate existing data.
 */
class IndexDefinitionHash {

  private static final char DELIMITER = ',';

  private IndexDefinitionHash() {
  }

  static String of(IndexDefinitions.Index index) {
    return of(index.getSettings().getAsMap(), index.getTypes());
  }

  private static String of(Map... maps) {
    StringBuilder sb = new StringBuilder();
    for (Map map : maps) {
      appendMap(sb, map);
    }
    return DigestUtils.sha256Hex(sb.toString());
  }

  private static void appendObject(StringBuilder sb, Object value) {
    if (value instanceof IndexDefinitions.IndexType) {
      appendIndexType(sb, (IndexDefinitions.IndexType) value);
    } else if (value instanceof Map) {
      appendMap(sb, (Map) value);
    } else if (value instanceof Iterable) {
      appendIterable(sb, (Iterable) value);
    } else {
      sb.append(String.valueOf(value));
    }
  }

  private static void appendIndexType(StringBuilder sb, IndexDefinitions.IndexType type) {
    appendMap(sb, type.getAttributes());
  }

  private static void appendMap(StringBuilder sb, Map attributes) {
    for (Object entry : sort(attributes).entrySet()) {
      sb.append(((Map.Entry) entry).getKey());
      sb.append(DELIMITER);
      appendObject(sb, ((Map.Entry) entry).getValue());
      sb.append(DELIMITER);
    }
  }

  private static void appendIterable(StringBuilder sb, Iterable value) {
    List sorted = Lists.newArrayList(value);
    Collections.sort(sorted);
    for (Object o : sorted) {
      appendObject(sb, o);
      sb.append(DELIMITER);
    }
  }

  private static SortedMap sort(Map map) {
    return ImmutableSortedMap.copyOf(map);
  }
}
