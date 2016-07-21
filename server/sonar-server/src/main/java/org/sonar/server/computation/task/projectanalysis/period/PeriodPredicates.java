/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.server.computation.task.projectanalysis.period;

import com.google.common.base.Predicate;
import javax.annotation.Nonnull;

public final class PeriodPredicates {
  private PeriodPredicates() {
    // prevents instantiation
  }

  /**
   * Since Periods 4 and 5 can be customized per project and/or per view/subview, aggregating variation on these periods
   * for NEW_* metrics will only generate garbage data which will make no sense. These Periods should be ignored
   * when processing views/subviews.
   */
  public static Predicate<Period> viewsRestrictedPeriods() {
    return ViewsSupportedPeriods.INSTANCE;
  }

  private enum ViewsSupportedPeriods implements Predicate<Period> {
    INSTANCE;

    @Override
    public boolean apply(@Nonnull Period input) {
      return input.getIndex() < 4;
    }
  }
}
