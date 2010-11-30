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
package org.sonar.plugins.core.timemachine;

import org.apache.commons.configuration.Configuration;
import org.sonar.api.BatchExtension;
import org.sonar.api.CoreProperties;
import org.sonar.api.database.model.Snapshot;

public final class TimeMachineConfiguration implements BatchExtension {

  private Configuration configuration;
  private PeriodLocator periodLocator;

  public TimeMachineConfiguration(Configuration configuration, PeriodLocator periodLocator) {
    this.configuration = configuration;
    this.periodLocator = periodLocator;
  }

  boolean skipTendencies() {
    return configuration.getBoolean(CoreProperties.SKIP_TENDENCIES_PROPERTY, CoreProperties.SKIP_TENDENCIES_DEFAULT_VALUE);
  }

  int getTendencyPeriodInDays() {
    return configuration.getInt(CoreProperties.CORE_TENDENCY_DEPTH_PROPERTY, CoreProperties.CORE_TENDENCY_DEPTH_DEFAULT_VALUE);
  }

  Integer getDiffPeriodInDays(int index) {
    String property = configuration.getString("sonar.timemachine.diff" + index);
    return property == null ? null : Integer.valueOf(property);
  }

  Snapshot getProjectSnapshotForDiffValues(int index) {
    Integer days = getDiffPeriodInDays(index);
    return days == null ? null : periodLocator.locate(days);
  }
}
