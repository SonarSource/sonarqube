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

import org.sonar.api.ServerComponent;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Facade for all Index components
 */
public class IndexClient implements ServerComponent {

  private final Map<Class<?>, Index<?,?,?>> indexComponents;

  public IndexClient(Index<?,?,?>... indexComponents) {

    this.indexComponents = new HashMap<Class<?>,  Index<?,?,?>>();

    for(Index<?,?,?> indexComponent : indexComponents){
      this.indexComponents.put(indexComponent.getClass(), indexComponent);
    }
  }

  public <K extends Index> K get(Class<K> clazz){
    return (K) this.indexComponents.get(clazz);
  }

  public <K extends Index> K getByType(String indexType) {
    for(Index<?,?,?> index:indexComponents.values()){
      if(index.getIndexType().equals(indexType)){
        return (K) index;
      }
    }
    throw new IllegalStateException("no index for type '"+indexType+"' is registered");
  }

  public Collection<Index<?, ?, ?>> allIndices() {
    return indexComponents.values();
  }
}
