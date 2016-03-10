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
package org.sonar.db.profiling;

import org.junit.Test;

import static org.apache.commons.lang.StringUtils.repeat;
import static org.assertj.core.api.Assertions.assertThat;

public class SqlLogFormatterTest {

  SqlLogFormatter underTest = SqlLogFormatter.FORMATTER;

  @Test
  public void formatSql() {
    assertThat(underTest.formatSql("select *")).isEqualTo("select *");
  }

  @Test
  public void formatSql_removes_newlines() {
    assertThat(underTest.formatSql("select *\nfrom issues")).isEqualTo("select * from issues");
  }

  @Test
  public void formatParam() {
    assertThat(underTest.formatParam(null)).isEqualTo("[null]");
    assertThat(underTest.formatParam("")).isEqualTo("");
    assertThat(underTest.formatParam("foo")).isEqualTo("foo");
  }

  @Test
  public void formatParam_escapes_newlines() {
    assertThat(underTest.formatParam("foo\nbar")).isEqualTo("foo\\nbar");
  }

  @Test
  public void formatParam_truncates_if_too_long() {
    String param = repeat("a", SqlLogFormatter.PARAM_MAX_WIDTH + 10);
    String formattedParam = underTest.formatParam(param);
    assertThat(formattedParam)
      .hasSize(SqlLogFormatter.PARAM_MAX_WIDTH)
      .endsWith("...")
      .startsWith(repeat("a", SqlLogFormatter.PARAM_MAX_WIDTH - 3));
  }

  @Test
  public void formatParams() {
    String formattedParams = underTest.formatParams(new Object[] {"foo", 42, null, true});
    assertThat(formattedParams).isEqualTo("foo, 42, [null], true");
  }

  @Test
  public void formatParams_returns_blank_if_zero_params() {
    String formattedParams = underTest.formatParams(new Object[0]);
    assertThat(formattedParams).isEqualTo("");
  }
}
