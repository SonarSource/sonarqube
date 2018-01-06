/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
 * mailto:info AT sonarsource DOT com
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
package org.sonar.api.web;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class CriterionTest {
  @Rule
  public ExpectedException exception = ExpectedException.none();

  @Test
  public void should_accept_valid_operators() {
    Criterion.createForMetric("", "lte", "", false);
    Criterion.createForMetric("", "lt", "", false);
    Criterion.createForMetric("", "eq", "", false);
    Criterion.createForMetric("", "gt", "", false);
    Criterion.createForMetric("", "gte", "", false);
  }

  @Test
  public void should_fail_on_invalid_operators() {
    exception.expect(IllegalArgumentException.class);
    exception.expectMessage("Valid operators are [eq, gt, gte, lt, lte], not 'xxx'");

    Criterion.createForMetric("", "xxx", "", false);
  }
}
