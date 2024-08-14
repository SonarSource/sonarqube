/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import java.util.Arrays;
import java.util.Map;
import java.util.SortedMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.codec.digest.DigestUtils;
import org.sonar.server.es.newindex.BuiltIndex;

/**
 * Hash of index definition is stored in the index itself in order to detect changes of mappings
 * between SonarQube versions. In this case, contrary to database tables, indices are dropped
 * and re-populated from scratch. There's no attempt to migrate existing data.
 */
class IndexDefinitionHash {
  private static final char DELIMITER = ',';

  private IndexDefinitionHash() {
  }

  static String of(BuiltIndex<?> index) {
    IndexType.IndexMainType mainType = index.getMainType();
    return of(
      index.getSettings().toString(),
      Map.of(mainType.getIndex(), mainType),
      index.getRelationTypes().stream().collect(Collectors.toMap(IndexType.IndexRelationType::getName, Function.identity())),
      index.getAttributes(),
      index.getCustomHashMetadata());
  }

  private static String of(String str, Map<?, ?>... maps) {
    StringBuilder sb = new StringBuilder(str);
    for (Map<?, ?> map : maps) {
      appendMap(sb, map);
    }
    return DigestUtils.sha256Hex(sb.toString());
  }

  private static void appendMap(StringBuilder sb, Map<?, ?> attributes) {
    for (Object entry : sort(attributes).entrySet()) {
      sb.append(((Map.Entry) entry).getKey());
      sb.append(DELIMITER);
      appendObject(sb, ((Map.Entry) entry).getValue());
      sb.append(DELIMITER);
    }
  }

  private static void appendObject(StringBuilder sb, Object value) {
    if (value instanceof Object[] arrayValue) {
      sb.append(Arrays.toString(arrayValue));
    } else if (value instanceof Map<?, ?> map) {
      appendMap(sb, map);
    } else if (value instanceof IndexType indexType) {
      sb.append(indexType.format());
    } else {
      sb.append(value);
    }
  }

  private static SortedMap<?, ?> sort(Map<?, ?> map) {
    if (map instanceof ImmutableSortedMap) {
      return (ImmutableSortedMap<?, ?>) map;
    }
    return ImmutableSortedMap.copyOf(map);
  }
}
