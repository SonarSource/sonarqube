/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import java.util.OptionalDouble;
import java.util.function.BiConsumer;
import org.sonar.api.measures.Metric;
import org.sonar.db.component.ComponentDto;
import org.sonar.server.computation.task.projectanalysis.qualitymodel.DebtRatingGrid;
import org.sonar.server.computation.task.projectanalysis.qualitymodel.Rating;

import static java.util.Collections.emptyList;

public class IssueMetricFormula {

  private final Metric metric;
  private final boolean onLeak;
  private final BiConsumer<Context, IssueCounter> formula;
  private final Collection<Metric> dependentMetrics;

  public IssueMetricFormula(Metric metric, boolean onLeak, BiConsumer<Context, IssueCounter> formula) {
    this(metric, onLeak, formula, emptyList());
  }

  public IssueMetricFormula(Metric metric, boolean onLeak, BiConsumer<Context, IssueCounter> formula, Collection<Metric> dependentMetrics) {
    this.metric = metric;
    this.onLeak = onLeak;
    this.formula = formula;
    this.dependentMetrics = dependentMetrics;
  }

  public Metric getMetric() {
    return metric;
  }

  public boolean isOnLeak() {
    return onLeak;
  }

  public Collection<Metric> getDependentMetrics() {
    return dependentMetrics;
  }

  public void compute(Context context, IssueCounter issues) {
    formula.accept(context, issues);
  }

  interface Context {
    ComponentDto getComponent();

    DebtRatingGrid getDebtRatingGrid();

    /**
     * FIXME improve description
     * If the requested metric is based on issues, then returns the value just computed, else
     * returns the value computed during last analysis.
     */
    OptionalDouble getValue(Metric metric);

    void setValue(double value);

    void setValue(Rating value);
  }
}
