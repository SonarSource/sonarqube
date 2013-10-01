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

import org.junit.Test;
import org.sonar.api.config.Settings;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class WorkUnitConverterTest {

  @Test
  public void concert_value_to_days() {
    Settings settings = new Settings();
    settings.setProperty(WorkUnitConverter.PROPERTY_HOURS_IN_DAY, "12");

    WorkUnitConverter converter = new WorkUnitConverter(settings);

    assertThat(converter.toDays(WorkUnit.create(6.0, WorkUnit.DAYS)), is(6.0));
    assertThat(converter.toDays(WorkUnit.create(6.0, WorkUnit.HOURS)), is(6.0 / 12.0));
    assertThat(converter.toDays(WorkUnit.create(60.0, WorkUnit.MINUTES)), is(1.0 / 12.0));
  }

}
