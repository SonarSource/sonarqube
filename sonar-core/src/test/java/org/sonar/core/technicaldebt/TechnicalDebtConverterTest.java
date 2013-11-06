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
package org.sonar.core.technicaldebt;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.config.Settings;
import org.sonar.api.issue.internal.WorkDayDuration;

import static org.fest.assertions.Assertions.assertThat;


public class TechnicalDebtConverterTest {

  private TechnicalDebtConverter converter;

  @Before
  public void before(){
    Settings settings = new Settings();
    settings.setProperty(TechnicalDebtConverter.PROPERTY_HOURS_IN_DAY, "12");

    converter = new TechnicalDebtConverter(settings);
  }

  @Test
  public void convert_work_unit_to_days() {
    assertThat(converter.toDays(WorkUnit.create(6.0, WorkUnit.DAYS))).isEqualTo(6.0);
    assertThat(converter.toDays(WorkUnit.create(6.0, WorkUnit.HOURS))).isEqualTo(6.0 / 12.0);
    assertThat(converter.toDays(WorkUnit.create(60.0, WorkUnit.MINUTES))).isEqualTo(1.0 / 12.0);
  }

  @Test
  public void concert_work_unit_to_minutes() {
    assertThat(converter.toMinutes(WorkUnit.create(2.0, WorkUnit.DAYS))).isEqualTo(2 * 12 * 60L);
    assertThat(converter.toMinutes(WorkUnit.create(6.0, WorkUnit.HOURS))).isEqualTo(6 * 60L);
    assertThat(converter.toMinutes(WorkUnit.create(60.0, WorkUnit.MINUTES))).isEqualTo(60L);
  }

  @Test
  public void convert_simple_values() {
    checkValues(converter.fromMinutes(15L), 15, 0, 0);
    checkValues(converter.fromMinutes(120L), 0, 2, 0);
    checkValues(converter.fromMinutes(720L), 0, 0, 1);
  }

  @Test
  public void convert_complex_values() {
    checkValues(converter.fromMinutes(70L), 10, 1, 0);
    checkValues(converter.fromMinutes(730L), 10, 0, 1);
    checkValues(converter.fromMinutes(790L), 10, 1, 1);
  }

  @Test
  public void convert_technical_debt_to_days() {
    assertThat(converter.toDays(WorkDayDuration.of(0, 0, 6))).isEqualTo(6.0);
    assertThat(converter.toDays(WorkDayDuration.of(0, 6, 0))).isEqualTo(0.5);
    assertThat(converter.toDays(WorkDayDuration.of(360, 0, 0))).isEqualTo(0.5);
    assertThat(converter.toDays(WorkDayDuration.of(45, 0, 0))).isEqualTo(0.0625);

    assertThat(converter.toDays(WorkDayDuration.of(45, 6, 1))).isEqualTo(1.5625);
  }
  
  private void checkValues(WorkDayDuration technicalDebt, int expectedMinutes, int expectedHours, int expectedDays) {
    assertThat(technicalDebt.minutes()).isEqualTo(expectedMinutes);
    assertThat(technicalDebt.hours()).isEqualTo(expectedHours);
    assertThat(technicalDebt.days()).isEqualTo(expectedDays);
  }

}
