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
package org.sonar.batch.duplication;

import org.sonar.api.BatchComponent;
import org.sonar.api.batch.sensor.duplication.DuplicationGroup;
import org.sonar.batch.index.Cache;
import org.sonar.batch.index.Cache.Entry;
import org.sonar.batch.index.Caches;

import javax.annotation.CheckForNull;

import java.util.List;

/**
 * Cache of duplication blocks. This cache is shared amongst all project modules.
 */
public class DuplicationCache implements BatchComponent {

  private final Cache<List<DuplicationGroup>> cache;

  public DuplicationCache(Caches caches) {
    caches.registerValueCoder(DuplicationGroup.class, new DuplicationGroupValueCoder());
    cache = caches.createCache("duplications");
  }

  public Iterable<Entry<List<DuplicationGroup>>> entries() {
    return cache.entries();
  }

  @CheckForNull
  public List<DuplicationGroup> byComponent(String effectiveKey) {
    return cache.get(effectiveKey);
  }

  public DuplicationCache put(String effectiveKey, List<DuplicationGroup> blocks) {
    cache.put(effectiveKey, blocks);
    return this;
  }

}
