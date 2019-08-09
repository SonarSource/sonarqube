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
package org.sonar.server.notification.ws;

import java.util.List;
import org.sonar.api.Startable;

import static org.sonar.core.util.stream.MoreCollectors.toList;
import static org.sonar.server.notification.NotificationDispatcherMetadata.GLOBAL_NOTIFICATION;
import static org.sonar.server.notification.NotificationDispatcherMetadata.PER_PROJECT_NOTIFICATION;

public class DispatchersImpl implements Dispatchers, Startable {

  private final NotificationCenter notificationCenter;

  private List<String> projectDispatchers;
  private List<String> globalDispatchers;

  public DispatchersImpl(NotificationCenter notificationCenter) {
    this.notificationCenter = notificationCenter;
  }

  @Override
  public List<String> getGlobalDispatchers() {
    return globalDispatchers;
  }

  @Override
  public List<String> getProjectDispatchers() {
    return projectDispatchers;
  }

  @Override
  public void start() {
    this.globalDispatchers = notificationCenter.getDispatcherKeysForProperty(GLOBAL_NOTIFICATION, "true")
      .stream()
      .sorted()
      .collect(toList());
    this.projectDispatchers = notificationCenter.getDispatcherKeysForProperty(PER_PROJECT_NOTIFICATION, "true")
      .stream()
      .sorted()
      .collect(toList());
  }

  @Override
  public void stop() {
    // nothing to do
  }
}
