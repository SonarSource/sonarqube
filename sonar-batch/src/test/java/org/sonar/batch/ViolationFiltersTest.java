/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
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
package org.sonar.batch;

import org.junit.Test;
import org.sonar.api.rules.Violation;
import org.sonar.api.rules.ViolationFilter;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class ViolationFiltersTest {

  @Test
  public void doNotFailIfNoFilters() {
    ViolationFilters filters = new ViolationFilters();
    assertThat(filters.isIgnored(new Violation(null)), is(false));
  }

  @Test
  public void ignoreViolation() {
    ViolationFilters filters = new ViolationFilters(new ViolationFilter[]{
        new FakeFilter(false),
        new FakeFilter(true),
        new FakeFilter(false),
    });
    assertThat(filters.isIgnored(new Violation(null)), is(true));
  }

  @Test
  public void doNotIgnoreValidViolations() {
    ViolationFilters filters = new ViolationFilters(new ViolationFilter[]{
        new FakeFilter(false),
        new FakeFilter(false),
        new FakeFilter(false),
    });
    assertThat(filters.isIgnored(new Violation(null)), is(false));
  }

  private static class FakeFilter implements ViolationFilter {
    private boolean ignore;

    private FakeFilter(boolean ignore) {
      this.ignore = ignore;
    }

    public boolean isIgnored(Violation violation) {
      return ignore;
    }
  }
}
