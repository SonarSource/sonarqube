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
package org.sonar.plugins.core.technicaldebt;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class WorkUnitConverterTest {

  @Test
  public void shouldUseDefaultConfiguration() {
    WorkUnitConverter converter = new WorkUnitConverter(new PropertiesConfiguration());
    assertThat(converter.getHoursInDay(), is(WorkUnitConverter.DEFAULT_HOURS_IN_DAY));
  }

  @Test
  public void shouldConvertValueToDays() {
    PropertiesConfiguration configuration = new PropertiesConfiguration();
    configuration.setProperty(WorkUnitConverter.PROPERTY_HOURS_IN_DAY, "12");
    WorkUnitConverter converter = new WorkUnitConverter(configuration);

    assertThat(converter.toDays(WorkUnit.create(6.0, WorkUnit.DAYS)), is(6.0));
    assertThat(converter.toDays(WorkUnit.create(6.0, WorkUnit.HOURS)), is(6.0 / 12.0));
    assertThat(converter.toDays(WorkUnit.create(60.0 , WorkUnit.MINUTES)), is(1.0/12.0));
  }

}
