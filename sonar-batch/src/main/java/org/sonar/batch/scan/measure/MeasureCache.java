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
package org.sonar.batch.scan.measure;

import com.google.common.base.Preconditions;
import org.sonar.api.BatchComponent;
import org.sonar.api.measures.Measure;
import org.sonar.api.resources.Resource;
import org.sonar.batch.index.Cache;
import org.sonar.batch.index.Cache.Entry;
import org.sonar.batch.index.Caches;

/**
 * Cache of all measures. This cache is shared amongst all project modules.
 */
public class MeasureCache implements BatchComponent {

  private final Cache<Measure> cache;

  public MeasureCache(Caches caches) {
    cache = caches.createCache("measures");
  }

  public Iterable<Entry<Measure>> entries() {
    return cache.entries();
  }

  public Iterable<Measure> byResource(Resource r) {
    return cache.allValues(r.getEffectiveKey());
  }

  public MeasureCache put(Resource resource, Measure measure) {
    Preconditions.checkNotNull(resource.getEffectiveKey());
    Preconditions.checkNotNull(measure.getMetricKey());
    cache.put(resource.getEffectiveKey(), measure.getMetricKey(), measure.hashCode(), measure);
    return this;
  }

  public Iterable<Measure> byMetric(Resource resource, String metricKey) {
    Preconditions.checkNotNull(resource.getEffectiveKey());
    Preconditions.checkNotNull(metricKey);
    return cache.values(resource.getEffectiveKey(), metricKey);
  }
}
