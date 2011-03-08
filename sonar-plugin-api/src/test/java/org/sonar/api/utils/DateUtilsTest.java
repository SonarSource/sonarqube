/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
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
package org.sonar.api.utils;

import org.junit.Test;

import java.util.Date;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.number.OrderingComparisons.greaterThan;
import static org.hamcrest.text.StringStartsWith.startsWith;
import static org.junit.Assert.assertThat;

public class DateUtilsTest {

  @Test
  public void shouldParseDate() {
    Date date = DateUtils.parseDate("2010-05-18");
    assertThat(date.getDate(), is(18));
  }

  @Test(expected = SonarException.class)
  public void shouldNotParseDate() {
    DateUtils.parseDate("2010/05/18");
  }

  @Test
  public void shouldParseDateTime() {
    Date date = DateUtils.parseDateTime("2010-05-18T15:50:45+0100");
    assertThat(date.getMinutes(), is(50));
  }

  @Test(expected = SonarException.class)
  public void shouldNotParseDateTime() {
    DateUtils.parseDate("2010/05/18 10:55");
  }

  @Test
  public void shouldFormatDate() {
    assertThat(DateUtils.formatDate(new Date()), startsWith("20"));
    assertThat(DateUtils.formatDate(new Date()).length(), is(10));
  }

  @Test
  public void shouldFormatDateTime() {
    assertThat(DateUtils.formatDateTime(new Date()), startsWith("20"));
    assertThat(DateUtils.formatDateTime(new Date()).length(), greaterThan(20));
  }
}
