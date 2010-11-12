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
package org.sonar.api.purge;

import org.apache.commons.configuration.Configuration;

public final class PurgeUtils {

  public static final int DEFAULT_MINIMUM_PERIOD_IN_HOURS = 12;
  public static final String PROP_KEY_MINIMUM_PERIOD_IN_HOURS = "sonar.purge.minimumPeriodInHours";

  private PurgeUtils() {
    // only static methods
  }

  public static int getMinimumPeriodInHours(Configuration conf) {
    int hours = DEFAULT_MINIMUM_PERIOD_IN_HOURS;
    if (conf != null) {
      hours = conf.getInt(PROP_KEY_MINIMUM_PERIOD_IN_HOURS, DEFAULT_MINIMUM_PERIOD_IN_HOURS);
    }
    return hours;
  }
}
