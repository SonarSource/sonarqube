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

package org.sonar.server.technicaldebt;

import org.sonar.api.ServerComponent;
import org.sonar.api.technicaldebt.server.Characteristic;
import org.sonar.api.utils.WorkDuration;
import org.sonar.api.utils.WorkDurationFactory;
import org.sonar.core.technicaldebt.DefaultTechnicalDebtManager;
import org.sonar.server.ui.WorkDurationFormatter;

import javax.annotation.CheckForNull;
import java.util.List;

public class DebtService implements ServerComponent {

  private final WorkDurationFormatter workDurationFormatter;
  private final DefaultTechnicalDebtManager finder;
  private final WorkDurationFactory workDurationFactory;

  public DebtService(WorkDurationFormatter workDurationFormatter, DefaultTechnicalDebtManager finder, WorkDurationFactory workDurationFactory) {
    this.workDurationFormatter = workDurationFormatter;
    this.finder = finder;
    this.workDurationFactory = workDurationFactory;
  }

  public String format(long debt) {
    return workDurationFormatter.format(debt, WorkDurationFormatter.Format.SHORT);
  }

  public WorkDuration toWorkDuration(long debt) {
    return workDurationFactory.createFromMinutes(debt);
  }

  public List<Characteristic> findRootCharacteristics() {
    return finder.findRootCharacteristics();
  }

  @CheckForNull
  public Characteristic findRequirementByRuleId(int ruleId) {
    return finder.findRequirementByRuleId(ruleId);
  }

  @CheckForNull
  public Characteristic findCharacteristic(int id) {
    return finder.findCharacteristicById(id);
  }

}
