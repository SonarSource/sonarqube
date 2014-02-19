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

package org.sonar.api.utils;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.Settings;

import static org.fest.assertions.Assertions.assertThat;

public class WorkDurationFactoryTest {

  WorkDurationFactory factory;

  @Before
  public void setUp() throws Exception {
    Settings settings = new Settings();
    settings.setProperty(CoreProperties.HOURS_IN_DAY, 8);
    factory = new WorkDurationFactory(settings);
  }

  @Test
  public void create_from_working_value() throws Exception {
    // 1 working day -> 8 hours
    assertThat(factory.createFromWorkingValue(1, WorkDuration.UNIT.DAYS).toSeconds()).isEqualTo(8 * 60 * 60);
    // 8 hours
    assertThat(factory.createFromWorkingValue(8, WorkDuration.UNIT.HOURS).toSeconds()).isEqualTo(8 * 60 * 60);
  }

  @Test
  public void create_from_working_long() throws Exception {
    WorkDuration workDuration = factory.createFromWorkingLong(1l);
    assertThat(workDuration.days()).isEqualTo(0);
    assertThat(workDuration.hours()).isEqualTo(0);
    assertThat(workDuration.minutes()).isEqualTo(1);
  }

  @Test
  public void create_from_seconds() throws Exception {
    WorkDuration workDuration = factory.createFromSeconds(8 * 60 * 60L);
    assertThat(workDuration.days()).isEqualTo(1);
    assertThat(workDuration.hours()).isEqualTo(0);
    assertThat(workDuration.minutes()).isEqualTo(0);
  }
}
