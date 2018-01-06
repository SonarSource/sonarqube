/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
 * mailto:info AT sonarsource DOT com
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
import org.sonar.api.batch.sensor.measure.internal.DefaultMeasure;
import org.sonar.scanner.storage.Storage;
import org.sonar.scanner.storage.Storage.Entry;
import org.sonar.scanner.storage.Storages;

/**
 * Cache of all measures. This cache is shared amongst all project modules.
 */
@ScannerSide
public class MeasureCache {

  private final Storage<DefaultMeasure<?>> cache;

  public MeasureCache(Storages caches, MetricFinder metricFinder) {
    caches.registerValueCoder(DefaultMeasure.class, new MeasureValueCoder(metricFinder));
    cache = caches.createCache("measures");
  }

  public Iterable<Entry<DefaultMeasure<?>>> entries() {
    return cache.entries();
  }

  public Iterable<DefaultMeasure<?>> all() {
    return cache.values();
  }

  public Iterable<DefaultMeasure<?>> byComponentKey(String effectiveKey) {
    return cache.values(effectiveKey);
  }

  @CheckForNull
  public DefaultMeasure<?> byMetric(String componentKey, String metricKey) {
    return cache.get(componentKey, metricKey);
  }

  public MeasureCache put(String componentKey, String metricKey, DefaultMeasure<?> measure) {
    Preconditions.checkNotNull(componentKey);
    Preconditions.checkNotNull(metricKey);
    cache.put(componentKey, metricKey, measure);
    return this;
  }

  public boolean contains(String componentKey, String metricKey) {
    Preconditions.checkNotNull(componentKey);
    Preconditions.checkNotNull(metricKey);
    return cache.containsKey(componentKey, metricKey);
  }

}
