/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.server.measure.live;

import java.util.Collection;
import java.util.Optional;
import java.util.function.BiConsumer;
import org.sonar.api.measures.Metric;
import org.sonar.db.component.ComponentDto;
import org.sonar.server.measure.DebtRatingGrid;
import org.sonar.server.measure.Rating;

import static java.util.Collections.emptyList;

class IssueMetricFormula {

  private final Metric metric;
  private final boolean onLeak;
  private final BiConsumer<Context, IssueCounter> formula;
  private final Collection<Metric> dependentMetrics;

  IssueMetricFormula(Metric metric, boolean onLeak, BiConsumer<Context, IssueCounter> formula) {
    this(metric, onLeak, formula, emptyList());
  }

  IssueMetricFormula(Metric metric, boolean onLeak, BiConsumer<Context, IssueCounter> formula, Collection<Metric> dependentMetrics) {
    this.metric = metric;
    this.onLeak = onLeak;
    this.formula = formula;
    this.dependentMetrics = dependentMetrics;
  }

  Metric getMetric() {
    return metric;
  }

  boolean isOnLeak() {
    return onLeak;
  }

  Collection<Metric> getDependentMetrics() {
    return dependentMetrics;
  }

  void compute(Context context, IssueCounter issues) {
    formula.accept(context, issues);
  }

  interface Context {
    ComponentDto getComponent();

    DebtRatingGrid getDebtRatingGrid();

    /**
     * Value that was just refreshed, otherwise value as computed
     * during last analysis.
     * The metric must be declared in the formula dependencies
     * (see {@link IssueMetricFormula#getDependentMetrics()}).
     */
    Optional<Double> getValue(Metric metric);

    Optional<Double> getLeakValue(Metric metric);

    void setValue(double value);

    void setValue(Rating value);

    void setLeakValue(double value);

    void setLeakValue(Rating value);
  }
}
