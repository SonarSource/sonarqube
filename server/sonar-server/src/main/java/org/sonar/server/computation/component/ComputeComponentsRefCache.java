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
 * Cache of components (uuid and key) that can be used in compute steps (For instance for issue computation)
 */
public class ComputeComponentsRefCache {

  private final Map<Integer, ComputeComponent> componentsByRef;

  public ComputeComponentsRefCache() {
    componentsByRef = new HashMap<>();
  }

  public ComputeComponentsRefCache addComponent(Integer ref, ComputeComponent computeComponent) {
    componentsByRef.put(ref, computeComponent);
    return this;
  }

  public ComputeComponent getByRef(Integer ref) {
    ComputeComponent computeComponent = componentsByRef.get(ref);
    if (computeComponent == null) {
      throw new IllegalArgumentException(String.format("Component ref '%s' does not exists", ref));
    }
    return componentsByRef.get(ref);
  }

  public static class ComputeComponent {
    private String uuid;
    private String key;

    public ComputeComponent(String key, String uuid) {
      this.key = key;
      this.uuid = uuid;
    }

    public String getKey() {
      return key;
    }

    public String getUuid() {
      return uuid;
    }

  }
}
