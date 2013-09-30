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
package org.sonar.plugins.core.technicaldebt.functions;

import org.sonar.api.rules.Violation;
import org.sonar.plugins.core.technicaldebt.TechnicalDebtRequirement;
import org.sonar.plugins.core.technicaldebt.WorkUnitConverter;

import java.util.Collection;

public final class LinearWithOffsetFunction extends LinearFunction {

  public static final String FUNCTION_LINEAR_WITH_OFFSET = "linear_offset";

  public LinearWithOffsetFunction(WorkUnitConverter converter) {
    super(converter);
  }

  public String getKey() {
    return FUNCTION_LINEAR_WITH_OFFSET;
  }

  public double calculateCost(TechnicalDebtRequirement requirement, Collection<Violation> violations) {
    if (violations.isEmpty()) {
      return 0.0;
    }
    double minimunCost = getConverter().toDays(requirement.getOffset());
    return minimunCost + super.calculateCost(requirement, violations);
  }

}
