/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Date;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.sonar.api.utils.DateUtils.parseDateOrDateTime;
import static org.sonar.api.utils.DateUtils.parseEndingDateOrDateTime;
import static org.sonar.api.utils.DateUtils.parseStartingDateOrDateTime;

@RunWith(DataProviderRunner.class)
public class DateUtilsTest {

  @Test
  public void parseDate_valid_format() {
    Date date = DateUtils.parseDate("2010-05-18");
    assertThat(date.getDate()).isEqualTo(18);
  }

  @Test
  public void parseDate_not_valid_format() {
    assertThatThrownBy(() -> DateUtils.parseDate("2010/05/18"))
      .isInstanceOf(MessageException.class);
  }

  @Test
  public void parseDate_not_lenient() {
    assertThatThrownBy(() -> DateUtils.parseDate("2010-13-18"))
      .isInstanceOf(MessageException.class);
  }

  @Test
  public void parseDateQuietly() {
    assertThat(DateUtils.parseDateQuietly("2010/05/18")).isNull();
    Date date = DateUtils.parseDateQuietly("2010-05-18");
    assertThat(date.getDate()).isEqualTo(18);
  }

  @Test
  public void parseDate_fail_if_additional_characters() {
    assertThatThrownBy(() -> DateUtils.parseDate("1986-12-04foo"))
      .isInstanceOf(MessageException.class);
  }

  @Test
  public void parseDateTime_valid_format() {
    Date date = DateUtils.parseDateTime("2010-05-18T15:50:45+0100");
    assertThat(date.getMinutes()).isEqualTo(50);
  }

  @Test
  public void parseDateTime_not_valid_format() {
    assertThatThrownBy(() -> DateUtils.parseDate("2010/05/18 10:55"))
      .isInstanceOf(MessageException.class);
  }

  @Test
  public void parseDateTime_fail_if_additional_characters() {
    assertThatThrownBy(() -> DateUtils.parseDate("1986-12-04T01:02:03+0300foo"))
      .isInstanceOf(MessageException.class);
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
    assertThat(DateUtils.dateToLong(null)).isNull();
  }

  @DataProvider
  public static Object[][] date_times() {
    return new Object[][]{
      {"2014-05-27", Date.from(LocalDate.parse("2014-05-27").atStartOfDay(ZoneId.systemDefault()).toInstant())},
      {"2014-05-27T15:50:45+0100", Date.from(OffsetDateTime.parse("2014-05-27T15:50:45+01:00").toInstant())},
      {null, null}
    };
  }

  @Test
  @UseDataProvider("date_times")
  public void param_as__date_time(String stringDate, Date expectedDate) {
    assertThat(parseDateOrDateTime(stringDate)).isEqualTo(expectedDate);
    assertThat(parseStartingDateOrDateTime(stringDate)).isEqualTo(expectedDate);
  }

  @Test
  public void param_as__date_time_provided_timezone() {
    final ZoneId zoneId = ZoneId.of("Europe/Moscow");
    assertThat(parseDateOrDateTime("2020-05-27", zoneId)).isEqualTo(Date.from(OffsetDateTime.parse("2020-05-27T00:00:00+03:00").toInstant()));
    assertThat(parseStartingDateOrDateTime("2020-05-27", zoneId)).isEqualTo(Date.from(OffsetDateTime.parse("2020-05-27T00:00:00+03:00").toInstant()));
  }

  @Test
  public void param_as_ending_date_time_default_timezone() {
    assertThat(parseEndingDateOrDateTime("2014-05-27")).isEqualTo(Date.from(LocalDate.parse("2014-05-28").atStartOfDay(ZoneId.systemDefault()).toInstant()));
    assertThat(parseEndingDateOrDateTime("2014-05-27T15:50:45+0100")).isEqualTo(Date.from(OffsetDateTime.parse("2014-05-27T15:50:45+01:00").toInstant()));
    assertThat(parseEndingDateOrDateTime(null)).isNull();
  }

  @Test
  public void param_as_ending_date_time_provided_timezone() {
    final ZoneId zoneId = ZoneId.of("Europe/Moscow");
    assertThat(parseEndingDateOrDateTime("2020-05-27", zoneId)).isEqualTo(Date.from(OffsetDateTime.parse("2020-05-28T00:00:00+03:00").toInstant()));
    assertThat(parseEndingDateOrDateTime("2014-05-27T15:50:45+0100", zoneId)).isEqualTo(Date.from(OffsetDateTime.parse("2014-05-27T15:50:45+01:00").toInstant()));
    assertThat(parseEndingDateOrDateTime(null, zoneId)).isNull();
  }

  @Test
  public void fail_when_param_as_date_or_datetime_not_a_datetime() {
    assertThatThrownBy(() -> parseDateOrDateTime("polop"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Date 'polop' cannot be parsed as either a date or date+time");
  }

  @Test
  public void fail_when_param_as_starting_datetime_not_a_datetime() {
    assertThatThrownBy(() -> parseStartingDateOrDateTime("polop"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Date 'polop' cannot be parsed as either a date or date+time");
  }

  @Test
  public void fail_when_param_as_ending_datetime_not_a_datetime() {
    assertThatThrownBy(() -> parseEndingDateOrDateTime("polop"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("'polop' cannot be parsed as either a date or date+time");
  }

}
