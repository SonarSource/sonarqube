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

package org.sonar.server.computation.step;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import org.sonar.batch.protocol.output.BatchReportReader;

import java.util.concurrent.ExecutionException;

/**
 * Waiting for having all components persisted by the Compute Engine, this class contains a cache of component uuids by their report reference
 */
public class ComponentUuidsCache {

  private final Cache<Integer, String> componentRefToUuidCache;

  public ComponentUuidsCache(final BatchReportReader reader) {
    this.componentRefToUuidCache = CacheBuilder.newBuilder()
      .maximumSize(500_000)
      .build(
        new CacheLoader<Integer, String>() {
          @Override
          public String load(Integer ref) {
            return reader.readComponent(ref).getUuid();
          }
        });
  }

  public String getUuidFromRef(int componentRef) {
    try {
      return componentRefToUuidCache.get(componentRef);
    } catch (ExecutionException e) {
      throw new IllegalStateException(String.format("Error while retrieving uuid of component ref '%d'", componentRef), e);
    }
  }
}
