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
package org.sonar.api.batch;

import com.google.common.collect.Lists;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.sonar.api.measures.Metric;
import org.sonar.api.resources.Resource;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * A class to query TimeMachine on a given resource
 * <p/>
 * <p>The query is constructed by setting filters on metrics and on dates</p>
 * <p>It is to be noted that all filters will be applied regardless of their coherence</p>
 *
 * @since 1.10
 */
public class TimeMachineQuery {

  private Resource resource;
  private List<Metric> metrics;
  private List<String> metricKeys;
  private Date from;
  private Date to;
  private boolean onlyLastAnalysis = false;
  private boolean fromCurrentAnalysis = false;
  private boolean toCurrentAnalysis = false;

  /**
   * <p>Create a TimeMachine query for a given resource
   * </p>
   * Apart from the resource the query is empty, i.e. will return all data for the resource
   *
   * @param resource the resource
   */
  public TimeMachineQuery(Resource resource) {
    this.resource = resource;
  }

  /**
   * @return the resource of the Query
   */
  public Resource getResource() {
    return resource;
  }

  /**
   * Sets the resource of the query
   *
   * @param resource the resource
   * @return this
   */
  public TimeMachineQuery setResource(Resource resource) {
    this.resource = resource;
    return this;
  }

  /**
   * @return the metrics beloging to the query
   */
  public List<Metric> getMetrics() {
    return metrics;
  }

  /**
   * Sets the metrics to return
   *
   * @param metrics the list of metrics
   * @return this
   */
  public TimeMachineQuery setMetrics(List<Metric> metrics) {
    this.metrics = metrics;
    this.metricKeys = Lists.newLinkedList();
    for (Metric metric : this.metrics) {
      this.metricKeys.add(metric.getKey());
    }
    return this;
  }

  public TimeMachineQuery setMetricKeys(String... metricKeys) {
    this.metricKeys = Arrays.asList(metricKeys);
    return this;
  }

  public List<String> getMetricKeys() {
    return metricKeys;
  }

  public TimeMachineQuery setMetricKeys(List<String> metricKeys) {
    this.metricKeys = metricKeys;
    return this;
  }

  /**
   * Sets the metrics to return
   *
   * @param metrics the list of metrics
   * @return this
   */
  public TimeMachineQuery setMetrics(Metric... metrics) {
    this.metrics = Arrays.asList(metrics);
    this.metricKeys = Lists.newLinkedList();
    for (Metric metric : this.metrics) {
      this.metricKeys.add(metric.getKey());
    }
    return this;
  }

  /**
   * Unsets the metrics
   *
   * @return this
   */
  public TimeMachineQuery unsetMetrics() {
    this.metrics = null;
    return this;
  }

  /**
   * @return the from date of the query
   */
  public Date getFrom() {
    return from;
  }

  /**
   * Sets the from date to be used in the query
   *
   * @param from the from date
   * @return this
   */
  public TimeMachineQuery setFrom(Date from) {
    this.from = from;
    return this;
  }

  /**
   * @param b whether to use the latest analysis as a from date
   * @return this
   */
  public TimeMachineQuery setFromCurrentAnalysis(boolean b) {
    this.fromCurrentAnalysis = b;
    return this;
  }

  /**
   * @param b whether to use the latest analysis as a to date
   * @return this
   */
  public TimeMachineQuery setToCurrentAnalysis(boolean b) {
    this.toCurrentAnalysis = b;
    return this;
  }

  /**
   * @return whether the latest analysis is used as a from date
   */
  public boolean isFromCurrentAnalysis() {
    return fromCurrentAnalysis;
  }

  /**
   * @return whether the latest analysis is used as a to date
   */
  public boolean isToCurrentAnalysis() {
    return toCurrentAnalysis;
  }

  /**
   * @return the to date of the query
   */
  public Date getTo() {
    return to;
  }

  /**
   * Sets the to date to be used in the query
   *
   * @param to the to date
   * @return this
   */
  public TimeMachineQuery setTo(Date to) {
    this.to = to;
    return this;
  }

  /**
   * @return whether to return only the latest analysis
   */
  public boolean isOnlyLastAnalysis() {
    return onlyLastAnalysis;
  }

  /**
   *
   * @param onlyLastAnalysis whether to only return the latest analysis
   * @return this
   */
  public TimeMachineQuery setOnlyLastAnalysis(boolean onlyLastAnalysis) {
    this.onlyLastAnalysis = onlyLastAnalysis;
    return this;
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this)
        .append("resource", resource)
        .append("metrics", metrics)
        .append("from", from)
        .append("to", to)
        .toString();
  }
}
