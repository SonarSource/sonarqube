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
package org.sonar.scanner.index;

import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import org.sonar.api.batch.fs.InputComponent;
import org.sonar.api.batch.fs.InputDir;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.InputModule;
import org.sonar.api.batch.fs.internal.DefaultInputModule;
import org.sonar.api.batch.fs.internal.InputComponentTree;
import org.sonar.api.batch.measure.MetricFinder;
import org.sonar.api.batch.sensor.measure.internal.DefaultMeasure;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.MeasuresFilter;
import org.sonar.api.measures.MeasuresFilters;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.Metric.ValueType;
import org.sonar.api.resources.Directory;
import org.sonar.api.resources.File;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.scanner.scan.filesystem.InputComponentStore;
import org.sonar.scanner.scan.measure.MeasureCache;
import org.sonar.scanner.sensor.DefaultSensorStorage;

public class DefaultIndex {
  private final InputComponentStore componentStore;
  private final MeasureCache measureCache;
  private final MetricFinder metricFinder;
  // caches
  private DefaultSensorStorage sensorStorage;

  private InputComponentTree tree;

  public DefaultIndex(InputComponentStore componentStore, InputComponentTree tree, MeasureCache measureCache, MetricFinder metricFinder) {
    this.componentStore = componentStore;
    this.tree = tree;
    this.measureCache = measureCache;
    this.metricFinder = metricFinder;
  }

  public void setCurrentStorage(DefaultSensorStorage sensorStorage) {
    // the following components depend on the current module, so they need to be reloaded.
    this.sensorStorage = sensorStorage;
  }

  @CheckForNull
  public Measure getMeasure(String key, org.sonar.api.batch.measure.Metric<?> metric) {
    return getMeasures(key, MeasuresFilters.metric(metric));
  }

  @CheckForNull
  public <M> M getMeasures(String key, MeasuresFilter<M> filter) {
    Collection<DefaultMeasure<?>> unfiltered = new ArrayList<>();
    if (filter instanceof MeasuresFilters.MetricFilter) {
      // optimization
      DefaultMeasure<?> byMetric = measureCache.byMetric(key, ((MeasuresFilters.MetricFilter<M>) filter).filterOnMetricKey());
      if (byMetric != null) {
        unfiltered.add(byMetric);
      }
    } else {
      for (DefaultMeasure<?> measure : measureCache.byComponentKey(key)) {
        unfiltered.add(measure);
      }
    }
    return filter.filter(unfiltered.stream().map(DefaultIndex::toDeprecated).collect(Collectors.toList()));
  }

  private static Measure toDeprecated(org.sonar.api.batch.sensor.measure.Measure<?> measure) {
    Measure deprecatedMeasure = new Measure((Metric<?>) measure.metric());
    setValueAccordingToMetricType(measure, deprecatedMeasure);
    return deprecatedMeasure;
  }

  private static void setValueAccordingToMetricType(org.sonar.api.batch.sensor.measure.Measure<?> measure, Measure measureToSave) {
    ValueType deprecatedType = ((Metric<?>) measure.metric()).getType();
    switch (deprecatedType) {
      case BOOL:
        measureToSave.setValue(Boolean.TRUE.equals(measure.value()) ? 1.0 : 0.0);
        break;
      case INT:
      case MILLISEC:
      case WORK_DUR:
      case FLOAT:
      case PERCENT:
      case RATING:
        measureToSave.setValue(((Number) measure.value()).doubleValue());
        break;
      case STRING:
      case LEVEL:
      case DATA:
      case DISTRIB:
        measureToSave.setData((String) measure.value());
        break;
      default:
        throw new UnsupportedOperationException("Unsupported type :" + deprecatedType);
    }
  }

  public Measure addMeasure(String key, Measure measure) {
    InputComponent component = componentStore.getByKey(key);
    if (component == null) {
      throw new IllegalStateException("Invalid component key: " + key);
    }
    if (DefaultSensorStorage.isDeprecatedMetric(measure.getMetricKey())) {
      // Ignore deprecated metrics
      return measure;
    }
    org.sonar.api.batch.measure.Metric<?> metric = metricFinder.findByKey(measure.getMetricKey());
    if (metric == null) {
      throw new UnsupportedOperationException("Unknown metric: " + measure.getMetricKey());
    }
    DefaultMeasure<?> newMeasure;
    if (Boolean.class.equals(metric.valueType())) {
      newMeasure = new DefaultMeasure<Boolean>().forMetric((Metric<Boolean>) metric)
        .withValue(measure.getValue() != 0.0);
    } else if (Integer.class.equals(metric.valueType())) {
      newMeasure = new DefaultMeasure<Integer>().forMetric((Metric<Integer>) metric)
        .withValue(measure.getValue().intValue());
    } else if (Double.class.equals(metric.valueType())) {
      newMeasure = new DefaultMeasure<Double>().forMetric((Metric<Double>) metric)
        .withValue(measure.getValue());
    } else if (String.class.equals(metric.valueType())) {
      newMeasure = new DefaultMeasure<String>().forMetric((Metric<String>) metric)
        .withValue(measure.getData());
    } else if (Long.class.equals(metric.valueType())) {
      newMeasure = new DefaultMeasure<Long>().forMetric((Metric<Long>) metric)
        .withValue(measure.getValue().longValue());
    } else {
      throw new UnsupportedOperationException("Unsupported type :" + metric.valueType());
    }
    sensorStorage.saveMeasure(component, newMeasure);
    return measure;
  }

  /**
   * @param key Effective key, without branch
   */
  @CheckForNull
  public Resource getParent(String key) {
    InputComponent component = componentStore.getByKey(key);
    if (component == null) {
      return null;
    }
    InputComponent parent = tree.getParent(component);
    if (parent == null) {
      return null;
    }

    return toResource(parent);
  }

  /**
   * @param key Effective key, without branch
   */
  public Collection<Resource> getChildren(String key) {
    InputComponent component = componentStore.getByKey(key);
    Collection<InputComponent> children = tree.getChildren(component);
    return children.stream().map(this::toResource).collect(Collectors.toList());
  }

  public Resource toResource(InputComponent inputComponent) {
    Resource r;
    if (inputComponent instanceof InputDir) {
      r = Directory.create(((InputDir) inputComponent).relativePath());
    } else if (inputComponent instanceof InputFile) {
      r = File.create(((InputFile) inputComponent).relativePath());
    } else if (inputComponent instanceof InputModule) {
      r = new Project(((DefaultInputModule) inputComponent));
    } else {
      throw new IllegalArgumentException("Unknow input path type: " + inputComponent);
    }

    return r;
  }

  /**
   * Gets a component from the store as a resource.
   * @param key Effective key, without branch
   */
  @CheckForNull
  public Resource getResource(String key) {
    InputComponent component = componentStore.getByKey(key);
    if (component == null) {
      return null;
    }
    return toResource(component);
  }
}
