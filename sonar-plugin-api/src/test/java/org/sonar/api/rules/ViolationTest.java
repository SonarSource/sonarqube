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
package org.sonar.api.rules;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import org.junit.Before;
import org.junit.Test;

public class ViolationTest {
  private Violation violation;

  @Before
  public void setUp() {
    violation = Violation.create((Rule) null, null);
  }

  /**
   * See http://jira.codehaus.org/browse/SONAR-2386
   */
  @Test
  public void testLineIdContract() {
    violation.setLineId(null);
    assertThat(violation.hasLineId(), is(false));
    assertThat(violation.getLineId(), nullValue());

    violation.setLineId(0);
    assertThat(violation.hasLineId(), is(false));
    assertThat(violation.getLineId(), nullValue());

    violation.setLineId(1);
    assertThat(violation.hasLineId(), is(true));
    assertThat(violation.getLineId(), is(1));
  }

  @Test
  public void testCostContract() {
    violation.setCost(null);
    assertThat(violation.getCost(), nullValue());

    violation.setCost(1.0);
    assertThat(violation.getCost(), is(1.0));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testCostContract_NaN() {
    violation.setCost(Double.NaN);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testCostContract_Negative() {
    violation.setCost(-1.0);
  }
}
