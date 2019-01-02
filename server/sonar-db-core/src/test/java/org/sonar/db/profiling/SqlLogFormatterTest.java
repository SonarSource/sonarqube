/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.db.profiling;

import org.junit.Test;

import static org.apache.commons.lang.StringUtils.repeat;
import static org.assertj.core.api.Assertions.assertThat;

public class SqlLogFormatterTest {

  @Test
  public void reformatSql() {
    assertThat(SqlLogFormatter.reformatSql("")).isEqualTo("");
    assertThat(SqlLogFormatter.reformatSql("select *")).isEqualTo("select *");
    assertThat(SqlLogFormatter.reformatSql("select *\nfrom issues")).isEqualTo("select * from issues");
    assertThat(SqlLogFormatter.reformatSql("select *\n from issues")).isEqualTo("select * from issues");
    assertThat(SqlLogFormatter.reformatSql("select *\n   from issues")).isEqualTo("select * from issues");
    assertThat(SqlLogFormatter.reformatSql("select    *\n   from    issues")).isEqualTo("select * from issues");
    assertThat(SqlLogFormatter.reformatSql("select    *\n\t\t  from  \tissues")).isEqualTo("select * from issues");
  }

  @Test
  public void reformatParam() {
    assertThat(SqlLogFormatter.reformatParam(null)).isEqualTo("[null]");
    assertThat(SqlLogFormatter.reformatParam("")).isEqualTo("");
    assertThat(SqlLogFormatter.reformatParam("foo")).isEqualTo("foo");
    assertThat(SqlLogFormatter.reformatParam("foo  bar ")).isEqualTo("foo  bar ");
  }

  @Test
  public void reformatParam_escapes_newlines() {
    assertThat(SqlLogFormatter.reformatParam("foo\n  bar\nbaz")).isEqualTo("foo\\n  bar\\nbaz");
  }

  @Test
  public void reformatParam_truncates_if_too_long() {
    String param = repeat("a", SqlLogFormatter.PARAM_MAX_WIDTH + 10);
    String formattedParam = SqlLogFormatter.reformatParam(param);
    assertThat(formattedParam)
      .hasSize(SqlLogFormatter.PARAM_MAX_WIDTH)
      .endsWith("...")
      .startsWith(repeat("a", SqlLogFormatter.PARAM_MAX_WIDTH - 3));
  }

  @Test
  public void reformatParams() {
    String formattedParams = SqlLogFormatter.reformatParams(new Object[] {"foo", 42, null, true});
    assertThat(formattedParams).isEqualTo("foo, 42, [null], true");
  }

  @Test
  public void reformatParams_returns_blank_if_zero_params() {
    String formattedParams = SqlLogFormatter.reformatParams(new Object[0]);
    assertThat(formattedParams).isEqualTo("");
  }

  @Test
  public void countArguments() {
    assertThat(SqlLogFormatter.countArguments("select * from issues")).isEqualTo(0);
    assertThat(SqlLogFormatter.countArguments("select * from issues where id=?")).isEqualTo(1);
    assertThat(SqlLogFormatter.countArguments("select * from issues where id=? and kee=?")).isEqualTo(2);
  }
}
