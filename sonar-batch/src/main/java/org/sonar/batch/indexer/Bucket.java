/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.batch.indexer;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import org.sonar.api.database.model.Snapshot;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.MeasuresFilter;
import org.sonar.api.measures.MeasuresFilters;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class Bucket<RESOURCE extends Resource> {

  private RESOURCE resource;
  private Snapshot snapshot;
  private ListMultimap<String, Measure> measuresByMetric = ArrayListMultimap.create();
  private boolean sourceSaved = false;
  private Bucket<Project> project;
  private Bucket<?> parent;
  private List<Bucket<?>> children;

  public Bucket(RESOURCE resource) {
    this.resource = resource;
  }

  public RESOURCE getResource() {
    return resource;
  }

  public Bucket<Project> getProject() {
    return project;
  }

  public Bucket<RESOURCE> setProject(Bucket<Project> project) {
    this.project = project;
    return this;
  }

  public Snapshot getSnapshot() {
    return snapshot;
  }

  public Integer getSnapshotId() {
    if (snapshot != null) {
      return snapshot.getId();
    }
    return null;
  }

  public void setSnapshot(Snapshot snapshot) {
    this.snapshot = snapshot;
  }

  public void setParent(Bucket parent) {
    this.parent = parent;
    if (parent != null) {
      parent.addChild(this);
    }
  }

  private void addChild(Bucket bucket) {
    if (children == null) {
      children = Lists.newArrayList();
    }
    children.add(bucket);
  }

  private void removeChild(Bucket bucket) {
    if (children != null) {
      children.remove(bucket);
    }
  }

  public List<Bucket<?>> getChildren() {
    if (children == null) {
      return Collections.emptyList();
    }
    return children;
  }

  public Bucket getParent() {
    return parent;
  }

  public void addMeasure(Measure measure) {
    measuresByMetric.put(measure.getMetric().getKey(), measure);
  }

  public boolean isSourceSaved() {
    return sourceSaved;
  }

  public void setSourceSaved(boolean b) {
    this.sourceSaved = b;
  }

  public void clear() {
    measuresByMetric = null;
    children = null;
    if (parent != null) {
      parent.removeChild(this);
    }
  }

  public boolean isExcluded() {
    return resource.isExcluded();
  }

  public boolean isPersisted() {
    return resource.getId() != null;
  }

  public Integer getResourceId() {
    return resource.getId();
  }

  public <M> M getMeasures(final MeasuresFilter<M> filter) {
    Collection<Measure> unfiltered;
    if (filter instanceof MeasuresFilters.MetricFilter) {
      unfiltered = measuresByMetric.get(((MeasuresFilters.MetricFilter) filter).filterOnMetric().getKey());
    } else {
      unfiltered = measuresByMetric.values();
    }
    return filter.filter(unfiltered);
  }
}
