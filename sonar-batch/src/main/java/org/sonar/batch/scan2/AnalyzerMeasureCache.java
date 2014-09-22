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
import org.sonar.api.batch.measure.MetricFinder;
import org.sonar.api.batch.sensor.measure.internal.DefaultMeasure;
import org.sonar.batch.index.Cache;
import org.sonar.batch.index.Cache.Entry;
import org.sonar.batch.index.Caches;
import org.sonar.batch.scan.filesystem.InputPathCache;

/**
 * Cache of all measures. This cache is shared amongst all project modules.
 */
public class AnalyzerMeasureCache implements BatchComponent {

  // project key -> component key -> metric key -> measure
  private final Cache<DefaultMeasure> cache;

  public AnalyzerMeasureCache(Caches caches, MetricFinder metricFinder, InputPathCache inputPathCache) {
    caches.registerValueCoder(DefaultMeasure.class, new DefaultMeasureValueCoder(metricFinder, inputPathCache));
    cache = caches.createCache("measures");
  }

  public Iterable<Entry<DefaultMeasure>> entries() {
    return cache.entries();
  }

  public Iterable<DefaultMeasure> byModule(String projectKey) {
    return cache.values(projectKey);
  }

  public DefaultMeasure<?> byMetric(String projectKey, String resourceKey, String metricKey) {
    return cache.get(projectKey, resourceKey, metricKey);
  }

  public AnalyzerMeasureCache put(String projectKey, String resourceKey, DefaultMeasure<?> measure) {
    Preconditions.checkNotNull(projectKey);
    Preconditions.checkNotNull(resourceKey);
    Preconditions.checkNotNull(measure);
    cache.put(projectKey, resourceKey, measure.metric().key(), measure);
    return this;
  }

  public boolean contains(String projectKey, String resourceKey, DefaultMeasure<?> measure) {
    Preconditions.checkNotNull(projectKey);
    Preconditions.checkNotNull(resourceKey);
    Preconditions.checkNotNull(measure);
    return cache.containsKey(projectKey, resourceKey, measure.metric().key());
  }

  public Iterable<DefaultMeasure> all() {
    return cache.values();
  }

}
