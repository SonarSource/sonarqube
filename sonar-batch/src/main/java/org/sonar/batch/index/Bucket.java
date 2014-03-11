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
package org.sonar.batch.index;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.MeasuresFilter;
import org.sonar.api.measures.MeasuresFilters;
import org.sonar.api.resources.Resource;
import org.sonar.api.utils.SonarException;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public final class Bucket {

  private Resource resource;
  private ListMultimap<String, Measure> measuresByMetric = ArrayListMultimap.create();

  private Bucket parent;
  private List<Bucket> children;

  public Bucket(Resource resource) {
    this.resource = resource;
  }

  public Resource getResource() {
    return resource;
  }

  public Bucket setParent(Bucket parent) {
    this.parent = parent;
    if (parent != null) {
      parent.addChild(this);
    }
    return this;
  }

  private Bucket addChild(Bucket child) {
    if (children == null) {
      children = Lists.newArrayList();
    }
    children.add(child);
    return this;
  }

  private void removeChild(Bucket child) {
    if (children != null) {
      children.remove(child);
    }
  }

  public List<Bucket> getChildren() {
    return children == null ? Collections.<Bucket>emptyList() : children;
  }

  public Bucket getParent() {
    return parent;
  }

  public void addMeasure(Measure measure) {
    List<Measure> metricMeasures = measuresByMetric.get(measure.getMetric().getKey());

    boolean add = true;
    if (metricMeasures != null) {
      int index = metricMeasures.indexOf(measure);
      if (index > -1) {
        if (metricMeasures.get(index) == measure) {
          add = false;
        } else {
          throw new SonarException("Can not add twice the same measure on " + resource + ": " + measure);
        }
      }
    }
    if (add) {
      measuresByMetric.put(measure.getMetric().getKey(), measure);
    }
  }

  public void clear() {
    measuresByMetric = null;
    children = null;
    if (parent != null) {
      parent.removeChild(this);
      parent = null;
    }
  }

  public <M> M getMeasures(final MeasuresFilter<M> filter) {
    Collection<Measure> unfiltered;
    if (filter instanceof MeasuresFilters.MetricFilter) {
      unfiltered = measuresByMetric.get(((MeasuresFilters.MetricFilter) filter).filterOnMetricKey());
    } else {
      unfiltered = measuresByMetric.values();
    }
    return filter.filter(unfiltered);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Bucket that = (Bucket) o;
    return resource.equals(that.resource);
  }

  @Override
  public int hashCode() {
    return resource.hashCode();
  }
}
