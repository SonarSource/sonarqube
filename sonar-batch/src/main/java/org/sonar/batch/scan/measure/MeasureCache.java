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
import org.sonar.api.BatchSide;
import org.sonar.api.batch.measure.MetricFinder;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.RuleMeasure;
import org.sonar.api.resources.Resource;
import org.sonar.api.technicaldebt.batch.Characteristic;
import org.sonar.api.technicaldebt.batch.TechnicalDebtModel;
import org.sonar.batch.index.Cache;
import org.sonar.batch.index.Cache.Entry;
import org.sonar.batch.index.Caches;

/**
 * Cache of all measures. This cache is shared amongst all project modules.
 */
@BatchSide
public class MeasureCache {

  private final Cache<Measure> cache;

  public MeasureCache(Caches caches, MetricFinder metricFinder, TechnicalDebtModel techDebtModel) {
    caches.registerValueCoder(Measure.class, new MeasureValueCoder(metricFinder, techDebtModel));
    cache = caches.createCache("measures");
  }

  public MeasureCache(Caches caches, MetricFinder metricFinder) {
    caches.registerValueCoder(Measure.class, new MeasureValueCoder(metricFinder, null));
    cache = caches.createCache("measures");
  }

  public Iterable<Entry<Measure>> entries() {
    return cache.entries();
  }

  public Iterable<Measure> all() {
    return cache.values();
  }

  public Iterable<Measure> byResource(Resource r) {
    return cache.values(r.getEffectiveKey());
  }

  public Iterable<Measure> byMetric(Resource r, String metricKey) {
    return byMetric(r.getEffectiveKey(), metricKey);
  }

  public Iterable<Measure> byMetric(String resourceKey, String metricKey) {
    return cache.values(resourceKey, metricKey);
  }

  public MeasureCache put(Resource resource, Measure measure) {
    Preconditions.checkNotNull(resource.getEffectiveKey());
    Preconditions.checkNotNull(measure.getMetricKey());
    cache.put(resource.getEffectiveKey(), measure.getMetricKey(), computeMeasureKey(measure), measure);
    return this;
  }

  public boolean contains(Resource resource, Measure measure) {
    Preconditions.checkNotNull(resource.getEffectiveKey());
    Preconditions.checkNotNull(measure.getMetricKey());
    return cache.containsKey(resource.getEffectiveKey(), measure.getMetricKey(), computeMeasureKey(measure));
  }

  private static String computeMeasureKey(Measure m) {
    StringBuilder sb = new StringBuilder();
    if (m.getMetricKey() != null) {
      sb.append(m.getMetricKey());
    }
    sb.append("|");
    Characteristic characteristic = m.getCharacteristic();
    if (characteristic != null) {
      sb.append(characteristic.key());
    }
    sb.append("|");
    Integer personId = m.getPersonId();
    if (personId != null) {
      sb.append(personId);
    }
    if (m instanceof RuleMeasure) {
      sb.append("|");
      sb.append(((RuleMeasure) m).ruleKey());
    }
    return sb.toString();
  }

}
