/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import org.sonar.api.measures.Metric;
import org.sonar.db.component.ComponentDto;
import org.sonar.server.measure.DebtRatingGrid;
import org.sonar.server.measure.Rating;

import static java.util.Collections.emptyList;

class MeasureUpdateFormula {

  private final Metric metric;
  private final boolean onLeak;
  private final BiConsumer<Context, MeasureUpdateFormula> hierarchyFormula;
  private final BiConsumer<Context, IssueCounter> formula;
  private final Collection<Metric> dependentMetrics;

  /**
   * @param hierarchyFormula Called in a second pass through all the components, after 'formula' is called. Used to calculate the aggregate values for each component.
   *                         For many metrics, we sum the value of the children to the value of the component
   * @param formula          Used to calculate new values for a metric for each component, based on the issue counts
   */
  MeasureUpdateFormula(Metric metric, boolean onLeak, BiConsumer<Context, MeasureUpdateFormula> hierarchyFormula, BiConsumer<Context, IssueCounter> formula) {
    this(metric, onLeak, hierarchyFormula, formula, emptyList());
  }

  MeasureUpdateFormula(Metric metric, boolean onLeak, BiConsumer<Context, MeasureUpdateFormula> hierarchyFormula, BiConsumer<Context, IssueCounter> formula,
    Collection<Metric> dependentMetrics) {
    this.metric = metric;
    this.onLeak = onLeak;
    this.hierarchyFormula = hierarchyFormula;
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

  void computeHierarchy(Context context) {
    hierarchyFormula.accept(context, this);
  }

  interface Context {
    List<Double> getChildrenValues();

    long getChildrenHotspotsReviewed();

    long getChildrenHotspotsToReview();

    long getChildrenNewHotspotsReviewed();

    long getChildrenNewHotspotsToReview();

    ComponentDto getComponent();

    DebtRatingGrid getDebtRatingGrid();

    /**
     * Value that was just refreshed, otherwise value as computed
     * during last analysis.
     * The metric must be declared in the formula dependencies
     * (see {@link MeasureUpdateFormula#getDependentMetrics()}).
     */
    Optional<Double> getValue(Metric metric);

    Optional<String> getText(Metric metrc);

    void setValue(double value);

    void setValue(Rating value);
  }
}
