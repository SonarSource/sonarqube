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
package org.sonar.core.technicaldebt.functions;

import org.sonar.api.issue.Issue;
import org.sonar.api.rules.Violation;
import org.sonar.core.technicaldebt.TechnicalDebtRequirement;
import org.sonar.core.technicaldebt.WorkUnitConverter;

import java.util.Collection;

public abstract class AbstractFunction implements Function {

  private WorkUnitConverter converter;

  public AbstractFunction(WorkUnitConverter converter) {
    this.converter = converter;
  }

  protected WorkUnitConverter getConverter() {
    return converter;
  }

  public abstract String getKey();

  public abstract double costInHours(TechnicalDebtRequirement requirement, Collection<Violation> violations);

  public abstract long costInMinutes(TechnicalDebtRequirement requirement, Issue issue);

  protected long factorInMinutes(TechnicalDebtRequirement requirement) {
    return getConverter().toMinutes(requirement.getRemediationFactor());
  }

}
