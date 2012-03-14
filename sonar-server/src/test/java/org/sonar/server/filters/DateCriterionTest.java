/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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

import org.apache.commons.lang.time.DateUtils;
import org.junit.Test;

import java.text.ParseException;
import java.util.Date;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class DateCriterionTest {
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
    assertThat(date.getMinutes(), is(0));
    assertThat(date.getHours(), is(0));
    assertThat(DateUtils.isSameDay(date, DateUtils.addDays(new Date(), -3)), is(true));
  }
}
