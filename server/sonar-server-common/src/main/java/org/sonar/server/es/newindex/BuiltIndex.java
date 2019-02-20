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
import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.elasticsearch.common.settings.Settings;
import org.sonar.server.es.IndexType;
import org.sonar.server.es.IndexType.IndexRelationType;

import static org.sonar.core.util.stream.MoreCollectors.toSet;
import static org.sonar.server.es.IndexType.FIELD_INDEX_TYPE;
import static org.sonar.server.es.newindex.DefaultIndexSettings.NORMS;
import static org.sonar.server.es.newindex.DefaultIndexSettings.STORE;
import static org.sonar.server.es.newindex.DefaultIndexSettings.TYPE;

/**
 * Immutable copy of {@link NewIndex}
 */
public final class BuiltIndex<T extends NewIndex<T>> {
  private final IndexType.IndexMainType mainType;
  private final Set<IndexRelationType> relationTypes;
  private final Settings settings;
  private final Map<String, Object> attributes;

  BuiltIndex(T newIndex) {
    this.mainType = newIndex.getMainType();
    this.settings = newIndex.getSettings().build();
    this.relationTypes = newIndex.getRelationsStream().collect(toSet());
    this.attributes = buildAttributes(newIndex);
  }

  private static Map<String, Object> buildAttributes(NewIndex<?> newIndex) {
    Map<String, Object> indexAttributes = new TreeMap<>(newIndex.getAttributes());
    setRouting(indexAttributes, newIndex);
    indexAttributes.put("properties", buildProperties(newIndex));
    return ImmutableSortedMap.copyOf(indexAttributes);
  }

  private static void setRouting(Map<String, Object> indexAttributes, NewIndex newIndex) {
    if (!newIndex.getRelations().isEmpty()) {
      indexAttributes.put("_routing", ImmutableMap.of("required", true));
    }
  }

  private static TreeMap<String, Object> buildProperties(NewIndex<?> newIndex) {
    TreeMap<String, Object> indexProperties = new TreeMap<>(newIndex.getProperties());
    setTypeField(indexProperties, newIndex);
    setJoinField(indexProperties, newIndex);
    return indexProperties;
  }

  private static void setTypeField(TreeMap<String, Object> indexProperties, NewIndex newIndex) {
    Collection<IndexRelationType> relations = newIndex.getRelations();
    if (!relations.isEmpty()) {
      indexProperties.put(
        FIELD_INDEX_TYPE,
        ImmutableMap.of(
          TYPE, "keyword",
          NORMS, false,
          STORE, false,
          "doc_values", false));
    }
  }

  private static void setJoinField(TreeMap<String, Object> indexProperties, NewIndex newIndex) {
    Collection<IndexRelationType> relations = newIndex.getRelations();
    IndexType.IndexMainType mainType = newIndex.getMainType();
    if (!relations.isEmpty()) {
      indexProperties.put(mainType.getIndex().getJoinField(), ImmutableMap.of(
        TYPE, "join",
        "relations", ImmutableMap.of(mainType.getType(), namesToStringOrStringArray(relations))));
    }
  }

  private static Serializable namesToStringOrStringArray(Collection<IndexRelationType> relations) {
    if (relations.size() == 1) {
      return relations.iterator().next().getName();
    }

    return relations.stream()
      .map(IndexRelationType::getName)
      .sorted()
      .toArray(String[]::new);
  }

  public IndexType.IndexMainType getMainType() {
    return mainType;
  }

  public Set<IndexRelationType> getRelationTypes() {
    return relationTypes;
  }

  public Settings getSettings() {
    return settings;
  }

  public Map<String, Object> getAttributes() {
    return attributes;
  }
}
