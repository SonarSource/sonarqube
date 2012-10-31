/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.batch.bootstrap;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.config.Settings;

import static org.fest.assertions.Assertions.assertThat;

public class DryRunTest {
  DryRun dryRun;
  Settings settings;

  @Before
  public void setUp() {
    settings = new Settings(new PropertyDefinitions(DryRun.class));

    dryRun = new DryRun(settings);
  }

  @Test
  public void should_be_disabled() {
    dryRun.start();

    assertThat(dryRun.isEnabled()).isFalse();
  }

  @Test
  public void should_enable() {
    settings.setProperty("sonar.dryRun", "true");

    dryRun.start();

    assertThat(dryRun.isEnabled()).isTrue();
  }

  @Test
  public void should_get_default_export_path() {
    String exportPath = dryRun.getExportPath();

    assertThat(exportPath).isEqualTo("dryRun.json");
  }

  @Test
  public void should_get_custom_export_path() {
    settings.setProperty("sonar.dryRun.export.path", "export.json");

    String exportPath = dryRun.getExportPath();

    assertThat(exportPath).isEqualTo("export.json");
  }
}
