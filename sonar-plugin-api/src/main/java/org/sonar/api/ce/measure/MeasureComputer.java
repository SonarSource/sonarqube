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

package org.sonar.api.ce.measure;

import java.util.List;
import java.util.Set;
import javax.annotation.CheckForNull;

/**
 * This class is used to define which metrics are required to compute some measures on some given metrics, and to define the implementation of the measures computation
 */
public interface MeasureComputer {

  /**
   * Return the metric keys that can be read using {@link Implementation.Context}.
   *
   * Can never be empty as it's checked in the builder
   */
  Set<String> getInputMetrics();

  /**
   * Return the metric keys that can be create using {@link Implementation.Context}.
   *
   * Can never ne empty as it's checked om the builder
   */
  Set<String> getOutputMetrics();

  Implementation getImplementation();

  interface MeasureComputerBuilder {

    /**
     * Input metrics can be empty (for instance when only issues are needed)
     * @throws NullPointerException if inputMetrics is null
     * @throws NullPointerException if the metrics contains a {@code null}
     * */
    MeasureComputerBuilder setInputMetrics(String... inputMetrics);

    /**
     * @throws IllegalArgumentException if there's not at least one output metrics
     * @throws NullPointerException if the metrics contains a {@code null}
     */
    MeasureComputerBuilder setOutputMetrics(String... outMetrics);

    /**
     * @throws NullPointerException if there's no implementation
     */
    MeasureComputerBuilder setImplementation(Implementation impl);

    /**
     * @throws NullPointerException if inputMetrics is null
     * @throws NullPointerException if inputs metrics contains a {@code null}
     * @throws IllegalArgumentException if there's not at least one output metrics
     * @throws NullPointerException if outputs metrics contains a {@code null}
     * @throws NullPointerException if there's no implementation
     */
    MeasureComputer build();
  }

  /**
   * This interface must be implemented to define how the measures are computed.
   */
  interface Implementation {

    /**
     * This method will be called on each component of the projects.
     */
    void compute(Context ctx);

    /**
     * Context specific to the computation of the measure(s) of a given component
     */
    interface Context {

      /**
       * Returns the current component.
       */
      Component getComponent();

      /**
       * Returns settings of the current component.
       */
      Settings getSettings();

      /**
       * Returns the measure from a given metric on the current component.
       *
       * @throws IllegalArgumentException if the metric is not listed in {@link MeasureComputer#getInputMetrics()}
       */
      @CheckForNull
      Measure getMeasure(String metric);

      /**
       * Returns measures from a given metric on children of the current component.
       * It no measure is found for a child, this measure is ignored
       *
       * @throws IllegalArgumentException if the metric is not listed in {@link MeasureComputer#getInputMetrics()} or in {@link MeasureComputer#getOutputMetrics()}
       */
      Iterable<Measure> getChildrenMeasures(String metric);

      /**
       * Add a new measure of a given metric which measure type will be int
       *
       * @throws IllegalArgumentException if the metric is not listed in {@link MeasureComputer#getOutputMetrics()}
       * @throws UnsupportedOperationException if a measure for the specified metric already exists for the current component
       */
      void addMeasure(String metric, int value);

      /**
       * Add a new measure of a given metric which measure type will be double
       *
       * @throws IllegalArgumentException if the metric is not listed in {@link MeasureComputer#getOutputMetrics()}
       * @throws UnsupportedOperationException if a measure for the specified metric already exists for the current component
       */
      void addMeasure(String metric, double value);

      /**
       * Add a new measure of a given metric which measure type will be long
       *
       * @throws IllegalArgumentException if the metric is not listed in {@link MeasureComputer#getOutputMetrics()}
       * @throws UnsupportedOperationException if a measure for the specified metric already exists for the current component
       */
      void addMeasure(String metric, long value);

      /**
       * Add a new measure of a given metric which measure type will be string
       *
       * @throws IllegalArgumentException if the metric is not listed in {@link MeasureComputer#getOutputMetrics()}
       * @throws UnsupportedOperationException if a measure for the specified metric already exists for the current component
       */
      void addMeasure(String metric, String value);

      /**
       * Return list of issues of current component.
       */
      List<? extends Issue> getIssues();

    }
  }
}
