/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
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
package org.sonar.plugins.core.metrics;

import org.sonar.api.measures.Metric;
import org.sonar.api.measures.Metrics;

import java.util.Arrays;
import java.util.List;

public final class UserManagedMetrics implements Metrics {
  private static final String DOMAIN = "Management";

  public List<Metric> getMetrics() {
    return Arrays.asList(
        new Metric("burned_budget", "Burned budget", "The budget already used in the project", Metric.ValueType.FLOAT,
            Metric.DIRECTION_NONE, false, DOMAIN)
            .setUserManaged(true),
        new Metric("team_size", "Team size", "Size of the project team", Metric.ValueType.INT, Metric.DIRECTION_NONE, false, DOMAIN)
            .setUserManaged(true),
        new Metric("business_value", "Business value", "An indication on the value of the project for the business",
            Metric.ValueType.FLOAT, Metric.DIRECTION_BETTER, true, DOMAIN)
            .setUserManaged(true));
  }
}
