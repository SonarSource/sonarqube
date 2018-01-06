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
package com.sonarsource;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import org.sonar.api.Properties;
import org.sonar.api.Property;
import org.sonar.api.PropertyType;
import org.sonar.api.batch.Initializer;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.Project;

@Properties({
  @Property(
    key = DumpSettingsInitializer.SONAR_SHOW_SETTINGS,
    type = PropertyType.STRING,
    name = "Property to decide if it should output settings",
    multiValues = true,
    defaultValue = "")
})
public class DumpSettingsInitializer extends Initializer {

  public static final String SONAR_SHOW_SETTINGS = "sonar.showSettings";
  private Settings settings;

  public DumpSettingsInitializer(Settings settings) {
    this.settings = settings;
  }

  @Override
  public boolean shouldExecuteOnProject(Project project) {
    return true;
  }

  @Override
  public void execute(Project project) {
    Set<String> settingsToDump = new HashSet<>(Arrays.asList(settings.getStringArray(SONAR_SHOW_SETTINGS)));
    if (!settingsToDump.isEmpty()) {
      TreeMap<String, String> treemap = new TreeMap<String, String>(settings.getProperties());
      for (Entry<String, String> prop : treemap.entrySet()) {
        if (settingsToDump.contains(prop.getKey())) {
          System.out.println("  o " + project.getKey() + ":" + prop.getKey() + " = " + prop.getValue());
        }
      }
    }
  }
}
