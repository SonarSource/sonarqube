/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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

package org.sonar.server.db.migrations.debt;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.config.Settings;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;

public class DebtConvertorTest {

  DebtConvertor convertor;

  Settings settings = new Settings();

  @Before
  public void setUp() throws Exception {
    convertor = new DebtConvertor(settings);
  }

  @Test
  public void convert_fromn_long() throws Exception {
    settings.setProperty(DebtConvertor.HOURS_IN_DAY_PROPERTY, 8);

    assertThat(convertor.createFromLong(1)).isEqualTo(60);
    assertThat(convertor.createFromLong(100)).isEqualTo(3600);
    assertThat(convertor.createFromLong(10000)).isEqualTo(28800);
    assertThat(convertor.createFromLong(10101)).isEqualTo(32460);
  }

  @Test
  public void convert_fromn_long_use_default_value_for_hours_in_day_when_no_property() throws Exception {
    assertThat(convertor.createFromLong(1)).isEqualTo(60);
  }

  @Test
  public void fail_convert_fromn_long_on_bad_hours_in_day_property() throws Exception {
    try {
      settings.setProperty(DebtConvertor.HOURS_IN_DAY_PROPERTY, -2);
      convertor.createFromLong(1);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalArgumentException.class);
    }
  }

  @Test
  public void convert_from_days() throws Exception {
    settings.setProperty(DebtConvertor.HOURS_IN_DAY_PROPERTY, 8);

    assertThat(convertor.createFromDays(1.0)).isEqualTo(28800);
    assertThat(convertor.createFromDays(0.1)).isEqualTo(2880);

    // Should be 1.88 but as it's a long it's truncated after comma
    assertThat(convertor.createFromDays(0.0001)).isEqualTo(2);
  }

}
