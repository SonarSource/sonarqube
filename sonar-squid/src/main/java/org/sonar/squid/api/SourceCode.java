/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */

package org.sonar.squid.api;

import org.sonar.squid.measures.Measurable;
import org.sonar.squid.measures.Measures;
import org.sonar.squid.measures.Metric;
import org.sonar.squid.measures.MetricDef;

import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

public abstract class SourceCode implements Measurable, Comparable<SourceCode> {

  private final String name;
  private Measures measures = new Measures();
  private final String key;
  private int startAtLine = -1;
  private int endAtLine = -1;
  private SourceCode parent;
  private SortedSet<SourceCode> children;
  private SourceCodeIndexer indexer;
  private Set<CheckMessage> messages;

  public SourceCode(String key) {
    this(key, null);
  }

  public SourceCode(String key, String name) {
    this.key = key;
    this.name = name;
  }

  public String getKey() {
    return key;
  }

  public int compareTo(SourceCode resource) {
    return key.compareTo(resource.getKey());
  }

  public String getName() {
    return name;
  }

  public final void setSourceCodeIndexer(SourceCodeIndexer indexer) {
    this.indexer = indexer;
  }

  private void index(SourceCode sourceCode) {
    if (indexer != null) {
      indexer.index(sourceCode);
    }
  }

  @Override
  public boolean equals(Object obj) {
    return (obj instanceof SourceCode) && key.equals(((SourceCode) obj).key);
  }

  @Override
  public int hashCode() {
    return key.hashCode();
  }

  @Override
  public String toString() {
    return getKey();
  }

  public boolean isType(Class<? extends SourceCode> resourceType) {
    return this.getClass() == resourceType;
  }

  /**
   * {@inheritDoc}
   */
  public int getInt(MetricDef metric) {
    return (int) getMeasure(metric);
  }

  /**
   * @deprecated since 2.1. It's replaced by getInt(MetricDef). It's still defined for binary compatibility.
   */
  @Deprecated
  public int getInt(Metric metric) {
    return (int) getMeasure(metric);
  }

  /**
   * {@inheritDoc}
   */
  public double getDouble(MetricDef metric) {
    return getMeasure(metric);
  }

  /**
   * @deprecated since 2.1. It's replaced by getDouble(MetricDef). It's still defined for binary compatibility.
   */
  @Deprecated
  public double getDouble(Metric metric) {
    return getMeasure(metric);
  }

  public void add(MetricDef metric, SourceCode child) {
    add(metric, child.getMeasure(metric));
  }

  public void add(MetricDef metric, double value) {
    setMeasure(metric, getMeasure(metric) + value);
  }

  public void addData(MetricDef metric, Object data) {
    measures.setData(metric, data);
  }

  public Object getData(MetricDef metric) {
    return measures.getData(metric);
  }

  /**
   * @deprecated since 2.1. It's replaced by getData(MetricDef). It's still defined for binary compatibility.
   */
  @Deprecated
  public Object getData(Metric metric) {
    return measures.getData(metric);
  }

  private double getMeasure(MetricDef metric) {
    if (metric.isCalculatedMetric()) {
      return metric.getCalculatedMetricFormula().calculate(this);
    }
    return measures.getValue(metric);
  }

  /**
   * {@inheritDoc}
   */
  public void setMeasure(MetricDef metric, double measure) {
    if (metric.isCalculatedMetric()) {
      throw new IllegalStateException("It's not allowed to set the value of a calculated metric : " + metric.getName());
    }
    measures.setValue(metric, measure);
  }

  /**
   * {@inheritDoc}
   */
  public void setMeasure(MetricDef metric, int measure) {
    setMeasure(metric, (double) measure);
  }

  public void removeMeasure(MetricDef metric) {
    measures.removeMeasure(metric);
  }

  public void setStartAtLine(int startAtLine) {
    this.startAtLine = startAtLine;
    this.endAtLine = startAtLine;
  }

  public void setEndAtLine(int endAtLine) {
    this.endAtLine = endAtLine;
  }

  public int getStartAtLine() {
    return startAtLine;
  }

  public int getEndAtLine() {
    return endAtLine;
  }

  public SourceCode addChild(SourceCode sourceCode) {
    if (children == null) {
      children = new TreeSet<SourceCode>();
    }
    sourceCode.setParent(this);
    if ( !children.contains(sourceCode)) {
      children.add(sourceCode);
      index(sourceCode);
    }
    return this;
  }

  public <S extends SourceCode> S getParent(Class<S> sourceCode) {
    if (parent == null) {
      return null;
    }
    if (parent.getClass().equals(sourceCode)) {
      return (S) parent;
    }
    return parent.getParent(sourceCode);
  }

  public <S extends SourceCode> S getAncestor(Class<S> withClass) {
    S ancestor = getParent(withClass);
    if (ancestor!=null) {
      S parentAncestor = ancestor.getAncestor(withClass);
      if (parentAncestor!=null) {
        ancestor = parentAncestor;
      }
    }
    return ancestor;
  }

  public void log(CheckMessage message) {
    message.setSourceCode(this);
    getCheckMessages().add(message);
  }

  public Set<CheckMessage> getCheckMessages() {
    if (messages == null) {
      messages = new HashSet<CheckMessage>();
    }
    return messages;
  }

  public boolean hasCheckMessages() {
    return messages != null && !messages.isEmpty();
  }

  public SourceCode getFirstChild() {
    return !children.isEmpty() ? children.first() : null;
  }

  public SourceCode getLastChild() {
    return !children.isEmpty() ? children.last() : null;
  }

  private void setParent(SourceCode parent) {
    this.parent = parent;
  }

  public SourceCode getParent() {
    return parent;
  }

  public Set<SourceCode> getChildren() {
    return children;
  }

  public boolean hasChild(SourceCode squidUnit) {
    if ( !hasChildren()) {
      return false;
    }
    if (children.contains(squidUnit)) {
      return true;
    }
    for (SourceCode child : children) {
      if (child.hasChild(squidUnit)) {
        return true;
      }
    }
    return false;
  }

  public boolean hasChildren() {
    return children != null && children.size() != 0;
  }

  public boolean hasAmongParents(SourceCode expectedParent) {
    if (parent == null) {
      return false;
    }
    return parent.equals(expectedParent) || parent.hasAmongParents(expectedParent);
  }
}
