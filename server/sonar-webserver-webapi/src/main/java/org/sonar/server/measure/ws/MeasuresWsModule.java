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
package org.sonar.server.measure.ws;

import org.sonar.core.platform.Module;

public class MeasuresWsModule extends Module {
  @Override
  protected void configureModule() {
    add(
      MeasuresWs.class,
      ComponentTreeAction.class,
      ComponentAction.class,
      SearchAction.class,
      SearchHistoryAction.class);
  }


  public static String getDeprecatedMetricsInSonarQube93() {
    return String.join(", ", "releasability_effort", "security_rating_effort", "reliability_rating_effort", "security_review_rating_effort",
      "maintainability_rating_effort", "last_change_on_maintainability_rating", "last_change_on_releasability_rating", "last_change_on_reliability_rating",
      "last_change_on_security_rating", "last_change_on_security_review_rating");
  }
}
