/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.batch.index;

import org.sonar.api.BatchComponent;

public class ComponentDataCache implements BatchComponent {
  private final Cache cache;

  public ComponentDataCache(Caches caches) {
    cache = caches.createCache("componentData");
  }

  public <D extends Data> ComponentDataCache setData(String componentKey, String dataType, D data) {
    cache.put(componentKey, dataType, data);
    return this;
  }

  public ComponentDataCache setStringData(String componentKey, String dataType, String data) {
    return setData(componentKey, dataType, new StringData(data));
  }

  public <D extends Data> D getData(String componentKey, String dataType) {
    return (D) cache.get(componentKey, dataType);
  }

  public String getStringData(String componentKey, String dataType) {
    Data data = (Data) cache.get(componentKey, dataType);
    return data==null ? null : ((StringData)data).data();
    }

  public <D extends Data> Iterable<Cache.Entry<D>> entries(String componentKey) {
    return cache.entries(componentKey);
  }
}
