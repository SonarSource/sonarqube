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

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import org.elasticsearch.common.settings.Settings;
import org.sonar.api.server.ServerSide;

import java.util.Map;

@ServerSide
public interface IndexDefinition {

  class IndexDefinitionContext {
    private final Map<String, NewIndex> byKey = Maps.newHashMap();

    public NewIndex create(String key, NewIndex.SettingsConfiguration settingsConfiguration) {
      Preconditions.checkArgument(!byKey.containsKey(key), String.format("Index already exists: %s", key));
      NewIndex index = new NewIndex(key, settingsConfiguration);
      byKey.put(key, index);
      return index;
    }

    public Map<String, NewIndex> getIndices() {
      return byKey;
    }
  }

  void define(IndexDefinitionContext context);

  /**
   * Immutable copy of {@link NewIndex}
   */
  class Index {
    private final String name;
    private final Settings settings;
    private final Map<String, Type> types;

    Index(NewIndex newIndex) {
      this.name = newIndex.getName();
      this.settings = newIndex.getSettings().build();
      ImmutableMap.Builder<String, Type> builder = ImmutableMap.builder();
      for (NewIndex.NewIndexType newIndexType : newIndex.getTypes().values()) {
        Type type = new Type(newIndexType);
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

    public Map<String, Type> getTypes() {
      return types;
    }
  }

  /**
   * Immutable copy of {@link NewIndex.NewIndexType}
   */
  class Type {
    private final String name;
    private final Map<String, Object> attributes;

    private Type(NewIndex.NewIndexType newType) {
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
}
