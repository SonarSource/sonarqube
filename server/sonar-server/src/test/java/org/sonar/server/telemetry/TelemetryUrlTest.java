/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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

package org.sonar.server.telemetry;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.config.internal.MapSettings;

import static org.assertj.core.api.Assertions.assertThat;

public class TelemetryUrlTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private MapSettings settings = new MapSettings();

  private TelemetryUrl underTest;

  @Test
  public void return_url_as_is_when_no_ending_slash() {
    settings.setProperty("sonar.telemetry.url", "http://localhost:9001");
    underTest = new TelemetryUrl(settings.asConfig());

    assertThat(underTest.get()).isEqualTo("http://localhost:9001");
  }

  @Test
  public void fail_when_no_settings_to_define_muppet_url() {
    underTest = new TelemetryUrl(settings.asConfig());

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Setting 'sonar.telemetry.url' must be provided.");

    underTest.get();
  }
}
