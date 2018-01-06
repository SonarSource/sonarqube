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
import org.sonar.api.Properties;
import org.sonar.api.Property;
import org.sonar.api.batch.Sensor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.config.Settings;
import org.sonar.api.task.TaskExtension;
import org.sonar.api.resources.Project;

@Properties({
    @Property(
        key = "accessSecuredFromTask",
        name = "Property to decide if task extension should access secured properties",
        defaultValue = "false")
})
public class AccessSecuredPropsTaskExtension implements TaskExtension {

  private Settings settings;

  public AccessSecuredPropsTaskExtension(Settings settings) {
    this.settings = settings;
  }

  public void start() {
    if ("true".equals(settings.getString("accessSecuredFromTask"))) {
      settings.getString("foo.bar.secured");
    }
  }
}
