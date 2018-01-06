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
import org.sonar.api.Startable;
import org.sonar.api.config.Settings;
import org.sonar.api.server.ServerSide;

@ServerSide
public class StartupCrash implements Startable {

  private final Settings settings;

  public StartupCrash(Settings settings) {
    this.settings = settings;
  }

  @Override
  public void start() {
    if (settings.getBoolean("failAtStartup")) {
      throw new IllegalStateException("Error in plugin [server]");
    }
  }

  @Override
  public void stop() {
    // do nothing
  }
}
