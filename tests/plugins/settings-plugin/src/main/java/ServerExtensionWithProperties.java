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
import org.sonar.api.ServerExtension;
import org.sonar.api.config.Settings;

@Properties({
    @Property(key = "settings.extension.hidden", name = "Hidden Property", description = "Hidden Property defined on extension but not plugin", global = false, project = false, module = false, defaultValue = "teahupoo"),
    @Property(key = "settings.extension.global", name = "Global Property", global = true, project = false, module = false)
})
public final class ServerExtensionWithProperties implements ServerExtension {

  private Settings settings;

  public ServerExtensionWithProperties(Settings settings) {
    this.settings = settings;
  }

  public void start() {
    System.out.println("Test that the default value of properties are automatically injected by the component Settings");
    if (!"teahupoo".equals(settings.getString("settings.extension.hidden"))) {
      throw new IllegalStateException("The property settings.extension.hidden is not registered");
    }
  }
}
