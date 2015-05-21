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

package org.sonar.server.computation.component;

import java.util.HashMap;
import java.util.Map;

/**
 * Cache of components (id, uuid and key) that can be used in the persistence steps
 * Snapshot id will also be added in this cache
 */
public class DbComponentsRefCache {

  private final Map<Integer, DbComponent> componentsByRef;

  public DbComponentsRefCache() {
    componentsByRef = new HashMap<>();
  }

  public DbComponentsRefCache addComponent(Integer ref, DbComponent component) {
    componentsByRef.put(ref, component);
    return this;
  }

  public DbComponent getByRef(Integer ref) {
    DbComponent component = componentsByRef.get(ref);
    if (component == null) {
      throw new IllegalArgumentException(String.format("Component ref '%s' does not exists", ref));
    }
    return componentsByRef.get(ref);
  }

  public static class DbComponent {

    private Long id;
    private String uuid;
    private String key;

    public DbComponent(Long id, String key, String uuid) {
      this.id = id;
      this.key = key;
      this.uuid = uuid;
    }

    public Long getId() {
      return id;
    }

    public String getKey() {
      return key;
    }

    public String getUuid() {
      return uuid;
    }

  }
}
