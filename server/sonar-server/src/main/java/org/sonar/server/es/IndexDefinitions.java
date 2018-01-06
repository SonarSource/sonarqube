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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import java.util.Map;
import org.elasticsearch.common.settings.Settings;
import org.picocontainer.Startable;
import org.sonar.api.config.Configuration;
import org.sonar.api.server.ServerSide;

/**
 * This class collects definitions of all Elasticsearch indices during server startup
 */
@ServerSide
public class IndexDefinitions implements Startable {

  /**
   * Immutable copy of {@link org.sonar.server.es.NewIndex}
   */
  public static class Index {
    private final String name;
    private final Settings settings;
    private final Map<String, IndexType> types;

    Index(NewIndex newIndex) {
      this.name = newIndex.getName();
      this.settings = newIndex.getSettings().build();
      ImmutableMap.Builder<String, IndexType> builder = ImmutableMap.builder();
      for (NewIndex.NewIndexType newIndexType : newIndex.getTypes().values()) {
        IndexType type = new IndexType(newIndexType);
        builder.put(type.getName(), type);
      }
      this.types = builder.build();
    }

    public String getName() {
      return name;
    }

    public Settings getSettings() {
      return settings;
    }

    public Map<String, IndexType> getTypes() {
      return types;
    }
  }

  /**
   * Immutable copy of {@link org.sonar.server.es.NewIndex.NewIndexType}
   */
  public static class IndexType {
    private final String name;
    private final Map<String, Object> attributes;

    private IndexType(NewIndex.NewIndexType newType) {
      this.name = newType.getName();
      this.attributes = ImmutableMap.copyOf(newType.getAttributes());
    }

    public String getName() {
      return name;
    }

    public Map<String, Object> getAttributes() {
      return attributes;
    }
  }

  private final Map<String, Index> byKey = Maps.newHashMap();
  private final IndexDefinition[] defs;
  private final Configuration config;

  public IndexDefinitions(IndexDefinition[] defs, Configuration config) {
    this.defs = defs;
    this.config = config;
  }

  public Map<String, Index> getIndices() {
    return byKey;
  }

  @Override
  public void start() {
    // collect definitions
    IndexDefinition.IndexDefinitionContext context = new IndexDefinition.IndexDefinitionContext();

    if (!config.getBoolean("sonar.internal.es.disableIndexes").orElse(false)) {
      for (IndexDefinition definition : defs) {
        definition.define(context);
      }

      for (Map.Entry<String, NewIndex> entry : context.getIndices().entrySet()) {
        byKey.put(entry.getKey(), new Index(entry.getValue()));
      }
    }
  }

  @Override
  public void stop() {
    // nothing to do
  }
}
