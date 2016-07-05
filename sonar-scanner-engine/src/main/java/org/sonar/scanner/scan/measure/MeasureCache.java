/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.scanner.scan.measure;

import com.google.common.base.Preconditions;
import javax.annotation.CheckForNull;
import org.sonar.api.batch.ScannerSide;
import org.sonar.api.batch.measure.MetricFinder;
import org.sonar.api.measures.Measure;
import org.sonar.api.resources.Resource;
import org.sonar.scanner.index.Cache;
import org.sonar.scanner.index.Caches;
import org.sonar.scanner.index.Cache.Entry;

/**
 * Cache of all measures. This cache is shared amongst all project modules.
 */
@ScannerSide
public class MeasureCache {

  private final Cache<Measure> cache;

  public MeasureCache(Caches caches, MetricFinder metricFinder) {
    caches.registerValueCoder(Measure.class, new MeasureValueCoder(metricFinder));
    cache = caches.createCache("measures");
  }

  public Iterable<Entry<Measure>> entries() {
    return cache.entries();
  }

  public Iterable<Measure> all() {
    return cache.values();
  }

  public Iterable<Measure> byResource(Resource r) {
    return byComponentKey(r.getEffectiveKey());
  }

  public Iterable<Measure> byComponentKey(String effectiveKey) {
    return cache.values(effectiveKey);
  }

  @CheckForNull
  public Measure byMetric(Resource r, String metricKey) {
    return byMetric(r.getEffectiveKey(), metricKey);
  }

  @CheckForNull
  public Measure byMetric(String componentKey, String metricKey) {
    return cache.get(componentKey, metricKey);
  }

  public MeasureCache put(Resource resource, Measure measure) {
    Preconditions.checkNotNull(resource.getEffectiveKey());
    Preconditions.checkNotNull(measure.getMetricKey());
    cache.put(resource.getEffectiveKey(), measure.getMetricKey(), measure);
    return this;
  }

  public boolean contains(Resource resource, Measure measure) {
    Preconditions.checkNotNull(resource.getEffectiveKey());
    Preconditions.checkNotNull(measure.getMetricKey());
    return cache.containsKey(resource.getEffectiveKey(), measure.getMetricKey());
  }

}
