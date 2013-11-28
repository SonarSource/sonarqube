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
import org.sonar.api.issue.internal.WorkDayDuration;
import org.sonar.api.rules.Rule;
import org.sonar.api.technicaldebt.Requirement;
import org.sonar.core.technicaldebt.TechnicalDebtFinder;
import org.sonar.core.technicaldebt.TechnicalDebtModel;
import org.sonar.server.user.UserSession;

public class InternalRubyTechnicalDebtService implements ServerComponent {

  private final TechnicalDebtFormatter technicalDebtFormatter;
  private final TechnicalDebtFinder finder;

  public InternalRubyTechnicalDebtService(TechnicalDebtFormatter technicalDebtFormatter, TechnicalDebtFinder finder) {
    this.technicalDebtFormatter = technicalDebtFormatter;
    this.finder = finder;
  }

  public String format(WorkDayDuration technicalDebt){
    return technicalDebtFormatter.format(UserSession.get().locale(), technicalDebt);
  }

  public WorkDayDuration toTechnicalDebt(String technicalDebtInLong){
    return WorkDayDuration.fromLong(Long.parseLong(technicalDebtInLong));
  }

  public TechnicalDebtModel findRootCharacteristics(){
    return finder.findRootCharacteristics();
  }

  public Requirement findRequirement(Rule rule){
    return finder.findRequirement(rule);
  }

}
