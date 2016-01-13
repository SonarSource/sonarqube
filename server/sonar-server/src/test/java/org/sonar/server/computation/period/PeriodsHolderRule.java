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
package org.sonar.server.computation.period;

import java.util.Arrays;
import java.util.List;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public class PeriodsHolderRule implements TestRule, PeriodsHolder {
  private PeriodsHolderImpl delegate = new PeriodsHolderImpl();

  @Override
  public Statement apply(final Statement statement, Description description) {
    return new Statement() {
      @Override
      public void evaluate() throws Throwable {
        try {
          statement.evaluate();
        } finally {
          clear();
        }
      }
    };
  }

  private void clear() {
    this.delegate = new PeriodsHolderImpl();
  }

  public PeriodsHolderRule setPeriods(Period... periods) {
    delegate = new PeriodsHolderImpl();
    delegate.setPeriods(Arrays.asList(periods));
    return this;
  }

  @Override
  public List<Period> getPeriods() {
    return delegate.getPeriods();
  }

  @Override
  public boolean hasPeriod(int i) {
    return delegate.hasPeriod(i);
  }

  @Override
  public Period getPeriod(int i) {
    return delegate.getPeriod(i);
  }
}
