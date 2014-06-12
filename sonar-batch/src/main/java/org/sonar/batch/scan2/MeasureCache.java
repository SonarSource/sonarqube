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
package org.sonar.batch.scan2;

import com.google.common.base.Preconditions;
import org.sonar.api.BatchComponent;
import org.sonar.batch.api.analyzer.measure.AnalyzerMeasure;
import org.sonar.batch.index.Cache;
import org.sonar.batch.index.Cache.Entry;
import org.sonar.batch.index.Caches;

/**
 * Cache of all measures. This cache is shared amongst all project modules.
 */
public class MeasureCache implements BatchComponent {

  private final Cache<AnalyzerMeasure<?>> cache;

  public MeasureCache(Caches caches) {
    cache = caches.createCache("measures");
  }

  public Iterable<Entry<AnalyzerMeasure<?>>> entries() {
    return cache.entries();
  }

  public AnalyzerMeasure<?> byMetric(String resourceKey, String metricKey) {
    return cache.get(resourceKey, metricKey);
  }

  public MeasureCache put(String resourceKey, AnalyzerMeasure<?> measure) {
    Preconditions.checkNotNull(resourceKey);
    Preconditions.checkNotNull(measure.metricKey());
    cache.put(resourceKey, measure.metricKey(), measure);
    return this;
  }

  public boolean contains(String resourceKey, AnalyzerMeasure<?> measure) {
    Preconditions.checkNotNull(resourceKey);
    Preconditions.checkNotNull(measure);
    return cache.containsKey(resourceKey, measure.metricKey());
  }

  public Iterable<AnalyzerMeasure<?>> all() {
    return cache.values();
  }

}
