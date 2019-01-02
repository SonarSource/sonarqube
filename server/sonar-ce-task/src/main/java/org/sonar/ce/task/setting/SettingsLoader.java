/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.ce.task.setting;

import org.picocontainer.Startable;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.ce.task.container.EagerStart;
import org.sonar.ce.task.container.TaskContainerImpl;
import org.sonar.server.setting.ThreadLocalSettings;

/**
 * Add this class as the first components in the {@link TaskContainerImpl}
 * to trigger loading of Thread local specific {@link org.sonar.api.config.Settings} in {@link ThreadLocalSettings}.
 */
@EagerStart
@ComputeEngineSide
public class SettingsLoader implements Startable {
  private final ThreadLocalSettings threadLocalSettings;

  public SettingsLoader(ThreadLocalSettings threadLocalSettings) {
    this.threadLocalSettings = threadLocalSettings;
  }

  @Override
  public void start() {
    threadLocalSettings.load();
  }

  @Override
  public void stop() {
    threadLocalSettings.unload();
  }
}
