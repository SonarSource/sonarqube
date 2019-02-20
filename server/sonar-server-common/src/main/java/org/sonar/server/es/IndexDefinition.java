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

import com.google.common.collect.Maps;
import java.util.Map;
import org.sonar.api.server.ServerSide;
import org.sonar.server.es.newindex.NewAuthorizedIndex;
import org.sonar.server.es.newindex.NewIndex;
import org.sonar.server.es.newindex.NewRegularIndex;
import org.sonar.server.es.newindex.SettingsConfiguration;

import static com.google.common.base.Preconditions.checkArgument;

@ServerSide
public interface IndexDefinition {

  class IndexDefinitionContext {
    private final Map<String, NewIndex> byKey = Maps.newHashMap();

    public NewRegularIndex create(Index index, SettingsConfiguration settingsConfiguration) {
      String indexName = index.getName();
      checkArgument(!byKey.containsKey(indexName), String.format("Index already exists: %s", indexName));
      NewRegularIndex newIndex = new NewRegularIndex(index, settingsConfiguration);
      byKey.put(indexName, newIndex);
      return newIndex;
    }

    public NewAuthorizedIndex createWithAuthorization(Index index, SettingsConfiguration settingsConfiguration) {
      checkArgument(index.acceptsRelations(), "Index with authorization must accept relations");
      String indexName = index.getName();
      checkArgument(!byKey.containsKey(indexName), String.format("Index already exists: %s", indexName));

      NewAuthorizedIndex newIndex = new NewAuthorizedIndex(index, settingsConfiguration);
      byKey.put(indexName, newIndex);
      return newIndex;
    }

    public Map<String, NewIndex> getIndices() {
      return byKey;
    }
  }

  void define(IndexDefinitionContext context);

}
