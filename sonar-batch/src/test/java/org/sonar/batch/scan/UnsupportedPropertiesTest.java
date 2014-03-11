/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
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
package org.sonar.batch.scan;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.config.Settings;
import org.sonar.api.utils.MessageException;

public class UnsupportedPropertiesTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void should_fail_if_sonar_light_is_set() {
    thrown.expect(MessageException.class);
    thrown.expectMessage("The property 'sonar.light' is no longer supported. Please use 'sonar.dynamicAnalysis'");

    Settings settings = new Settings();
    settings.setProperty("sonar.light", true);
    new UnsupportedProperties(settings).start();
  }

  @Test
  public void should_not_fail_if_sonar_light_is_not_set() {
    Settings settings = new Settings();
    new UnsupportedProperties(settings).start();
  }
}
