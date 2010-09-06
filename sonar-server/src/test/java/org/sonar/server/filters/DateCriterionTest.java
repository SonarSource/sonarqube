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
package org.sonar.server.filters;

import org.junit.Test;

import java.text.ParseException;
import java.util.Date;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class DateCriterionTest {
  private static final int DAYS = 24 * 60 * 60 * 1000;

  @Test
  public void ignoreTime() throws ParseException {
    DateCriterion criterion = new DateCriterion().setDate(3);
    Date date = criterion.getDate();
    assertThat(date.getHours(), is(0));
    assertThat(date.getMinutes(), is(0));
  }
  
  @Test
  public void testDaysAgo() throws ParseException {
    DateCriterion criterion = new DateCriterion().setDate(3);
    Date date = criterion.getDate();
    assertTrue(date.before(new Date(System.currentTimeMillis() - 2 * DAYS)));
    assertTrue(date.after(new Date(System.currentTimeMillis() - 4 * DAYS)));
  }
}
