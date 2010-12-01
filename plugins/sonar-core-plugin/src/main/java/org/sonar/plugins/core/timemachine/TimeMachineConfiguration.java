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

import com.google.common.collect.Lists;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.BatchExtension;
import org.sonar.api.CoreProperties;
import org.sonar.api.database.DatabaseSession;
import org.sonar.api.database.model.Snapshot;

import java.util.List;

public final class TimeMachineConfiguration implements BatchExtension {

  private static final int NUMBER_OF_VARIATION_TARGETS = 3;

  private final Configuration configuration;
  private List<VariationTarget> variationTargets;

  public TimeMachineConfiguration(Configuration configuration, DatabaseSession session, PeriodLocator periodLocator) {
    this.configuration = configuration;
    initVariationTargets(periodLocator, session);
  }

  /**
   * for unit tests
   */
  TimeMachineConfiguration(Configuration configuration, List<VariationTarget> variationTargets) {
    this.configuration = configuration;
    this.variationTargets = variationTargets;
  }

  private void initVariationTargets(PeriodLocator periodLocator, DatabaseSession session) {
    variationTargets = Lists.newLinkedList();
    for (int index = 1; index <= NUMBER_OF_VARIATION_TARGETS; index++) {
      VariationTarget target = loadVariationTarget(index, periodLocator);
      if (target != null) {
        save(target, session);
        variationTargets.add(target);
      }
    }
  }

  private void save(VariationTarget target, DatabaseSession session) {
    Snapshot projectSnapshot = target.getProjectSnapshot();
    switch (target.getIndex()) {
      case 1:
        projectSnapshot.setVarMode1("PERIOD_IN_DAYS");
        break;
      case 2:
        projectSnapshot.setVarMode2("PERIOD_IN_DAYS");
        break;
      case 3:
        projectSnapshot.setVarMode3("PERIOD_IN_DAYS");
        break;
    }
    session.save(projectSnapshot);
  }

  private VariationTarget loadVariationTarget(int index, PeriodLocator periodLocator) {
    String property = configuration.getString("sonar.timemachine.variation" + index);
    if (StringUtils.isNotBlank(property)) {
      // todo manage non-integer values
      Snapshot projectSnapshot = periodLocator.locate(Integer.parseInt(property));
      if (projectSnapshot != null) {
        return new VariationTarget(index, projectSnapshot);
      }
    }
    return null;
  }

  public boolean skipTendencies() {
    return configuration.getBoolean(CoreProperties.SKIP_TENDENCIES_PROPERTY, CoreProperties.SKIP_TENDENCIES_DEFAULT_VALUE);
  }

  public int getTendencyPeriodInDays() {
    return configuration.getInt(CoreProperties.CORE_TENDENCY_DEPTH_PROPERTY, CoreProperties.CORE_TENDENCY_DEPTH_DEFAULT_VALUE);
  }

  public List<VariationTarget> getVariationTargets() {
    return variationTargets;
  }
}
