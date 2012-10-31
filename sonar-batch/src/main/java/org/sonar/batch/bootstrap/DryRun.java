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

import org.slf4j.LoggerFactory;
import org.sonar.api.BatchComponent;
import org.sonar.api.Properties;
import org.sonar.api.Property;
import org.sonar.api.PropertyType;
import org.sonar.api.config.Settings;

@Properties({
  @Property(key = "sonar.dryRun", defaultValue = "false", name = "Dry Run", type = PropertyType.BOOLEAN),
  @Property(key = "sonar.dryRun.export.path", defaultValue = "dryRun.json", name = "Dry Run Results Export File", type = PropertyType.STRING)
})
public class DryRun implements BatchComponent {
  private final Settings settings;

  public DryRun(Settings settings) {
    this.settings = settings;
  }

  public boolean isEnabled() {
    return settings.getBoolean("sonar.dryRun");
  }

  public String getExportPath() {
    return settings.getString("sonar.dryRun.export.path");
  }

  public void start() {
    if (isEnabled()) {
      LoggerFactory.getLogger(DryRun.class).info("Dry run");
    }
  }
}
