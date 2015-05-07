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

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import org.sonar.api.BatchSide;
import org.sonar.api.batch.sensor.duplication.internal.DefaultDuplication;
import org.sonar.batch.index.Cache;
import org.sonar.batch.index.Caches;

/**
 * Cache of duplication blocks. This cache is shared amongst all project modules.
 */
@BatchSide
public class DuplicationCache {

  private final Cache<DefaultDuplication> cache;
  private int sequence = 1;

  public DuplicationCache(Caches caches) {
    caches.registerValueCoder(DefaultDuplication.class, new DefaultDuplicationValueCoder());
    cache = caches.createCache("duplications");
  }

  public Iterable<String> componentKeys() {
    return Iterables.transform(cache.keySet(), new Function<Object, String>() {
      @Override
      public String apply(Object input) {
        return input.toString();
      }
    });
  }

  public Iterable<DefaultDuplication> byComponent(String effectiveKey) {
    return cache.values(effectiveKey);
  }

  public DuplicationCache put(String effectiveKey, DefaultDuplication duplication) {
    cache.put(effectiveKey, sequence, duplication);
    sequence++;
    return this;
  }

}
