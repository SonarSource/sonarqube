/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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

package org.sonar.squid.measures;

public enum Metric implements MetricDef {

  PACKAGES, CLASSES, ANONYMOUS_INNER_CLASSES, FILES, METHODS, CONSTRUCTORS, STATEMENTS, LINES(false), BLANK_LINES(false), COMMENT_LINES(
      false), HEADER_COMMENT_LINES(false), COMMENTED_OUT_CODE_LINES(false), BRANCHES, PUBLIC_API, PUBLIC_DOC_API, ACCESSORS,
  COMMENT_BLANK_LINES(false), LINES_OF_CODE(false), COMMENT_LINES_WITHOUT_HEADER(new CommentLinesWithoutHeaderFormula()),
  PUBLIC_DOCUMENTED_API_DENSITY(new PublicDocumentedApiDensityFormula()), COMMENT_LINES_DENSITY(new CommentLinesDensityFormula()),
  COMPLEXITY, INTERFACES, ABSTRACT_CLASSES, ABSTRACTNESS(new AbstractnessFormula()), CA(new NoAggregationFormula()), CE(
      new NoAggregationFormula()), INSTABILITY(new InstabilityFormula()), DISTANCE(new DistanceFormula()), DIT(new NoAggregationFormula()),
  RFC(new NoAggregationFormula()), NOC(new NoAggregationFormula()), LCOM4(new NoAggregationFormula()), LCOM4_BLOCKS;

  private CalculatedMetricFormula formula = null;

  private AggregationFormula aggregationFormula = new SumAggregationFormula();

  private boolean aggregateIfThereIsAlreadyAValue = true;

  Metric() {
  }

  Metric(boolean aggregateIfThereIsAlreadyAValue) {
    this.aggregateIfThereIsAlreadyAValue = aggregateIfThereIsAlreadyAValue;
  }

  Metric(AggregationFormula aggregationFormula) {
    this.aggregationFormula = aggregationFormula;
  }

  Metric(CalculatedMetricFormula formula) {
    this.formula = formula;
  }

  public String getName() {
    return name();
  }

  public boolean isCalculatedMetric() {
    return formula != null;
  }

  public boolean aggregateIfThereIsAlreadyAValue() {
    return aggregateIfThereIsAlreadyAValue;
  }

  public boolean isThereAggregationFormula() {
    return !(aggregationFormula instanceof NoAggregationFormula);
  }

  public CalculatedMetricFormula getCalculatedMetricFormula() {
    return formula;
  }

}
