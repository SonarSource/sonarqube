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
package org.sonar.api.utils;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.Date;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.utils.DateUtils.parseDate;
import static org.sonar.api.utils.DateUtils.parseDateOrDateTime;
import static org.sonar.api.utils.DateUtils.parseDateTime;
import static org.sonar.api.utils.DateUtils.parseEndingDateOrDateTime;
import static org.sonar.api.utils.DateUtils.parseStartingDateOrDateTime;

@RunWith(DataProviderRunner.class)
public class DateUtilsTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void parseDate_valid_format() {
    Date date = DateUtils.parseDate("2010-05-18");
    assertThat(date.getDate()).isEqualTo(18);
  }

  @Test
  public void parseDate_not_valid_format() {
    expectedException.expect(MessageException.class);
    DateUtils.parseDate("2010/05/18");
  }

  @Test
  public void parseDate_not_lenient() {
    expectedException.expect(MessageException.class);
    DateUtils.parseDate("2010-13-18");
  }

  @Test
  public void parseDateQuietly() {
    assertThat(DateUtils.parseDateQuietly("2010/05/18")).isNull();
    Date date = DateUtils.parseDateQuietly("2010-05-18");
    assertThat(date.getDate()).isEqualTo(18);
  }

  @Test
  public void parseDate_fail_if_additional_characters() {
    expectedException.expect(MessageException.class);
    DateUtils.parseDate("1986-12-04foo");
  }

  @Test
  public void parseDateTime_valid_format() {
    Date date = DateUtils.parseDateTime("2010-05-18T15:50:45+0100");
    assertThat(date.getMinutes()).isEqualTo(50);
  }

  @Test
  public void parseDateTime_not_valid_format() {
    expectedException.expect(MessageException.class);
    DateUtils.parseDate("2010/05/18 10:55");
  }

  @Test
  public void parseDateTime_fail_if_additional_characters() {
    expectedException.expect(MessageException.class);
    DateUtils.parseDateTime("1986-12-04T01:02:03+0300foo");
  }

  @Test
  public void parseDateTimeQuietly() {
    assertThat(DateUtils.parseDateTimeQuietly("2010/05/18 10:55")).isNull();
    Date date = DateUtils.parseDateTimeQuietly("2010-05-18T15:50:45+0100");
    assertThat(date.getMinutes()).isEqualTo(50);
  }

  @Test
  public void shouldFormatDate() {
    assertThat(DateUtils.formatDate(new Date())).startsWith("20");
    assertThat(DateUtils.formatDate(new Date())).hasSize(10);
  }

  @Test
  public void shouldFormatDateTime() {
    assertThat(DateUtils.formatDateTime(new Date())).startsWith("20");
    assertThat(DateUtils.formatDateTime(new Date()).length()).isGreaterThan(20);
  }

  @Test
  public void shouldFormatDateTime_with_long() {
    assertThat(DateUtils.formatDateTime(System.currentTimeMillis())).startsWith("20");
    assertThat(DateUtils.formatDateTime(System.currentTimeMillis()).length()).isGreaterThan(20);
  }

  @Test
  public void format_date_time_null_safe() {
    assertThat(DateUtils.formatDateTimeNullSafe(new Date())).startsWith("20");
    assertThat(DateUtils.formatDateTimeNullSafe(new Date()).length()).isGreaterThan(20);
    assertThat(DateUtils.formatDateTimeNullSafe(null)).isEmpty();
  }

  @Test
  public void long_to_date() {
    Date date = new Date();
    assertThat(DateUtils.longToDate(date.getTime())).isEqualTo(date);
    assertThat(DateUtils.longToDate(null)).isNull();
  }

  @Test
  public void date_to_long() {
    Date date = new Date();
    assertThat(DateUtils.dateToLong(date)).isEqualTo(date.getTime());
    assertThat(DateUtils.dateToLong(null)).isEqualTo(null);
  }

  @DataProvider
  public static Object[][] date_times() {
    return new Object[][] {
      {"2014-05-27", parseDate("2014-05-27")},
      {"2014-05-27T15:50:45+0100", parseDateTime("2014-05-27T15:50:45+0100")},
      {null, null}
    };
  }

  @Test
  @UseDataProvider("date_times")
  public void param_as__date_time(String stringDate, Date expectedDate) {
    assertThat(parseDateOrDateTime(stringDate)).isEqualTo(expectedDate);
    assertThat(parseStartingDateOrDateTime(stringDate)).isEqualTo(expectedDate);
  }

  @DataProvider
  public static Object[][] ending_date_times() {
    return new Object[][] {
      {"2014-05-27", parseDate("2014-05-28")},
      {"2014-05-27T15:50:45+0100", parseDateTime("2014-05-27T15:50:45+0100")},
      {null, null}
    };
  }

  @Test
  @UseDataProvider("ending_date_times")
  public void param_as_ending_date_time(String stringDate, Date expectedDate) {
    assertThat(parseEndingDateOrDateTime(stringDate)).isEqualTo(expectedDate);
  }

  @Test
  public void fail_when_param_as_date_or_datetime_not_a_datetime() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Date 'polop' cannot be parsed as either a date or date+time");

    parseDateOrDateTime("polop");
  }

  @Test
  public void fail_when_param_as_starting_datetime_not_a_datetime() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Date 'polop' cannot be parsed as either a date or date+time");

    parseStartingDateOrDateTime("polop");
  }

  @Test
  public void fail_when_param_as_ending_datetime_not_a_datetime() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("'polop' cannot be parsed as either a date or date+time");

    parseEndingDateOrDateTime("polop");
  }

}
