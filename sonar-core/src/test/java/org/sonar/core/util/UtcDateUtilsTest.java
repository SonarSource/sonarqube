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
package org.sonar.core.util;

import java.util.Date;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class UtcDateUtilsTest {

  @Test
  public void parse_then_format() {
    Date date = UtcDateUtils.parseDateTime("2014-01-14T14:00:00+0200");
    assertThat(UtcDateUtils.formatDateTime(date)).isEqualTo("2014-01-14T12:00:00+0000");
  }

  @Test
  public void fail_if_bad_format() {
    try {
      UtcDateUtils.parseDateTime("2014-01-14");
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Fail to parse date: 2014-01-14");
    }
  }
}
