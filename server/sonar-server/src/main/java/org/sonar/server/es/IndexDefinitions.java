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
import org.picocontainer.Startable;
import org.sonar.api.config.Configuration;
import org.sonar.api.server.ServerSide;
import org.sonar.server.es.newindex.BuiltIndex;
import org.sonar.server.es.newindex.NewIndex;

/**
 * This class collects definitions of all Elasticsearch indices during server startup
 */
@ServerSide
public class IndexDefinitions implements Startable {

  private final Map<String, BuiltIndex> byKey = Maps.newHashMap();
  private final IndexDefinition[] defs;
  private final Configuration config;

  public IndexDefinitions(IndexDefinition[] defs, Configuration config) {
    this.defs = defs;
    this.config = config;
  }

  public Map<String, BuiltIndex> getIndices() {
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
        byKey.put(entry.getKey(), entry.getValue().build());
      }
    }
  }

  @Override
  public void stop() {
    // nothing to do
  }
}
